package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.AuthUserSummary
import com.example.model.ScreenState
import com.example.model.UserProfile
import com.example.model.UsernameValidationResult
import com.example.services.AuthService
import com.example.services.PreferencesService
import com.example.services.UserService
import com.example.services.UsernameAlreadyTakenException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UsernameCheckState {
  data object Idle : UsernameCheckState
  data object Checking : UsernameCheckState
  data object Available : UsernameCheckState
  data object Taken : UsernameCheckState
  data class InvalidFormat(val reason: UsernameValidationResult) : UsernameCheckState
  data class Error(val message: String) : UsernameCheckState
}

class AuthenticatedUserViewModel(
  private val authUser: AuthUserSummary,
  private val userService: UserService,
  private val authService: AuthService,
  private val preferencesService: PreferencesService
) : ViewModel() {

  // Two-tier Kotlin Flow error handling architecture per firebase-firestore-android skill
  val profileState: StateFlow<ScreenState<UserProfile?>> =
    userService.observeMyProfile(authUser.uid)
      .onEach { profile ->
        if (profile != null) {
          preferencesService.setLanguage(AppLanguage.fromCode(profile.language))
          preferencesService.setThemeMode(AppThemeMode.fromValue(profile.themeMode))
        }
      }
      .map<UserProfile?, ScreenState<UserProfile?>> { profile ->
        ScreenState.Success(profile)
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing user profile", rootError)
        emit(
          ScreenState.Error(
            messageBn = "প্রোফাইল লোড করতে সমস্যা হয়েছে: ${rootError.message ?: "অজানা ত্রুটি"}",
            messageEn = "Failed to load profile: ${rootError.message ?: "Unknown error"}"
          )
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ScreenState.Loading
      )

  private val _usernameCheckState = MutableStateFlow<UsernameCheckState>(UsernameCheckState.Idle)
  val usernameCheckState: StateFlow<UsernameCheckState> = _usernameCheckState.asStateFlow()

  private val _isSubmittingOnboarding = MutableStateFlow(false)
  val isSubmittingOnboarding: StateFlow<Boolean> = _isSubmittingOnboarding.asStateFlow()

  private val _onboardingError = MutableStateFlow<String?>(null)
  val onboardingError: StateFlow<String?> = _onboardingError.asStateFlow()

  private val _statusBannerMessage = MutableStateFlow<String?>(null)
  val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

  private val _isEmailVerified = MutableStateFlow(authUser.isEmailVerified)
  val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()

  fun onUsernameInputChanged(rawInput: String) {
    _onboardingError.value = null
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty()) {
      _usernameCheckState.value = UsernameCheckState.Idle
      return
    }
    val validation = userService.validateUsernameFormat(trimmed)
    if (validation != UsernameValidationResult.Valid) {
      _usernameCheckState.value = UsernameCheckState.InvalidFormat(validation)
    } else {
      _usernameCheckState.value = UsernameCheckState.Idle
    }
  }

  fun checkUsernameAvailability(rawUsername: String) {
    val trimmed = rawUsername.trim()
    val validation = userService.validateUsernameFormat(trimmed)
    if (validation != UsernameValidationResult.Valid) {
      _usernameCheckState.value = UsernameCheckState.InvalidFormat(validation)
      return
    }
    _usernameCheckState.value = UsernameCheckState.Checking
    viewModelScope.launch {
      val result = userService.checkUsernameAvailability(trimmed, authUser.uid)
      result.onSuccess { available ->
        _usernameCheckState.value = if (available) {
          UsernameCheckState.Available
        } else {
          UsernameCheckState.Taken
        }
      }.onFailure { err ->
        _usernameCheckState.value = UsernameCheckState.Error(
          err.localizedMessage ?: "Failed to check availability"
        )
      }
    }
  }

  fun completeOnboarding(
    displayName: String,
    rawUsername: String,
    bio: String,
    language: AppLanguage,
    themeMode: AppThemeMode
  ) {
    if (_isSubmittingOnboarding.value) return
    val cleanName = displayName.trim()
    val cleanUsername = rawUsername.trim()

    if (cleanName.isEmpty()) {
      _onboardingError.value = if (language == AppLanguage.BN) {
        "অনুগ্রহ করে আপনার পূর্ণ নাম লিখুন।"
      } else {
        "Please enter your full display name."
      }
      return
    }

    val validation = userService.validateUsernameFormat(cleanUsername)
    if (validation != UsernameValidationResult.Valid) {
      _usernameCheckState.value = UsernameCheckState.InvalidFormat(validation)
      return
    }

    _isSubmittingOnboarding.value = true
    _onboardingError.value = null

    viewModelScope.launch {
      val result = userService.completeOnboardingWithUniqueUsername(
        uid = authUser.uid,
        email = authUser.email,
        photoUrl = authUser.photoUrl,
        displayName = cleanName,
        rawUsername = cleanUsername,
        bio = bio,
        language = language.code,
        themeMode = themeMode.value
      )
      _isSubmittingOnboarding.value = false
      result.onSuccess {
        _usernameCheckState.value = UsernameCheckState.Available
      }.onFailure { err ->
        if (err is UsernameAlreadyTakenException ||
          err.cause is UsernameAlreadyTakenException ||
          err.message?.contains("already taken", ignoreCase = true) == true
        ) {
          _usernameCheckState.value = UsernameCheckState.Taken
          _onboardingError.value = if (language == AppLanguage.BN) {
            "এই ইউজারনেমটি ইতিমধ্যে অন্য কেউ নিয়েছেন। অন্য একটি চেষ্টা করুন।"
          } else {
            "This username is already taken. Please try another one."
          }
        } else {
          _onboardingError.value = err.localizedMessage ?: "Failed to save profile"
        }
      }
    }
  }

  fun updateLanguagePreference(newLanguage: AppLanguage, currentTheme: AppThemeMode) {
    viewModelScope.launch {
      preferencesService.setLanguage(newLanguage)
      userService.updatePreferences(authUser.uid, newLanguage.code, currentTheme.value)
    }
  }

  fun updateThemePreference(currentLanguage: AppLanguage, newTheme: AppThemeMode) {
    viewModelScope.launch {
      preferencesService.setThemeMode(newTheme)
      userService.updatePreferences(authUser.uid, currentLanguage.code, newTheme.value)
    }
  }

  fun updateDisplayNameAndBio(
    displayName: String,
    bio: String,
    language: AppLanguage,
    onDone: () -> Unit
  ) {
    viewModelScope.launch {
      val res = userService.updateProfile(authUser.uid, displayName, bio)
      res.onSuccess {
        _statusBannerMessage.value = if (language == AppLanguage.BN) {
          "প্রোফাইল সফলভাবে হালনাগাদ হয়েছে।"
        } else {
          "Profile updated successfully."
        }
        onDone()
      }.onFailure { err ->
        _statusBannerMessage.value = err.localizedMessage ?: "Update failed"
      }
    }
  }

  fun refreshEmailVerification(language: AppLanguage) {
    viewModelScope.launch {
      val res = authService.refreshEmailVerificationStatus()
      res.onSuccess { verified ->
        _isEmailVerified.value = verified
        _statusBannerMessage.value = if (verified) {
          if (language == AppLanguage.BN) "ইমেইল যাচাইকৃত!" else "Email is verified!"
        } else {
          if (language == AppLanguage.BN) "ইমেইল এখনো যাচাই হয়নি।" else "Email is not verified yet."
        }
      }
    }
  }

  fun sendVerificationEmail(language: AppLanguage) {
    viewModelScope.launch {
      val res = authService.sendEmailVerification()
      res.onSuccess {
        _statusBannerMessage.value = if (language == AppLanguage.BN) {
          "ভেরিফিকেশন ইমেইল পাঠানো হয়েছে। আপনার ইনবক্স চেক করুন।"
        } else {
          "Verification email sent. Please check your inbox."
        }
      }.onFailure { err ->
        _statusBannerMessage.value = err.localizedMessage ?: "Could not send verification email"
      }
    }
  }

  fun clearStatusBanner() {
    _statusBannerMessage.value = null
  }

  private companion object {
    const val TAG = "AuthUserVM"
  }
}
