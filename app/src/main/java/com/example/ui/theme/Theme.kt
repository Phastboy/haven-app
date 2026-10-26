package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HavenColorScheme = lightColorScheme(
  primary = HavenColors.Primary,
  onPrimary = HavenColors.TextOnDark,
  primaryContainer = HavenColors.Primary,
  onPrimaryContainer = HavenColors.TextOnDark,
  secondary = HavenColors.TextSecondary,
  onSecondary = HavenColors.Canvas,
  background = HavenColors.Canvas,
  onBackground = HavenColors.TextPrimary,
  surface = HavenColors.Canvas,
  onSurface = HavenColors.TextPrimary,
  surfaceVariant = HavenColors.SurfaceContainer,
  onSurfaceVariant = HavenColors.TextSecondary,
  outline = HavenColors.BorderDefault,
  outlineVariant = HavenColors.BorderSubtle,
  error = HavenColors.Primary, // Strict monastic design: no decorative red
  onError = HavenColors.TextOnDark
)

@Composable
fun HavenTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = HavenColorScheme,
    typography = HavenTypography,
    content = content
  )
}

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  HavenTheme(content = content)
}
