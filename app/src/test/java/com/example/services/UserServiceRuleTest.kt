package com.example.services

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserServiceRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun completeOnboarding_validUniqueUsername_createsUserAndUsernameClaimAtomically() = runBlocking {
    val uid = signInTestUser("alice_${UUID.randomUUID().toString().take(6)}@test.com")
    val userService = FirebaseUserService(firestore, auth)
    val uniqueHandle = "alice_${UUID.randomUUID().toString().replace("-", "").take(10).lowercase()}"

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      userService.completeOnboardingWithUniqueUsername(
        uid = uid,
        email = "alice@test.com",
        photoUrl = null,
        displayName = "Alice Rahman",
        rawUsername = uniqueHandle,
        bio = "হ্যালো বাংলাদেশ",
        language = "bn",
        themeMode = "system"
      )
    }

    assertTrue("Expected onboarding to succeed, got: ${result.exceptionOrNull()}", result.isSuccess)
    val profile = result.getOrThrow()
    assertEquals(uid, profile.uid)
    assertEquals(uniqueHandle, profile.username)
    assertTrue(profile.onboardingCompleted)

    val observed = withTimeout(FLOW_TIMEOUT_MS) {
      userService.observeMyProfile(uid).first { it != null && it.username == uniqueHandle }
    }
    assertNotNull(observed)
    assertEquals("Alice Rahman", observed?.displayName)
  }

  @Test
  fun claimUsername_withAnotherUsersUid_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser("alice_${UUID.randomUUID().toString().take(6)}@test.com")
    val spoofedHandle = "spoof_${UUID.randomUUID().toString().replace("-", "").take(10).lowercase()}"

    var caught: Throwable? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        val batch = firestore.batch()
        val usernameRef = firestore.collection("usernames").document(spoofedHandle)
        val userRef = firestore.collection("users").document(aliceUid)

        // Alice tries to claim username with someone else's UID ("other_user_uid_999")
        batch.set(
          usernameRef,
          mapOf(
            "uid" to "other_user_uid_999",
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
        batch.set(
          userRef,
          mapOf(
            "uid" to aliceUid,
            "displayName" to "Alice",
            "username" to spoofedHandle,
            "email" to "alice@test.com",
            "language" to "bn",
            "themeMode" to "system",
            "onboardingCompleted" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        batch.commit().await()
      }
    } catch (e: Throwable) {
      caught = e
    }
    assertNotNull("Expected claiming username with another user's UID to fail", caught)
    val firestoreEx = caught?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
  }

  @Test
  fun completeOnboarding_overwritingExistingTakenUsername_failsAndPreservesOriginalClaim() = runBlocking {
    val sharedHandle = "taken_${UUID.randomUUID().toString().replace("-", "").take(10).lowercase()}"

    val aliceUid = signInTestUser("alice_${UUID.randomUUID().toString().take(6)}@test.com")
    val aliceService = FirebaseUserService(firestore, auth)
    val firstClaim = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceService.completeOnboardingWithUniqueUsername(
        uid = aliceUid,
        email = "alice@test.com",
        photoUrl = null,
        displayName = "Alice",
        rawUsername = sharedHandle,
        bio = "",
        language = "bn",
        themeMode = "light"
      )
    }
    assertTrue(firstClaim.isSuccess)

    // Bob signs in and tries both service transaction and direct batch overwrite
    val bobUid = signInTestUser("bob_${UUID.randomUUID().toString().take(6)}@test.com")
    val bobService = FirebaseUserService(firestore, auth)
    val availableCheck = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobService.checkUsernameAvailability(sharedHandle, bobUid)
    }
    assertTrue(availableCheck.isSuccess)
    assertFalse("Username should report unavailable for Bob", availableCheck.getOrThrow())

    // 1. Via UserService transaction
    val secondClaim = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobService.completeOnboardingWithUniqueUsername(
        uid = bobUid,
        email = "bob@test.com",
        photoUrl = null,
        displayName = "Bob",
        rawUsername = sharedHandle,
        bio = "",
        language = "en",
        themeMode = "dark"
      )
    }
    assertTrue("Expected duplicate username claim via UserService to fail", secondClaim.isFailure)

    // 2. Direct batch overwrite attempt bypassing client check -> rejected by firestore.rules (!exists)
    var directBatchCaught: Throwable? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        val batch = firestore.batch()
        batch.set(
          firestore.collection("usernames").document(sharedHandle),
          mapOf("uid" to bobUid, "createdAt" to FieldValue.serverTimestamp())
        )
        batch.set(
          firestore.collection("users").document(bobUid),
          mapOf(
            "uid" to bobUid,
            "displayName" to "Bob",
            "username" to sharedHandle,
            "email" to "bob@test.com",
            "language" to "en",
            "themeMode" to "dark",
            "onboardingCompleted" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        batch.commit().await()
      }
    } catch (e: Throwable) {
      directBatchCaught = e
    }
    assertNotNull("Expected direct batch overwrite of taken username to fail", directBatchCaught)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      directBatchCaught?.unwrapFirestoreException()?.code
    )
  }

  @Test
  fun crossUserRead_readingAnotherUsersProfile_failsWithPermissionDeniedInPhase1() = runBlocking {
    val aliceUid = signInTestUser("alice_${UUID.randomUUID().toString().take(6)}@test.com")
    val aliceService = FirebaseUserService(firestore, auth)
    val handle = "u_${UUID.randomUUID().toString().replace("-", "").take(10).lowercase()}"

    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceService.completeOnboardingWithUniqueUsername(
        uid = aliceUid,
        email = "alice@test.com",
        photoUrl = null,
        displayName = "Alice",
        rawUsername = handle,
        bio = "",
        language = "bn",
        themeMode = "system"
      ).getOrThrow()
    }

    // Bob signs in and tries to read Alice's user document
    signInTestUser("bob_${UUID.randomUUID().toString().take(6)}@test.com")
    val bobService = FirebaseUserService(firestore, auth)
    val crossRead = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobService.getMyProfile(aliceUid)
    }
    assertTrue(crossRead.isFailure)
    val firestoreEx = crossRead.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(firestoreEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
  }

  @Test
  fun clientSystemFieldEscalation_writingFollowersCountOrIsBanned_rejectedByRules() = runBlocking {
    val uid = signInTestUser("hacker_${UUID.randomUUID().toString().take(6)}@test.com")

    // 1. Attempt writing followersCount on create
    var followersCaught: Throwable? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(uid).set(
          mapOf(
            "uid" to uid,
            "displayName" to "Hacker",
            "username" to "hacker_01",
            "email" to "hacker@test.com",
            "language" to "bn",
            "themeMode" to "dark",
            "onboardingCompleted" to true,
            "followersCount" to 9999L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
    } catch (e: Throwable) {
      followersCaught = e
    }
    assertNotNull(followersCaught)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      followersCaught?.unwrapFirestoreException()?.code
    )

    // 2. Attempt writing isBanned on create
    var isBannedCaught: Throwable? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(uid).set(
          mapOf(
            "uid" to uid,
            "displayName" to "Hacker",
            "username" to "hacker_01",
            "email" to "hacker@test.com",
            "language" to "bn",
            "themeMode" to "dark",
            "onboardingCompleted" to true,
            "isBanned" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
    } catch (e: Throwable) {
      isBannedCaught = e
    }
    assertNotNull(isBannedCaught)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      isBannedCaught?.unwrapFirestoreException()?.code
    )
  }

  @Test
  fun unauthenticatedCaller_observingProfile_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val service = FirebaseUserService(firestore, auth)

    var caughtException: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        service.observeMyProfile("some_user_123").first()
      }
    } catch (e: Throwable) {
      caughtException = e
    }
    assertNotNull("Expected exception when unauthenticated", caughtException)
    val firestoreEx = caughtException?.unwrapFirestoreException()
    assertNotNull("Expected nested FirebaseFirestoreException, got $caughtException", firestoreEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
