package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HavenColors

/**
 * Reusable Save/Favorite Button for Haven offers:
 * - Circular pill with 2dp shadow and subtle border (matching the 32x32dp design specification)
 * - Tactile spring scale feedback when tapped (active:scale-95)
 * - Filled heart icon when saved; outlined heart icon when unsaved
 * - Monastic styling adhering strictly to Haven's greyscale design language
 * - Reusable across OfferCard (offers feed), SavedOffersScreen, OfferDetailScreen, and anywhere an offer is saved
 */
@Composable
fun HavenSaveButton(
  isSaved: Boolean,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 32.dp,
  iconSize: Dp = 16.dp,
  elevation: Dp = 2.dp,
  backgroundColor: Color = HavenColors.Canvas.copy(alpha = 0.95f),
  contentDescription: String = if (isSaved) "Remove from saved" else "Save offer",
  testTag: String = "save_offer_button"
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.92f else 1f,
    animationSpec = spring(),
    label = "save_button_scale"
  )

  Box(
    modifier = modifier
      .size(size)
      .scale(scale)
      .shadow(elevation = elevation, shape = CircleShape)
      .clip(CircleShape)
      .background(backgroundColor)
      .border(0.5.dp, HavenColors.BorderSubtle.copy(alpha = 0.6f), CircleShape)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        role = Role.Button,
        onClick = onToggle
      )
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
      contentDescription = contentDescription,
      tint = HavenColors.TextPrimary,
      modifier = Modifier.size(iconSize)
    )
  }
}
