package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HavenBackButton
import com.example.ui.components.HavenInsets
import com.example.ui.components.HavenTopBarContainer
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Check your email screen (/auth/check-email) - Frame 03:
 * Magic Link Sent state with back-only top bar, 48px circle icon, and 52px outline button.
 */
@Composable
fun CheckEmailScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  onBackToSignIn: () -> Unit = onBack
) {
  BackHandler { onBack() }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .windowInsetsPadding(HavenInsets.navigationBars.only(WindowInsetsSides.Bottom))
      .testTag("check_email_screen")
  ) {
    // Back-only Top Bar
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
          testTag = "check_email_back_button"
        )
      }
    }

    // Centered Content
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // 48px Icon Circle
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(HavenColors.SurfaceContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.MailOutline,
          contentDescription = null,
          tint = HavenColors.TextPrimary,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "Check your email",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        letterSpacing = (-0.5).sp,
        color = HavenColors.TextPrimary,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "If an account exists for that email, we've sent a magic link.",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = HavenColors.TextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
      )

      Spacer(modifier = Modifier.height(32.dp))

      // 52px Outline Button
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .clip(RoundedCornerShape(8.dp))
          .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(8.dp))
          .background(HavenColors.Canvas)
          .clickable { onBackToSignIn() }
          .testTag("back_to_sign_in_button"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Back to sign in",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 16.sp,
          color = HavenColors.TextPrimary
        )
      }
    }
  }
}

/**
 * Expired Link Screen (/auth/link-expired) - Frame 04:
 * 15-minute expired link state with back-only top bar, clock icon, and 52px solid primary button.
 */
@Composable
fun LinkExpiredScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  onRequestNewLink: () -> Unit = onBack
) {
  BackHandler { onBack() }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .windowInsetsPadding(HavenInsets.navigationBars.only(WindowInsetsSides.Bottom))
      .testTag("link_expired_screen")
  ) {
    // Back-only Top Bar
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
          testTag = "link_expired_back_button"
        )
      }
    }

    // Centered Content
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // 48px Icon Circle
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(HavenColors.SurfaceContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.AccessTime,
          contentDescription = null,
          tint = HavenColors.TextPrimary,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "This link has expired",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        letterSpacing = (-0.5).sp,
        color = HavenColors.TextPrimary,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Sign-in links work once and for 15 minutes. Request a new one to continue.",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = HavenColors.TextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
      )

      Spacer(modifier = Modifier.height(32.dp))

      // 52px Solid Primary Button
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(HavenColors.TextPrimary)
          .clickable { onRequestNewLink() }
          .testTag("request_new_link_button"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Back to sign in",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 16.sp,
          color = HavenColors.TextOnDark
        )
      }
    }
  }
}

/**
 * Error State Screen (Frame 05 for 500 & Frame 06 for 404):
 * Main-style error state with status code, headline, message, 48px "Browse offers" button,
 * and persistent 80px bottom navigation bar.
 */
@Composable
fun ErrorStateScreen(
  onBrowseOffers: () -> Unit,
  modifier: Modifier = Modifier,
  errorCode: Int = 500,
  onNavigateOffers: () -> Unit = onBrowseOffers,
  onNavigateMessages: () -> Unit = {},
  onNavigateProfile: () -> Unit = {},
  hasUnreadMessages: Boolean = true
) {
  val is500 = errorCode == 500
  val statusCodeText = if (is500) "500" else "404"
  val title = if (is500) "Something went wrong" else "Page not found"
  val desc = if (is500) {
    "We could not complete that. Please try again in a moment. If it keeps happening, come back later."
  } else {
    "The page you are looking for does not exist, or it is not available to you."
  }
  val iconVector = if (is500) Icons.Outlined.CloudOff else Icons.Outlined.Search

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .windowInsetsPadding(HavenInsets.statusBarsAndCutouts.only(WindowInsetsSides.Top))
      .windowInsetsPadding(HavenInsets.navigationBars.only(WindowInsetsSides.Bottom))
      .testTag("error_state_screen_$statusCodeText")
  ) {
    // Centered Content Area
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // 48px Icon Circle
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(HavenColors.SurfaceContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = iconVector,
          contentDescription = null,
          tint = HavenColors.TextPrimary,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Status Code
      Text(
        text = statusCodeText,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.6.sp,
        color = HavenColors.TextSecondary
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Title
      Text(
        text = title,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        letterSpacing = (-0.5).sp,
        color = HavenColors.TextPrimary,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Description
      Text(
        text = desc,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = HavenColors.TextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 8.dp)
      )

      Spacer(modifier = Modifier.height(32.dp))

      // 48px Action Button
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(HavenColors.TextPrimary)
          .clickable { onBrowseOffers() }
          .testTag("error_browse_offers_button"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Browse offers",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 15.sp,
          color = HavenColors.TextOnDark
        )
      }
    }

    // 80px Bottom Navigation Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(80.dp)
        .background(HavenColors.Canvas)
        .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(0.dp))
        .padding(horizontal = 12.dp),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Offers Tab (Active)
      Column(
        modifier = Modifier
          .clickable { onNavigateOffers() }
          .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Box(
          modifier = Modifier
            .width(44.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(HavenColors.SurfaceContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Storefront,
            contentDescription = "Offers",
            tint = HavenColors.TextPrimary,
            modifier = Modifier.size(20.dp)
          )
        }
        Text(
          text = "Offers",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 12.sp,
          color = HavenColors.TextPrimary
        )
      }

      // Messages Tab
      Box(
        modifier = Modifier
          .clickable { onNavigateMessages() }
          .padding(vertical = 8.dp)
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Box(
            modifier = Modifier
              .width(44.dp)
              .height(28.dp),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.ChatBubbleOutline,
              contentDescription = "Messages",
              tint = HavenColors.TextSecondary,
              modifier = Modifier.size(20.dp)
            )
          }
          Text(
            text = "Messages",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = HavenColors.TextSecondary
          )
        }

        if (hasUnreadMessages) {
          Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(top = 2.dp, end = 6.dp)
              .size(7.dp)
              .clip(CircleShape)
              .background(HavenColors.TextPrimary)
          )
        }
      }

      // Profile Tab
      Column(
        modifier = Modifier
          .clickable { onNavigateProfile() }
          .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Box(
          modifier = Modifier
            .width(44.dp)
            .height(28.dp),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.PersonOutline,
            contentDescription = "Profile",
            tint = HavenColors.TextSecondary,
            modifier = Modifier.size(20.dp)
          )
        }
        Text(
          text = "Profile",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 12.sp,
          color = HavenColors.TextSecondary
        )
      }
    }
  }
}
