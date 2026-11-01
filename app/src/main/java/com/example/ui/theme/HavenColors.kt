package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Palette contract supporting both Light and Dark monastic modes.
 */
@Immutable
data class HavenColorPalette(
  val canvas: Color,
  val primary: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val textMuted: Color,
  val textOnDark: Color,
  val borderSubtle: Color,
  val borderDefault: Color,
  val borderStrong: Color,
  val surfaceCard: Color,
  val surfaceContainer: Color,
  val surfaceDisabled: Color,
  val buttonDisabled: Color,
  val buttonDisabledText: Color,
  val statusBarText: Color,
  val homeIndicator: Color,
  val snackbarBackground: Color,
  val snackbarText: Color,
  val isDark: Boolean
)

/**
 * Light Haven Monastic Theme Palette:
 * Strictly adheres to pure black-and-white visual weight:
 * #FFFFFF canvas, #111827 near-black actions, #E5E7EB / #D1D5DB dividers,
 * and #6B7280 muted labels. Zero red or decorative color.
 */
val HavenLightPalette = HavenColorPalette(
  canvas = Color(0xFFFFFFFF),
  primary = Color(0xFF111827),
  textPrimary = Color(0xFF111827),
  textSecondary = Color(0xFF6B7280),
  textMuted = Color(0xFF9CA3AF),
  textOnDark = Color(0xFFFFFFFF),
  borderSubtle = Color(0xFFE5E7EB),
  borderDefault = Color(0xFFD1D5DB),
  borderStrong = Color(0xFF111827),
  surfaceCard = Color(0xFFFFFFFF),
  surfaceContainer = Color(0xFFF9FAFB),
  surfaceDisabled = Color(0xFFF9FAFB),
  buttonDisabled = Color(0xFF9CA3AF),
  buttonDisabledText = Color(0xFFFFFFFF),
  statusBarText = Color(0xFF111827),
  homeIndicator = Color(0xFF111827),
  snackbarBackground = Color(0xFF111827),
  snackbarText = Color(0xFFFFFFFF),
  isDark = false
)

/**
 * Dark Haven Monastic Theme Palette:
 * Deep near-black canvas, high-contrast light typography, subtle dark borders.
 */
val HavenDarkPalette = HavenColorPalette(
  canvas = Color(0xFF121316),
  primary = Color(0xFFFFFFFF),
  textPrimary = Color(0xFFF3F4F6),
  textSecondary = Color(0xFF9CA3AF),
  textMuted = Color(0xFF6B7280),
  textOnDark = Color(0xFF111827),
  borderSubtle = Color(0xFF2E3038),
  borderDefault = Color(0xFF40424D),
  borderStrong = Color(0xFFFFFFFF),
  surfaceCard = Color(0xFF1C1D22),
  surfaceContainer = Color(0xFF23252A),
  surfaceDisabled = Color(0xFF23252A),
  buttonDisabled = Color(0xFF374151),
  buttonDisabledText = Color(0xFF6B7280),
  statusBarText = Color(0xFFF3F4F6),
  homeIndicator = Color(0xFFE5E7EB),
  snackbarBackground = Color(0xFF2E3038),
  snackbarText = Color(0xFFF3F4F6),
  isDark = true
)

val LocalHavenColors = staticCompositionLocalOf { HavenLightPalette }

/**
 * Dynamic access to the active Haven color palette.
 * Recomposes cleanly whenever the user toggles between Light, Dark, and System themes.
 */
object HavenColors {
  val Canvas: Color
    @Composable
    get() = LocalHavenColors.current.canvas

  val Primary: Color
    @Composable
    get() = LocalHavenColors.current.primary

  val TextPrimary: Color
    @Composable
    get() = LocalHavenColors.current.textPrimary

  val TextSecondary: Color
    @Composable
    get() = LocalHavenColors.current.textSecondary

  val TextMuted: Color
    @Composable
    get() = LocalHavenColors.current.textMuted

  val TextOnDark: Color
    @Composable
    get() = LocalHavenColors.current.textOnDark

  val BorderSubtle: Color
    @Composable
    get() = LocalHavenColors.current.borderSubtle

  val BorderDefault: Color
    @Composable
    get() = LocalHavenColors.current.borderDefault

  val BorderStrong: Color
    @Composable
    get() = LocalHavenColors.current.borderStrong

  val SurfaceCard: Color
    @Composable
    get() = LocalHavenColors.current.surfaceCard

  val SurfaceContainer: Color
    @Composable
    get() = LocalHavenColors.current.surfaceContainer

  val SurfaceDisabled: Color
    @Composable
    get() = LocalHavenColors.current.surfaceDisabled

  val ButtonDisabled: Color
    @Composable
    get() = LocalHavenColors.current.buttonDisabled

  val ButtonDisabledText: Color
    @Composable
    get() = LocalHavenColors.current.buttonDisabledText

  val StatusBarText: Color
    @Composable
    get() = LocalHavenColors.current.statusBarText

  val HomeIndicator: Color
    @Composable
    get() = LocalHavenColors.current.homeIndicator

  val SnackbarBackground: Color
    @Composable
    get() = LocalHavenColors.current.snackbarBackground

  val SnackbarText: Color
    @Composable
    get() = LocalHavenColors.current.snackbarText

  val isDark: Boolean
    @Composable
    get() = LocalHavenColors.current.isDark
}
