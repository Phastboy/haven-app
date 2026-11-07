package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.example.ui.model.Offer
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * OfferCard with card-level skeleton loading:
 * - If isLoading is true or offer is null, renders the card's skeleton placeholder
 * - While the network image is downloading, renders an image-level shimmer placeholder
 * - Matches the 1:1 aspect ratio square container with 12dp corner radius
 */
@Composable
fun OfferCard(
  offer: Offer?,
  onClick: () -> Unit = {},
  onToggleFavorite: () -> Unit = {},
  modifier: Modifier = Modifier,
  isLoading: Boolean = false
) {
  if (isLoading || offer == null) {
    OfferCardSkeleton(modifier = modifier)
    return
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .testTag("offer_card_${offer.id}")
  ) {
    // 1:1 Aspect Ratio Image Box
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(RoundedCornerShape(12.dp))
        .background(HavenColors.SurfaceContainer)
        .border(1.dp, HavenColors.BorderSubtle.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
    ) {
      // Load image with fallback to bundled local drawable and shimmer placeholder
      SubcomposeAsyncImage(
        model = offer.imageUrl,
        contentDescription = offer.title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
        loading = {
          // Card-level image shimmer while image data is being fetched
          Box(
            modifier = Modifier
              .fillMaxSize()
              .shimmerBackground(RoundedCornerShape(12.dp))
          )
        },
        error = {
          // Offline local drawable fallback
          Image(
            painter = painterResource(id = offer.imageResId),
            contentDescription = offer.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }
      )

      // Reusable Save/Favorite Button on top-right (32x32dp, matching design spec)
      HavenSaveButton(
        isSaved = offer.isFavorite,
        onToggle = onToggleFavorite,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(8.dp),
        testTag = "save_offer_${offer.id}"
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Creator Row: 14dp circle with initial + author name
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Box(
        modifier = Modifier
          .size(14.dp)
          .clip(CircleShape)
          .background(Color(0xFFE5E5E5)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = offer.creatorInitial,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 8.sp,
          color = HavenColors.TextPrimary
        )
      }

      Text(
        text = offer.creatorName,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = HavenColors.TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    Spacer(modifier = Modifier.height(2.dp))

    // Offer Title (13sp Medium #111111, single line truncated)
    Text(
      text = offer.title,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 13.sp,
      color = HavenColors.TextPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )

    Spacer(modifier = Modifier.height(2.dp))

    // Price Display (13sp SemiBold #111111)
    Text(
      text = offer.priceDisplay,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 13.sp,
      color = HavenColors.TextPrimary
    )
  }
}
