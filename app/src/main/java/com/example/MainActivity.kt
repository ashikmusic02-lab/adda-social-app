package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.i18n.LocalAppStrings
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.AuthUserSummary
import com.example.model.ScreenState
import com.example.services.AndroidConnectivityService
import com.example.services.DataStorePreferencesService
import com.example.services.FirebaseAuthService
import com.example.services.FirebaseUserService
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LoadingStateCard
import com.example.ui.components.OfflineStateBanner
import com.example.ui.screens.MainShellScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SplashScreenView
import com.example.ui.screens.WelcomeAuthScreen
import com.example.ui.theme.AddaTheme
import com.example.viewmodel.AuthenticatedUserViewModel
import com.example.viewmodel.RootAuthState
import com.example.viewmodel.SessionViewModel
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AddaAppRoot()
    }
  }
}

@Composable
fun AddaAppRoot() {
  val context = LocalContext.current
  val sessionViewModel: SessionViewModel = viewModel(
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY])
        SessionViewModel(
          authService = FirebaseAuthService(),
          preferencesService = DataStorePreferencesService(app),
          connectivityService = AndroidConnectivityService(app)
        )
      }
    }
  )

  val rootAuthState by sessionViewModel.rootAuthState.collectAsStateWithLifecycle()
  val isSigningIn by sessionViewModel.isSigningIn.collectAsStateWithLifecycle()
  val language by sessionViewModel.language.collectAsStateWithLifecycle()
  val themeMode by sessionViewModel.themeMode.collectAsStateWithLifecycle()
  val isOnline by sessionViewModel.isOnline.collectAsStateWithLifecycle()

  AddaTheme(
    themeMode = themeMode,
    language = language
  ) {
    when (val state = rootAuthState) {
      RootAuthState.Splash -> {
        SplashScreenView(isOnline = isOnline)
      }

      is RootAuthState.Unauthenticated -> {
        WelcomeAuthScreen(
          isOnline = isOnline,
          isSigningIn = isSigningIn,
          errorMessage = state.errorMessage,
          infoMessage = state.infoMessage,
          currentLanguage = language,
          currentThemeMode = themeMode,
          onLanguageChange = { sessionViewModel.updateLanguageLocal(it) },
          onThemeChange = { sessionViewModel.updateThemeModeLocal(it) },
          onEmailLoginClick = { email, password ->
            sessionViewModel.signInWithEmail(email, password)
          },
          onEmailRegisterClick = { email, password, confirmPassword ->
            sessionViewModel.createAccountWithEmail(email, password, confirmPassword)
          },
          onPasswordResetClick = { email ->
            sessionViewModel.sendPasswordReset(email)
          },
          onGoogleSignInClick = { sessionViewModel.signInWithGoogle(context) },
          onAttemptAutoSignIn = { sessionViewModel.attemptSilentAutoSignIn(context) },
          onRetryClearError = { sessionViewModel.clearAuthError() }
        )
      }

      is RootAuthState.Authenticated -> {
        val authUser = state.user
        key(authUser.uid) {
          AuthenticatedGate(
            authUser = authUser,
            isOnline = isOnline,
            currentLanguage = language,
            currentThemeMode = themeMode,
            onSignOut = { sessionViewModel.signOut(context) }
          )
        }
      }
    }
  }
}

@Composable
private fun AuthenticatedGate(
  authUser: AuthUserSummary,
  isOnline: Boolean,
  currentLanguage: AppLanguage,
  currentThemeMode: AppThemeMode,
  onSignOut: () -> Unit
) {
  val userViewModel: AuthenticatedUserViewModel = viewModel(
    key = authUser.uid,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY])
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        AuthenticatedUserViewModel(
          authUser = authUser,
          userService = FirebaseUserService(db),
          authService = FirebaseAuthService(),
          preferencesService = DataStorePreferencesService(app)
        )
      }
    }
  )

  val profileState by userViewModel.profileState.collectAsStateWithLifecycle()
  val usernameCheckState by userViewModel.usernameCheckState.collectAsStateWithLifecycle()
  val isSubmitting by userViewModel.isSubmittingOnboarding.collectAsStateWithLifecycle()
  val onboardingError by userViewModel.onboardingError.collectAsStateWithLifecycle()
  val statusBannerMessage by userViewModel.statusBannerMessage.collectAsStateWithLifecycle()
  val isEmailVerified by userViewModel.isEmailVerified.collectAsStateWithLifecycle()
  val strings = LocalAppStrings.current

  when (val pState = profileState) {
    ScreenState.Loading -> {
      Column(modifier = Modifier.fillMaxSize()) {
        OfflineStateBanner(isOnline = isOnline)
        LoadingStateCard(message = strings.savingProfile)
      }
    }

    is ScreenState.Error -> {
      Column(modifier = Modifier.fillMaxSize()) {
        OfflineStateBanner(isOnline = isOnline)
        ErrorStateCard(
          message = if (currentLanguage == AppLanguage.BN) pState.messageBn else pState.messageEn
        )
      }
    }

    is ScreenState.Empty -> {
      Column(modifier = Modifier.fillMaxSize()) {
        OfflineStateBanner(isOnline = isOnline)
      }
    }

    is ScreenState.Offline -> {
      Column(modifier = Modifier.fillMaxSize()) {
        OfflineStateBanner(isOnline = false)
      }
    }

    is ScreenState.Success -> {
      val profile = pState.data
      if (profile == null || !profile.onboardingCompleted || profile.username.isBlank()) {
        OnboardingScreen(
          authUser = authUser,
          isOnline = isOnline,
          currentLanguage = currentLanguage,
          currentThemeMode = currentThemeMode,
          usernameCheckState = usernameCheckState,
          isSubmitting = isSubmitting,
          errorMessage = onboardingError,
          onLanguageChange = { userViewModel.updateLanguagePreference(it, currentThemeMode) },
          onThemeChange = { userViewModel.updateThemePreference(currentLanguage, it) },
          onUsernameTyped = { userViewModel.onUsernameInputChanged(it) },
          onCheckAvailability = { userViewModel.checkUsernameAvailability(it) },
          onSubmitOnboarding = { displayName, username, bio ->
            userViewModel.completeOnboarding(
              displayName = displayName,
              rawUsername = username,
              bio = bio,
              language = currentLanguage,
              themeMode = currentThemeMode
            )
          },
          onSignOut = onSignOut
        )
      } else {
        MainShellScreen(
          profile = profile,
          isOnline = isOnline,
          isEmailVerified = isEmailVerified,
          currentLanguage = currentLanguage,
          currentThemeMode = currentThemeMode,
          statusBannerMessage = statusBannerMessage,
          onClearStatusBanner = { userViewModel.clearStatusBanner() },
          onLanguageChange = { userViewModel.updateLanguagePreference(it, currentThemeMode) },
          onThemeChange = { userViewModel.updateThemePreference(currentLanguage, it) },
          onUpdateProfile = { name, bio, onDone ->
            userViewModel.updateDisplayNameAndBio(name, bio, currentLanguage, onDone)
          },
          onRefreshEmailVerified = { userViewModel.refreshEmailVerification(currentLanguage) },
          onSendVerificationEmail = { userViewModel.sendVerificationEmail(currentLanguage) },
          onSignOut = onSignOut
        )
      }
    }
  }
}
