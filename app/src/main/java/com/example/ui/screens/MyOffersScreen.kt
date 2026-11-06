package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.components.HavenTopBarContainer
import com.example.ui.components.havenBottomSafePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.FeedOffer
import com.example.data.PaginatedOffers
import com.example.ui.components.shimmerBackground
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.model.MyOffersViewModel

@Composable
fun MyOffersScreen(
  viewModel: MyOffersViewModel = viewModel(factory = MyOffersViewModel.factory(LocalContext.current)),
  onBack: () -> Unit,
  onOpenOffer: (String) -> Unit = {},
  onCreateOffer: () -> Unit = {},
  onEditOffer: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val state by viewModel.state.collectAsState()
  MyOffersScreen(
    offers = state.offers,
    loading = state.loading,
    error = state.error,
    onBack = onBack,
    onRetry = viewModel::load,
    onOpenOffer = onOpenOffer,
    onCreateOffer = onCreateOffer,
    onEditOffer = onEditOffer,
    onLoadMore = viewModel::loadMore,
    modifier = modifier
  )
}

@Composable
fun MyOffersScreen(
  offers: PaginatedOffers?,
  loading: Boolean,
  error: String?,
  onBack: () -> Unit,
  onRetry: () -> Unit,
  onOpenOffer: (String) -> Unit,
  onCreateOffer: () -> Unit,
  onEditOffer: (String) -> Unit,
  onLoadMore: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onBack)

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .testTag("my_offers_screen")
  ) {
    // Header
    HavenTopBarContainer {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(44.dp)
            .testTag("my_offers_back")
            .testTag("my_offers_back_button")
        ) {
          Icon(
            Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
            tint = HavenColors.TextPrimary
          )
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(horizontal = 8.dp)
        ) {
          Text(
            text = "My offers",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = HavenColors.TextPrimary,
            modifier = Modifier.testTag("my_offers_title")
          )
          val count = offers?.items?.size ?: 0
          Text(
            text = if (count == 1) "1 offer" else "$count offers",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = HavenColors.TextSecondary
          )
        }

        // Create button
        TextButton(
          onClick = onCreateOffer,
          modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(HavenColors.TextPrimary)
            .testTag("my_offers_create_button")
            .testTag("create_offer_action_button"),
          contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
          Icon(
            Icons.Outlined.Add,
            contentDescription = null,
            tint = HavenColors.Canvas,
            modifier = Modifier.size(16.dp)
          )
          Spacer(Modifier.width(4.dp))
          Text(
            "Create",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = HavenColors.Canvas
          )
        }
      }
    }

    if (loading && offers == null) {
      MyOffersLoadingSkeleton()
      return
    }

    if (error != null && offers == null) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = error,
          fontFamily = InterFontFamily,
          fontSize = 15.sp,
          color = HavenColors.TextPrimary,
          modifier = Modifier.testTag("my_offers_error")
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
          Text("Try again")
        }
      }
      return
    }

    val items = offers?.items ?: emptyList()

    if (items.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(
          Icons.Outlined.GridView,
          contentDescription = null,
          tint = HavenColors.TextSecondary,
          modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
          text = "No offers yet",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 16.sp,
          color = HavenColors.TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
          text = "Publish your first offer to start connecting with buyers.",
          fontFamily = InterFontFamily,
          fontSize = 14.sp,
          color = HavenColors.TextSecondary,
          textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        TextButton(
          onClick = onCreateOffer,
          modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(HavenColors.TextPrimary)
            .testTag("my_offers_empty_create")
        ) {
          Text(
            "Create offer",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = HavenColors.Canvas,
            modifier = Modifier.padding(horizontal = 16.dp)
          )
        }
      }
      return
    }

    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .havenBottomSafePadding()
        .testTag("my_offers_grid"),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
      items(items, key = { it.id }) { offer ->
        MyOfferCard(
          offer = offer,
          onOpen = { if (offer.isPublic) onOpenOffer(offer.slug) else onEditOffer(offer.id) },
          onEdit = { onEditOffer(offer.id) }
        )
      }

      if (offers?.nextCursor != null) {
        item(span = { GridItemSpan(2) }) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            contentAlignment = Alignment.Center
          ) {
            if (loading) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = HavenColors.Primary
              )
            } else {
              TextButton(onClick = onLoadMore) {
                Text("Load more")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MyOfferCard(
  offer: FeedOffer,
  onOpen: () -> Unit,
  onEdit: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("my_offer_card_${offer.id}")
  ) {
    // Thumbnail (1:1 ratio)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(RoundedCornerShape(12.dp))
        .background(HavenColors.SurfaceContainer)
        .clickable(onClick = onOpen)
    ) {
      if (!offer.coverUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
          model = offer.coverUrl,
          contentDescription = offer.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
          loading = {
            Box(Modifier.fillMaxSize().shimmerBackground())
          },
          error = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Icon(Icons.Outlined.Image, null, tint = HavenColors.TextMuted)
            }
          }
        )
      } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Icon(Icons.Outlined.Image, null, tint = HavenColors.TextMuted)
        }
      }
    }

    // Copy
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onOpen)
        .padding(top = 8.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = offer.title,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = HavenColors.TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      if (offer.category.isNotBlank()) {
        Text(
          text = offer.category.replaceFirstChar { it.uppercase() },
          fontFamily = InterFontFamily,
          fontSize = 13.sp,
          color = HavenColors.TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Text(
        text = if (offer.isPublic) offer.priceDisplay else "Private · add a photo to publish",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = HavenColors.TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    // Card Action: Edit
    TextButton(
      onClick = onEdit,
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp)
        .height(40.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(HavenColors.SurfaceContainer)
        .testTag("my_offer_edit_${offer.id}")
    ) {
      Icon(
        Icons.Outlined.Edit,
        contentDescription = null,
        tint = HavenColors.TextPrimary,
        modifier = Modifier.size(14.dp)
      )
      Spacer(Modifier.width(6.dp))
      Text(
        "Edit",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = HavenColors.TextPrimary
      )
    }
  }
}

@Composable
private fun MyOffersLoadingSkeleton() {
  LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
  ) {
    items(4) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .shimmerBackground()
        )
        Box(
          modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(16.dp)
            .clip(RoundedCornerShape(4.dp))
            .shimmerBackground()
        )
        Box(
          modifier = Modifier
            .fillMaxWidth(0.5f)
            .height(14.dp)
            .clip(RoundedCornerShape(4.dp))
            .shimmerBackground()
        )
        Box(
          modifier = Modifier
            .fillMaxWidth(0.4f)
            .height(16.dp)
            .clip(RoundedCornerShape(4.dp))
            .shimmerBackground()
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .shimmerBackground()
        )
      }
    }
  }
}
