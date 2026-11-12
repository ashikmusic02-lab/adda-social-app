package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppLanguage(val code: String, val nativeLabel: String) {
  BN("bn", "বাংলা"),
  EN("en", "English");

  companion object {
    fun fromCode(code: String?): AppLanguage =
      entries.firstOrNull { it.code == code } ?: BN
  }
}

enum class AppThemeMode(val value: String) {
  SYSTEM("system"),
  LIGHT("light"),
  DARK("dark");

  companion object {
    fun fromValue(value: String?): AppThemeMode =
      entries.firstOrNull { it.value == value } ?: SYSTEM
  }
}

data class AuthUserSummary(
  val uid: String,
  val email: String,
  val displayName: String,
  val photoUrl: String?,
  val isEmailVerified: Boolean,
  val providerIds: List<String>
)

/**
 * Phase 1 Firestore User Document (`/users/{userId}`).
 *
 * Note:
 * - No `role` field is stored in Firestore; admin roles will be managed via Firebase Auth Custom Claims in Phase 6.
 * - Counter and moderation fields (`followersCount`, `followingCount`, `postsCount`,
 *   `isVerified`, `isBanned`) have safe client-side defaults when deserialized, and are
 *   NEVER sent in client write payloads (`toCreateMap` / `toUpdateMap`) as enforced by `firestore.rules`.
 */
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val docId: String = "",
  val uid: String = "",
  val displayName: String = "",
  val username: String = "",
  val email: String = "",
  val photoUrl: String? = null,
  val bio: String? = null,
  val language: String = "bn",
  val themeMode: String = "system",
  val onboardingCompleted: Boolean = false,
  val followersCount: Long = 0L,
  val followingCount: Long = 0L,
  val postsCount: Long = 0L,
  val isVerified: Boolean = false,
  val isBanned: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toCreateMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
      "uid" to uid,
      "displayName" to displayName.trim(),
      "username" to username.trim().lowercase(),
      "email" to email.trim(),
      "language" to language,
      "themeMode" to themeMode,
      "onboardingCompleted" to onboardingCompleted,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (!photoUrl.isNullOrBlank()) {
      map["photoUrl"] = photoUrl.trim()
    }
    if (!bio.isNullOrBlank()) {
      map["bio"] = bio.trim()
    }
    return map
  }

  fun toUpdateMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>(
      "displayName" to displayName.trim(),
      "username" to username.trim().lowercase(),
      "language" to language,
      "themeMode" to themeMode,
      "onboardingCompleted" to onboardingCompleted,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (!photoUrl.isNullOrBlank()) {
      map["photoUrl"] = photoUrl.trim()
    }
    if (bio != null) {
      map["bio"] = bio.trim()
    }
    return map
  }
}

@IgnoreExtraProperties
data class UsernameClaim(
  @DocumentId val username: String = "",
  val uid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

sealed interface UsernameValidationResult {
  data object Valid : UsernameValidationResult
  data object TooShort : UsernameValidationResult
  data object TooLong : UsernameValidationResult
  data object InvalidCharacters : UsernameValidationResult
  data object ReservedUsername : UsernameValidationResult
}

/**
 * Standard screen state supporting Loading, Success, Empty, Error, and Offline states.
 */
sealed interface ScreenState<out T> {
  data object Loading : ScreenState<Nothing>
  data class Success<T>(val data: T) : ScreenState<T>
  data class Empty(val messageBn: String, val messageEn: String) : ScreenState<Nothing>
  data class Error(val messageBn: String, val messageEn: String) : ScreenState<Nothing>
  data class Offline<T>(val cachedData: T? = null) : ScreenState<T>
}

// ============================================================================
// FUTURE PHASE DOMAIN MODELS (Planned for Phase 2–6; not active in Phase 1)
// ============================================================================

data class PostDraft(
  val id: String = "",
  val authorUid: String = "",
  val authorUsername: String = "",
  val authorDisplayName: String = "",
  val content: String = "",
  val imageUrls: List<String> = emptyList(),
  val categoryId: String? = null,
  val likesCount: Long = 0L,
  val commentsCount: Long = 0L,
  val createdAt: Timestamp? = null
)

data class CommentItem(
  val id: String = "",
  val postId: String = "",
  val authorUid: String = "",
  val authorUsername: String = "",
  val text: String = "",
  val createdAt: Timestamp? = null
)

data class FollowRelation(
  val followerUid: String = "",
  val followingUid: String = "",
  val createdAt: Timestamp? = null
)

data class AppNotificationItem(
  val id: String = "",
  val recipientUid: String = "",
  val actorUid: String = "",
  val type: String = "",
  val isRead: Boolean = false,
  val createdAt: Timestamp? = null
)

data class ContentReport(
  val id: String = "",
  val reporterUid: String = "",
  val targetType: String = "",
  val targetId: String = "",
  val reason: String = "",
  val status: String = "pending"
)
