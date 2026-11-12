package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.i18n.LocalAppStrings
import com.example.model.AppLanguage
import com.example.model.AppThemeMode

@Composable
fun OfflineStateBanner(
  isOnline: Boolean,
  modifier: Modifier = Modifier
) {
  val strings = LocalAppStrings.current
  AnimatedVisibility(visible = !isOnline, modifier = modifier) {
    Surface(
      color = MaterialTheme.colorScheme.errorContainer,
      contentColor = MaterialTheme.colorScheme.onErrorContainer,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("offline_state_banner")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.CloudOff,
          contentDescription = strings.offlineBannerText,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = strings.offlineBannerText,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}

@Composable
fun LoadingStateCard(
  message: String,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp)
      .testTag("loading_state_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      CircularProgressIndicator(
        modifier = Modifier.size(40.dp),
        color = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun ErrorStateCard(
  message: String,
  onRetry: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val strings = LocalAppStrings.current
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp)
      .testTag("error_state_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        imageVector = Icons.Default.ErrorOutline,
        contentDescription = message,
        tint = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.size(36.dp)
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onErrorContainer,
        textAlign = TextAlign.Center
      )
      if (onRetry != null) {
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = onRetry,
          modifier = Modifier.testTag("error_retry_button")
        ) {
          Text(strings.retryBtn)
        }
      }
    }
  }
}

@Composable
fun EmptyStateCard(
  title: String,
  subtitle: String,
  badgeText: String? = null,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("empty_state_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (badgeText != null) {
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          shape = RoundedCornerShape(50)
        ) {
          Text(
            text = badgeText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
          )
        }
        Spacer(modifier = Modifier.height(16.dp))
      }
      Icon(
        imageVector = Icons.Default.Inbox,
        contentDescription = title,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(48.dp)
      )
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun LanguageAndThemeQuickBar(
  currentLanguage: AppLanguage,
  currentThemeMode: AppThemeMode,
  onLanguageChange: (AppLanguage) -> Unit,
  onThemeChange: (AppThemeMode) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    FilterChip(
      selected = true,
      onClick = {
        val next = if (currentLanguage == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN
        onLanguageChange(next)
      },
      label = {
        Text(
          text = if (currentLanguage == AppLanguage.BN) "বাংলা • EN" else "English • বাং",
          style = MaterialTheme.typography.labelLarge
        )
      },
      leadingIcon = {
        Icon(
          imageVector = Icons.Default.Language,
          contentDescription = "Switch Language",
          modifier = Modifier.size(18.dp)
        )
      },
      modifier = Modifier.testTag("quick_language_toggle")
    )

    FilterChip(
      selected = true,
      onClick = {
        val next = when (currentThemeMode) {
          AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
          AppThemeMode.LIGHT -> AppThemeMode.DARK
          AppThemeMode.DARK -> AppThemeMode.SYSTEM
        }
        onThemeChange(next)
      },
      label = {
        val label = when (currentThemeMode) {
          AppThemeMode.SYSTEM -> "Auto"
          AppThemeMode.LIGHT -> "Light"
          AppThemeMode.DARK -> "Dark"
        }
        Text(text = label, style = MaterialTheme.typography.labelLarge)
      },
      leadingIcon = {
        val icon = when (currentThemeMode) {
          AppThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
          AppThemeMode.LIGHT -> Icons.Default.LightMode
          AppThemeMode.DARK -> Icons.Default.DarkMode
        }
        Icon(
          imageVector = icon,
          contentDescription = "Switch Theme",
          modifier = Modifier.size(18.dp)
        )
      },
      modifier = Modifier.testTag("quick_theme_toggle")
    )
  }
}
