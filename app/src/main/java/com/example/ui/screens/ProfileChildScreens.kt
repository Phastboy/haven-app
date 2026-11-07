package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.ui.components.HavenInsets
import com.example.ui.components.HavenPrimaryButton
import com.example.ui.components.HavenTopBarContainer
import com.example.ui.model.Offer
import com.example.ui.model.SampleOffers
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

// =========================================================================
// SECTION 1: SAVED OFFERS SCREEN (/profile/saved)
// Frame 01 (Grid with 8 cards + Unavailable state) & Frame 02 (Empty state)
// =========================================================================

data class SavedOfferItem(
  val id: String,
  val title: String,
  val priceDisplay: String,
  val imageResId: Int,
  val isAvailable: Boolean = true
)

@Composable
fun SavedOffersScreen(
  onBack: () -> Unit,
  onBrowseOffers: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler { onBack() }

  val savedOffers = remember {
    mutableStateListOf(
      SavedOfferItem("s-1", "Hand-woven Aso Oke fabric, 6 yards", "45,000 NGN", R.drawable.fabric_craft),
      SavedOfferItem("s-2", "Brass table lamp, 1950s", "38,000 NGN", R.drawable.lamp_craft),
      SavedOfferItem("s-3", "Beginner's pottery class, Saturday", "12,000 NGN", R.drawable.pottery_craft),
      SavedOfferItem("s-4", "Offer no longer available", "", 0, isAvailable = false),
      SavedOfferItem("s-5", "Used acoustic guitar with case", "90,000 NGN", R.drawable.guitar_lessons),
      SavedOfferItem("s-6", "Home-cooked jollof, party tray", "25,000 NGN", R.drawable.veg_box),
      SavedOfferItem("s-7", "Free: box of children's books", "Free", R.drawable.textbooks),
      SavedOfferItem("s-8", "Custom wedding invitations", "18,000 NGN", R.drawable.fabric_craft)
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .windowInsetsPadding(HavenInsets.navigationBars.only(WindowInsetsSides.Bottom))
      .testTag("saved_offers_screen")
  ) {
    // Back-only Top Bar with status bar safe integration
    HavenTopBarContainer(
      backgroundColor = HavenColors.Canvas,
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(44.dp)
            .testTag("saved_offers_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Navigate back",
            tint = HavenColors.TextPrimary,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }

    if (savedOffers.isEmpty()) {
      // Frame 02: Empty State
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(HavenColors.SurfaceContainer)
            .border(1.dp, HavenColors.BorderSubtle, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            tint = HavenColors.TextPrimary,
            modifier = Modifier.size(24.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "No saved offers yet",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 20.sp,
          color = HavenColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Tap the heart on an offer to keep it here.",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 15.sp,
          lineHeight = 22.sp,
          color = HavenColors.TextSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        HavenPrimaryButton(
          text = "Browse offers",
          onClick = onBrowseOffers,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .testTag("saved_empty_browse_button")
        )
      }
    } else {
      // Frame 01: 2-Column Grid
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
      ) {
        item {
          Text(
            text = "Saved offers",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 25.sp,
            letterSpacing = (-0.4).sp,
            color = HavenColors.TextPrimary,
            modifier = Modifier
              .padding(bottom = 16.dp)
              .testTag("saved_offers_title")
          )
        }

        // Pairs of cards in 2-column grid
        val rowCount = (savedOffers.size + 1) / 2
        items(rowCount) { rowIndex ->
          val firstIndex = rowIndex * 2
          val secondIndex = firstIndex + 1

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(modifier = Modifier.weight(1f)) {
              val item = savedOffers[firstIndex]
              SavedOfferCard(
                item = item,
                onRemove = { savedOffers.remove(item) }
              )
            }

            Box(modifier = Modifier.weight(1f)) {
              if (secondIndex < savedOffers.size) {
                val item = savedOffers[secondIndex]
                SavedOfferCard(
                  item = item,
                  onRemove = { savedOffers.remove(item) }
                )
              } else {
                Spacer(modifier = Modifier.fillMaxWidth())
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SavedOfferCard(
  item: SavedOfferItem,
  onRemove: () -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    if (!item.isAvailable) {
      // Unavailable Placeholder State
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1f)
          .clip(RoundedCornerShape(12.dp))
          .background(HavenColors.SurfaceContainer)
          .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
          .padding(12.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.BrokenImage,
            contentDescription = null,
            tint = HavenColors.TextMuted,
            modifier = Modifier.size(32.dp)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Unavailable",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = HavenColors.TextSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = item.title,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = HavenColors.TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = "Remove",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = HavenColors.TextPrimary,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
          .clickable { onRemove() }
          .padding(vertical = 2.dp)
      )
    } else {
      // Normal Available Card with Filled Heart Button
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1f)
          .clip(RoundedCornerShape(12.dp))
          .background(HavenColors.SurfaceContainer)
          .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
      ) {
        Image(
          painter = painterResource(id = item.imageResId),
          contentDescription = item.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )

        // Filled Heart Toggle Button
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp)
            .size(32.dp)
            .clip(CircleShape)
            .background(HavenColors.Canvas.copy(alpha = 0.95f))
            .clickable { onRemove() },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = "Saved offer",
            tint = HavenColors.TextPrimary,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = item.title,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        color = HavenColors.TextPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = item.priceDisplay,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = HavenColors.TextPrimary
      )
    }
  }
}

// =========================================================================
// SECTION 2: FOLLOWING SCREEN (/profile/following)
// Frame 03 (7 Person Rows) & Frame 04 (Empty State)
// =========================================================================

data class FollowingPerson(
  val id: String,
  val name: String,
  val handle: String,
  val initial: String,
  val avatarUrl: String? = null,
  var isFollowing: Boolean = true
)

@Composable
fun FollowingScreen(
  onBack: () -> Unit,
  onBrowseOffers: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler { onBack() }

  val people = remember {
    mutableStateListOf(
      FollowingPerson(
        "f-1",
        "Adaeze Obi",
        "@adaeze-obi",
        "A",
        "https://lh3.googleusercontent.com/aida/AEtjO1XT1RMubL8e8hGOBhw0NvVZyAtLhFXR1IlcXKlP77a0ZvKEXNjhZYJWYiQGy_NxHAEs-n9GJhFMoevM7g5HjrJWBU4NxEXmz-YJXxy_k9A9-crh3u1LNXdTOLbxoETTKILuDGSJC5ZCM01HqG-2e71Ilkdm7DFBWuO6ljlIEjq7PSEAW7L-ddjXkY-fO0VH3GqF89oc8Cu0CWZISQZu8NwYUFWgcU_RSDU4cM2x6Oju8m0IrHNemURl_yzw"
      ),
      FollowingPerson("f-2", "Kofi Mensah", "@kofi-m", "K"),
      FollowingPerson("f-3", "Tolu Adebayo", "@tolu-adebayo", "T"),
      FollowingPerson("f-4", "Ngozi Eze", "@ngozi-eze", "N"),
      FollowingPerson("f-5", "Samuel Boateng", "@s-boateng", "S"),
      FollowingPerson("f-6", "Amara Nwosu", "@amara-nwosu", "A"),
      FollowingPerson("f-7", "Chidi Okafor", "@chidi-okafor", "C")
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .windowInsetsPadding(HavenInsets.navigationBars.only(WindowInsetsSides.Bottom))
      .testTag("following_screen")
  ) {
    HavenTopBarContainer(
      backgroundColor = HavenColors.Canvas,
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(44.dp)
            .testTag("following_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Navigate back",
            tint = HavenColors.TextPrimary,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }

    if (people.isEmpty()) {
      // Frame 04: Empty State
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(HavenColors.SurfaceContainer)
            .border(1.dp, HavenColors.BorderSubtle, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Group,
            contentDescription = null,
            tint = HavenColors.TextPrimary,
            modifier = Modifier.size(24.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "You are not following anyone yet",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 20.sp,
          color = HavenColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Follow makers from their profile and they will appear here.",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 15.sp,
          lineHeight = 22.sp,
          color = HavenColors.TextSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        HavenPrimaryButton(
          text = "Browse offers",
          onClick = onBrowseOffers,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .testTag("following_empty_browse_button")
        )
      }
    } else {
      // Frame 03: Grouped 7 Person Rows in 12px Rounded Container
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
      ) {
        item {
          Text(
            text = "Following",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 25.sp,
            letterSpacing = (-0.4).sp,
            color = HavenColors.TextPrimary,
            modifier = Modifier
              .padding(bottom = 16.dp)
              .testTag("following_title")
          )
        }

        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(HavenColors.Canvas)
              .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
          ) {
            people.forEachIndexed { index, person ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(64.dp)
                  .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f, fill = false)
                ) {
                  // 40px Avatar
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(CircleShape)
                      .background(HavenColors.SurfaceContainer)
                      .border(1.dp, HavenColors.BorderSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    if (!person.avatarUrl.isNullOrBlank()) {
                      SubcomposeAsyncImage(
                        model = person.avatarUrl,
                        contentDescription = person.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                          Text(
                            text = person.initial,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = HavenColors.TextPrimary
                          )
                        },
                        error = {
                          Text(
                            text = person.initial,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = HavenColors.TextPrimary
                          )
                        }
                      )
                    } else {
                      Text(
                        text = person.initial,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = HavenColors.TextPrimary
                      )
                    }
                  }

                  Spacer(modifier = Modifier.width(12.dp))

                  Column {
                    Text(
                      text = person.name,
                      fontFamily = InterFontFamily,
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 15.sp,
                      color = HavenColors.TextPrimary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = person.handle,
                      fontFamily = InterFontFamily,
                      fontSize = 13.sp,
                      color = HavenColors.TextSecondary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }

                // 36px Outline "Following" action
                Box(
                  modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (person.isFollowing) HavenColors.Canvas else HavenColors.Primary)
                    .border(
                      1.dp,
                      if (person.isFollowing) HavenColors.BorderDefault else HavenColors.Primary,
                      RoundedCornerShape(8.dp)
                    )
                    .clickable { person.isFollowing = !person.isFollowing }
                    .padding(horizontal = 14.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = if (person.isFollowing) "Following" else "Follow",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (person.isFollowing) HavenColors.TextPrimary else HavenColors.TextOnDark
                  )
                }
              }

              if (index < people.size - 1) {
                HorizontalDivider(color = HavenColors.BorderSubtle, thickness = 1.dp)
              }
            }
          }
        }
      }
    }
  }
}

// =========================================================================
// SECTION 3: MY OFFERS SCREEN (/profile/offers)
// Frame 05 (6 Offers + Photo Missing State), Frame 06 (Delete Dialog), Frame 07 (Empty)
// =========================================================================

data class MyOfferItem(
  val id: String,
  val title: String,
  val priceDisplay: String,
  val imageResId: Int,
  val hasPhoto: Boolean = true
)

@Composable
fun MyOffersScreen(
  onBack: () -> Unit,
  onCreateOffer: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler { onBack() }

  val myOffers = remember {
    mutableStateListOf(
      MyOfferItem("m-1", "Hand-woven Aso Oke fabric, 6 yards", "45,000 NGN", R.drawable.fabric_craft),
      MyOfferItem("m-2", "Aso Oke gele, made to order", "15,000 NGN", R.drawable.fabric_craft),
      MyOfferItem("m-3", "Aso Oke cap (fila), hand-woven", "12,000 NGN", R.drawable.fabric_craft),
      MyOfferItem("m-4", "Custom Aso Oke outfit for weddings", "120,000 NGN", R.drawable.fabric_craft),
      MyOfferItem("m-5", "Weaving lessons for beginners, 1 day", "20,000 NGN", 0, hasPhoto = false),
      MyOfferItem("m-6", "Offcut bundle, assorted colours", "Free", R.drawable.fabric_craft)
    )
  }

  var offerToDelete by remember { mutableStateOf<MyOfferItem?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .windowInsetsPadding(HavenInsets.navigationBars.only(WindowInsetsSides.Bottom))
      .testTag("my_offers_screen")
  ) {
    HavenTopBarContainer(
      backgroundColor = HavenColors.Canvas,
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(44.dp)
            .testTag("my_offers_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Navigate back",
            tint = HavenColors.TextPrimary,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }

    if (myOffers.isEmpty()) {
      // Frame 07: Empty State
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(HavenColors.SurfaceContainer)
            .border(1.dp, HavenColors.BorderSubtle, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Storefront,
            contentDescription = null,
            tint = HavenColors.TextPrimary,
            modifier = Modifier.size(24.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "You haven't listed any offers yet",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 20.sp,
          color = HavenColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Create an offer to make what you provide available for others to see.",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 15.sp,
          lineHeight = 22.sp,
          color = HavenColors.TextSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        HavenPrimaryButton(
          text = "Create an offer",
          onClick = onCreateOffer,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .testTag("my_offers_empty_create_button")
        )
      }
    } else {
      // Frame 05: 6 Offers Grid
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
      ) {
        item {
          Text(
            text = "My offers",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 25.sp,
            letterSpacing = (-0.4).sp,
            color = HavenColors.TextPrimary,
            modifier = Modifier.testTag("my_offers_title")
          )

          val notVisibleCount = myOffers.count { !it.hasPhoto }
          Text(
            text = "${myOffers.size} offers · $notVisibleCount not visible yet",
            fontFamily = InterFontFamily,
            fontSize = 14.sp,
            color = HavenColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
          )

          // 48px Solid Create Button with Plus Icon
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(HavenColors.Primary)
              .clickable { onCreateOffer() }
              .padding(horizontal = 16.dp)
              .testTag("create_offer_action_button"),
            contentAlignment = Alignment.Center
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = HavenColors.TextOnDark,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = "Create an offer",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = HavenColors.TextOnDark
              )
            }
          }

          Spacer(modifier = Modifier.height(24.dp))
        }

        // Pairs of cards in 2-column grid
        val rowCount = (myOffers.size + 1) / 2
        items(rowCount) { rowIndex ->
          val firstIndex = rowIndex * 2
          val secondIndex = firstIndex + 1

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(modifier = Modifier.weight(1f)) {
              val item = myOffers[firstIndex]
              MyOfferCard(
                item = item,
                onEdit = { /* edit offer */ },
                onDelete = { offerToDelete = item }
              )
            }

            Box(modifier = Modifier.weight(1f)) {
              if (secondIndex < myOffers.size) {
                val item = myOffers[secondIndex]
                MyOfferCard(
                  item = item,
                  onEdit = { /* edit offer */ },
                  onDelete = { offerToDelete = item }
                )
              } else {
                Spacer(modifier = Modifier.fillMaxWidth())
              }
            }
          }
        }
      }
    }

    // Frame 06: Delete Confirmation Dialog
    offerToDelete?.let { offer ->
      Dialog(
        onDismissRequest = { offerToDelete = null },
        properties = DialogProperties(usePlatformDefaultWidth = false)
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null
            ) { offerToDelete = null },
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .width(320.dp)
              .clip(RoundedCornerShape(28.dp))
              .background(HavenColors.SurfaceCard)
              .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(28.dp))
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
              ) { /* consume inside */ }
              .padding(24.dp)
              .testTag("delete_offer_dialog")
          ) {
            Column {
              Text(
                text = "Delete offer",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 25.sp,
                color = HavenColors.TextPrimary,
                modifier = Modifier.testTag("delete_dialog_title")
              )

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = "Are you sure you want to delete “${offer.title}”? This action cannot be undone.",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = HavenColors.TextSecondary
              )

              Spacer(modifier = Modifier.height(24.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { offerToDelete = null }
                    .padding(horizontal = 16.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "Cancel",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = HavenColors.TextPrimary,
                    modifier = Modifier.testTag("delete_dialog_cancel_button")
                  )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                  modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HavenColors.Primary)
                    .clickable {
                      myOffers.remove(offer)
                      offerToDelete = null
                    }
                    .padding(horizontal = 16.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "Confirm delete",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = HavenColors.TextOnDark,
                    modifier = Modifier.testTag("delete_dialog_confirm_button")
                  )
                }
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
  item: MyOfferItem,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    if (!item.hasPhoto) {
      // Offer 5: Unpublished State with "Not visible yet" pill
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1f)
          .clip(RoundedCornerShape(12.dp))
          .background(HavenColors.SurfaceContainer)
          .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
          .padding(10.dp),
        contentAlignment = Alignment.Center
      ) {
        // Top-Left Pill
        Box(
          modifier = Modifier
            .align(Alignment.TopStart)
            .clip(RoundedCornerShape(10.dp))
            .background(HavenColors.TextPrimary)
            .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
          Text(
            text = "Not visible yet",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            color = HavenColors.TextOnDark
          )
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.padding(top = 12.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.CameraAlt,
            contentDescription = null,
            tint = HavenColors.TextSecondary,
            modifier = Modifier.size(28.dp)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Add a photo to publish",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = HavenColors.TextSecondary,
            textAlign = TextAlign.Center
          )
        }
      }
    } else {
      // Normal Available Card
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1f)
          .clip(RoundedCornerShape(12.dp))
          .background(HavenColors.SurfaceContainer)
          .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
      ) {
        Image(
          painter = painterResource(id = item.imageResId),
          contentDescription = item.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = item.title,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 14.sp,
      color = HavenColors.TextPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )

    Spacer(modifier = Modifier.height(2.dp))

    Text(
      text = item.priceDisplay,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 14.sp,
      color = HavenColors.TextPrimary
    )

    Spacer(modifier = Modifier.height(10.dp))

    // 36px Outline Actions (Edit and Delete)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .height(36.dp)
          .clip(RoundedCornerShape(6.dp))
          .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(6.dp))
          .clickable { onEdit() },
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Edit",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 13.sp,
          color = HavenColors.TextPrimary
        )
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .height(36.dp)
          .clip(RoundedCornerShape(6.dp))
          .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(6.dp))
          .clickable { onDelete() },
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Delete",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 13.sp,
          color = HavenColors.TextPrimary
        )
      }
    }
  }
}
