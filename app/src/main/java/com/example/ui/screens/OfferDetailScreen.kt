package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.ui.components.shimmerBackground
import com.example.ui.model.Offer
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class GalleryImage(val url: String?, val resId: Int)

private enum class DetailAuthIntent {
  SAVE,
  MESSAGE
}

/**
 * Full-screen Offer Detail matching the Haven prototype:
 * - 4:3 swipeable gallery with frosted back/save controls
 * - Category, title, price, provider card, description and location table
 * - Fixed "Message provider" action with a guest sign-in gate
 */
@Composable
fun OfferDetailScreen(
  offer: Offer,
  isSaved: Boolean,
  isAuthenticated: Boolean,
  onBack: () -> Unit,
  onToggleSave: () -> Unit,
  onMessageProvider: () -> Unit,
  onSignInRequested: () -> Unit,
  modifier: Modifier = Modifier,
  onOpenProvider: (() -> Unit)? = null
) {
  val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  var authIntent by remember { mutableStateOf<DetailAuthIntent?>(null) }
  var toastMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(toastMessage) {
    if (toastMessage != null) {
      delay(3_200)
      toastMessage = null
    }
  }

  BackHandler {
    if (authIntent != null) authIntent = null else onBack()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .testTag("offer_detail_screen")
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        OfferGallery(
          offer = offer,
          isSaved = isSaved,
          statusBarTop = statusBarTop,
          onBack = onBack,
          onToggleSave = {
            if (isAuthenticated) {
              toastMessage = if (isSaved) "Offer removed from saved offers" else "Offer saved"
              onToggleSave()
            } else {
              authIntent = DetailAuthIntent.SAVE
            }
          }
        )

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 28.dp),
          verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
          OfferSummary(offer = offer)

          ProviderCard(
            offer = offer,
            onClick = {
              if (onOpenProvider != null) {
                onOpenProvider()
              } else {
                toastMessage = "Provider profile is ready to connect in the prototype."
              }
            }
          )

          OfferDescription(description = offer.description)

          OfferLocation(location = offer.location)
        }
      }

      OfferActionArea(
        onMessageProvider = {
          if (isAuthenticated) {
            onMessageProvider()
          } else {
            authIntent = DetailAuthIntent.MESSAGE
          }
        }
      )
    }

    toastMessage?.let { message ->
      AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = statusBarTop + 12.dp)
          .padding(horizontal = 16.dp)
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(HavenColors.Primary)
            .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
          Text(
            text = message,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = HavenColors.TextOnDark,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }

  authIntent?.let { intent ->
    SignInRequiredDialog(
      title = if (intent == DetailAuthIntent.SAVE) {
        "Sign in to save this offer"
      } else {
        "Sign in to message this provider"
      },
      onDismiss = { authIntent = null },
      onSignIn = {
        authIntent = null
        onSignInRequested()
      }
    )
  }
}

@Composable
private fun OfferGallery(
  offer: Offer,
  isSaved: Boolean,
  statusBarTop: androidx.compose.ui.unit.Dp,
  onBack: () -> Unit,
  onToggleSave: () -> Unit
) {
  val images = remember(offer) {
    val extras = offer.galleryResIds.filter { it != offer.imageResId }
    buildList {
      add(GalleryImage(url = offer.imageUrl, resId = offer.imageResId))
      extras.forEach { add(GalleryImage(url = null, resId = it)) }
    }
  }
  val pagerState = rememberPagerState(pageCount = { images.size })
  val scope = rememberCoroutineScope()

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(4f / 3f)
      .background(HavenColors.SurfaceContainer)
      .testTag("offer_detail_gallery")
  ) {
    HorizontalPager(
      state = pagerState,
      modifier = Modifier.fillMaxSize()
    ) { page ->
      val image = images[page]
      if (image.url.isNullOrBlank()) {
        Image(
          painter = painterResource(id = image.resId),
          contentDescription = "${offer.title}, image ${page + 1} of ${images.size}",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        SubcomposeAsyncImage(
          model = image.url,
          contentDescription = "${offer.title}, image ${page + 1} of ${images.size}",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
          loading = {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .shimmerBackground()
            )
          },
          error = {
            Image(
              painter = painterResource(id = image.resId),
              contentDescription = "${offer.title}, image ${page + 1} of ${images.size}",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          }
        )
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .padding(top = statusBarTop + 12.dp, start = 12.dp, end = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      FrostedCircleButton(
        contentDescription = "Back",
        onClick = onBack,
        testTag = "offer_detail_back"
      ) {
        Icon(
          imageVector = Icons.Default.ChevronLeft,
          contentDescription = null,
          tint = HavenColors.TextPrimary,
          modifier = Modifier.size(24.dp)
        )
      }

      FrostedCircleButton(
        contentDescription = if (isSaved) "Unsave offer" else "Save offer",
        onClick = onToggleSave,
        testTag = "offer_detail_save"
      ) {
        Icon(
          imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
          contentDescription = null,
          tint = HavenColors.TextPrimary,
          modifier = Modifier.size(22.dp)
        )
      }
    }

    if (images.size > 1) {
      FrostedCircleButton(
        contentDescription = "Previous image",
        enabled = pagerState.currentPage > 0,
        onClick = {
          scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        },
        modifier = Modifier
          .align(Alignment.CenterStart)
          .padding(start = 12.dp)
      ) {
        Icon(
          imageVector = Icons.Default.ChevronLeft,
          contentDescription = null,
          tint = HavenColors.TextPrimary,
          modifier = Modifier.size(24.dp)
        )
      }

      FrostedCircleButton(
        contentDescription = "Next image",
        enabled = pagerState.currentPage < images.lastIndex,
        onClick = {
          scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
        },
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 12.dp)
      ) {
        Icon(
          imageVector = Icons.Default.ChevronRight,
          contentDescription = null,
          tint = HavenColors.TextPrimary,
          modifier = Modifier.size(24.dp)
        )
      }

      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 12.dp, bottom = 12.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(HavenColors.Canvas.copy(alpha = 0.9f))
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("offer_detail_gallery_count")
      ) {
        Text(
          text = "${pagerState.currentPage + 1} / ${images.size}",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 12.sp,
          color = HavenColors.TextPrimary
        )
      }
    }
  }
}

@Composable
private fun OfferSummary(offer: Offer) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = offer.category,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 12.sp,
      color = HavenColors.TextSecondary
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = offer.title,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 24.sp,
      lineHeight = 31.sp,
      letterSpacing = (-0.6).sp,
      color = HavenColors.TextPrimary
    )

    Row(
      modifier = Modifier.padding(top = 10.dp),
      verticalAlignment = Alignment.Bottom
    ) {
      Text(
        text = priceAmount(offer),
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 28.sp,
        color = HavenColors.TextPrimary
      )

      if (offer.priceNumeric > 0.0 && offer.currency.isNotBlank()) {
        Spacer(modifier = Modifier.size(6.dp))
        Text(
          text = offer.currency,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 14.sp,
          lineHeight = 18.sp,
          color = HavenColors.TextSecondary,
          modifier = Modifier.padding(bottom = 2.dp)
        )
      }
    }
  }
}

@Composable
private fun ProviderCard(
  offer: Offer,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 68.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(HavenColors.SurfaceContainer)
      .clickable(role = Role.Button, onClick = onClick)
      .padding(12.dp)
      .testTag("offer_detail_provider_card"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .background(
          if (HavenColors.isDark) HavenColors.BorderSubtle else Color(0xFFE5E7EB)
        ),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = offer.creatorInitial,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = HavenColors.TextPrimary
      )
    }

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = offer.creatorName,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        color = HavenColors.TextPrimary
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = "Offer provider",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        color = HavenColors.TextSecondary
      )
    }

    Icon(
      imageVector = Icons.Default.ChevronRight,
      contentDescription = null,
      tint = HavenColors.TextSecondary,
      modifier = Modifier.size(22.dp)
    )
  }
}

@Composable
private fun OfferDescription(description: String) {
  Column(modifier = Modifier.fillMaxWidth()) {
    SectionHeading(text = "About this offer")

    Text(
      text = description.ifBlank { "No description has been provided for this offer." },
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 16.sp,
      lineHeight = 26.sp,
      color = HavenColors.TextPrimary
    )
  }
}

@Composable
private fun OfferLocation(location: List<Pair<String, String>>) {
  Column(modifier = Modifier.fillMaxWidth()) {
    SectionHeading(text = "Offer location")

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(HavenColors.SurfaceContainer)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      if (location.isEmpty()) {
        Text(
          text = "Location information has not been provided.",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 14.sp,
          lineHeight = 20.sp,
          color = HavenColors.TextSecondary
        )
      } else {
        location.forEach { (label, value) ->
          Row(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = label,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Normal,
              fontSize = 13.sp,
              lineHeight = 18.sp,
              color = HavenColors.TextSecondary,
              modifier = Modifier.weight(1f)
            )
            Text(
              text = value,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Normal,
              fontSize = 14.sp,
              lineHeight = 19.sp,
              color = HavenColors.TextPrimary,
              textAlign = TextAlign.End,
              modifier = Modifier.weight(1.3f)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SectionHeading(text: String) {
  Text(
    text = text,
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    color = HavenColors.TextPrimary
  )
  Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun OfferActionArea(onMessageProvider: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(HavenColors.Canvas)
      .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(HavenColors.Primary)
        .clickable(role = Role.Button, onClick = onMessageProvider)
        .testTag("offer_detail_message_button"),
      contentAlignment = Alignment.Center
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Outlined.ChatBubbleOutline,
          contentDescription = null,
          tint = HavenColors.TextOnDark,
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = "Message provider",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 15.sp,
          color = HavenColors.TextOnDark
        )
      }
    }
  }
}

@Composable
private fun FrostedCircleButton(
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  testTag: String? = null,
  content: @Composable () -> Unit
) {
  Box(
    modifier = modifier
      .size(44.dp)
      .alpha(if (enabled) 1f else 0.4f)
      .clip(CircleShape)
      .background(HavenColors.Canvas.copy(alpha = 0.85f))
      .clickable(
        enabled = enabled,
        role = Role.Button,
        onClick = onClick
      )
      .semantics { this.contentDescription = contentDescription }
      .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
    contentAlignment = Alignment.Center
  ) {
    content()
  }
}

@Composable
private fun SignInRequiredDialog(
  title: String,
  onDismiss: () -> Unit,
  onSignIn: () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      decorFitsSystemWindows = false
    )
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.4f))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onDismiss
        )
        .testTag("offer_detail_auth_dialog"),
      contentAlignment = Alignment.BottomCenter
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 480.dp)
          .fillMaxWidth()
          .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
          .background(HavenColors.Canvas)
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
          ) {}
          .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
          .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)
      ) {
        Text(
          text = title,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 20.sp,
          lineHeight = 26.sp,
          color = HavenColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Continue with your Haven account.",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 15.sp,
          lineHeight = 22.sp,
          color = HavenColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(HavenColors.Primary)
            .clickable(role = Role.Button, onClick = onSignIn)
            .testTag("offer_detail_auth_sign_in"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Sign in",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = HavenColors.TextOnDark
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(HavenColors.SurfaceContainer)
            .clickable(role = Role.Button, onClick = onDismiss)
            .testTag("offer_detail_auth_cancel"),
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
    }
  }
}

private fun priceAmount(offer: Offer): String {
  if (offer.priceNumeric <= 0.0) return "Free"
  val suffix = " ${offer.currency}"
  return offer.priceDisplay.removeSuffix(suffix).ifBlank { offer.priceDisplay }
}
