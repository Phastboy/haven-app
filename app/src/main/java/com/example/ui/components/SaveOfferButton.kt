package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HavenColors

/**
 * Card/detail presets for [SaveOfferButton].
 *
 * Visible container and icon sizes are intentionally separate from the
 * accessible touch target: every button keeps at least [MinTouchTarget]
 * regardless of how large the visible circle is.
 */
object SaveOfferButtonDefaults {
  /** Feed, provider and saved-card preset. */
  val CardContainerSize = 28.dp
  val CardIconSize = 16.dp

  /** Offer-detail gallery preset (a little larger over the 4:3 hero image). */
  val DetailContainerSize = 44.dp
  val DetailIconSize = 22.dp

  /** Accessible touch target shared by every save button. */
  val MinTouchTarget = 48.dp

  /** Opacity of the translucent "glass" scrim drawn behind the heart. */
  const val GlassAlpha = 0.85f
}

/**
 * The single save/unsave control used by every Haven surface: feed, provider
 * and saved cards plus offer detail.
 *
 * Styling is centralized and theme-aware. The outlined heart sits on a
 * translucent canvas scrim, and both the saved fill and unsaved outline use
 * semantic Haven tokens, so the control stays readable over light, dark and
 * busy cover images in either theme. No platform backdrop blur is assumed
 * (minSdk 24); the translucent scrim itself provides the contrast.
 *
 * Callers keep product policy (authentication, network writes, rollback and
 * ownership) in their own screen/state layers and only supply the current
 * confirmed/optimistic [saved] state, the [saving]/[enabled] presentation and
 * the [onClick] action. Visible [containerSize]/[iconSize] are configurable
 * while the touch target stays at least 48dp.
 */
@Composable
fun SaveOfferButton(
  saved: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  saving: Boolean = false,
  enabled: Boolean = true,
  containerSize: Dp = SaveOfferButtonDefaults.CardContainerSize,
  iconSize: Dp = SaveOfferButtonDefaults.CardIconSize,
  testTag: String? = null
) {
  val interactive = enabled && !saving
  val label = when {
    saving -> "Saving offer"
    saved -> "Unsave offer"
    else -> "Save offer"
  }
  Box(
    modifier = modifier
      .sizeIn(
        minWidth = SaveOfferButtonDefaults.MinTouchTarget,
        minHeight = SaveOfferButtonDefaults.MinTouchTarget
      )
      .alpha(if (interactive) 1f else 0.55f)
      .clickable(enabled = interactive, role = Role.Button, onClick = onClick)
      .semantics { contentDescription = label }
      .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .size(containerSize)
        .clip(CircleShape)
        .background(HavenColors.Canvas.copy(alpha = SaveOfferButtonDefaults.GlassAlpha))
        .then(if (testTag != null) Modifier.testTag("${testTag}_glass") else Modifier),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = if (saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        contentDescription = null,
        tint = if (saved) HavenColors.Primary else HavenColors.TextPrimary,
        modifier = Modifier.size(iconSize)
      )
    }
  }
}
