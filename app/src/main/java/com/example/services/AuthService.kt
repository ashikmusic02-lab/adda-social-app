package com.example.services

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.model.AuthUserSummary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * AuthService interface — UI layers communicate strictly through this interface
 * and NEVER call FirebaseAuth or CredentialManager directly.
 */
interface AuthService {
  val authStateFlow: Flow<AuthUserSummary?>
  fun getCurrentUser(): AuthUserSummary?
  suspend fun attemptAutoGoogleSignIn(context: Context): Result<AuthUserSummary>
  suspend fun signInWithGoogleInteractive(activityContext: Context): Result<AuthUserSummary>
  suspend fun createAccountWithEmail(email: String, password: String): Result<AuthUserSummary>
  suspend fun signInWithEmail(email: String, password: String): Result<AuthUserSummary>
  suspend fun refreshEmailVerificationStatus(): Result<Boolean>
  suspend fun sendEmailVerification(): Result<Unit>
  suspend fun sendPasswordResetEmail(email: String): Result<Unit>
  suspend fun signOut(context: Context): Result<Unit>
}

class AuthCancelledException(message: String) : Exception(message)

class FirebaseAuthService(
  private val auth: FirebaseAuth = Firebase.auth
) : AuthService {

  override val authStateFlow: Flow<AuthUserSummary?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      trySend(firebaseAuth.currentUser?.toSummary())
    }
    auth.addAuthStateListener(listener)
    awaitClose {
      auth.removeAuthStateListener(listener)
    }
  }

  override fun getCurrentUser(): AuthUserSummary? = auth.currentUser?.toSummary()

  /**
   * Silent Auto Sign-In using GetGoogleIdOption (strictly 1 option in GetCredentialRequest).
   */
  override suspend fun attemptAutoGoogleSignIn(context: Context): Result<AuthUserSummary> {
    auth.currentUser?.let { existing ->
      return Result.success(existing.toSummary())
    }

    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      return Result.failure(e)
    }

    val credentialManager = CredentialManager.create(context)
    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    return try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val authResult = auth.signInWithCredential(authCredential).await()
        val user = checkNotNull(authResult.user) { "Firebase user null after Google sign-in" }
        Result.success(user.toSummary())
      } else {
        Result.failure(IllegalStateException("Unexpected credential type"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Interactive Google Sign-In using GetSignInWithGoogleOption (strictly 1 option per request).
   */
  override suspend fun signInWithGoogleInteractive(activityContext: Context): Result<AuthUserSummary> {
    val clientId = try {
      activityContext.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      return Result.failure(
        IllegalStateException("Google Sign-In configuration missing: default_web_client_id not found", e)
      )
    }

    val credentialManager = CredentialManager.create(activityContext)
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()

    return try {
      val activity = activityContext as? Activity ?: activityContext
      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val authResult = auth.signInWithCredential(authCredential).await()
        val user = checkNotNull(authResult.user) { "Firebase user null after Google sign-in" }
        Result.success(user.toSummary())
      } else {
        Result.failure(IllegalStateException("Unexpected credential type"))
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
      Result.failure(AuthCancelledException(e.message ?: "Sign-in cancelled"))
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      Result.failure(e)
    }
  }

  /**
   * Real Firebase Auth Email/Password Account Creation + Verification Email dispatch.
   */
  override suspend fun createAccountWithEmail(
    email: String,
    password: String
  ): Result<AuthUserSummary> {
    return try {
      val cleanEmail = email.trim()
      require(cleanEmail.isNotEmpty() && cleanEmail.contains("@")) {
        "Valid email address is required"
      }
      require(password.length >= 6) {
        "Password must be at least 6 characters"
      }
      val authResult = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
      val user = checkNotNull(authResult.user) { "Firebase user null after account creation" }
      try {
        user.sendEmailVerification().await()
      } catch (verifyErr: Exception) {
        Log.w("Auth", "Failed to send initial verification email: ${verifyErr.message}")
      }
      Result.success(user.toSummary())
    } catch (e: Exception) {
      Log.e("Auth", "Email registration failed", e)
      Result.failure(e)
    }
  }

  /**
   * Real Firebase Auth Email/Password Sign-In via EmailAuthProvider credential.
   */
  override suspend fun signInWithEmail(
    email: String,
    password: String
  ): Result<AuthUserSummary> {
    return try {
      val cleanEmail = email.trim()
      require(cleanEmail.isNotEmpty() && cleanEmail.contains("@")) {
        "Valid email address is required"
      }
      require(password.isNotEmpty()) {
        "Password is required"
      }
      val credential = EmailAuthProvider.getCredential(cleanEmail, password)
      val authResult = auth.signInWithCredential(credential).await()
      val user = checkNotNull(authResult.user) { "Firebase user null after email sign-in" }
      Result.success(user.toSummary())
    } catch (e: Exception) {
      Log.e("Auth", "Email sign-in failed", e)
      Result.failure(e)
    }
  }

  override suspend fun refreshEmailVerificationStatus(): Result<Boolean> {
    return try {
      val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
      user.reload().await()
      Result.success(auth.currentUser?.isEmailVerified == true)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun sendEmailVerification(): Result<Unit> {
    return try {
      val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
      user.sendEmailVerification().await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
    return try {
      val cleanEmail = email.trim()
      require(cleanEmail.isNotEmpty() && cleanEmail.contains("@")) {
        "Valid email address is required"
      }
      auth.sendPasswordResetEmail(cleanEmail).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun signOut(context: Context): Result<Unit> {
    return withContext(NonCancellable) {
      try {
        val credentialManager = CredentialManager.create(context)
        try {
          credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
          Log.w("Auth", "Failed to clear credential state: ${e.message}")
        }
        auth.signOut()
        Result.success(Unit)
      } catch (e: Exception) {
        auth.signOut()
        Result.failure(e)
      }
    }
  }

  private fun FirebaseUser.toSummary(): AuthUserSummary {
    return AuthUserSummary(
      uid = uid,
      email = email.orEmpty(),
      displayName = displayName.orEmpty(),
      photoUrl = photoUrl?.toString(),
      isEmailVerified = isEmailVerified,
      providerIds = providerData.map { it.providerId }
    )
  }
}
