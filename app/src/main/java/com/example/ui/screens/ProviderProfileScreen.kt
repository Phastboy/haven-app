package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import coil3.compose.SubcomposeAsyncImage
import com.example.data.FeedOffer
import com.example.data.Person
import com.example.data.ProviderProfile
import com.example.ui.components.HavenTopBarContainer
import com.example.ui.components.havenBottomSafePadding
import com.example.ui.components.shimmerBackground
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Provider profile screen aligned with prototype haven_provider_profile:
 * - Centered identity section with 72dp avatar, name, joined, and bio
 * - Equal-width Follow (tonal) and Message provider (solid black) actions
 * - Category filter rail when provider offers span 2+ categories
 * - 2-column grid of 1:1 square thumbnail offer cards
 * - Unified HavenTopBarContainer and havenBottomSafePadding safe area handling
 */
@Composable
fun ProviderProfileScreen(
  profile: ProviderProfile?,
  loading: Boolean,
  error: String?,
  onBack: () -> Unit,
  onRetry: () -> Unit,
  onOpenOffer: (String) -> Unit,
  onMessageProvider: (String) -> Unit,
  onFollowProvider: (String) -> Unit,
  modifier: Modifier = Modifier,
  onLoadMore: () -> Unit = {},
  isOwnProfile: Boolean = false,
  isFollowing: Boolean = false
) {
  BackHandler(onBack = onBack)

  var selectedCategory by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .testTag("provider_profile_screen")
  ) {
    // Pinned Header Bar
    HavenTopBarContainer {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(44.dp)
            .testTag("provider_profile_back")
        ) {
          Icon(
            Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
            tint = HavenColors.TextPrimary
          )
        }

        Text(
          text = profile?.person?.displayName ?: "Provider",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 15.sp,
          color = HavenColors.TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.size(44.dp))
      }
    }

    if (loading && profile == null) {
      ProviderProfileLoadingSkeleton()
      return
    }

    if (error != null && profile == null) {
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
          modifier = Modifier.testTag("provider_profile_error")
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
          Text("Try again")
        }
      }
      return
    }

    val person = profile?.person ?: return
    val allOffers = profile.offers
    val categories = remember(allOffers) {
      allOffers.map { it.category }.filter { it.isNotBlank() }.distinct()
    }

    val visibleOffers = remember(allOffers, selectedCategory) {
      if (selectedCategory == null) allOffers
      else allOffers.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .havenBottomSafePadding()
        .verticalScroll(rememberScrollState())
        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp)
        .testTag("provider_profile_content"),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Identity Section (Left-aligned matching prototype .profile-intro and .name-bio)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("provider_identity_section"),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Left-aligned Avatar
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(HavenColors.SurfaceContainer)
            .testTag("provider_avatar"),
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
            val initials = person.displayName
              .split("\\s+".toRegex())
              .filter { it.isNotBlank() }
              .take(2)
              .map { it.first().uppercase() }
              .joinToString("")
            Text(
              text = if (initials.isNotEmpty()) initials else "H",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 24.sp,
              color = HavenColors.TextPrimary
            )
          }
        }

        // Provider Name
        Text(
          text = person.displayName,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 24.sp,
          lineHeight = 30.sp,
          textAlign = TextAlign.Start,
          color = HavenColors.TextPrimary,
          modifier = Modifier.testTag("provider_name")
        )

        // Joined date (for metadata and tests)
        Text(
          text = "Joined ${person.joined}",
          fontFamily = InterFontFamily,
          fontSize = 13.sp,
          textAlign = TextAlign.Start,
          color = HavenColors.TextSecondary,
          modifier = Modifier.testTag("provider_joined")
        )

        // Bio
        if (!person.bio.isNullOrBlank()) {
          Text(
            text = person.bio,
            fontFamily = InterFontFamily,
            fontSize = 15.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Start,
            color = HavenColors.TextPrimary,
            modifier = Modifier
              .padding(top = 2.dp, bottom = 4.dp)
              .testTag("provider_bio")
          )
        }

        // Action Buttons: Message provider (primary) & Follow (tonal secondary)
        if (!isOwnProfile) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Message button (Solid dark matching prototype .btn-message)
            Row(
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(HavenColors.Primary)
                .clickable { onMessageProvider(person.handle) }
                .testTag("provider_message_button"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = HavenColors.Canvas,
                modifier = Modifier.size(18.dp)
              )
              Spacer(Modifier.width(8.dp))
              Text(
                text = "Message provider",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = HavenColors.Canvas
              )
            }

            // Follow button (Tonal matching prototype .btn-follow)
            Row(
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isFollowing) HavenColors.Canvas else HavenColors.SurfaceContainer)
                .then(
                  if (isFollowing) Modifier.border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(8.dp))
                  else Modifier
                )
                .clickable { onFollowProvider(person.handle) }
                .testTag("provider_follow_button"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                if (isFollowing) Icons.Outlined.Check else Icons.Outlined.Add,
                contentDescription = null,
                tint = HavenColors.TextPrimary,
                modifier = Modifier.size(18.dp)
              )
              Spacer(Modifier.width(6.dp))
              Text(
                text = if (isFollowing) "Following" else "Follow",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = HavenColors.TextPrimary
              )
            }
          }
        }
      }

      // Divider
      HorizontalDivider(color = HavenColors.BorderSubtle, modifier = Modifier.padding(vertical = 4.dp))

      // Offers Section Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Offers",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 16.sp,
          color = HavenColors.TextPrimary
        )
        Text(
          text = "${visibleOffers.size} ${if (visibleOffers.size == 1) "offer" else "offers"}",
          fontFamily = InterFontFamily,
          fontSize = 13.sp,
          color = HavenColors.TextSecondary
        )
      }

      // Category filter rail (shown when offers span 2+ categories, matching prototype)
      if (categories.size >= 2) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          CategoryFilterChip(
            label = "All",
            selected = selectedCategory == null,
            onClick = { selectedCategory = null }
          )
          categories.forEach { cat ->
            CategoryFilterChip(
              label = cat.replaceFirstChar { it.uppercase() },
              selected = selectedCategory.equals(cat, ignoreCase = true),
              onClick = { selectedCategory = cat }
            )
          }
        }
      }

      // Empty offers state
      if (visibleOffers.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "No offers to show yet.",
            fontFamily = InterFontFamily,
            fontSize = 14.sp,
            color = HavenColors.TextSecondary
          )
        }
      }

      // Full-width cards matching prototype .offers-list, .offer-card, .offer-thumb (16:9)
      visibleOffers.forEach { offer ->
        ProviderOfferCard(
          offer = offer,
          onClick = { onOpenOffer(offer.slug) }
        )
      }

      // Pagination indicator
      if (profile.nextCursor != null) {
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
              Text("Load more offers")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CategoryFilterChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .height(36.dp)
      .clip(RoundedCornerShape(18.dp))
      .background(if (selected) HavenColors.Primary else HavenColors.SurfaceContainer)
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp)
      .testTag("provider_category_chip_$label"),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      fontFamily = InterFontFamily,
      fontSize = 13.sp,
      fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
      color = if (selected) HavenColors.Canvas else HavenColors.TextPrimary
    )
  }
}

/**
 * Full-width offer card matching prototype .offer-card:
 * - 16:9 thumbnail with rounded top corners
 * - Category, title, and price in .offer-details
 */
@Composable
private fun ProviderOfferCard(
  offer: FeedOffer,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(HavenColors.SurfaceContainer)
      .clickable(onClick = onClick)
      .testTag("provider_offer_${offer.id}")
  ) {
    // 16:9 aspect-ratio thumbnail matching prototype .offer-thumb
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
        .background(HavenColors.SurfaceContainer)
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

    // Card details matching prototype .offer-details
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      if (offer.category.isNotBlank()) {
        Text(
          text = offer.category.replaceFirstChar { it.uppercase() },
          fontFamily = InterFontFamily,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          color = HavenColors.TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Text(
        text = offer.title,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = HavenColors.TextPrimary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = offer.priceDisplay,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = HavenColors.TextPrimary
      )
    }
  }
}

@Composable
private fun ProviderProfileLoadingSkeleton() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Box(
      modifier = Modifier
        .size(72.dp)
        .clip(CircleShape)
        .shimmerBackground()
    )
    Box(
      modifier = Modifier
        .size(width = 180.dp, height = 28.dp)
        .clip(RoundedCornerShape(6.dp))
        .shimmerBackground()
    )
    Box(
      modifier = Modifier
        .size(width = 120.dp, height = 16.dp)
        .clip(RoundedCornerShape(4.dp))
        .shimmerBackground()
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .height(44.dp)
          .clip(RoundedCornerShape(8.dp))
          .shimmerBackground()
      )
      Box(
        modifier = Modifier
          .weight(1f)
          .height(44.dp)
          .clip(RoundedCornerShape(8.dp))
          .shimmerBackground()
      )
    }
    Spacer(Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .aspectRatio(1f)
          .clip(RoundedCornerShape(10.dp))
          .shimmerBackground()
      )
      Box(
        modifier = Modifier
          .weight(1f)
          .aspectRatio(1f)
          .clip(RoundedCornerShape(10.dp))
          .shimmerBackground()
      )
    }
  }
}
