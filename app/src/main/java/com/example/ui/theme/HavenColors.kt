package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Haven Monastic Greyscale Design System Colors.
 * Strictly adheres to pure black-and-white visual weight:
 * #FFFFFF canvas, #111827 near-black actions, #E5E7EB / #D1D5DB dividers,
 * and #6B7280 muted labels. Zero red or decorative color.
 */
@Immutable
object HavenColors {
  val Canvas = Color(0xFFFFFFFF)
  val Primary = Color(0xFF111827)
  val TextPrimary = Color(0xFF111827)
  val TextSecondary = Color(0xFF6B7280)
  val TextMuted = Color(0xFF9CA3AF)
  val TextOnDark = Color(0xFFFFFFFF)

  val BorderSubtle = Color(0xFFE5E7EB)
  val BorderDefault = Color(0xFFD1D5DB)
  val BorderStrong = Color(0xFF111827)

  val SurfaceCard = Color(0xFFFFFFFF)
  val SurfaceContainer = Color(0xFFF9FAFB)
  val SurfaceDisabled = Color(0xFFF9FAFB)

  val ButtonDisabled = Color(0xFF9CA3AF)
  val ButtonDisabledText = Color(0xFFFFFFFF)

  // Status bar and overlay
  val StatusBarText = Color(0xFF111827)
  val HomeIndicator = Color(0xFF111827)
}
