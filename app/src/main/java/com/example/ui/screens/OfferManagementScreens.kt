package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.ui.components.HavenBackButton
import com.example.ui.components.HavenInsets
import com.example.ui.components.HavenTopBarContainer
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily
import kotlinx.coroutines.delay

private const val SAMPLE_COVER_PHOTO_URL =
  "https://lh3.googleusercontent.com/aida/AEtjO1XPeKhs-jdQJzrx4DgSpBY5d2U7viS72LSoBderGt3o3yQ4kNfJlUSu1sF5HpcX3VkfKpj2YzZmIA9mPyU6xPv8K2Lc3NaZGOiNxwpKUw-AMr8LiAL7h98SG0f_UTJ9BgHthDunh92Q_tGYi1v6tJb4mtZz0L8tK23Fb7opxbKsfUSw_iBkPZNZHv6ecFwG9wW5PLfWzYU_Cd0JMw-g9mCIGBQkS0aM7wSs2s-J9u219OEL1ukWYWw2mvij"

private const val SAMPLE_PHOTO_2_URL =
  "https://lh3.googleusercontent.com/aida/AEtjO1XLWYt0F5VxrDL3wJOuaKKtLlZF8H2ww3K9lH2Nd8Q_ddYjIsrpKGHx3hwN_X5FRNr2F5cqjYyAbsSJ-Kc9czVqk4VVj5al8XpSc7xFqQyKl8ON114p3dmJ2VRveIJ4QEWWBzoI1Lo90cPVs866Nevur3CIUEEt_WBvKoeb-mbwE8bQtmd5wSgXw9_CtR98QdcGo8yhPCjSl6Vm5ATqygFAVUMGqoW2POFosyBTmbgXNXsV9wact1rsMVFN"

private const val SAMPLE_PHOTO_3_URL =
  "https://lh3.googleusercontent.com/aida/AEtjO1WsTEroDCJvKwpyC1Q2rOipT83ueZAPxkcTjLNubMajFh3hNXWbMPcf3bMyL4RRJ72lxtLsP0Zfp7sCwbvoqskWFHuhOokHNpOspfIM12_Ue9qkiSYAuEV_Z1Iy2wEdjHkWBL6rRNOArh50ac35cDMIAg7RnY_aRu1AXwXYy_VgFfxJemsP_3EPNYWhjcJ09Jug16Oioa9UYgjKfejgu0tItnsJSCn7TjV4MeUogetNGe8AmN-8amOugXw"

// =========================================================================
// SECTION 1: NEW OFFER SCREEN (/profile/offers/new)
// =========================================================================

@Composable
fun NewOfferScreen(
  onBack: () -> Unit,
  onOfferCreated: () -> Unit = onBack,
  modifier: Modifier = Modifier
) {
  BackHandler { onBack() }
  val focusManager = LocalFocusManager.current

  var title by remember { mutableStateOf("Hand-woven Aso Oke fabric, 6 yards") }
  var description by remember {
    mutableStateOf("Narrow-loom Aso Oke in indigo with gold thread, six yards, enough for a full wedding outfit. Made to order in about two weeks.")
  }
  var currency by remember { mutableStateOf("NGN") }
  var price by remember { mutableStateOf("45000") }
  var hasPhoto by remember { mutableStateOf(true) }
  var showCurrencyDropdown by remember { mutableStateOf(false) }

  var validationError by remember { mutableStateOf<String?>(null) }
  var showSnackbar by remember { mutableStateOf(false) }

  val currencies = listOf("NGN", "USD", "EUR", "GBP")

  fun validate(): Boolean {
    val cleanTitle = title.trim()
    if (cleanTitle.length < 3 || cleanTitle.length > 50) {
      validationError = "Title must be between 3 and 50 characters."
      return false
    }

    val cleanDesc = description.trim()
    if (cleanDesc.length < 10 || cleanDesc.length > 2000) {
      validationError = "Description must be between 10 and 2,000 characters."
      return false
    }

    val priceNum = price.toLongOrNull() ?: 0L
    if (priceNum > 0 && currency.isBlank()) {
      validationError = "Currency is required when price is greater than zero."
      return false
    }

    validationError = null
    return true
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .windowInsetsPadding(HavenInsets.bottomWithIme)
      .testTag("new_offer_screen")
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Back-only Top Bar (48px)
      HavenTopBarContainer(
        backgroundColor = HavenColors.Canvas,
        showBottomBorder = false
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
            testTag = "new_offer_back_button"
          )
        }
      }

      // Scrollable Content
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 6.dp)
      ) {
        Text(
          text = "New offer",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 25.sp,
          letterSpacing = (-0.5).sp,
          color = HavenColors.TextPrimary,
          modifier = Modifier.testTag("new_offer_title")
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "Create a new offer to list on Haven.",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 16.sp,
          color = HavenColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Strict Greyscale Error Banner
        if (validationError != null) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .border(2.dp, HavenColors.TextPrimary, RoundedCornerShape(8.dp))
              .background(HavenColors.SurfaceContainer)
              .padding(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = HavenColors.TextPrimary,
                modifier = Modifier
                  .size(20.dp)
                  .padding(top = 1.dp)
              )
              Text(
                text = validationError!!,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = HavenColors.TextPrimary
              )
            }
          }

          Spacer(modifier = Modifier.height(18.dp))
        }

        // Field: Title
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Title",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = HavenColors.TextPrimary
          )

          Spacer(modifier = Modifier.height(6.dp))

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(HavenColors.Canvas)
              .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(8.dp))
              .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
          ) {
            BasicTextField(
              value = title,
              onValueChange = {
                title = it
                if (validationError != null) validate()
              },
              textStyle = TextStyle(
                fontFamily = InterFontFamily,
                fontSize = 15.sp,
                color = HavenColors.TextPrimary
              ),
              cursorBrush = SolidColor(HavenColors.TextPrimary),
              singleLine = true,
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("new_offer_title_input")
            )
          }

          Spacer(modifier = Modifier.height(5.dp))

          Text(
            text = "3 to 50 characters.",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = HavenColors.TextSecondary
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Field: Description (6 lines / ~120dp)
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Description",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = HavenColors.TextPrimary
          )

          Spacer(modifier = Modifier.height(6.dp))

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(130.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(HavenColors.Canvas)
              .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(8.dp))
              .padding(14.dp),
            contentAlignment = Alignment.TopStart
          ) {
            BasicTextField(
              value = description,
              onValueChange = {
                description = it
                if (validationError != null) validate()
              },
              textStyle = TextStyle(
                fontFamily = InterFontFamily,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = HavenColors.TextPrimary
              ),
              cursorBrush = SolidColor(HavenColors.TextPrimary),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
              modifier = Modifier
                .fillMaxSize()
                .testTag("new_offer_desc_input")
            )
          }

          Spacer(modifier = Modifier.height(5.dp))

          Text(
            text = "10 to 2,000 characters.",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = HavenColors.TextSecondary
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Field Row: Currency & Price
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // Currency (130dp)
            Column(modifier = Modifier.width(130.dp)) {
              Text(
                text = "Currency",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = HavenColors.TextPrimary
              )

              Spacer(modifier = Modifier.height(6.dp))

              Box {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HavenColors.Canvas)
                    .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(8.dp))
                    .clickable { showCurrencyDropdown = true }
                    .padding(horizontal = 14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = currency,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = HavenColors.TextPrimary
                  )
                  Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = HavenColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                  )
                }

                DropdownMenu(
                  expanded = showCurrencyDropdown,
                  onDismissRequest = { showCurrencyDropdown = false }
                ) {
                  currencies.forEach { item ->
                    DropdownMenuItem(
                      text = { Text(item, fontFamily = InterFontFamily) },
                      onClick = {
                        currency = item
                        showCurrencyDropdown = false
                      }
                    )
                  }
                }
              }
            }

            // Price (flex 1)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Price",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = HavenColors.TextPrimary
              )

              Spacer(modifier = Modifier.height(6.dp))

              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(HavenColors.Canvas)
                  .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(8.dp))
                  .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
              ) {
                BasicTextField(
                  value = price,
                  onValueChange = { price = it },
                  textStyle = TextStyle(
                    fontFamily = InterFontFamily,
                    fontSize = 15.sp,
                    color = HavenColors.TextPrimary
                  ),
                  cursorBrush = SolidColor(HavenColors.TextPrimary),
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                  ),
                  keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_offer_price_input")
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(5.dp))

          Text(
            text = "Enter 0 for a free offer.",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = HavenColors.TextSecondary
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Field: Cover Photo (Optional)
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Cover photo (optional)",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = HavenColors.TextPrimary
          )

          Spacer(modifier = Modifier.height(6.dp))

          if (hasPhoto) {
            // Chosen Photo State: 96x72 preview + details + Remove
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(HavenColors.SurfaceContainer)
                .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              SubcomposeAsyncImage(
                model = SAMPLE_COVER_PHOTO_URL,
                contentDescription = "Cover preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .size(width = 96.dp, height = 72.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(8.dp)),
                error = {
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .background(HavenColors.SurfaceContainer),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Image,
                      contentDescription = null,
                      tint = HavenColors.TextSecondary
                    )
                  }
                }
              )

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "aso-oke-indigo.jpg",
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.Medium,
                  fontSize = 14.sp,
                  color = HavenColors.TextPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                  text = "2.4 MB · Ready to publish",
                  fontFamily = InterFontFamily,
                  fontSize = 12.sp,
                  color = HavenColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                  text = "Remove",
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 12.sp,
                  color = HavenColors.TextPrimary,
                  textDecoration = TextDecoration.Underline,
                  modifier = Modifier
                    .clickable { hasPhoto = false }
                    .padding(vertical = 2.dp)
                )
              }
            }
          } else {
            // Dashed Empty State Box
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(12.dp))
                .background(HavenColors.SurfaceContainer)
                .padding(20.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Box(
                  modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HavenColors.Canvas)
                    .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(8.dp))
                    .clickable { hasPhoto = true }
                    .padding(horizontal = 20.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "Choose a photo",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = HavenColors.TextPrimary
                  )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                  text = "JPEG, PNG or WebP up to 5 MB. Offers without a photo stay private to you.",
                  fontFamily = InterFontFamily,
                  fontSize = 12.sp,
                  color = HavenColors.TextSecondary,
                  textAlign = TextAlign.Center
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(5.dp))

          Text(
            text = "JPEG, PNG or WebP up to 5 MB. Offers without a photo stay private to you.",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = HavenColors.TextSecondary
          )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Action Buttons: Create offer (52dp) & Cancel (48dp)
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(HavenColors.TextPrimary)
              .clickable {
                focusManager.clearFocus()
                if (validate()) {
                  showSnackbar = true
                  onOfferCreated()
                }
              }
              .testTag("create_offer_submit_button"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Create offer",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 16.sp,
              color = HavenColors.TextOnDark
            )
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .clip(RoundedCornerShape(8.dp))
              .clickable { onBack() },
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Cancel",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 15.sp,
              color = HavenColors.TextPrimary
            )
          }
        }

        Spacer(modifier = Modifier.height(32.dp))
      }
    }
  }
}

// =========================================================================
// SECTION 2: "YOUR OFFER" OWNER SCREEN (/profile/offers/{id})
// =========================================================================

data class OfferPhotoItem(
  val id: String,
  val url: String,
  val label: String
)

data class OfferInquiryItem(
  val id: String,
  val name: String,
  val messageCount: Int,
  val timestamp: String
)

@Composable
fun OwnerOfferDetailScreen(
  onBack: () -> Unit,
  onEditOffer: () -> Unit = {},
  onDeleteOffer: () -> Unit = onBack,
  onViewPublicPage: () -> Unit = {},
  offerTitle: String = "Hand-woven Aso Oke fabric, 6 yards",
  priceDisplay: String = "45,000 NGN",
  isPublicInitial: Boolean = true,
  descriptionText: String = "Narrow-loom Aso Oke in indigo with gold thread, six yards, enough for a full wedding outfit. Made to order in about two weeks.",
  modifier: Modifier = Modifier
) {
  BackHandler { onBack() }

  var isPublic by remember { mutableStateOf(isPublicInitial) }
  var showDeleteDialog by remember { mutableStateOf(false) }

  val photos = remember {
    mutableStateListOf(
      OfferPhotoItem("p-1", SAMPLE_COVER_PHOTO_URL, "Cover photo"),
      OfferPhotoItem("p-2", SAMPLE_PHOTO_2_URL, "Photo 2"),
      OfferPhotoItem("p-3", SAMPLE_PHOTO_3_URL, "Photo 3")
    )
  }

  val inquiries = remember {
    mutableStateListOf(
      OfferInquiryItem("inq-1", "Kofi Mensah", 4, "Oct 25, 09:02"),
      OfferInquiryItem("inq-2", "Tolu Adebayo", 4, "Oct 25, 09:02"),
      OfferInquiryItem("inq-3", "Ngozi Eze", 4, "Oct 25, 09:02")
    )
  }

  fun movePhotoUp(index: Int) {
    if (index > 0) {
      val item = photos.removeAt(index)
      photos.add(index - 1, item)
    }
  }

  fun movePhotoDown(index: Int) {
    if (index < photos.size - 1) {
      val item = photos.removeAt(index)
      photos.add(index + 1, item)
    }
  }

  fun removePhoto(index: Int) {
    if (index in photos.indices) {
      photos.removeAt(index)
      if (photos.isEmpty()) {
        isPublic = false
      }
    }
  }

  fun addPhoto() {
    if (photos.size < 5) {
      val nextNum = photos.size + 1
      photos.add(
        OfferPhotoItem(
          id = "p-$nextNum",
          url = SAMPLE_PHOTO_2_URL,
          label = if (photos.isEmpty()) "Cover photo" else "Photo $nextNum"
        )
      )
      isPublic = true
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .windowInsetsPadding(HavenInsets.navigationBars.only(WindowInsetsSides.Bottom))
      .testTag("owner_offer_detail_screen")
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Back Top Bar (48px)
      HavenTopBarContainer(
        backgroundColor = HavenColors.Canvas,
        showBottomBorder = false
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
            testTag = "owner_offer_back_button"
          )
        }
      }

      // Scrollable Content
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 6.dp)
      ) {
        // Header Block
        Text(
          text = offerTitle,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 25.sp,
          letterSpacing = (-0.5).sp,
          lineHeight = 31.sp,
          color = HavenColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = priceDisplay,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 20.sp,
          color = HavenColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Status Line with Dot
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(if (isPublic) HavenColors.TextPrimary else HavenColors.BorderStrong)
          )
          Text(
            text = if (isPublic) "Public" else "Only you can see this",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = if (isPublic) HavenColors.TextPrimary else HavenColors.TextSecondary
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Private Warning Banner
        if (!isPublic) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(8.dp))
              .background(HavenColors.SurfaceContainer)
              .padding(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = HavenColors.TextSecondary,
                modifier = Modifier
                  .size(20.dp)
                  .padding(top = 1.dp)
              )
              Text(
                text = "Only you can see this offer. Add a photo to make it public.",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = HavenColors.TextPrimary
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
        }

        // Description Paragraph
        Text(
          text = descriptionText,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 16.sp,
          lineHeight = 24.sp,
          color = HavenColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Three 48px Action Buttons
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Edit Offer (Solid Primary)
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(HavenColors.TextPrimary)
              .clickable { onEditOffer() }
              .testTag("owner_edit_offer_button"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Edit offer",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 16.sp,
              color = HavenColors.TextOnDark
            )
          }

          // View Public Page (Outline)
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(HavenColors.Canvas)
              .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(8.dp))
              .clickable(enabled = isPublic) { onViewPublicPage() }
              .testTag("owner_view_public_page_button"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (isPublic) "View public page" else "View public page (Draft)",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 16.sp,
              color = if (isPublic) HavenColors.TextPrimary else HavenColors.BorderStrong
            )
          }

          // Delete Offer (Outline)
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(HavenColors.Canvas)
              .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(8.dp))
              .clickable { showDeleteDialog = true }
              .testTag("owner_delete_offer_button"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Delete offer",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 16.sp,
              color = HavenColors.TextPrimary
            )
          }
        }

        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider(thickness = 1.dp, color = HavenColors.BorderSubtle)
        Spacer(modifier = Modifier.height(20.dp))

        // Photos Section
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom
        ) {
          Text(
            text = "Photos",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = HavenColors.TextPrimary
          )
          Text(
            text = "${photos.size} of 5 photos.",
            fontFamily = InterFontFamily,
            fontSize = 14.sp,
            color = HavenColors.TextSecondary
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (photos.isEmpty()) {
          // Empty Photo Prompt
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(HavenColors.SurfaceContainer)
              .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
              .padding(20.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(HavenColors.Canvas),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Outlined.Image,
                  contentDescription = null,
                  tint = HavenColors.TextSecondary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "No photos yet. Add at least one photo to make this offer public.",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = HavenColors.TextPrimary,
                textAlign = TextAlign.Center
              )
            }
          }
          Spacer(modifier = Modifier.height(14.dp))
        } else {
          // Reorderable Photo Rows
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            photos.forEachIndexed { index, photo ->
              val isCover = index == 0
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(HavenColors.Canvas)
                  .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                // 96x72 Thumbnail
                SubcomposeAsyncImage(
                  model = photo.url,
                  contentDescription = "Offer photo $index",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .size(width = 96.dp, height = 72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(8.dp))
                )

                // Middle Info + Controls
                Column(modifier = Modifier.weight(1f)) {
                  // Badge Pill
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(10.dp))
                      .background(if (isCover) HavenColors.TextPrimary else HavenColors.SurfaceContainer)
                      .padding(horizontal = 8.dp, vertical = 2.dp)
                  ) {
                    Text(
                      text = if (isCover) "Cover photo" else "Photo ${index + 1}",
                      fontFamily = InterFontFamily,
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 11.sp,
                      color = if (isCover) HavenColors.TextOnDark else HavenColors.TextSecondary
                    )
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  // Actions: Up, Down, Remove (40-48dp touch targets)
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    // Move Up
                    IconButton(
                      onClick = { movePhotoUp(index) },
                      enabled = index > 0,
                      modifier = Modifier.size(40.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Move photo up",
                        tint = if (index > 0) HavenColors.TextPrimary else HavenColors.BorderStrong,
                        modifier = Modifier.size(20.dp)
                      )
                    }

                    // Move Down
                    IconButton(
                      onClick = { movePhotoDown(index) },
                      enabled = index < photos.size - 1,
                      modifier = Modifier.size(40.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Move photo down",
                        tint = if (index < photos.size - 1) HavenColors.TextPrimary else HavenColors.BorderStrong,
                        modifier = Modifier.size(20.dp)
                      )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Remove Outline Pill
                    Box(
                      modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(6.dp))
                        .clickable { removePhoto(index) }
                        .padding(horizontal = 10.dp),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = "Remove",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = HavenColors.TextPrimary
                      )
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
        }

        // Add a Photo Dashed Box
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(12.dp))
            .background(HavenColors.SurfaceContainer)
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Text(
              text = "Add a photo",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp,
              color = HavenColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Box(
                modifier = Modifier
                  .height(40.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(HavenColors.Canvas)
                  .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(8.dp))
                  .clickable { addPhoto() }
                  .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "Choose a photo",
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp,
                  color = HavenColors.TextPrimary
                )
              }

              Box(
                modifier = Modifier
                  .height(40.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(HavenColors.TextPrimary)
                  .clickable { addPhoto() }
                  .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "Upload photo",
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp,
                  color = HavenColors.TextOnDark
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "JPEG, PNG or WebP up to 5 MB. Keep the subject near the centre, since the cover is cropped to a square on cards.",
              fontFamily = InterFontFamily,
              fontSize = 12.sp,
              lineHeight = 16.sp,
              color = HavenColors.TextSecondary,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(horizontal = 8.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider(thickness = 1.dp, color = HavenColors.BorderSubtle)
        Spacer(modifier = Modifier.height(20.dp))

        // Inquiries Section
        Text(
          text = "Inquiries",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 20.sp,
          color = HavenColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (inquiries.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(HavenColors.SurfaceContainer)
              .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No one has asked about this offer yet.",
              fontFamily = InterFontFamily,
              fontSize = 14.sp,
              color = HavenColors.TextSecondary
            )
          }
        } else {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(HavenColors.Canvas)
              .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
          ) {
            inquiries.forEachIndexed { index, inquiry ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { /* open inquiry thread */ }
                  .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                // Avatar Initial
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(HavenColors.SurfaceContainer),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = inquiry.name.firstOrNull()?.toString() ?: "U",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = HavenColors.TextPrimary
                  )
                }

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = inquiry.name,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = HavenColors.TextPrimary
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "${inquiry.messageCount} messages · ${inquiry.timestamp}",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = HavenColors.TextSecondary
                  )
                }

                Icon(
                  imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                  contentDescription = null,
                  tint = HavenColors.TextSecondary,
                  modifier = Modifier.size(18.dp)
                )
              }

              if (index < inquiries.size - 1) {
                HorizontalDivider(thickness = 1.dp, color = HavenColors.BorderSubtle)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(36.dp))
      }
    }

    // Delete Confirmation Dialog (320dp centered, 28dp radius)
    if (showDeleteDialog) {
      Dialog(
        onDismissRequest = { showDeleteDialog = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null
            ) { showDeleteDialog = false },
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .width(320.dp)
              .clip(RoundedCornerShape(28.dp))
              .background(HavenColors.Canvas)
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
              ) { /* consume clicks */ }
              .padding(24.dp)
          ) {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "Delete offer?",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = HavenColors.TextPrimary,
                textAlign = TextAlign.Center
              )

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = "This will remove your offer and cancel any pending inquiries. This cannot be undone.",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = HavenColors.TextSecondary,
                textAlign = TextAlign.Center
              )

              Spacer(modifier = Modifier.height(24.dp))

              // Delete button (Solid Black)
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(HavenColors.TextPrimary)
                  .clickable {
                    showDeleteDialog = false
                    onDeleteOffer()
                  }
                  .testTag("confirm_delete_offer_button"),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "Delete",
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 15.sp,
                  color = HavenColors.TextOnDark
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Keep Offer button (Text/Outline)
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(44.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { showDeleteDialog = false },
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "Keep offer",
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.Medium,
                  fontSize = 15.sp,
                  color = HavenColors.TextPrimary
                )
              }
            }
          }
        }
      }
    }
  }
}
