package com.example.services

import android.content.Context
import com.example.R
import com.example.model.UserProfile
import com.example.model.UsernameValidationResult
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * UserService interface — manages `/users/{userId}` and `/usernames/{username}`
 * with atomic Firestore transactions/batches for unique username claims and swaps.
 */
interface UserService {
  fun validateUsernameFormat(rawUsername: String): UsernameValidationResult
  fun observeMyProfile(uid: String): Flow<UserProfile?>
  suspend fun getMyProfile(uid: String): Result<UserProfile?>
  suspend fun checkUsernameAvailability(rawUsername: String, currentUid: String): Result<Boolean>
  suspend fun completeOnboardingWithUniqueUsername(
    uid: String,
    email: String,
    photoUrl: String?,
    displayName: String,
    rawUsername: String,
    bio: String,
    language: String,
    themeMode: String
  ): Result<UserProfile>
  suspend fun changeUsername(uid: String, rawNewUsername: String): Result<String>
  suspend fun updatePreferences(uid: String, language: String, themeMode: String): Result<Unit>
  suspend fun updateProfile(
    uid: String,
    displayName: String,
    bio: String
  ): Result<Unit>
}

class UsernameAlreadyTakenException(val username: String) :
  Exception("Username @$username is already taken")

class FirebaseUserService(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) : UserService {

  // Convenience constructor resolving the provisioned custom database ID from string resources
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val usernameRegex = Regex("^[a-z0-9_]+$")

  private fun requireAuthenticatedUid(expectedUid: String): String {
    val activeUid = auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    require(activeUid == expectedUid) { "Authenticated UID mismatch." }
    return activeUid
  }

  override fun validateUsernameFormat(rawUsername: String): UsernameValidationResult {
    val normalized = rawUsername.trim()
    if (normalized.length < 3) return UsernameValidationResult.TooShort
    if (normalized.length > 24) return UsernameValidationResult.TooLong
    if (!usernameRegex.matches(normalized)) return UsernameValidationResult.InvalidCharacters
    if (normalized in RESERVED_USERNAMES) return UsernameValidationResult.ReservedUsername
    return UsernameValidationResult.Valid
  }

  override fun observeMyProfile(uid: String): Flow<UserProfile?> = flow {
    val path = "users/$uid"
    emitAll(
      db.collection("users").document(uid)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              UserProfile::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  override suspend fun getMyProfile(uid: String): Result<UserProfile?> {
    val docRef = db.collection("users").document(uid)
    return try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        Result.success(null)
      } else {
        val profile = snapshot.toObject(
          UserProfile::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
        Result.success(profile)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  override suspend fun checkUsernameAvailability(
    rawUsername: String,
    currentUid: String
  ): Result<Boolean> {
    val normalized = rawUsername.trim()
    val validation = validateUsernameFormat(normalized)
    if (validation != UsernameValidationResult.Valid) {
      return Result.failure(IllegalArgumentException("Invalid username format: $validation"))
    }
    val docRef = db.collection("usernames").document(normalized)
    return try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        Result.success(true)
      } else {
        val ownerUid = snapshot.getString("uid")
        Result.success(ownerUid == currentUid)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * Phase 1 Onboarding: Atomically claims `usernames/{username}` and creates/updates `users/{uid}`
   * inside a single Firestore transaction (`db.runTransaction`).
   */
  override suspend fun completeOnboardingWithUniqueUsername(
    uid: String,
    email: String,
    photoUrl: String?,
    displayName: String,
    rawUsername: String,
    bio: String,
    language: String,
    themeMode: String
  ): Result<UserProfile> {
    val verifiedUid = try {
      requireAuthenticatedUid(uid)
    } catch (e: Exception) {
      return Result.failure(e)
    }

    val cleanDisplayName = displayName.trim()
    val normalizedUsername = rawUsername.trim()
    val verifiedEmail = email.trim().ifEmpty { auth.currentUser?.email?.trim().orEmpty() }

    if (cleanDisplayName.isEmpty() || cleanDisplayName.length > 60) {
      return Result.failure(IllegalArgumentException("Display name must be 1–60 characters."))
    }
    val validation = validateUsernameFormat(normalizedUsername)
    if (validation != UsernameValidationResult.Valid) {
      return Result.failure(IllegalArgumentException("Username format invalid: $validation"))
    }

    val userDocRef = db.collection("users").document(verifiedUid)
    val usernameDocRef = db.collection("usernames").document(normalizedUsername)

    return try {
      db.runTransaction { transaction ->
        val usernameSnap = transaction.get(usernameDocRef)
        val userSnap = transaction.get(userDocRef)

        if (usernameSnap.exists()) {
          val existingOwnerUid = usernameSnap.getString("uid")
          if (existingOwnerUid != verifiedUid) {
            throw UsernameAlreadyTakenException(normalizedUsername)
          }
        } else {
          // Claim the new unique username
          val claimPayload = mapOf(
            "uid" to verifiedUid,
            "createdAt" to FieldValue.serverTimestamp()
          )
          transaction.set(usernameDocRef, claimPayload)
        }

        // If the user previously claimed a different username, release the old one in the same batch
        val previousUsername = if (userSnap.exists()) userSnap.getString("username") else null
        if (!previousUsername.isNullOrBlank() && previousUsername != normalizedUsername) {
          val oldUsernameRef = db.collection("usernames").document(previousUsername)
          transaction.delete(oldUsernameRef)
        }

        if (!userSnap.exists()) {
          val newProfile = UserProfile(
            uid = verifiedUid,
            displayName = cleanDisplayName,
            username = normalizedUsername,
            email = verifiedEmail,
            photoUrl = photoUrl,
            bio = bio.trim().ifBlank { null },
            language = language,
            themeMode = themeMode,
            onboardingCompleted = true
          )
          transaction.set(userDocRef, newProfile.toCreateMap())
        } else {
          val updatePayload = mutableMapOf<String, Any>(
            "displayName" to cleanDisplayName,
            "username" to normalizedUsername,
            "language" to language,
            "themeMode" to themeMode,
            "onboardingCompleted" to true,
            "updatedAt" to FieldValue.serverTimestamp()
          )
          if (bio.trim().isNotEmpty()) {
            updatePayload["bio"] = bio.trim()
          }
          transaction.update(userDocRef, updatePayload)
        }
      }.await()

      val updatedSnap = userDocRef.get().await()
      val savedProfile = updatedSnap.toObject(
        UserProfile::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: UserProfile(
        docId = verifiedUid,
        uid = verifiedUid,
        displayName = cleanDisplayName,
        username = normalizedUsername,
        email = verifiedEmail,
        photoUrl = photoUrl,
        bio = bio.trim().ifBlank { null },
        language = language,
        themeMode = themeMode,
        onboardingCompleted = true
      )
      Result.success(savedProfile)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, userDocRef.path)
      Result.failure(e)
    }
  }

  /**
   * Atomically changes a user's username in a single transaction/batch:
   * 1. Creates new claim `usernames/{newUsername}` owned by `uid`
   * 2. Deletes old claim `usernames/{oldUsername}`
   * 3. Updates `users/{uid}.username` to `newUsername`
   */
  override suspend fun changeUsername(uid: String, rawNewUsername: String): Result<String> {
    val verifiedUid = try {
      requireAuthenticatedUid(uid)
    } catch (e: Exception) {
      return Result.failure(e)
    }

    val normalizedNewUsername = rawNewUsername.trim()
    val validation = validateUsernameFormat(normalizedNewUsername)
    if (validation != UsernameValidationResult.Valid) {
      return Result.failure(IllegalArgumentException("Username format invalid: $validation"))
    }

    val userDocRef = db.collection("users").document(verifiedUid)
    val newUsernameDocRef = db.collection("usernames").document(normalizedNewUsername)

    return try {
      db.runTransaction { transaction ->
        val userSnap = transaction.get(userDocRef)
        if (!userSnap.exists()) {
          throw IllegalStateException("User profile does not exist for uid=$verifiedUid")
        }

        val currentUsername = userSnap.getString("username")
          ?: throw IllegalStateException("Current profile has no username.")

        if (currentUsername == normalizedNewUsername) {
          return@runTransaction normalizedNewUsername
        }

        val newUsernameSnap = transaction.get(newUsernameDocRef)
        if (newUsernameSnap.exists()) {
          throw UsernameAlreadyTakenException(normalizedNewUsername)
        }

        val oldUsernameDocRef = db.collection("usernames").document(currentUsername)

        // All 3 operations happen atomically in the same transaction commit:
        // (1) Create new username claim
        transaction.set(
          newUsernameDocRef,
          mapOf(
            "uid" to verifiedUid,
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
        // (2) Delete old username claim
        transaction.delete(oldUsernameDocRef)
        // (3) Update users/{uid} with new username
        transaction.update(
          userDocRef,
          mapOf(
            "username" to normalizedNewUsername,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )

        normalizedNewUsername
      }.await()

      Result.success(normalizedNewUsername)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
      Result.failure(e)
    }
  }

  override suspend fun updatePreferences(
    uid: String,
    language: String,
    themeMode: String
  ): Result<Unit> {
    val verifiedUid = try {
      requireAuthenticatedUid(uid)
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val docRef = db.collection("users").document(verifiedUid)
    return try {
      val snapshot = docRef.get().await()
      if (snapshot.exists()) {
        docRef.update(
          mapOf(
            "language" to language,
            "themeMode" to themeMode,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  override suspend fun updateProfile(
    uid: String,
    displayName: String,
    bio: String
  ): Result<Unit> {
    val verifiedUid = try {
      requireAuthenticatedUid(uid)
    } catch (e: Exception) {
      return Result.failure(e)
    }
    val cleanName = displayName.trim()
    if (cleanName.isEmpty() || cleanName.length > 60) {
      return Result.failure(IllegalArgumentException("Display name must be 1–60 characters."))
    }
    if (bio.trim().length > 160) {
      return Result.failure(IllegalArgumentException("Bio must be at most 160 characters."))
    }
    val docRef = db.collection("users").document(verifiedUid)
    return try {
      docRef.update(
        mapOf(
          "displayName" to cleanName,
          "bio" to bio.trim(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  companion object {
    val RESERVED_USERNAMES = setOf(
      "admin",
      "administrator",
      "support",
      "moderator",
      "mod",
      "adda",
      "official",
      "staff",
      "help",
      "system",
      "root",
      "null",
      "undefined"
    )
  }
}
