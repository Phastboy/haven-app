package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.PaginatedOffers
import com.example.ui.components.FeedOfferCard
import com.example.ui.components.HavenTopBarContainer
import com.example.ui.components.havenBottomSafePadding
import com.example.ui.components.shimmerBackground
import com.example.ui.model.InquiredViewModel
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

@Composable
fun InquiredScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: InquiredViewModel = viewModel(factory = InquiredViewModel.factory(LocalContext.current, com.example.ui.model.SessionManager.session.value.personId)),
  onOpenOffer: (String) -> Unit = {},
  onBrowseOffers: () -> Unit = {},
  onOpenProvider: ((String) -> Unit)? = null
) {
  DisposableEffect(viewModel) {
    viewModel.load()
    onDispose { viewModel.cancelLoading() }
  }
  val state by viewModel.state.collectAsState()
  InquiredScreen(
    offers = state.offers,
    loading = state.loading,
    error = state.error,
    pagingError = state.pagingError,
    loadingMore = state.loadingMore,
    onBack = onBack,
    onRetry = viewModel::load,
    onOpenOffer = onOpenOffer,
    onBrowseOffers = onBrowseOffers,
    onOpenProvider = onOpenProvider,
    onLoadMore = viewModel::loadMore,
    modifier = modifier
  )
}

@Composable
fun InquiredScreen(
  offers: PaginatedOffers?,
  loading: Boolean,
  error: String?,
  onBack: () -> Unit,
  onRetry: () -> Unit,
  onOpenOffer: (String) -> Unit,
  onBrowseOffers: () -> Unit,
  modifier: Modifier = Modifier,
  onOpenProvider: ((String) -> Unit)? = null,
  onLoadMore: () -> Unit = {},
  pagingError: String? = null,
  loadingMore: Boolean = false
) {
  BackHandler(onBack = onBack)

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .testTag("inquired_screen")
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
            .testTag("inquired_back_button")
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
            text = "Inquired offers",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = HavenColors.TextPrimary,
            modifier = Modifier.testTag("inquired_title")
          )
          val count = offers?.items?.size ?: 0
          Text(
            text = "$count inquired",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = HavenColors.TextSecondary
          )
        }

        Spacer(Modifier.size(44.dp))
      }
    }

    if (loading && offers == null) {
      InquiredLoadingSkeleton()
      return
    }

    if (error != null && offers != null) {
      Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(error, modifier = Modifier.weight(1f), color = HavenColors.TextPrimary)
        TextButton(onClick = onRetry) { Text("Try again") }
      }
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
          modifier = Modifier.testTag("inquired_error")
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
          Icons.Outlined.ChatBubbleOutline,
          contentDescription = null,
          tint = HavenColors.TextSecondary,
          modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
          text = "No inquiries yet",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 16.sp,
          color = HavenColors.TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
          text = "Offers you message someone about will appear here.",
          fontFamily = InterFontFamily,
          fontSize = 14.sp,
          color = HavenColors.TextSecondary,
          textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        TextButton(
          onClick = onBrowseOffers,
          modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(HavenColors.TextPrimary)
            .testTag("inquired_empty_browse")
        ) {
          Text(
            "Browse offers",
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
        .testTag("inquired_grid"),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
      items(items, key = { it.id }) { offer ->
        FeedOfferCard(
          offer = offer,
          showSave = false,
          onOpenProvider = onOpenProvider,
          onOpen = { onOpenOffer(offer.slug) }
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
private fun InquiredLoadingSkeleton() {
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
      }
    }
  }
}
