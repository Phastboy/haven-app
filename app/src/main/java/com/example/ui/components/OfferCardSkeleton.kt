package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HavenColors

/**
 * Animated Shimmer Modifier:
 * Renders a smooth, infinite grey gradient animation for loading states.
 * Automatically adapts between Light (#F3F4F6 -> #E5E7EB) and Dark (#23252A -> #32353E) themes.
 */
@Composable
fun Modifier.shimmerBackground(
  shape: Shape = RoundedCornerShape(8.dp),
  baseColor: Color? = null,
  highlightColor: Color? = null
): Modifier {
  val isDark = HavenColors.isDark
  val resolvedBase = baseColor ?: if (isDark) Color(0xFF22242B) else Color(0xFFF3F4F6)
  val resolvedHighlight = highlightColor ?: if (isDark) Color(0xFF2F323B) else Color(0xFFE5E7EB)

  val transition = rememberInfiniteTransition(label = "shimmer_transition")
  val translateAnim by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1000f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1100, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "shimmer_translate"
  )

  val brush = Brush.linearGradient(
    colors = listOf(
      resolvedBase,
      resolvedHighlight,
      resolvedBase
    ),
    start = Offset(translateAnim - 400f, translateAnim - 400f),
    end = Offset(translateAnim, translateAnim)
  )

  return this
    .clip(shape)
    .background(brush)
}

/**
 * Reusable OfferCardSkeleton component mimicking the layout of the current OfferCard:
 * - 1:1 Aspect ratio square image container with 12dp rounded corners and subtle border
 * - Top-right circular 28x28dp favorite button placeholder
 * - 8dp spacer
 * - Creator row (14dp circle avatar + 68x10dp author name bar)
 * - 2dp spacer
 * - Offer title bar placeholder (85% width, 12dp height)
 * - 2dp spacer
 * - Price placeholder bar (56x13dp)
 *
 * Uses animated shimmer to clearly indicate loading states in both light and dark themes.
 */
@Composable
fun OfferCardSkeleton(
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .testTag("offer_card_skeleton")
  ) {
    // 1:1 Aspect Ratio Image Placeholder with corner radius 12dp & border
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(RoundedCornerShape(12.dp))
        .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
        .shimmerBackground(RoundedCornerShape(12.dp))
    ) {
      // Top-right circular favorite button skeleton (28x28dp)
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(8.dp)
          .size(28.dp)
          .clip(CircleShape)
          .shimmerBackground(CircleShape)
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Creator Row: 14dp circle + author name bar
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Box(
        modifier = Modifier
          .size(14.dp)
          .clip(CircleShape)
          .shimmerBackground(CircleShape)
      )
      Box(
        modifier = Modifier
          .size(width = 68.dp, height = 10.dp)
          .clip(RoundedCornerShape(4.dp))
          .shimmerBackground(RoundedCornerShape(4.dp))
      )
    }

    Spacer(modifier = Modifier.height(2.dp))

    // Title line placeholder (mimics 13sp medium title)
    Box(
      modifier = Modifier
        .fillMaxWidth(0.85f)
        .height(12.dp)
        .clip(RoundedCornerShape(4.dp))
        .shimmerBackground(RoundedCornerShape(4.dp))
    )

    Spacer(modifier = Modifier.height(2.dp))

    // Price line placeholder (mimics 13sp semibold price)
    Box(
      modifier = Modifier
        .size(width = 56.dp, height = 13.dp)
        .clip(RoundedCornerShape(4.dp))
        .shimmerBackground(RoundedCornerShape(4.dp))
    )
  }
}

/**
 * 2-column Grid of Skeleton Cards for the Offers Feed
 */
@Composable
fun OffersGridSkeleton(
  modifier: Modifier = Modifier,
  itemCount: Int = 6
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    val rows = (itemCount + 1) / 2
    for (i in 0 until rows) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(modifier = Modifier.weight(1f)) {
          OfferCardSkeleton()
        }
        Box(modifier = Modifier.weight(1f)) {
          if (i * 2 + 1 < itemCount) {
            OfferCardSkeleton()
          }
        }
      }
    }
  }
}
