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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
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
fun FeedOfferCard(offer: FeedOffer, modifier: Modifier = Modifier, onOpen: () -> Unit) {
  Column(modifier.fillMaxWidth().clickable(onClick = onOpen).testTag("offer_card_${offer.id}")) {
    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(HavenColors.SurfaceContainer)) {
      SubcomposeAsyncImage(model = offer.coverUrl, contentDescription = offer.title,
        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
        error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Icon(Icons.Outlined.Image, "Image unavailable", tint = HavenColors.TextMuted)
        } })
      IconButton(onClick = onOpen, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
        .background(HavenColors.Canvas.copy(alpha = 0.8f), RoundedCornerShape(24.dp))) {
        Icon(Icons.Outlined.FavoriteBorder, "Open offer to save on Haven", tint = HavenColors.Primary)
      }
    }
    Spacer(Modifier.height(10.dp))
    Text(offer.title, modifier = Modifier.testTag("offer_title_${offer.id}"), color = HavenColors.TextPrimary, fontFamily = InterFontFamily, fontSize = 14.sp, lineHeight = 20.sp,
      fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(4.dp))
    Text(listOfNotNull(offer.makerName, offerDistanceLabel(offer.distanceMeters)).joinToString(" · "), fontFamily = InterFontFamily, fontSize = 13.sp, lineHeight = 19.sp,
      color = HavenColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(4.dp))
    Text(offer.priceDisplay, modifier = Modifier.testTag("offer_price_${offer.id}"), color = HavenColors.TextPrimary, fontFamily = InterFontFamily, fontSize = 14.sp, lineHeight = 20.sp,
      fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

/** Distance is optional server data; absent/invalid data never becomes a fictional distance. */
fun offerDistanceLabel(meters: Double?): String? {
  if (meters == null || !meters.isFinite() || meters < 0) return null
  return if (meters < 1000) "${meters.toInt()} m" else
    java.lang.String.format(java.util.Locale.ROOT, "%.1f km", meters / 1000)
}
