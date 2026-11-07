package com.example.ui.components

import coil3.request.crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.example.data.FeedOffer
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

@Composable
fun FeedOfferCard(
  offer: FeedOffer,
  modifier: Modifier = Modifier,
  saved: Boolean = false,
  saving: Boolean = false,
  showDistance: Boolean = false,
  onSave: (() -> Unit)? = null,
  onOpenProvider: ((String) -> Unit)? = null,
  onOpen: () -> Unit
) {
  Column(modifier = modifier.fillMaxWidth().testTag("offer_card_${offer.id}")) {
    Box(
      Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(RoundedCornerShape(12.dp))
        .background(HavenColors.SurfaceContainer)
        .clickable(onClick = onOpen)
    ) {
      val context = androidx.compose.ui.platform.LocalContext.current
      val imageRequest = androidx.compose.runtime.remember(offer.coverUrl) {
        coil3.request.ImageRequest.Builder(context)
          .data(offer.coverUrl)
          .crossfade(true)
          .memoryCacheKey(offer.coverUrl)
          .diskCacheKey(offer.coverUrl)
          .build()
      }
      SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = offer.title,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        loading = { Box(Modifier.fillMaxSize().shimmerBackground(RoundedCornerShape(12.dp))) },
        success = { SubcomposeAsyncImageContent(Modifier.testTag("offer_image_${offer.id}")) },
        error = {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Image, "Image unavailable", tint = HavenColors.TextMuted)
          }
        }
      )
      Box(
        modifier = Modifier.align(Alignment.TopEnd).size(48.dp)
          .clickable(enabled = !saving, role = Role.Button, onClick = { (onSave ?: onOpen).invoke() }),
        contentAlignment = Alignment.Center
      ) {
        Box(
          Modifier.size(28.dp).clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.25f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (saving) "Saving offer" else if (saved) "Unsave offer" else "Save offer",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
    Spacer(Modifier.height(10.dp))
    Text(
      text = offer.title,
      modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).testTag("offer_title_${offer.id}"),
      color = HavenColors.TextPrimary,
      fontFamily = InterFontFamily,
      fontSize = 14.sp,
      lineHeight = 18.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Spacer(Modifier.height(4.dp))
    OfferPriceDistanceRow(offer, showDistance, onOpen)
    Spacer(Modifier.height(4.dp))
    val providerClickable = onOpenProvider != null && offer.makerHandle.isNotBlank()
    Text(
      text = offer.makerName,
      fontFamily = InterFontFamily,
      fontSize = 13.sp,
      lineHeight = 16.sp,
      color = HavenColors.TextSecondary,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.fillMaxWidth().testTag("offer_maker_${offer.id}").then(
        if (providerClickable) Modifier.clickable { onOpenProvider(offer.makerHandle) } else Modifier
      )
    )
  }
}

/** Measure price first, then give distance only the remaining width. Neither can wrap. */
@Composable
private fun OfferPriceDistanceRow(offer: FeedOffer, showDistance: Boolean, onOpen: () -> Unit) {
  val distance = if (showDistance) offerDistanceLabel(offer.distanceMeters) else null
  Layout(
    modifier = Modifier.fillMaxWidth().testTag("offer_price_row_${offer.id}"),
    content = {
      Text(
        offer.priceDisplay,
        modifier = Modifier.clickable(onClick = onOpen).testTag("offer_price_${offer.id}"),
        fontFamily = InterFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold, color = HavenColors.TextPrimary,
        maxLines = 1, overflow = TextOverflow.Ellipsis
      )
      if (distance != null) Text(
        distance,
        modifier = Modifier.testTag("offer_distance_${offer.id}"),
        fontFamily = InterFontFamily, fontSize = 12.sp, lineHeight = 16.sp,
        color = HavenColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis
      )
    }
  ) { measurables, constraints ->
    val price = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
    val gap = 6.dp.roundToPx()
    val remaining = (constraints.maxWidth - price.width - gap).coerceAtLeast(0)
    val distancePlaceable = measurables.getOrNull(1)?.measure(
      constraints.copy(minWidth = 0, maxWidth = remaining, minHeight = 0)
    )
    layout(constraints.maxWidth, maxOf(price.height, distancePlaceable?.height ?: 0)) {
      price.placeRelative(0, 0)
      if (distancePlaceable != null && remaining > 0) distancePlaceable.placeRelative(price.width + gap, 0)
    }
  }
}

/** Distance is optional server data; absent/invalid data never becomes a fictional distance. */
fun offerDistanceLabel(meters: Double?): String? {
  if (meters == null || !meters.isFinite() || meters < 0) return null
  return if (meters < 1000) "${meters.toInt()} m" else
    java.lang.String.format(java.util.Locale.ROOT, "%.1f km", meters / 1000)
}
