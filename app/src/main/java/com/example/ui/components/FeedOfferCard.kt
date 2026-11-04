package com.example.ui.components

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.FeedOffer
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

@Composable
fun FeedOfferCard(
  offer: FeedOffer,
  modifier: Modifier = Modifier,
  saved: Boolean = false,
  saving: Boolean = false,
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
        coil.request.ImageRequest.Builder(context)
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
        error = {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Image, "Image unavailable", tint = HavenColors.TextMuted)
          }
        }
      )
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(top = 8.dp, end = 8.dp)
          .size(32.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(HavenColors.Canvas.copy(alpha = 0.85f))
          .clickable(enabled = !saving, onClick = { (onSave ?: onOpen).invoke() }),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
          contentDescription = if (saving) "Saving offer" else if (saved) "Unsave offer" else "Save offer",
          tint = HavenColors.Primary,
          modifier = Modifier.size(16.dp)
        )
      }
    }
    Spacer(Modifier.height(8.dp))
    Text(
      text = offer.title,
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onOpen)
        .testTag("offer_title_${offer.id}"),
      color = HavenColors.TextPrimary,
      fontFamily = InterFontFamily,
      fontSize = 14.sp,
      lineHeight = 20.sp,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Spacer(Modifier.height(2.dp))
    val providerClickable = onOpenProvider != null && offer.makerHandle.isNotBlank()
    Text(
      text = listOfNotNull(offer.makerName, offerDistanceLabel(offer.distanceMeters)).joinToString(" · "),
      fontFamily = InterFontFamily,
      fontSize = 12.sp,
      lineHeight = 17.sp,
      color = HavenColors.TextSecondary,
      fontWeight = FontWeight.Normal,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .testTag("offer_maker_${offer.id}")
        .then(
          if (providerClickable) {
            Modifier.clickable { onOpenProvider?.invoke(offer.makerHandle) }
          } else {
            Modifier
          }
        )
    )
    Spacer(Modifier.height(2.dp))
    Text(
      text = offer.priceDisplay,
      modifier = Modifier
        .clickable(onClick = onOpen)
        .testTag("offer_price_${offer.id}"),
      color = HavenColors.TextPrimary,
      fontFamily = InterFontFamily,
      fontSize = 15.sp,
      lineHeight = 20.sp,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

/** Distance is optional server data; absent/invalid data never becomes a fictional distance. */
fun offerDistanceLabel(meters: Double?): String? {
  if (meters == null || !meters.isFinite() || meters < 0) return null
  return if (meters < 1000) "${meters.toInt()} m" else
    java.lang.String.format(java.util.Locale.ROOT, "%.1f km", meters / 1000)
}
