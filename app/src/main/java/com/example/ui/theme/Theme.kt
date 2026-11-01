package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val HavenLightColorScheme = lightColorScheme(
  primary = HavenLightPalette.primary,
  onPrimary = HavenLightPalette.textOnDark,
  primaryContainer = HavenLightPalette.primary,
  onPrimaryContainer = HavenLightPalette.textOnDark,
  secondary = HavenLightPalette.textSecondary,
  onSecondary = HavenLightPalette.canvas,
  background = HavenLightPalette.canvas,
  onBackground = HavenLightPalette.textPrimary,
  surface = HavenLightPalette.canvas,
  onSurface = HavenLightPalette.textPrimary,
  surfaceVariant = HavenLightPalette.surfaceContainer,
  onSurfaceVariant = HavenLightPalette.textSecondary,
  outline = HavenLightPalette.borderDefault,
  outlineVariant = HavenLightPalette.borderSubtle,
  error = HavenLightPalette.primary, // Strict monastic design: no decorative red
  onError = HavenLightPalette.textOnDark
)

val HavenDarkColorScheme = darkColorScheme(
  primary = HavenDarkPalette.primary,
  onPrimary = HavenDarkPalette.textOnDark,
  primaryContainer = HavenDarkPalette.primary,
  onPrimaryContainer = HavenDarkPalette.textOnDark,
  secondary = HavenDarkPalette.textSecondary,
  onSecondary = HavenDarkPalette.canvas,
  background = HavenDarkPalette.canvas,
  onBackground = HavenDarkPalette.textPrimary,
  surface = HavenDarkPalette.canvas,
  onSurface = HavenDarkPalette.textPrimary,
  surfaceVariant = HavenDarkPalette.surfaceContainer,
  onSurfaceVariant = HavenDarkPalette.textSecondary,
  outline = HavenDarkPalette.borderDefault,
  outlineVariant = HavenDarkPalette.borderSubtle,
  error = HavenDarkPalette.primary,
  onError = HavenDarkPalette.textOnDark
)

@Composable
fun HavenTheme(
  themeMode: ThemeMode = ThemeManager.themeMode,
  content: @Composable () -> Unit
) {
  val systemDark = isSystemInDarkTheme()
  val darkTheme = when (themeMode) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
  }

  val palette = if (darkTheme) HavenDarkPalette else HavenLightPalette
  val colorScheme = if (darkTheme) HavenDarkColorScheme else HavenLightColorScheme

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !darkTheme
        insetsController.isAppearanceLightNavigationBars = !darkTheme
      }
    }
  }

  CompositionLocalProvider(
    LocalHavenColors provides palette
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = HavenTypography,
      content = content
    )
  }
}

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  HavenTheme(content = content)
}
