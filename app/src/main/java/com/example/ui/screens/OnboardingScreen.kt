package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.i18n.LocalAppStrings
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.AuthUserSummary
import com.example.model.UsernameValidationResult
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LanguageAndThemeQuickBar
import com.example.ui.components.OfflineStateBanner
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.UsernameCheckState

@Composable
fun OnboardingScreen(
  authUser: AuthUserSummary,
  isOnline: Boolean,
  currentLanguage: AppLanguage,
  currentThemeMode: AppThemeMode,
  usernameCheckState: UsernameCheckState,
  isSubmitting: Boolean,
  errorMessage: String?,
  onLanguageChange: (AppLanguage) -> Unit,
  onThemeChange: (AppThemeMode) -> Unit,
  onUsernameTyped: (String) -> Unit,
  onCheckAvailability: (String) -> Unit,
  onSubmitOnboarding: (displayName: String, username: String, bio: String) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier
) {
  val strings = LocalAppStrings.current

  var displayName by rememberSaveable {
    mutableStateOf(authUser.displayName.ifBlank { "" })
  }
  var username by rememberSaveable {
    val suggested = authUser.email.substringBefore("@")
      .lowercase()
      .replace(Regex("[^a-z0-9_]"), "_")
      .take(20)
    mutableStateOf(if (suggested.length >= 3) suggested else "")
  }
  var bio by rememberSaveable { mutableStateOf("") }

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = modifier
      .fillMaxSize()
      .testTag("onboarding_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      OfflineStateBanner(isOnline = isOnline)

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 560.dp)
          .padding(20.dp)
      ) {
        LanguageAndThemeQuickBar(
          currentLanguage = currentLanguage,
          currentThemeMode = currentThemeMode,
          onLanguageChange = onLanguageChange,
          onThemeChange = onThemeChange
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(22.dp)) {
            Text(
              text = strings.onboardingTitle,
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = strings.onboardingSubtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Full Name Input
            OutlinedTextField(
              value = displayName,
              onValueChange = { if (it.length <= 60) displayName = it },
              label = { Text(strings.displayNameLabel) },
              placeholder = { Text(strings.displayNamePlaceholder) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null
                )
              },
              singleLine = true,
              supportingText = {
                Text("${displayName.trim().length}/60")
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_display_name_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Unique Username Input + Availability Check Button
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.Top
            ) {
              OutlinedTextField(
                value = username,
                onValueChange = { raw ->
                  val filtered = raw.lowercase().take(24)
                  username = filtered
                  onUsernameTyped(filtered)
                },
                label = { Text(strings.usernameLabel) },
                placeholder = { Text(strings.usernamePlaceholder) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.AlternateEmail,
                    contentDescription = null
                  )
                },
                singleLine = true,
                supportingText = {
                  Text("${username.trim().length}/24 • ${strings.usernameHelper}")
                },
                modifier = Modifier
                  .weight(1f)
                  .testTag("onboarding_username_input")
              )

              Spacer(modifier = Modifier.width(10.dp))

              OutlinedButton(
                onClick = { onCheckAvailability(username) },
                enabled = isOnline && username.trim().length >= 3 &&
                  usernameCheckState != UsernameCheckState.Checking,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .padding(top = 8.dp)
                  .height(54.dp)
                  .testTag("check_username_button")
              ) {
                if (usernameCheckState == UsernameCheckState.Checking) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                  )
                } else {
                  Text(strings.checkAvailabilityBtn)
                }
              }
            }

            // Live Username Validation & Availability Feedback
            when (usernameCheckState) {
              UsernameCheckState.Idle, UsernameCheckState.Checking -> Unit
              UsernameCheckState.Available -> {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("username_available_badge")
                ) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = strings.usernameAvailableMsg,
                    color = SuccessGreen,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              UsernameCheckState.Taken -> {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(top = 4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = strings.usernameTakenMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              is UsernameCheckState.InvalidFormat -> {
                val msg = when (usernameCheckState.reason) {
                  UsernameValidationResult.TooShort -> strings.usernameTooShortMsg
                  UsernameValidationResult.TooLong -> strings.usernameTooLongMsg
                  UsernameValidationResult.InvalidCharacters -> strings.usernameInvalidCharsMsg
                  UsernameValidationResult.ReservedUsername -> strings.usernameReservedMsg
                  UsernameValidationResult.Valid -> ""
                }
                Text(
                  text = msg,
                  color = MaterialTheme.colorScheme.error,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.padding(top = 4.dp)
                )
              }

              is UsernameCheckState.Error -> {
                Text(
                  text = usernameCheckState.message,
                  color = MaterialTheme.colorScheme.error,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.padding(top = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Optional Bio Input
            OutlinedTextField(
              value = bio,
              onValueChange = { if (it.length <= 160) bio = it },
              label = { Text(strings.bioLabel) },
              placeholder = { Text(strings.bioPlaceholder) },
              minLines = 2,
              maxLines = 4,
              supportingText = {
                Text("${bio.trim().length}/160")
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_bio_input")
            )

            if (errorMessage != null) {
              Spacer(modifier = Modifier.height(10.dp))
              ErrorStateCard(message = errorMessage)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
              onClick = {
                onSubmitOnboarding(displayName, username, bio)
              },
              enabled = isOnline &&
                !isSubmitting &&
                displayName.trim().isNotEmpty() &&
                username.trim().length in 3..24,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("complete_onboarding_button")
            ) {
              if (isSubmitting) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(strings.savingProfile)
              } else {
                Text(
                  text = strings.completeOnboardingBtn,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center
        ) {
          TextButton(
            onClick = onSignOut,
            modifier = Modifier.testTag("onboarding_sign_out_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.signOutBtn)
          }
        }
      }
    }
  }
}
