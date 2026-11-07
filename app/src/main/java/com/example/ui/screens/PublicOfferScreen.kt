package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.example.ui.components.HavenBackButton
import com.example.ui.components.HavenBackButtonVariant
import com.example.ui.components.HavenInsets
import com.example.ui.components.HavenSaveButton
import com.example.ui.model.Offer
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily
import kotlinx.coroutines.delay

private const val PHOTO_1_URL =
  "https://lh3.googleusercontent.com/aida/AEtjO1XPeKhs-jdQJzrx4DgSpBY5d2U7viS72LSoBderGt3o3yQ4kNfJlUSu1sF5HpcX3VkfKpj2YzZmIA9mPyU6xPv8K2Lc3NaZGOiNxwpKUw-AMr8LiAL7h98SG0f_UTJ9BgHthDunh92Q_tGYi1v6tJb4mtZz0L8tK23Fb7opxbKsfUSw_iBkPZNZHv6ecFwG9wW5PLfWzYU_Cd0JMw-g9mCIGBQkS0aM7wSs2s-J9u219OEL1ukWYWw2mvij"

private const val PHOTO_2_URL =
  "https://lh3.googleusercontent.com/aida/AEtjO1XLWYt0F5VxrDL3wJOuaKKtLlZF8H2ww3K9lH2Nd8Q_ddYjIsrpKGHx3hwN_X5FRNr2F5cqjYyAbsSJ-Kc9czVqk4VVj5al8XpSc7xFqQyKl8ON114p3dmJ2VRveIJ4QEWWBzoI1Lo90cPVs866Nevur3CIUEEt_WBvKoeb-mbwE8bQtmd5wSgXw9_CtR98QdcGo8yhPCjSl6Vm5ATqygFAVUMGqoW2POFosyBTmbgXNXsV9wact1rsMVFN"

private const val PHOTO_3_URL =
  "https://lh3.googleusercontent.com/aida/AEtjO1WsTEroDCJvKwpyC1Q2rOipT83ueZAPxkcTjLNubMajFh3hNXWbMPcf3bMyL4RRJ72lxtLsP0Zfp7sCwbvoqskWFHuhOokHNpOspfIM12_Ue9qkiSYAuEV_Z1Iy2wEdjHkWBL6rRNOArh50ac35cDMIAg7RnY_aRu1AXwXYy_VgFfxJemsP_3EPNYWhjcJ09Jug16Oioa9UYgjKfejgu0tItnsJSCn7TjV4MeUogetNGe8AmN-8amOugXw"

private const val MAKER_AVATAR_URL =
  "https://lh3.googleusercontent.com/aida/AEtjO1XT1RMubL8e8hGOBhw0NvVZyAtLhFXR1IlcXKlP77a0ZvKEXNjhZYJWYiQGy_NxHAEs-n9GJhFMoevM7g5HjrJWBU4NxEXmz-YJXxy_k9A9-crh3u1LNXdTOLbxoETTKILuDGSJC5ZCM01HqG-2e71Ilkdm7DFBWuO6ljlIEjq7PSEAW7L-ddjXkY-fO0VH3GqF89oc8Cu0CWZISQZu8NwYUFWgcU_RSDU4cM2x6Oju8m0IrHNemURl_yzw"

/**
 * Public Offer Screen (/offers/{slug})
 * Adheres strictly to the Haven monastic greyscale design system:
 * - No top bar; floating 40px circle controls over 4:3 photo carousel
 * - Counter pill (1 / 3) in bottom-right corner of carousel
 * - Tappable "Offered by" maker row linking directly to public profile
 * - Strict Owner View distinction (strictly NO heart button for owner, "Manage offer" action)
 * - Visitor View distinction (unfilled heart, "Sign in to message")
 * - Message Bottom Sheet with soft keyboard lifting (Frame 04)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PublicOfferScreen(
  offer: Offer,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  onOpenMakerProfile: (String) -> Unit = {},
  onManageOffer: () -> Unit = {},
  onSendMessage: (String) -> Unit = {},
  onSignInToMessage: () -> Unit = {},
  isOwnerInitial: Boolean = false,
  isAuthenticated: Boolean = true
) {
  BackHandler { onBack() }

  var isOwner by remember { mutableStateOf(isOwnerInitial) }
  var isSaved by remember { mutableStateOf(offer.isFavorite) }
  var isMessageSheetVisible by remember { mutableStateOf(false) }
  var messageDraft by remember {
    mutableStateOf("Hello Adaeze, is the indigo shade with gold thread still available for delivery by next Friday? Would love to order 6 yards.")
  }
  var toastMessage by remember { mutableStateOf<String?>(null) }
  var showToast by remember { mutableStateOf(false) }

  val photos = remember(offer) {
    listOf(
      offer.imageUrl.ifBlank { PHOTO_1_URL },
      PHOTO_2_URL,
      PHOTO_3_URL
    )
  }

  val pagerState = rememberPagerState(initialPage = 0, pageCount = { photos.size })

  LaunchedEffect(showToast) {
    if (showToast) {
      delay(3000)
      showToast = false
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .testTag("public_offer_screen")
  ) {
    // Scrollable Offer Content
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(bottom = 104.dp) // Clearance above 52dp button + insets
    ) {
      // 4:3 Hero Carousel Area (390px wide x 292.5px tall)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(4f / 3f)
          .background(HavenColors.SurfaceContainer)
      ) {
        HorizontalPager(
          state = pagerState,
          modifier = Modifier.fillMaxSize()
        ) { page ->
          SubcomposeAsyncImage(
            model = photos[page],
            contentDescription = offer.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(HavenColors.SurfaceContainer)
              )
            }
          )
        }

        // Floating Top Controls (Status bar protected, 36dp padding from top)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Floating 40px Circle Back Button
          HavenBackButton(
            onBack = onBack,
            variant = HavenBackButtonVariant.FLOATING,
            size = 40.dp,
            iconSize = 20.dp,
            testTag = "public_offer_back_button"
          )

          // Floating 40px Circle Heart Button (STRICTLY OMITTED FOR OWNER)
          if (!isOwner) {
            HavenSaveButton(
              isSaved = isSaved,
              onToggle = {
                isSaved = !isSaved
                toastMessage = if (isSaved) "Offer saved." else "Offer removed from saved."
                showToast = true
              },
              size = 40.dp,
              iconSize = 20.dp,
              testTag = "public_offer_heart_button"
            )
          }
        }

        // Bottom-Right Counter Pill (e.g. 1 / 3)
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.60f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = "${pagerState.currentPage + 1} / ${photos.size}",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp,
            color = Color.White
          )
        }
      }

      // Content Details (16px side margins)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 20.dp)
      ) {
        // Owner View Notice Line (Frame 03)
        if (isOwner) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(HavenColors.TextPrimary)
            )
            Text(
              text = "Public offer",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 13.sp,
              color = HavenColors.TextSecondary
            )
          }
        }

        // Offer Title (25sp semibold)
        Text(
          text = offer.title,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 25.sp,
          lineHeight = 31.sp,
          letterSpacing = (-0.5).sp,
          color = HavenColors.TextPrimary,
          modifier = Modifier.testTag("public_offer_title")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Price (20sp semibold)
        Text(
          text = offer.priceDisplay,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 20.sp,
          color = HavenColors.TextPrimary,
          modifier = Modifier.testTag("public_offer_price")
        )

        Spacer(modifier = Modifier.height(20.dp))

        // "Offered by" Row (Tappable to Public Profile)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onOpenMakerProfile(offer.creatorName.lowercase().replace(" ", "-")) }
            .padding(vertical = 4.dp)
            .testTag("public_offer_maker_row"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            SubcomposeAsyncImage(
              model = MAKER_AVATAR_URL,
              contentDescription = offer.creatorName,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, HavenColors.BorderSubtle, CircleShape)
            )

            Column {
              Text(
                text = if (isOwner) "OFFERED BY YOU" else "OFFERED BY",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp,
                color = HavenColors.TextSecondary
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = offer.creatorName,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = HavenColors.TextPrimary
              )
              Spacer(modifier = Modifier.height(1.dp))
              Text(
                text = "@${offer.creatorName.lowercase().replace(" ", "-")}",
                fontFamily = InterFontFamily,
                fontSize = 14.sp,
                color = HavenColors.TextSecondary
              )
            }
          }

          Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = "View profile",
            tint = HavenColors.BorderStrong,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(thickness = 1.dp, color = HavenColors.BorderSubtle)
        Spacer(modifier = Modifier.height(20.dp))

        // "About this offer"
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "ABOUT THIS OFFER",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 0.8.sp,
            color = HavenColors.TextSecondary
          )

          Text(
            text = if (offer.description.isNotBlank()) {
              offer.description
            } else {
              "Narrow-loom Aso Oke in indigo with gold thread, six yards, enough for a full wedding outfit. Made to order in about two weeks."
            },
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = HavenColors.TextPrimary
          )

          Text(
            text = "Each strip is hand-woven on traditional West African narrow looms using 100% organic cotton warp and metallic lurex weft. Custom sizing and ceremonial embroidery motifs available upon message request.",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = HavenColors.TextSecondary
          )
        }
      }
    }

    // Pinned Bottom Action Bar (52px button)
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .background(HavenColors.Canvas)
        .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(0.dp))
        .windowInsetsPadding(HavenInsets.navigationBars.only(WindowInsetsSides.Bottom))
        .padding(horizontal = 16.dp, vertical = 12.dp)
        .testTag("public_offer_bottom_bar")
    ) {
      if (isOwner) {
        // Frame 03: Owner View ("Manage offer")
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(HavenColors.TextPrimary)
            .clickable { onManageOffer() }
            .testTag("manage_offer_button"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Manage offer",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = HavenColors.TextOnDark
          )
        }
      } else if (!isAuthenticated) {
        // Frame 02: Visitor View ("Sign in to message")
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(HavenColors.TextPrimary)
            .clickable { onSignInToMessage() }
            .testTag("visitor_sign_in_button"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Sign in to message",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = HavenColors.TextOnDark
          )
        }
      } else {
        // Frame 01: Signed-in Buyer ("Send message")
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(HavenColors.TextPrimary)
            .clickable { isMessageSheetVisible = true }
            .testTag("send_message_cta_button"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Send message",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = HavenColors.TextOnDark
          )
        }
      }
    }

    // Frame 04: Message Bottom Sheet Modal Overlay & Content
    if (isMessageSheetVisible) {
      val focusManager = LocalFocusManager.current

      // Dimmed Background Overlay
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.45f))
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
          ) { isMessageSheetVisible = false }
      )

      // Bottom Sheet Container (Lifted smoothly above keyboard)
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .windowInsetsPadding(WindowInsets.ime)
          .windowInsetsPadding(HavenInsets.navigationBars.only(WindowInsetsSides.Bottom))
          .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
          .background(HavenColors.Canvas)
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
          ) { /* consume clicks */ }
          .testTag("message_bottom_sheet")
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Drag Handle
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 12.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(HavenColors.BorderDefault)
            )
          }

          // Sheet Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(0.dp))
              .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Message ${offer.creatorName}",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = HavenColors.TextPrimary
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Regarding: ${offer.title}",
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                color = HavenColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            // Close Button
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(HavenColors.SurfaceContainer)
                .clickable { isMessageSheetVisible = false },
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close sheet",
                tint = HavenColors.TextSecondary,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          // Form Body inside Sheet
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp)
          ) {
            Text(
              text = "Your message",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 14.sp,
              color = HavenColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4-Line Text Area (Up to 2000 characters)
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(HavenColors.Canvas)
                .border(1.dp, HavenColors.TextPrimary, RoundedCornerShape(8.dp))
                .padding(12.dp),
              contentAlignment = Alignment.TopStart
            ) {
              BasicTextField(
                value = messageDraft,
                onValueChange = {
                  if (it.length <= 2000) messageDraft = it
                },
                textStyle = TextStyle(
                  fontFamily = InterFontFamily,
                  fontSize = 16.sp,
                  lineHeight = 23.sp,
                  color = HavenColors.TextPrimary
                ),
                cursorBrush = SolidColor(HavenColors.TextPrimary),
                modifier = Modifier
                  .fillMaxSize()
                  .testTag("message_sheet_textarea")
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Character Counter
            Text(
              text = "${messageDraft.length} / 2,000 characters",
              fontFamily = InterFontFamily,
              fontSize = 12.sp,
              color = HavenColors.TextSecondary,
              textAlign = TextAlign.End,
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 52px Primary "Send message" button
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(HavenColors.TextPrimary)
                .clickable {
                  focusManager.clearFocus()
                  isMessageSheetVisible = false
                  onSendMessage(messageDraft)
                  toastMessage = "Message sent to ${offer.creatorName}."
                  showToast = true
                }
                .testTag("message_sheet_send_button"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Send message",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = HavenColors.TextOnDark
              )
            }
          }
        }
      }
    }

    // Feedback Toast Snackbar
    AnimatedVisibility(
      visible = showToast,
      enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(HavenColors.TextPrimary)
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = toastMessage ?: "",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = HavenColors.TextOnDark
          )
          Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = HavenColors.TextOnDark,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
