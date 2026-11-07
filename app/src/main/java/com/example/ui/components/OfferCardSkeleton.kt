package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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

/** Loading preserves the square cover and exactly three text rows of FeedOfferCard. */
@Composable
fun OfferCardSkeleton(modifier: Modifier = Modifier) {
  Column(modifier.fillMaxWidth().testTag("offer_card_skeleton")) {
    Box(Modifier.fillMaxWidth().aspectRatio(1f).shimmerBackground(RoundedCornerShape(12.dp)))
    Spacer(Modifier.height(10.dp))
    Box(Modifier.fillMaxWidth(0.9f).height(18.dp).shimmerBackground())
    Spacer(Modifier.height(4.dp))
    Box(Modifier.fillMaxWidth(0.65f).height(16.dp).shimmerBackground())
    Spacer(Modifier.height(4.dp))
    Box(Modifier.fillMaxWidth(0.75f).height(16.dp).shimmerBackground())
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
