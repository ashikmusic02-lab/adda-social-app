package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.i18n.LocalAppStrings
import com.example.i18n.stringsForLanguage
import com.example.model.AppLanguage
import com.example.model.AppThemeMode

private val DarkColorScheme = darkColorScheme(
  primary = IndigoPrimaryDark,
  onPrimary = OnIndigoPrimaryDark,
  primaryContainer = IndigoContainerDark,
  onPrimaryContainer = OnIndigoContainerDark,
  secondary = CoralSecondaryDark,
  onSecondary = OnCoralSecondaryDark,
  secondaryContainer = CoralContainerDark,
  onSecondaryContainer = OnCoralContainerDark,
  tertiary = TealTertiaryDark,
  onTertiary = OnTealTertiaryDark,
  tertiaryContainer = TealContainerDark,
  onTertiaryContainer = OnTealContainerDark,
  background = BackgroundDark,
  surface = SurfaceDark,
  surfaceVariant = SurfaceVariantDark,
  onBackground = OnSurfaceDark,
  onSurface = OnSurfaceDark,
  onSurfaceVariant = OnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
  primary = IndigoPrimaryLight,
  onPrimary = OnIndigoPrimaryLight,
  primaryContainer = IndigoContainerLight,
  onPrimaryContainer = OnIndigoContainerLight,
  secondary = CoralSecondaryLight,
  onSecondary = OnCoralSecondaryLight,
  secondaryContainer = CoralContainerLight,
  onSecondaryContainer = OnCoralContainerLight,
  tertiary = TealTertiaryLight,
  onTertiary = OnTealTertiaryLight,
  tertiaryContainer = TealContainerLight,
  onTertiaryContainer = OnTealContainerLight,
  background = BackgroundLight,
  surface = SurfaceLight,
  surfaceVariant = SurfaceVariantLight,
  onBackground = OnSurfaceLight,
  onSurface = OnSurfaceLight,
  onSurfaceVariant = OnSurfaceVariantLight
)

@Composable
fun AddaTheme(
  themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  language: AppLanguage = AppLanguage.BN,
  content: @Composable () -> Unit
) {
  val darkTheme = when (themeMode) {
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    AppThemeMode.DARK -> true
    AppThemeMode.LIGHT -> false
  }

  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val strings = stringsForLanguage(language)

  CompositionLocalProvider(LocalAppStrings provides strings) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = appTypography(isBengali = language == AppLanguage.BN),
      content = content
    )
  }
}
