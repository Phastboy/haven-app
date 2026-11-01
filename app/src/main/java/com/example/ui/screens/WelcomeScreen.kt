package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.HavenInsets
import com.example.ui.components.HavenPrimaryButton
import com.example.ui.components.HavenSecondaryButton
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Screen 1: Welcome Screen (First Launch)
 * Accurately reproducing the Haven design with:
 * - Wordmark "Haven"
 * - 3-photo documentary craft collage (272dp height, 16dp radii)
 * - Copy block: "Exchange offers directly"
 * - Action buttons: "Browse offers" and "Sign in"
 *
 * Safe Area & Inset Handling:
 * - Safe top status bar and camera punch-hole clearance
 * - Safe bottom system navigation bar / gesture pill clearance
 * - Scrollable container for compact screens and foldables
 */
@Composable
fun WelcomeScreen(
  onNavigateToSignIn: () -> Unit,
  onNavigateToOffers: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .testTag("welcome_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top Section: Wordmark and Content
      Column {
        // Dynamic status bar and camera cutout inset clearance
        Spacer(modifier = Modifier.windowInsetsTopHeight(HavenInsets.statusBarsAndCutouts))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 12.dp)
        ) {
          Text(
            text = "Haven",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            letterSpacing = (-0.3).sp,
            color = HavenColors.TextPrimary,
            modifier = Modifier.testTag("haven_wordmark")
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Photo Collage (272dp height, 2 columns with 12dp spacing)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(272.dp)
            .testTag("photo_collage"),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Left Column: tall single card (Aso Oke ceremonial woven fabric)
          Box(
            modifier = Modifier
              .weight(1f)
              .height(272.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(HavenColors.SurfaceContainer)
              .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(16.dp))
          ) {
            Image(
              painter = painterResource(id = R.drawable.fabric_craft),
              contentDescription = "Hand-woven ceremonial Aso Oke fabric",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          }

          // Right Column: two stacked rounded rectangles (Lamp + Pottery)
          Column(
            modifier = Modifier
              .weight(1f)
              .height(272.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // Upper right card: Brass lamp
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(HavenColors.SurfaceContainer)
                .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(16.dp))
            ) {
              Image(
                painter = painterResource(id = R.drawable.lamp_craft),
                contentDescription = "Artisanal brass and reclaimed wood table lamp",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
            }

            // Lower right card: Ceramic pottery
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(HavenColors.SurfaceContainer)
                .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(16.dp))
            ) {
              Image(
                painter = painterResource(id = R.drawable.pottery_craft),
                contentDescription = "Handcrafted ceramic pottery cups",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Copy Block
        Text(
          text = "Exchange offers directly",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 34.sp,
          lineHeight = 37.sp,
          letterSpacing = (-0.6).sp,
          color = HavenColors.TextPrimary,
          modifier = Modifier.testTag("welcome_headline")
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Put something you are offering on Haven, or browse what others have made available.",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 16.sp,
          lineHeight = 22.sp,
          color = HavenColors.TextSecondary,
          modifier = Modifier.testTag("welcome_subheadline")
        )
      }

      // Bottom Action Buttons with navigation bar safe padding
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .windowInsetsPadding(HavenInsets.navigationBars.only(WindowInsetsSides.Bottom))
          .padding(top = 32.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        HavenPrimaryButton(
          text = "Browse offers",
          onClick = onNavigateToOffers,
          testTag = "browse_offers_button"
        )

        HavenSecondaryButton(
          text = "Sign in",
          onClick = onNavigateToSignIn,
          testTag = "sign_in_nav_button"
        )
      }
    }
  }
}
