package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HavenColors

enum class HavenBackButtonVariant {
  STANDARD, // Top bar style (48x48dp touch area, clean icon)
  FLOATING  // Floating circular pill (40x40dp pill, flat fill, spring animation)
}

/**
 * Reusable Back Navigation Button for Haven:
 * - STANDARD: Clean 48x48dp touch target for top app bars across all child screens
 * - FLOATING: 40x40dp flat circular pill with spring press feedback
 * - Monastic greyscale styling ensuring visual harmony across all child and detail screens
 */
@Composable
fun HavenBackButton(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  variant: HavenBackButtonVariant = HavenBackButtonVariant.STANDARD,
  size: Dp = if (variant == HavenBackButtonVariant.FLOATING) 40.dp else 48.dp,
  iconSize: Dp = if (variant == HavenBackButtonVariant.FLOATING) 20.dp else 24.dp,
  icon: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack,
  backgroundColor: Color = if (variant == HavenBackButtonVariant.FLOATING) HavenColors.Canvas.copy(alpha = 0.95f) else Color.Transparent,
  tint: Color = if (enabled) HavenColors.TextPrimary else HavenColors.TextMuted,
  contentDescription: String = "Navigate back",
  testTag: String = "back_button"
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed && enabled) 0.92f else 1f,
    animationSpec = spring(),
    label = "back_button_scale"
  )

  if (variant == HavenBackButtonVariant.FLOATING) {
    Box(
      modifier = modifier
        .size(size)
        .scale(scale)
        .clip(CircleShape)
        .background(backgroundColor)
        .clickable(
          enabled = enabled,
          interactionSource = interactionSource,
          indication = LocalIndication.current,
          role = Role.Button,
          onClick = onBack
        )
        .testTag(testTag),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(iconSize)
      )
    }
  } else {
    IconButton(
      onClick = onBack,
      enabled = enabled,
      modifier = modifier
        .size(size)
        .testTag(testTag)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(iconSize)
      )
    }
  }
}
