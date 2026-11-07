package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.model.Offer
import com.example.ui.model.SampleOffers
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Bottom sheet allowing user to select an offer to attach to their message
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachOfferBottomSheet(
  onSelectOffer: (Offer) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val offers = SampleOffers.defaultOffers

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = HavenColors.Canvas,
    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    dragHandle = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .size(width = 36.dp, height = 4.dp)
            .clip(CircleShape)
            .background(HavenColors.BorderDefault)
        )
      }
    },
    modifier = modifier.testTag("attach_offer_bottom_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
    ) {
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Attach an offer",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = HavenColors.TextPrimary
          )
          Text(
            text = "Select an item to propose for barter or reference",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = HavenColors.TextSecondary
          )
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = HavenColors.TextPrimary,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      // Offer list
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(340.dp)
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(offers, key = { it.id }) { offer ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(HavenColors.SurfaceContainer)
              .clickable { onSelectOffer(offer) }
              .padding(10.dp)
              .testTag("select_offer_${offer.id}"),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(HavenColors.BorderSubtle)
            ) {
              Image(
                painter = painterResource(id = offer.imageResId),
                contentDescription = offer.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = offer.title,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = HavenColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "By ${offer.creatorName} · ${offer.category}",
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                color = HavenColors.TextSecondary
              )
              Text(
                text = offer.priceDisplay,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = HavenColors.TextPrimary
              )
            }

            Icon(
              imageVector = Icons.Outlined.CheckCircle,
              contentDescription = "Attach",
              tint = HavenColors.Primary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
  }
}
