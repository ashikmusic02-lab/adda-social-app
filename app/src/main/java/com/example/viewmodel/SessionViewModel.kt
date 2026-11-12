package com.example.viewmodel

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.i18n.stringsForLanguage
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.AuthUserSummary
import com.example.services.AuthCancelledException
import com.example.services.AuthService
import com.example.services.ConnectivityService
import com.example.services.PreferencesService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RootAuthState {
  data object Splash : RootAuthState
  data class Unauthenticated(
    val errorMessage: String? = null,
    val infoMessage: String? = null
  ) : RootAuthState
  data class Authenticated(val user: AuthUserSummary) : RootAuthState
}

class SessionViewModel(
  private val authService: AuthService,
  private val preferencesService: PreferencesService,
  connectivityService: ConnectivityService
) : ViewModel() {

  private val _rootAuthState = MutableStateFlow<RootAuthState>(RootAuthState.Splash)
  val rootAuthState: StateFlow<RootAuthState> = _rootAuthState.asStateFlow()

  private val _isSigningIn = MutableStateFlow(false)
  val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

  val language: StateFlow<AppLanguage> = preferencesService.languageFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppLanguage.BN)

  val themeMode: StateFlow<AppThemeMode> = preferencesService.themeModeFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppThemeMode.SYSTEM)

  val isOnline: StateFlow<Boolean> = connectivityService.isOnlineFlow
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000L),
      connectivityService.isCurrentlyOnline()
    )

  init {
    viewModelScope.launch {
      delay(400L)
      authService.authStateFlow.collect { user ->
        _isSigningIn.value = false
        if (user != null) {
          _rootAuthState.value = RootAuthState.Authenticated(user)
        } else {
          val current = _rootAuthState.value
          val existingError = (current as? RootAuthState.Unauthenticated)?.errorMessage
          val existingInfo = (current as? RootAuthState.Unauthenticated)?.infoMessage
          _rootAuthState.value = RootAuthState.Unauthenticated(existingError, existingInfo)
        }
      }
    }
  }

  private fun isValidEmail(raw: String): Boolean {
    val clean = raw.trim()
    return clean.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(clean).matches()
  }

  fun attemptSilentAutoSignIn(context: Context) {
    if (authService.getCurrentUser() != null) return
    viewModelScope.launch {
      authService.attemptAutoGoogleSignIn(context)
    }
  }

  fun signInWithGoogle(activityContext: Context) {
    if (_isSigningIn.value) return
    _isSigningIn.value = true
    _rootAuthState.value = RootAuthState.Unauthenticated()

    viewModelScope.launch {
      val result = authService.signInWithGoogleInteractive(activityContext)
      _isSigningIn.value = false
      result.onSuccess { user ->
        _rootAuthState.value = RootAuthState.Authenticated(user)
      }.onFailure { error ->
        if (error is AuthCancelledException) {
          _rootAuthState.value = RootAuthState.Unauthenticated()
        } else {
          _rootAuthState.value = RootAuthState.Unauthenticated(
            errorMessage = error.localizedMessage ?: "Google Sign-In failed"
          )
        }
      }
    }
  }

  fun signInWithEmail(email: String, password: String) {
    if (_isSigningIn.value) return
    val strings = stringsForLanguage(language.value)
    if (!isValidEmail(email)) {
      _rootAuthState.value = RootAuthState.Unauthenticated(errorMessage = strings.invalidEmailError)
      return
    }
    if (password.length < 6) {
      _rootAuthState.value = RootAuthState.Unauthenticated(errorMessage = strings.shortPasswordError)
      return
    }

    _isSigningIn.value = true
    _rootAuthState.value = RootAuthState.Unauthenticated()

    viewModelScope.launch {
      val result = authService.signInWithEmail(email, password)
      _isSigningIn.value = false
      result.onSuccess { user ->
        _rootAuthState.value = RootAuthState.Authenticated(user)
      }.onFailure { error ->
        _rootAuthState.value = RootAuthState.Unauthenticated(
          errorMessage = error.localizedMessage ?: "Email sign-in failed"
        )
      }
    }
  }

  fun createAccountWithEmail(email: String, password: String, confirmPassword: String) {
    if (_isSigningIn.value) return
    val strings = stringsForLanguage(language.value)
    if (!isValidEmail(email)) {
      _rootAuthState.value = RootAuthState.Unauthenticated(errorMessage = strings.invalidEmailError)
      return
    }
    if (password.length < 6) {
      _rootAuthState.value = RootAuthState.Unauthenticated(errorMessage = strings.shortPasswordError)
      return
    }
    if (password != confirmPassword) {
      _rootAuthState.value = RootAuthState.Unauthenticated(errorMessage = strings.passwordMismatchError)
      return
    }

    _isSigningIn.value = true
    _rootAuthState.value = RootAuthState.Unauthenticated()

    viewModelScope.launch {
      val result = authService.createAccountWithEmail(email, password)
      _isSigningIn.value = false
      result.onSuccess { user ->
        _rootAuthState.value = RootAuthState.Authenticated(user)
      }.onFailure { error ->
        _rootAuthState.value = RootAuthState.Unauthenticated(
          errorMessage = error.localizedMessage ?: "Account creation failed"
        )
      }
    }
  }

  fun sendPasswordReset(email: String) {
    if (_isSigningIn.value) return
    val strings = stringsForLanguage(language.value)
    if (!isValidEmail(email)) {
      _rootAuthState.value = RootAuthState.Unauthenticated(errorMessage = strings.invalidEmailError)
      return
    }

    _isSigningIn.value = true
    _rootAuthState.value = RootAuthState.Unauthenticated()

    viewModelScope.launch {
      val result = authService.sendPasswordResetEmail(email)
      _isSigningIn.value = false
      result.onSuccess {
        _rootAuthState.value = RootAuthState.Unauthenticated(
          infoMessage = strings.resetEmailSentSuccess
        )
      }.onFailure { error ->
        _rootAuthState.value = RootAuthState.Unauthenticated(
          errorMessage = error.localizedMessage ?: "Failed to send reset email"
        )
      }
    }
  }

  fun clearAuthError() {
    val current = _rootAuthState.value
    if (current is RootAuthState.Unauthenticated) {
      _rootAuthState.value = RootAuthState.Unauthenticated()
    }
  }

  fun updateLanguageLocal(newLanguage: AppLanguage) {
    viewModelScope.launch {
      preferencesService.setLanguage(newLanguage)
    }
  }

  fun updateThemeModeLocal(newTheme: AppThemeMode) {
    viewModelScope.launch {
      preferencesService.setThemeMode(newTheme)
    }
  }

  fun signOut(context: Context) {
    viewModelScope.launch {
      authService.signOut(context)
      _rootAuthState.value = RootAuthState.Unauthenticated()
    }
  }
}
