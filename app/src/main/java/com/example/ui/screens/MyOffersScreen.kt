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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.SubcomposeAsyncImage
import com.example.data.FeedOffer
import com.example.data.PaginatedOffers
import com.example.ui.components.HavenTopBarContainer
import com.example.ui.components.havenBottomSafePadding
import com.example.ui.components.shimmerBackground
import com.example.ui.model.MyOffersViewModel
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

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
    pagingError = state.pagingError,
    loadingMore = state.loadingMore,
    onBack = onBack,
    onRetry = viewModel::load,
    onOpenOffer = onOpenOffer,
    onCreateOffer = onCreateOffer,
    onEditOffer = onEditOffer,
    onDeleteOffer = viewModel::deleteOffer,
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
  onDeleteOffer: (String) -> Unit = {},
  onLoadMore: () -> Unit = {},
  pagingError: String? = null,
  loadingMore: Boolean = false,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onBack)
  var offerPendingDelete by remember { mutableStateOf<FeedOffer?>(null) }

  if (offerPendingDelete != null) {
    val target = offerPendingDelete!!
    AlertDialog(
      onDismissRequest = { offerPendingDelete = null },
      title = {
        Text(
          text = "Delete offer?",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold
        )
      },
      text = {
        Text(
          text = "Are you sure you want to delete “${target.title}”? This action cannot be undone.",
          fontFamily = InterFontFamily,
          fontSize = 14.sp,
          color = HavenColors.TextPrimary
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteOffer(target.slug)
            offerPendingDelete = null
          },
          modifier = Modifier.testTag("confirm_delete_button")
        ) {
          Text("Delete", color = HavenColors.TextPrimary, fontWeight = FontWeight.SemiBold)
        }
      },
      dismissButton = {
        TextButton(onClick = { offerPendingDelete = null }) {
          Text("Cancel", color = HavenColors.TextSecondary)
        }
      }
    )
  }

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
            text = "$count ${if (count == 1) "offer" else "offers"}",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = HavenColors.TextSecondary
          )
        }

        IconButton(
          onClick = onCreateOffer,
          modifier = Modifier
            .size(44.dp)
            .testTag("my_offers_create_button")
        ) {
          Icon(
            Icons.Outlined.Add,
            contentDescription = "Create offer",
            tint = HavenColors.TextPrimary,
            modifier = Modifier.testTag("create_offer_action_button")
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

    val items = offers?.items.orEmpty()
    if (items.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(HavenColors.SurfaceContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Outlined.Inventory2,
            contentDescription = null,
            tint = HavenColors.TextSecondary,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(Modifier.height(16.dp))
        Text(
          text = "You haven’t created any offers yet",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 16.sp,
          color = HavenColors.TextPrimary,
          textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
          text = "Post what you make or offer to reach people in your community.",
          fontFamily = InterFontFamily,
          fontSize = 14.sp,
          color = HavenColors.TextSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(24.dp))
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
          onEdit = { onEditOffer(offer.id) },
          onDelete = { offerPendingDelete = offer }
        )
      }

      if (pagingError != null) {
        item(span = { GridItemSpan(2) }) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = pagingError,
              fontFamily = InterFontFamily,
              fontSize = 13.sp,
              color = HavenColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onLoadMore) {
              Text("Retry")
            }
          }
        }
      } else if (offers?.nextCursor != null) {
        item(span = { GridItemSpan(2) }) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            contentAlignment = Alignment.Center
          ) {
            if (loadingMore) {
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
  onDelete: () -> Unit,
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

    // Card Actions: Edit & Delete
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      TextButton(
        onClick = onEdit,
        modifier = Modifier
          .weight(1f)
          .height(38.dp)
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
        Spacer(Modifier.width(4.dp))
        Text(
          "Edit",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 13.sp,
          color = HavenColors.TextPrimary
        )
      }

      IconButton(
        onClick = onDelete,
        modifier = Modifier
          .size(38.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(HavenColors.SurfaceContainer)
          .testTag("my_offer_delete_${offer.id}")
      ) {
        Icon(
          Icons.Outlined.DeleteOutline,
          contentDescription = "Delete offer",
          tint = HavenColors.TextPrimary,
          modifier = Modifier.size(16.dp)
        )
      }
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
    repeat(6) {
      item {
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
              .fillMaxWidth(0.8f)
              .height(14.dp)
              .clip(RoundedCornerShape(4.dp))
              .shimmerBackground()
          )
          Box(
            modifier = Modifier
              .fillMaxWidth(0.5f)
              .height(12.dp)
              .clip(RoundedCornerShape(4.dp))
              .shimmerBackground()
          )
        }
      }
    }
  }
}
