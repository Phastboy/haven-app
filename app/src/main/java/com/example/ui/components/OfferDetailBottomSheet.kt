package com.example.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.model.Offer
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Offer Detail sheet presenting full high-res photo, barter details, and exchange actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferDetailBottomSheet(
  offer: Offer,
  onDismiss: () -> Unit,
  onToggleFavorite: (String) -> Unit,
  modifier: Modifier = Modifier,
  onProposeExchange: (Offer) -> Unit = {}
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
    modifier = modifier.testTag("offer_detail_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp)
        .navigationBarsPadding(),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Top row: Category tag & close button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(HavenColors.SurfaceContainer)
            .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = offer.category,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
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

      // Large Hero Image
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1.2f)
          .clip(RoundedCornerShape(12.dp))
          .background(HavenColors.SurfaceContainer)
          .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
      ) {
        Image(
          painter = painterResource(id = offer.imageResId),
          contentDescription = offer.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxWidth()
        )
      }

      // Title & Price
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = offer.fullTitle,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            color = HavenColors.TextPrimary
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = offer.priceDisplay,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = HavenColors.TextPrimary
          )
        }

        // Reusable Favorite Toggle Button
        HavenSaveButton(
          isSaved = offer.isFavorite,
          onToggle = { onToggleFavorite(offer.id) },
          size = 38.dp,
          iconSize = 18.dp,
          testTag = "detail_favorite_button"
        )
      }

      // Creator Info Card
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(HavenColors.SurfaceContainer)
          .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(8.dp))
          .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xFFE5E5E5)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = offer.creatorInitial,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = HavenColors.TextPrimary
          )
        }

        Column {
          Text(
            text = offer.creatorName,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = HavenColors.TextPrimary
          )
          Text(
            text = "Verified Haven Member",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            color = HavenColors.TextSecondary
          )
        }
      }

      // Description text
      Text(
        text = offer.description,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = HavenColors.TextSecondary
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Direct Exchange Action Button
      Button(
        onClick = {
          onDismiss()
          onProposeExchange(offer)
        },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = HavenColors.Primary,
          contentColor = HavenColors.TextOnDark
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
      ) {
        Text(
          text = "Propose direct exchange",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 15.sp
        )
      }
    }
  }
}
