package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.i18n.LocalAppStrings
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LanguageAndThemeQuickBar
import com.example.ui.components.OfflineStateBanner
import com.example.ui.theme.SuccessGreen

enum class EmailAuthFormMode {
  LOGIN,
  REGISTER,
  RESET_PASSWORD
}

@Composable
fun SplashScreenView(
  isOnline: Boolean,
  modifier: Modifier = Modifier
) {
  val strings = LocalAppStrings.current
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.background
          )
        )
      )
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .testTag("splash_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(24.dp)
    ) {
      OfflineStateBanner(isOnline = isOnline)
      Spacer(modifier = Modifier.height(24.dp))
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 8.dp,
        modifier = Modifier.size(96.dp)
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_app_icon_fg),
          contentDescription = strings.appName,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }
      Spacer(modifier = Modifier.height(20.dp))
      Text(
        text = strings.appName,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onBackground
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = strings.tagline,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(28.dp))
      CircularProgressIndicator(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp)
      )
    }
  }
}

@Composable
fun WelcomeAuthScreen(
  isOnline: Boolean,
  isSigningIn: Boolean,
  errorMessage: String?,
  infoMessage: String?,
  currentLanguage: AppLanguage,
  currentThemeMode: AppThemeMode,
  onLanguageChange: (AppLanguage) -> Unit,
  onThemeChange: (AppThemeMode) -> Unit,
  onEmailLoginClick: (email: String, password: String) -> Unit,
  onEmailRegisterClick: (email: String, password: String, confirmPassword: String) -> Unit,
  onPasswordResetClick: (email: String) -> Unit,
  onGoogleSignInClick: () -> Unit,
  onAttemptAutoSignIn: () -> Unit,
  onRetryClearError: () -> Unit,
  modifier: Modifier = Modifier
) {
  val strings = LocalAppStrings.current
  var showSetupGuide by rememberSaveable { mutableStateOf(false) }
  var formMode by rememberSaveable { mutableStateOf(EmailAuthFormMode.LOGIN) }

  var email by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }
  var confirmPassword by rememberSaveable { mutableStateOf("") }
  var showPassword by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    onAttemptAutoSignIn()
  }

  if (showSetupGuide) {
    SetupAndArchitectureScreen(
      onBack = { showSetupGuide = false }
    )
    return
  }

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = modifier
      .fillMaxSize()
      .testTag("welcome_screen")
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
          .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        LanguageAndThemeQuickBar(
          currentLanguage = currentLanguage,
          currentThemeMode = currentThemeMode,
          onLanguageChange = onLanguageChange,
          onThemeChange = onThemeChange
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Phase 1 Pill Badge
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = strings.phaseBadge,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hero Banner Card
        Card(
          shape = RoundedCornerShape(24.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_welcome_hero),
            contentDescription = strings.welcomeTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxWidth()
              .height(160.dp)
              .clip(RoundedCornerShape(24.dp))
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = strings.welcomeTitle,
          style = MaterialTheme.typography.headlineMedium,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = strings.welcomeSubtitle,
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
          ErrorStateCard(
            message = errorMessage,
            onRetry = onRetryClearError
          )
          Spacer(modifier = Modifier.height(12.dp))
        }

        if (infoMessage != null) {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
              containerColor = SuccessGreen.copy(alpha = 0.15f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("auth_info_banner")
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = infoMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
        }

        // Email / Password Authentication Card (Login, Register, Reset Password)
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("email_password_auth_card")
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            // Mode Selector Chips
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              FilterChip(
                selected = formMode == EmailAuthFormMode.LOGIN,
                onClick = {
                  formMode = EmailAuthFormMode.LOGIN
                  onRetryClearError()
                },
                label = { Text(strings.tabLogin) },
                modifier = Modifier.testTag("mode_tab_login")
              )
              FilterChip(
                selected = formMode == EmailAuthFormMode.REGISTER,
                onClick = {
                  formMode = EmailAuthFormMode.REGISTER
                  onRetryClearError()
                },
                label = { Text(strings.tabRegister) },
                modifier = Modifier.testTag("mode_tab_register")
              )
              FilterChip(
                selected = formMode == EmailAuthFormMode.RESET_PASSWORD,
                onClick = {
                  formMode = EmailAuthFormMode.RESET_PASSWORD
                  onRetryClearError()
                },
                label = { Text(strings.tabResetPassword) },
                modifier = Modifier.testTag("mode_tab_reset")
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Email Field
            OutlinedTextField(
              value = email,
              onValueChange = { email = it },
              label = { Text(strings.emailLabel) },
              placeholder = { Text(strings.emailPlaceholder) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Email,
                  contentDescription = null
                )
              },
              singleLine = true,
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_email_input")
            )

            if (formMode != EmailAuthFormMode.RESET_PASSWORD) {
              Spacer(modifier = Modifier.height(12.dp))

              // Password Field
              OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(strings.passwordLabel) },
                placeholder = { Text(strings.passwordPlaceholder) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null
                  )
                },
                trailingIcon = {
                  IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                      imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                      contentDescription = "Toggle password visibility"
                    )
                  }
                },
                singleLine = true,
                visualTransformation = if (showPassword) {
                  VisualTransformation.None
                } else {
                  PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                  keyboardType = KeyboardType.Password,
                  imeAction = if (formMode == EmailAuthFormMode.REGISTER) ImeAction.Next else ImeAction.Done
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("auth_password_input")
              )
            }

            if (formMode == EmailAuthFormMode.REGISTER) {
              Spacer(modifier = Modifier.height(12.dp))

              OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(strings.confirmPasswordLabel) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null
                  )
                },
                singleLine = true,
                visualTransformation = if (showPassword) {
                  VisualTransformation.None
                } else {
                  PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                  keyboardType = KeyboardType.Password,
                  imeAction = ImeAction.Done
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("auth_confirm_password_input")
              )
            }

            if (formMode == EmailAuthFormMode.LOGIN) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
              ) {
                TextButton(
                  onClick = {
                    formMode = EmailAuthFormMode.RESET_PASSWORD
                    onRetryClearError()
                  },
                  modifier = Modifier.testTag("forgot_password_button")
                ) {
                  Text(strings.forgotPasswordLink)
                }
              }
            } else {
              Spacer(modifier = Modifier.height(16.dp))
            }

            // Primary Email Action Button
            Button(
              onClick = {
                when (formMode) {
                  EmailAuthFormMode.LOGIN -> onEmailLoginClick(email, password)
                  EmailAuthFormMode.REGISTER -> onEmailRegisterClick(email, password, confirmPassword)
                  EmailAuthFormMode.RESET_PASSWORD -> onPasswordResetClick(email)
                }
              },
              enabled = !isSigningIn && isOnline,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag(
                  when (formMode) {
                    EmailAuthFormMode.LOGIN -> "email_login_button"
                    EmailAuthFormMode.REGISTER -> "email_register_button"
                    EmailAuthFormMode.RESET_PASSWORD -> "email_reset_password_button"
                  }
                )
            ) {
              if (isSigningIn) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(strings.signingIn)
              } else {
                val btnText = when (formMode) {
                  EmailAuthFormMode.LOGIN -> strings.loginBtn
                  EmailAuthFormMode.REGISTER -> strings.createAccountBtn
                  EmailAuthFormMode.RESET_PASSWORD -> strings.sendResetEmailBtn
                }
                Text(
                  text = btnText,
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
          verticalAlignment = Alignment.CenterVertically
        ) {
          HorizontalDivider(modifier = Modifier.weight(1f))
          Text(
            text = strings.orDividerText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
          )
          HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign-In Button via AuthService -> CredentialManager
        OutlinedButton(
          onClick = onGoogleSignInClick,
          enabled = !isSigningIn && isOnline,
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("google_sign_in_button")
        ) {
          Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = strings.continueWithGoogle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        TextButton(
          onClick = { showSetupGuide = true },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("open_setup_guide_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = strings.viewSetupGuideBtn,
            style = MaterialTheme.typography.labelLarge
          )
        }
      }
    }
  }
}
