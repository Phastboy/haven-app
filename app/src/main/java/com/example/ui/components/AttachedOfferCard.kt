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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
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
import com.example.ui.model.Offer
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Attached offer badge embedded in message threads
 */
@Composable
fun AttachedOfferCard(
  offer: Offer,
  onOfferClick: () -> Unit,
  modifier: Modifier = Modifier,
  isFromMe: Boolean = false
) {
  val containerBg = if (isFromMe) Color.White.copy(alpha = 0.12f) else HavenColors.Canvas
  val borderColor = if (isFromMe) Color.White.copy(alpha = 0.25f) else HavenColors.BorderDefault
  val titleColor = if (isFromMe) HavenColors.TextOnDark else HavenColors.TextPrimary
  val priceColor = if (isFromMe) HavenColors.TextOnDark.copy(alpha = 0.9f) else HavenColors.TextPrimary

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(containerBg)
      .border(1.dp, borderColor, RoundedCornerShape(8.dp))
      .clickable { onOfferClick() }
      .padding(8.dp)
      .testTag("attached_offer_${offer.id}"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 44x44 thumbnail
    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(HavenColors.SurfaceContainer)
    ) {
      Image(
        painter = painterResource(id = offer.imageResId),
        contentDescription = offer.title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(44.dp)
      )
    }

    Spacer(modifier = Modifier.width(10.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = "Attached Offer",
        fontFamily = InterFontFamily,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        color = if (isFromMe) HavenColors.TextOnDark.copy(alpha = 0.7f) else HavenColors.TextSecondary
      )
      Text(
        text = offer.title,
        fontFamily = InterFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = titleColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = offer.priceDisplay,
        fontFamily = InterFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = priceColor
      )
    }

    Icon(
      imageVector = Icons.Outlined.OpenInNew,
      contentDescription = "View offer details",
      tint = if (isFromMe) HavenColors.TextOnDark.copy(alpha = 0.8f) else HavenColors.TextSecondary,
      modifier = Modifier.size(16.dp)
    )
  }
}
