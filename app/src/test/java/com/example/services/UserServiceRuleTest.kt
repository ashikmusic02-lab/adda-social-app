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
    val aliceEmail = "alice_${UUID.randomUUID().toString().take(6)}@test.com"
    val uid = signInTestUser(aliceEmail)
    val userService = FirebaseUserService(firestore, auth)
    val uniqueHandle = "alice_${UUID.randomUUID().toString().replace("-", "").take(10).lowercase()}"

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      userService.completeOnboardingWithUniqueUsername(
        uid = uid,
        email = aliceEmail,
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
    assertEquals(aliceEmail, profile.email)
    assertTrue(profile.onboardingCompleted)

    val observed = withTimeout(FLOW_TIMEOUT_MS) {
      userService.observeMyProfile(uid).first { it != null && it.username == uniqueHandle }
    }
    assertNotNull(observed)
    assertEquals("Alice Rahman", observed?.displayName)
  }

  @Test
  fun claimUsername_withAnotherUsersUid_failsWithPermissionDenied() = runBlocking {
    val aliceEmail = "alice_${UUID.randomUUID().toString().take(6)}@test.com"
    val aliceUid = signInTestUser(aliceEmail)
    val spoofedHandle = "spoof_${UUID.randomUUID().toString().replace("-", "").take(10).lowercase()}"

    var caught: Throwable? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        val batch = firestore.batch()
        val usernameRef = firestore.collection("usernames").document(spoofedHandle)
        val userRef = firestore.collection("users").document(aliceUid)

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
            "email" to aliceEmail,
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

    val aliceEmail = "alice_${UUID.randomUUID().toString().take(6)}@test.com"
    val aliceUid = signInTestUser(aliceEmail)
    val aliceService = FirebaseUserService(firestore, auth)
    val firstClaim = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceService.completeOnboardingWithUniqueUsername(
        uid = aliceUid,
        email = aliceEmail,
        photoUrl = null,
        displayName = "Alice",
        rawUsername = sharedHandle,
        bio = "",
        language = "bn",
        themeMode = "light"
      )
    }
    assertTrue(firstClaim.isSuccess)

    val bobEmail = "bob_${UUID.randomUUID().toString().take(6)}@test.com"
    val bobUid = signInTestUser(bobEmail)
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
        email = bobEmail,
        photoUrl = null,
        displayName = "Bob",
        rawUsername = sharedHandle,
        bio = "",
        language = "en",
        themeMode = "dark"
      )
    }
    assertTrue("Expected duplicate username claim via UserService to fail", secondClaim.isFailure)

    // 2. Direct batch overwrite attempt bypassing client check -> rejected by firestore.rules
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
            "email" to bobEmail,
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
  fun caseA_settingSomeoneElsesClaimedUsernameInProfile_failsOnCreateAndUpdate() = runBlocking {
    // (ক) অন্যের ক্লেইম করা নাম প্রোফাইলে বসানো ব্যর্থ
    val aliceEmail = "alice_${UUID.randomUUID().toString().take(6)}@test.com"
    val aliceUid = signInTestUser(aliceEmail)
    val aliceService = FirebaseUserService(firestore, auth)
    val aliceHandle = "alice_${UUID.randomUUID().toString().replace("-", "").take(8).lowercase()}"

    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceService.completeOnboardingWithUniqueUsername(
        uid = aliceUid,
        email = aliceEmail,
        photoUrl = null,
        displayName = "Alice",
        rawUsername = aliceHandle,
        bio = "",
        language = "bn",
        themeMode = "system"
      ).getOrThrow()
    }

    val bobEmail = "bob_${UUID.randomUUID().toString().take(6)}@test.com"
    val bobUid = signInTestUser(bobEmail)
    val bobService = FirebaseUserService(firestore, auth)

    // 1. Bob tries to create his users/{bobUid} doc pointing to Alice's claimed username
    var createCaught: Throwable? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(bobUid).set(
          mapOf(
            "uid" to bobUid,
            "displayName" to "Bob",
            "username" to aliceHandle,
            "email" to bobEmail,
            "language" to "bn",
            "themeMode" to "system",
            "onboardingCompleted" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
    } catch (e: Throwable) {
      createCaught = e
    }
    assertNotNull(createCaught)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      createCaught?.unwrapFirestoreException()?.code
    )

    // 2. Bob onboards with his own handle, then tries to update his profile username to Alice's handle
    val bobHandle = "bob_${UUID.randomUUID().toString().replace("-", "").take(8).lowercase()}"
    withTimeout(DEFAULT_TIMEOUT_MS) {
      bobService.completeOnboardingWithUniqueUsername(
        uid = bobUid,
        email = bobEmail,
        photoUrl = null,
        displayName = "Bob",
        rawUsername = bobHandle,
        bio = "",
        language = "bn",
        themeMode = "system"
      ).getOrThrow()
    }

    var updateCaught: Throwable? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(bobUid).update(
          mapOf(
            "username" to aliceHandle,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
    } catch (e: Throwable) {
      updateCaught = e
    }
    assertNotNull(updateCaught)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      updateCaught?.unwrapFirestoreException()?.code
    )
  }

  @Test
  fun caseB_changingUsernameWithoutCreatingClaimInSameBatch_failsWithPermissionDenied() = runBlocking {
    // (খ) ক্লেইম ছাড়া নাম বদলানো ব্যর্থ
    val aliceEmail = "alice_${UUID.randomUUID().toString().take(6)}@test.com"
    val aliceUid = signInTestUser(aliceEmail)
    val aliceService = FirebaseUserService(firestore, auth)
    val oldHandle = "old_${UUID.randomUUID().toString().replace("-", "").take(8).lowercase()}"
    val newHandle = "new_${UUID.randomUUID().toString().replace("-", "").take(8).lowercase()}"

    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceService.completeOnboardingWithUniqueUsername(
        uid = aliceUid,
        email = aliceEmail,
        photoUrl = null,
        displayName = "Alice",
        rawUsername = oldHandle,
        bio = "",
        language = "bn",
        themeMode = "system"
      ).getOrThrow()
    }

    var caught: Throwable? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        // Updating users/{aliceUid}.username directly without creating usernames/{newHandle}
        firestore.collection("users").document(aliceUid).update(
          mapOf(
            "username" to newHandle,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
    } catch (e: Throwable) {
      caught = e
    }
    assertNotNull("Expected changing username without claim to fail", caught)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      caught?.unwrapFirestoreException()?.code
    )
  }

  @Test
  fun caseC_changingUsernameWithoutDeletingOldClaim_failsWithPermissionDenied() = runBlocking {
    // (গ) পুরোনো ক্লেইম না মুছে নাম বদলানো ব্যর্থ
    val aliceEmail = "alice_${UUID.randomUUID().toString().take(6)}@test.com"
    val aliceUid = signInTestUser(aliceEmail)
    val aliceService = FirebaseUserService(firestore, auth)
    val firstHandle = "first_${UUID.randomUUID().toString().replace("-", "").take(8).lowercase()}"
    val secondHandle = "second_${UUID.randomUUID().toString().replace("-", "").take(8).lowercase()}"

    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceService.completeOnboardingWithUniqueUsername(
        uid = aliceUid,
        email = aliceEmail,
        photoUrl = null,
        displayName = "Alice",
        rawUsername = firstHandle,
        bio = "",
        language = "bn",
        themeMode = "system"
      ).getOrThrow()
    }

    var caught: Throwable? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        // Creates new claim + updates users doc, but omits deleting old claim
        val batch = firestore.batch()
        batch.set(
          firestore.collection("usernames").document(secondHandle),
          mapOf(
            "uid" to aliceUid,
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
        batch.update(
          firestore.collection("users").document(aliceUid),
          mapOf(
            "username" to secondHandle,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        batch.commit().await()
      }
    } catch (e: Throwable) {
      caught = e
    }
    assertNotNull("Expected changing username without deleting old claim to fail", caught)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      caught?.unwrapFirestoreException()?.code
    )
  }

  @Test
  fun caseD_deletingUsernameClaimWhileKeepingUsernameInProfile_failsWithPermissionDenied() = runBlocking {
    // (ঘ) প্রোফাইলে নাম রেখে ক্লেইম মোছা ব্যর্থ
    val aliceEmail = "alice_${UUID.randomUUID().toString().take(6)}@test.com"
    val aliceUid = signInTestUser(aliceEmail)
    val aliceService = FirebaseUserService(firestore, auth)
    val handle = "keep_${UUID.randomUUID().toString().replace("-", "").take(8).lowercase()}"

    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceService.completeOnboardingWithUniqueUsername(
        uid = aliceUid,
        email = aliceEmail,
        photoUrl = null,
        displayName = "Alice",
        rawUsername = handle,
        bio = "",
        language = "bn",
        themeMode = "system"
      ).getOrThrow()
    }

    var caught: Throwable? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("usernames").document(handle).delete().await()
      }
    } catch (e: Throwable) {
      caught = e
    }
    assertNotNull("Expected deleting username claim while keeping profile username to fail", caught)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      caught?.unwrapFirestoreException()?.code
    )
  }

  @Test
  fun caseE_claimingReservedUsernameAdmin_failsInServiceAndSecurityRules() = runBlocking {
    // (ঙ) "admin" ক্লেইম ব্যর্থ
    val aliceEmail = "alice_${UUID.randomUUID().toString().take(6)}@test.com"
    val aliceUid = signInTestUser(aliceEmail)
    val aliceService = FirebaseUserService(firestore, auth)

    // 1. Client-side validation rejects "admin"
    val serviceAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceService.completeOnboardingWithUniqueUsername(
        uid = aliceUid,
        email = aliceEmail,
        photoUrl = null,
        displayName = "Alice",
        rawUsername = "admin",
        bio = "",
        language = "bn",
        themeMode = "system"
      )
    }
    assertTrue("Expected UserService to reject reserved username 'admin'", serviceAttempt.isFailure)

    // 2. Direct Firestore batch bypassing client check is rejected by firestore.rules
    var ruleCaught: Throwable? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        val batch = firestore.batch()
        batch.set(
          firestore.collection("usernames").document("admin"),
          mapOf(
            "uid" to aliceUid,
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
        batch.set(
          firestore.collection("users").document(aliceUid),
          mapOf(
            "uid" to aliceUid,
            "displayName" to "Alice",
            "username" to "admin",
            "email" to aliceEmail,
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
      ruleCaught = e
    }
    assertNotNull("Expected firestore.rules to reject reserved username 'admin'", ruleCaught)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      ruleCaught?.unwrapFirestoreException()?.code
    )
  }

  @Test
  fun caseF_validAtomicUsernameChange_succeedsAndReleasesOldClaim() = runBlocking {
    // (চ) সঠিক নাম বদলানো সফল (UserService.changeUsername দিয়ে একই ব্যাচে নতুন ক্লেইম তৈরি, পুরোনো ক্লেইম মোছা ও users আপডেট)
    val aliceEmail = "alice_${UUID.randomUUID().toString().take(6)}@test.com"
    val aliceUid = signInTestUser(aliceEmail)
    val aliceService = FirebaseUserService(firestore, auth)
    val oldHandle = "v1_${UUID.randomUUID().toString().replace("-", "").take(8).lowercase()}"
    val newHandle = "v2_${UUID.randomUUID().toString().replace("-", "").take(8).lowercase()}"

    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceService.completeOnboardingWithUniqueUsername(
        uid = aliceUid,
        email = aliceEmail,
        photoUrl = null,
        displayName = "Alice",
        rawUsername = oldHandle,
        bio = "",
        language = "bn",
        themeMode = "system"
      ).getOrThrow()
    }

    val swapResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceService.changeUsername(aliceUid, newHandle)
    }
    assertTrue("Expected atomic username change to succeed, got: ${swapResult.exceptionOrNull()}", swapResult.isSuccess)
    assertEquals(newHandle, swapResult.getOrThrow())

    // Old username is now available again, new username belongs to Alice, profile has newHandle
    val oldClaimSnap = withTimeout(DEFAULT_TIMEOUT_MS) {
      firestore.collection("usernames").document(oldHandle).get().await()
    }
    assertFalse("Expected old username claim to be deleted", oldClaimSnap.exists())

    val newClaimSnap = withTimeout(DEFAULT_TIMEOUT_MS) {
      firestore.collection("usernames").document(newHandle).get().await()
    }
    assertTrue("Expected new username claim to exist", newClaimSnap.exists())
    assertEquals(aliceUid, newClaimSnap.getString("uid"))

    val updatedProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceService.getMyProfile(aliceUid).getOrThrow()
    }
    assertEquals(newHandle, updatedProfile?.username)
  }

  @Test
  fun caseG_mismatchedFakeEmail_failsWithPermissionDenied() = runBlocking {
    // (ছ) ভুয়া email ব্যর্থ (data.email != request.auth.token.email)
    val aliceEmail = "alice_${UUID.randomUUID().toString().take(6)}@test.com"
    val aliceUid = signInTestUser(aliceEmail)
    val handle = "mail_${UUID.randomUUID().toString().replace("-", "").take(8).lowercase()}"

    var caught: Throwable? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        val batch = firestore.batch()
        batch.set(
          firestore.collection("usernames").document(handle),
          mapOf(
            "uid" to aliceUid,
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
        batch.set(
          firestore.collection("users").document(aliceUid),
          mapOf(
            "uid" to aliceUid,
            "displayName" to "Alice",
            "username" to handle,
            "email" to "fake_different_email@test.com",
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
    assertNotNull("Expected mismatched fake email to fail", caught)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      caught?.unwrapFirestoreException()?.code
    )
  }

  @Test
  fun crossUserReadAndSystemFieldInjection_rejectedByRules() = runBlocking {
    val aliceEmail = "alice_${UUID.randomUUID().toString().take(6)}@test.com"
    val aliceUid = signInTestUser(aliceEmail)
    val aliceService = FirebaseUserService(firestore, auth)
    val handle = "u_${UUID.randomUUID().toString().replace("-", "").take(10).lowercase()}"

    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceService.completeOnboardingWithUniqueUsername(
        uid = aliceUid,
        email = aliceEmail,
        photoUrl = null,
        displayName = "Alice",
        rawUsername = handle,
        bio = "",
        language = "bn",
        themeMode = "system"
      ).getOrThrow()
    }

    // Bob signs in and tries to read Alice's user document
    val bobEmail = "bob_${UUID.randomUUID().toString().take(6)}@test.com"
    val bobUid = signInTestUser(bobEmail)
    val bobService = FirebaseUserService(firestore, auth)
    val crossRead = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobService.getMyProfile(aliceUid)
    }
    assertTrue(crossRead.isFailure)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      crossRead.exceptionOrNull()?.unwrapFirestoreException()?.code
    )

    // Bob tries to inject followersCount on create
    var followersCaught: Throwable? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        val batch = firestore.batch()
        val bobHandle = "bob_${UUID.randomUUID().toString().replace("-", "").take(8).lowercase()}"
        batch.set(
          firestore.collection("usernames").document(bobHandle),
          mapOf("uid" to bobUid, "createdAt" to FieldValue.serverTimestamp())
        )
        batch.set(
          firestore.collection("users").document(bobUid),
          mapOf(
            "uid" to bobUid,
            "displayName" to "Bob",
            "username" to bobHandle,
            "email" to bobEmail,
            "language" to "bn",
            "themeMode" to "dark",
            "onboardingCompleted" to true,
            "followersCount" to 9999L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        batch.commit().await()
      }
    } catch (e: Throwable) {
      followersCaught = e
    }
    assertNotNull(followersCaught)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      followersCaught?.unwrapFirestoreException()?.code
    )
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
