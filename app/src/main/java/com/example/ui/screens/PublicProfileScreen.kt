package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Inventory2
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.example.R
import com.example.ui.components.HavenBackButton
import com.example.ui.components.HavenInsets
import com.example.ui.components.HavenSaveButton
import com.example.ui.components.HavenTopBarContainer
import com.example.ui.model.Offer
import com.example.ui.model.SampleOffers
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

private const val DEFAULT_MAKER_AVATAR_URL =
  "https://lh3.googleusercontent.com/aida/AEtjO1XT1RMubL8e8hGOBhw0NvVZyAtLhFXR1IlcXKlP77a0ZvKEXNjhZYJWYiQGy_NxHAEs-n9GJhFMoevM7g5HjrJWBU4NxEXmz-YJXxy_k9A9-crh3u1LNXdTOLbxoETTKILuDGSJC5ZCM01HqG-2e71Ilkdm7DFBWuO6ljlIEjq7PSEAW7L-ddjXkY-fO0VH3GqF89oc8Cu0CWZISQZu8NwYUFWgcU_RSDU4cM2x6Oju8m0IrHNemURl_yzw"

data class PublicProfileOfferItem(
  val id: String,
  val title: String,
  val priceDisplay: String,
  val imageResId: Int,
  val imageUrl: String = "",
  var isSaved: Boolean = false
)

/**
 * Public Profile Screen (/profiles/{handle})
 * Adheres strictly to the Haven monastic greyscale design system:
 * - Back-only top bar (48px touch target)
 * - Centred 96px avatar, 22px name, 14px handle & joined date, 16px bio
 * - Viewer State: 48px Follow button (Solid black when unfollowed, outline with check when following)
 * - Owner State: Muted note "This is how your profile appears to everyone." (Frame 06)
 * - 2-column grid of 6 cards without maker line
 * - STRICTLY NO HEARTS FOR OWNER (Frame 06) vs. 32px White Circle Heart Button for Viewer (Frame 05)
 * - Empty State (Frame 07): "No offers yet" with box icon
 */
@Composable
fun PublicProfileScreen(
  onBack: () -> Unit,
  onOpenOffer: (Offer) -> Unit = {},
  makerName: String = "Adaeze Obi",
  makerHandle: String = "adaeze-obi",
  joinedDate: String = "Joined March 2026",
  bioText: String = "Master artisan weaver specializing in traditional ceremonial Aso Oke fabrics, heirloom textiles, and custom wedding attires.",
  avatarUrl: String = DEFAULT_MAKER_AVATAR_URL,
  isOwnerInitial: Boolean = false,
  isFollowingInitial: Boolean = true,
  isEmptyInitial: Boolean = false,
  modifier: Modifier = Modifier
) {
  BackHandler { onBack() }

  var isOwner by remember { mutableStateOf(isOwnerInitial) }
  var isFollowing by remember { mutableStateOf(isFollowingInitial) }
  var isEmpty by remember { mutableStateOf(isEmptyInitial) }

  // 6 sample listing cards matching Section 2 Frame 05 & 06
  val profileOffers = remember {
    mutableStateListOf(
      PublicProfileOfferItem(
        id = "p-1",
        title = "Hand-woven Aso Oke fabric, 6 yards",
        priceDisplay = "45,000 NGN",
        imageResId = R.drawable.fabric_craft,
        imageUrl = "https://lh3.googleusercontent.com/aida/AEtjO1XPeKhs-jdQJzrx4DgSpBY5d2U7viS72LSoBderGt3o3yQ4kNfJlUSu1sF5HpcX3VkfKpj2YzZmIA9mPyU6xPv8K2Lc3NaZGOiNxwpKUw-AMr8LiAL7h98SG0f_UTJ9BgHthDunh92Q_tGYi1v6tJb4mtZz0L8tK23Fb7opxbKsfUSw_iBkPZNZHv6ecFwG9wW5PLfWzYU_Cd0JMw-g9mCIGBQkS0aM7wSs2s-J9u219OEL1ukWYWw2mvij",
        isSaved = true
      ),
      PublicProfileOfferItem(
        id = "p-2",
        title = "Aso Oke gele, made to order",
        priceDisplay = "15,000 NGN",
        imageResId = R.drawable.fabric_craft,
        imageUrl = "https://lh3.googleusercontent.com/aida/AEtjO1XLWYt0F5VxrDL3wJOuaKKtLlZF8H2ww3K9lH2Nd8Q_ddYjIsrpKGHx3hwN_X5FRNr2F5cqjYyAbsSJ-Kc9czVqk4VVj5al8XpSc7xFqQyKl8ON114p3dmJ2VRveIJ4QEWWBzoI1Lo90cPVs866Nevur3CIUEEt_WBvKoeb-mbwE8bQtmd5wSgXw9_CtR98QdcGo8yhPCjSl6Vm5ATqygFAVUMGqoW2POFosyBTmbgXNXsV9wact1rsMVFN",
        isSaved = false
      ),
      PublicProfileOfferItem(
        id = "p-3",
        title = "Aso Oke cap (fila), hand-woven",
        priceDisplay = "12,000 NGN",
        imageResId = R.drawable.fabric_craft,
        imageUrl = "https://lh3.googleusercontent.com/aida/AEtjO1UEVbbaU_DTh5Yxl-gsrirVw_g4SP5G75PLgLCCtSwb9iFxoc7qLTngW3g4IBr1TWy_tifez__x7vpI05ppEkiQGN45YN183UP1PYQ6MWxmthCVAMMqEWlUmM_zrdVrM8aCm9rKx5ekapZLQkaE9fMa8xHBpffoV6jyMNIh15WA_7KLzf1wzsFocyXtKqJzpzOIcAIyHXAvOaqV69hwvbvvhOCopuSD8r8yXkxRWC43n33sG9I7jlePmXCz",
        isSaved = false
      ),
      PublicProfileOfferItem(
        id = "p-4",
        title = "Custom Aso Oke outfit for weddings",
        priceDisplay = "120,000 NGN",
        imageResId = R.drawable.fabric_craft,
        imageUrl = "https://lh3.googleusercontent.com/aida/AEtjO1Xmqt3LsLGxkpd5OHPN9TBWizqZclRQw1J56vroPbhKpNpxyW1DrDwL6r2j3dVVsBkI-qRPjZyguHFJ8jMp_U5xzFmHPQzcWQqOcJxwnqtmdcOFIVUzxZCKILxhLEoHuXVLGMm_k6mI9AMl9l-73aliMfErKlCAxZ3ivZaRy9nijOFX9CBbr9a5dYJ6Ehw-Sw73FvyE4OacWGIceV1TySiTLFVESC63OIqNL-eWT3pM3HlHFASQyojDQU-a",
        isSaved = false
      ),
      PublicProfileOfferItem(
        id = "p-5",
        title = "Weaving lessons for beginners, 1 day",
        priceDisplay = "20,000 NGN",
        imageResId = R.drawable.guitar_lessons,
        imageUrl = "https://lh3.googleusercontent.com/aida/AEtjO1WQbeECMZzTV3BxWp6k4qHqo4JA3PpgCC2qfN6QUcZrPoj820GEpZi6n_2hyaKIFtdaCYzq5g3ld-wmHAdo5RLt--MUBOTntkvZpFFFIpzENaXEFFWTIsHU8xopF7FRR8Uz9ZloTVk3-RLvLYV-0ElxLzgLyYXOEl3ESQ5pUuF-KQBzS1ySygO130pLYhK_PloEcnoVFExSAIxEqvKRxiI5J1QUTPL0vqHzTyHzk-VOma-iMVHM9c4tM8Rb",
        isSaved = false
      ),
      PublicProfileOfferItem(
        id = "p-6",
        title = "Offcut bundle, assorted colours",
        priceDisplay = "Free",
        imageResId = R.drawable.veg_box,
        imageUrl = "https://lh3.googleusercontent.com/aida/AEtjO1WsTEroDCJvKwpyC1Q2rOipT83ueZAPxkcTjLNubMajFh3hNXWbMPcf3bMyL4RRJ72lxtLsP0Zfp7sCwbvoqskWFHuhOokHNpOspfIM12_Ue9qkiSYAuEV_Z1Iy2wEdjHkWBL6rRNOArh50ac35cDMIAg7RnY_aRu1AXwXYy_VgFfxJemsP_3EPNYWhjcJ09Jug16Oioa9UYgjKfejgu0tItnsJSCn7TjV4MeUogetNGe8AmN-8amOugXw",
        isSaved = false
      )
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .windowInsetsPadding(HavenInsets.navigationBars.only(WindowInsetsSides.Bottom))
      .testTag("public_profile_screen")
  ) {
    // Back-Only Top Bar (48px)
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
        HavenBackButton(
          onBack = onBack,
          testTag = "public_profile_back_button"
        )
      }
    }

    // Scrollable Profile Content
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
      // Centred Identity Block
      item {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Centred 96px Avatar
          Box(
            modifier = Modifier
              .size(96.dp)
              .clip(CircleShape)
              .background(HavenColors.SurfaceContainer)
              .border(1.dp, HavenColors.BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            if (avatarUrl.isNotBlank()) {
              SubcomposeAsyncImage(
                model = avatarUrl,
                contentDescription = makerName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .background(HavenColors.SurfaceContainer),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = makerName.firstOrNull()?.uppercase() ?: "A",
                      fontFamily = InterFontFamily,
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 32.sp,
                      color = HavenColors.TextPrimary
                    )
                  }
                },
                error = {
                  Text(
                    text = makerName.firstOrNull()?.uppercase() ?: "A",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 32.sp,
                    color = HavenColors.TextPrimary
                  )
                }
              )
            } else {
              Text(
                text = makerName.firstOrNull()?.uppercase() ?: "A",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp,
                color = HavenColors.TextPrimary
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Maker Name (22sp semibold)
          Text(
            text = makerName,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            color = HavenColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("public_profile_name")
          )

          Spacer(modifier = Modifier.height(4.dp))

          // Handle & Joined
          Text(
            text = "@$makerHandle · $joinedDate",
            fontFamily = InterFontFamily,
            fontSize = 14.sp,
            color = HavenColors.TextSecondary,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Bio
          Text(
            text = bioText,
            fontFamily = InterFontFamily,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = HavenColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
          )

          Spacer(modifier = Modifier.height(20.dp))

          // Action / Note Block
          if (isOwner) {
            // Frame 06: Owner View Note
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(HavenColors.SurfaceContainer)
                .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(8.dp))
                .padding(vertical = 10.dp, horizontal = 12.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "This is how your profile appears to everyone.",
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                color = HavenColors.TextSecondary,
                textAlign = TextAlign.Center
              )
            }
          } else {
            // Frame 05 & 07: 48px Follow Button
            if (isFollowing) {
              // Following State: 48px Outline Button with Check icon
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .border(1.dp, HavenColors.TextPrimary, RoundedCornerShape(8.dp))
                  .background(HavenColors.Canvas)
                  .clickable { isFollowing = false }
                  .testTag("public_profile_following_button"),
                contentAlignment = Alignment.Center
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = HavenColors.TextPrimary,
                    modifier = Modifier.size(18.dp)
                  )
                  Text(
                    text = "Following",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = HavenColors.TextPrimary
                  )
                }
              }
            } else {
              // Unfollowed State: 48px Solid Black Button
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(HavenColors.TextPrimary)
                  .clickable { isFollowing = true }
                  .testTag("public_profile_follow_button"),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "Follow",
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.Medium,
                  fontSize = 16.sp,
                  color = HavenColors.TextOnDark
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(24.dp))
          HorizontalDivider(thickness = 1.dp, color = HavenColors.BorderSubtle)
          Spacer(modifier = Modifier.height(24.dp))
        }
      }

      // Offers Section Header
      item {
        Text(
          text = "Offers by $makerName",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 20.sp,
          color = HavenColors.TextPrimary,
          modifier = Modifier.padding(bottom = 16.dp)
        )
      }

      // Empty State (Frame 07)
      if (isEmpty) {
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(HavenColors.SurfaceContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = HavenColors.TextSecondary,
                modifier = Modifier.size(24.dp)
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = "No offers yet",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 20.sp,
              color = HavenColors.TextPrimary,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = "This person has not posted any offers yet. Check back later.",
              fontFamily = InterFontFamily,
              fontSize = 14.sp,
              lineHeight = 20.sp,
              color = HavenColors.TextSecondary,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(horizontal = 24.dp)
            )
          }
        }
      } else {
        // 2-Column Grid of 6 Listing Cards WITHOUT maker line
        val offers = profileOffers
        val rowCount = (offers.size + 1) / 2
        for (rowIndex in 0 until rowCount) {
          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              val firstIndex = rowIndex * 2
              val secondIndex = firstIndex + 1

              Box(modifier = Modifier.weight(1f)) {
                val item = offers[firstIndex]
                PublicProfileOfferCard(
                  item = item,
                  isOwner = isOwner,
                  onOpen = {
                    onOpenOffer(
                      Offer(
                        id = item.id,
                        title = item.title,
                        fullTitle = item.title,
                        creatorName = makerName,
                        creatorInitial = makerName.firstOrNull()?.toString() ?: "A",
                        priceDisplay = item.priceDisplay,
                        currency = "NGN",
                        priceNumeric = 45000.0,
                        imageResId = item.imageResId,
                        imageUrl = item.imageUrl,
                        isFavorite = item.isSaved
                      )
                    )
                  },
                  onToggleSave = {
                    item.isSaved = !item.isSaved
                  }
                )
              }

              Box(modifier = Modifier.weight(1f)) {
                if (secondIndex < offers.size) {
                  val item = offers[secondIndex]
                  PublicProfileOfferCard(
                    item = item,
                    isOwner = isOwner,
                    onOpen = {
                      onOpenOffer(
                        Offer(
                          id = item.id,
                          title = item.title,
                          fullTitle = item.title,
                          creatorName = makerName,
                          creatorInitial = makerName.firstOrNull()?.toString() ?: "A",
                          priceDisplay = item.priceDisplay,
                          currency = "NGN",
                          priceNumeric = 15000.0,
                          imageResId = item.imageResId,
                          imageUrl = item.imageUrl,
                          isFavorite = item.isSaved
                        )
                      )
                    },
                    onToggleSave = {
                      item.isSaved = !item.isSaved
                    }
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
}

/**
 * Public Profile Offer Card:
 * 1:1 Aspect ratio photo, title, price.
 * STRICTLY NO MAKER LINE (per design specification).
 * STRICTLY NO HEARTS FOR OWNER (Frame 06).
 */
@Composable
private fun PublicProfileOfferCard(
  item: PublicProfileOfferItem,
  isOwner: Boolean,
  onOpen: () -> Unit,
  onToggleSave: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onOpen() }
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(RoundedCornerShape(12.dp))
        .background(HavenColors.SurfaceContainer)
        .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
    ) {
      if (item.imageUrl.isNotBlank()) {
        SubcomposeAsyncImage(
          model = item.imageUrl,
          contentDescription = item.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
          loading = {
            Image(
              painter = painterResource(id = item.imageResId),
              contentDescription = item.title,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          },
          error = {
            Image(
              painter = painterResource(id = item.imageResId),
              contentDescription = item.title,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          }
        )
      } else {
        Image(
          painter = painterResource(id = item.imageResId),
          contentDescription = item.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }

      // STRICTLY NO HEART FOR OWNER (Frame 06) vs. White Circle Heart Button for Viewer (Frame 05)
      if (!isOwner) {
        HavenSaveButton(
          isSaved = item.isSaved,
          onToggle = onToggleSave,
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp)
            .testTag("public_profile_save_button_${item.id}")
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Title (14sp medium, truncate)
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

    // Price (14sp semibold)
    Text(
      text = item.priceDisplay,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 14.sp,
      color = HavenColors.TextPrimary
    )
  }
}
