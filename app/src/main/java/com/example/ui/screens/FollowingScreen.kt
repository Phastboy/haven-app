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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.example.ui.components.HavenTopBarContainer
import com.example.ui.components.havenBottomSafePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Group
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
import com.example.data.PaginatedFollowing
import com.example.data.Person
import com.example.ui.components.shimmerBackground
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.model.FollowingViewModel

@Composable
fun FollowingScreen(
  viewModel: FollowingViewModel = viewModel(factory = FollowingViewModel.factory(LocalContext.current)),
  onBack: () -> Unit,
  onOpenProvider: (String) -> Unit = {},
  onOpenWebFollow: (String) -> Unit = {},
  onDiscoverOffers: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val state by viewModel.state.collectAsState()
  FollowingScreen(
    following = state.following,
    loading = state.loading,
    error = state.error,
    onBack = onBack,
    onRetry = viewModel::load,
    onOpenProvider = onOpenProvider,
    onOpenWebFollow = onOpenWebFollow,
    onDiscoverOffers = onDiscoverOffers,
    onLoadMore = viewModel::loadMore,
    modifier = modifier
  )
}

@Composable
fun FollowingScreen(
  following: PaginatedFollowing?,
  loading: Boolean,
  error: String?,
  onBack: () -> Unit,
  onRetry: () -> Unit,
  onOpenProvider: (String) -> Unit,
  onOpenWebFollow: (String) -> Unit,
  onDiscoverOffers: () -> Unit,
  onLoadMore: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onBack)

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .testTag("following_screen")
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
            .testTag("following_back")
            .testTag("following_back_button")
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
            text = "Following",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = HavenColors.TextPrimary,
            modifier = Modifier.testTag("following_title")
          )
          val count = following?.items?.size ?: 0
          Text(
            text = "$count ${if (count == 1) "person" else "people"}",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = HavenColors.TextSecondary
          )
        }

        Spacer(Modifier.size(44.dp))
      }
    }

    if (loading && following == null) {
      FollowingLoadingSkeleton()
      return
    }

    if (error != null && following == null) {
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
          modifier = Modifier.testTag("following_error")
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
          Text("Try again")
        }
      }
      return
    }

    val items = following?.items ?: emptyList()

    if (items.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(
          Icons.Outlined.Group,
          contentDescription = null,
          tint = HavenColors.TextSecondary,
          modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
          text = "Not following anyone yet",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 16.sp,
          color = HavenColors.TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
          text = "Follow makers and providers to see their latest offers and updates.",
          fontFamily = InterFontFamily,
          fontSize = 14.sp,
          color = HavenColors.TextSecondary,
          textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        TextButton(
          onClick = onDiscoverOffers,
          modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(HavenColors.TextPrimary)
            .testTag("following_empty_discover")
        ) {
          Text(
            "Discover offers",
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

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .havenBottomSafePadding()
        .testTag("following_list"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
      items(items, key = { it.id }) { person ->
        FollowingRow(
          person = person,
          onOpen = { onOpenProvider(person.handle) },
          onAction = { onOpenWebFollow(person.handle) }
        )
      }

      if (following?.nextCursor != null) {
        item {
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
private fun FollowingRow(
  person: Person,
  onOpen: () -> Unit,
  onAction: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 12.dp)
      .testTag("following_row_${person.id}"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    // Left target link (avatar + name + bio)
    Row(
      modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(8.dp))
        .clickable(onClick = onOpen)
        .padding(end = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Initials Avatar
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(HavenColors.SurfaceContainer),
        contentAlignment = Alignment.Center
      ) {
        if (!person.avatarUrl.isNullOrBlank()) {
          SubcomposeAsyncImage(
            model = person.avatarUrl,
            contentDescription = person.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Text(
            text = person.displayName.firstOrNull()?.uppercase() ?: "H",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = HavenColors.TextPrimary
          )
        }
      }

      // Provider info
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = person.displayName,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 14.sp,
          color = HavenColors.TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        val subtitle = person.bio?.takeIf { it.isNotBlank() } ?: "Joined ${person.joined}"
        Text(
          text = subtitle,
          fontFamily = InterFontFamily,
          fontSize = 13.sp,
          color = HavenColors.TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    // Following button
    TextButton(
      onClick = onAction,
      modifier = Modifier
        .height(36.dp)
        .clip(RoundedCornerShape(20.dp))
        .background(HavenColors.SurfaceContainer)
        .testTag("following_button_${person.id}"),
      contentPadding = PaddingValues(horizontal = 14.dp)
    ) {
      Text(
        text = "Following",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        color = HavenColors.TextPrimary
      )
    }
  }
}

@Composable
private fun FollowingLoadingSkeleton() {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    repeat(6) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .shimmerBackground()
        )
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(0.5f)
              .height(14.dp)
              .clip(RoundedCornerShape(4.dp))
              .shimmerBackground()
          )
          Box(
            modifier = Modifier
              .fillMaxWidth(0.75f)
              .height(12.dp)
              .clip(RoundedCornerShape(4.dp))
              .shimmerBackground()
          )
        }
        Box(
          modifier = Modifier
            .size(width = 80.dp, height = 36.dp)
            .clip(RoundedCornerShape(20.dp))
            .shimmerBackground()
        )
      }
    }
  }
}
