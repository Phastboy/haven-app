package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.ui.model.UserSession
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.delay

/**
 * Inspection states matching Haven Android Profile Specification (390x844):
 * - DEFAULT: Adaeze Obi with photo, standard bio, grouped link rows & sign out
 * - STATE_A_UPDATED: Pinned 48dp snackbar "Your profile has been updated."
 * - STATE_B_SIGN_OUT_DIALOG: 320dp centered dialog (28dp radius) "Sign out?"
 * - STATE_C_NO_PHOTO_NO_BIO: 96px circular letter 'A' avatar, strictly zero bio space
 * - STATE_D_STRESS_TEST: 50-character name and 500-character bio wrapping cleanly with zero overflow
 */
enum class ProfileSpecState(val label: String) {
  DEFAULT("Default"),
  STATE_A_UPDATED("State A: Updated"),
  STATE_B_SIGN_OUT_DIALOG("State B: Sign Out"),
  STATE_C_NO_PHOTO_NO_BIO("State C: No Photo/Bio"),
  STATE_D_STRESS_TEST("State D: Stress Test")
}

private const val PROFILE_AVATAR_URL =
  "https://lh3.googleusercontent.com/aida/AEtjO1XT1RMubL8e8hGOBhw0NvVZyAtLhFXR1IlcXKlP77a0ZvKEXNjhZYJWYiQGy_NxHAEs-n9GJhFMoevM7g5HjrJWBU4NxEXmz-YJXxy_k9A9-crh3u1LNXdTOLbxoETTKILuDGSJC5ZCM01HqG-2e71Ilkdm7DFBWuO6ljlIEjq7PSEAW7L-ddjXkY-fO0VH3GqF89oc8Cu0CWZISQZu8NwYUFWgcU_RSDU4cM2x6Oju8m0IrHNemURl_yzw"

private const val DEFAULT_BIO =
  "I weave Aso Oke the traditional way on a narrow loom. I take custom orders for weddings and naming ceremonies, and every piece is made to order."

private const val STRESS_BIO =
  "Master weaver with over twenty years of experience crafting ceremonial Aso Oke fabrics, heirloom textiles, and hand-loomed cloth using sustainable organic cotton and wild botanical dyes harvested from ancestral orchards. I teach communal workshops every solstice, run zero-waste fiber spinning circles, and collaborate on bespoke architectural wall hangings for public sanctuaries across West Africa. Inquiries for ceremonial robes, wedding garments, and bespoke tapestry commissions are always welcome."

private const val STRESS_NAME =
  "Adaeze Chioma Obi-Okonkwo of the Eastern Loom Guild"

enum class ProfileSubScreen {
  MAIN,
  MY_OFFERS,
  SAVED_OFFERS,
  FOLLOWING
}

@Composable
fun ProfileScreen(
  session: UserSession,
  savedOffersCount: Int,
  onSignOut: () -> Unit,
  onNavigateToSavedOffers: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var currentSubScreen by remember { mutableStateOf(ProfileSubScreen.MAIN) }
  var specState by remember { mutableStateOf(ProfileSpecState.DEFAULT) }
  var showSnackbar by remember { mutableStateOf(false) }
  var showSignOutDialog by remember { mutableStateOf(false) }
  var showAppearanceDialog by remember { mutableStateOf(false) }
  var snackbarText by remember { mutableStateOf("Your profile has been updated.") }

  when (currentSubScreen) {
    ProfileSubScreen.MY_OFFERS -> {
      MyOffersScreen(
        onBack = { currentSubScreen = ProfileSubScreen.MAIN },
        onCreateOffer = { /* create offer action */ },
        modifier = modifier
      )
      return
    }
    ProfileSubScreen.SAVED_OFFERS -> {
      SavedOffersScreen(
        onBack = { currentSubScreen = ProfileSubScreen.MAIN },
        onBrowseOffers = {
          currentSubScreen = ProfileSubScreen.MAIN
          onNavigateToSavedOffers()
        },
        modifier = modifier
      )
      return
    }
    ProfileSubScreen.FOLLOWING -> {
      FollowingScreen(
        onBack = { currentSubScreen = ProfileSubScreen.MAIN },
        onBrowseOffers = {
          currentSubScreen = ProfileSubScreen.MAIN
          onNavigateToSavedOffers()
        },
        modifier = modifier
      )
      return
    }
    ProfileSubScreen.MAIN -> {
      // Continue rendering main profile screen
    }
  }

  // Synchronize state switcher with sub-states
  LaunchedEffect(specState) {
    when (specState) {
      ProfileSpecState.STATE_A_UPDATED -> {
        snackbarText = "Your profile has been updated."
        showSnackbar = true
        showSignOutDialog = false
      }
      ProfileSpecState.STATE_B_SIGN_OUT_DIALOG -> {
        showSignOutDialog = true
        showSnackbar = false
      }
      else -> {
        showSignOutDialog = false
        if (specState != ProfileSpecState.STATE_A_UPDATED) {
          showSnackbar = false
        }
      }
    }
  }

  // Auto-dismiss snackbar after 4 seconds if displayed
  LaunchedEffect(showSnackbar) {
    if (showSnackbar) {
      delay(4000)
      showSnackbar = false
      if (specState == ProfileSpecState.STATE_A_UPDATED) {
        specState = ProfileSpecState.DEFAULT
      }
    }
  }

  val isNoPhotoNoBio = specState == ProfileSpecState.STATE_C_NO_PHOTO_NO_BIO
  val isStressTest = specState == ProfileSpecState.STATE_D_STRESS_TEST

  val displayName = when {
    isStressTest -> STRESS_NAME
    isNoPhotoNoBio -> "Adaeze"
    !session.displayName.isNullOrBlank() && session.isAuthenticated -> session.displayName
    else -> "Adaeze Obi"
  }

  val handleJoined = when {
    isNoPhotoNoBio -> "@adaeze · Joined March 2026"
    else -> "@adaeze-obi · Joined March 2026"
  }

  val bioText: String? = when {
    isNoPhotoNoBio -> null
    isStressTest -> STRESS_BIO
    else -> DEFAULT_BIO
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .testTag("profile_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(bottom = 96.dp), // Clear space above 80dp bottom navigation
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Interactive Spec State Switcher Pills (Allows testing all 5 spec states)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp, bottom = 6.dp)
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        ProfileSpecState.values().forEach { state ->
          val isSelected = specState == state
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .background(if (isSelected) HavenColors.Primary else HavenColors.SurfaceContainer)
              .border(
                1.dp,
                if (isSelected) HavenColors.Primary else HavenColors.BorderSubtle,
                RoundedCornerShape(16.dp)
              )
              .clickable { specState = state }
              .padding(horizontal = 12.dp, vertical = 6.dp)
              .testTag("spec_chip_${state.name.lowercase()}")
          ) {
            Text(
              text = state.label,
              fontFamily = InterFontFamily,
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
              fontSize = 11.sp,
              color = if (isSelected) HavenColors.TextOnDark else HavenColors.TextPrimary
            )
          }
        }
      }

      // Starts 32px below status bar (no unadorned Profile title)
      Spacer(modifier = Modifier.height(20.dp))

      // Centered Identity Block
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .testTag("identity_block"),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // 96px Avatar Wrap (96dp x 96dp circle, 1px border, adaptive background)
        Box(
          modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(HavenColors.SurfaceContainer)
            .border(1.dp, HavenColors.BorderSubtle, CircleShape)
            .testTag("profile_avatar"),
          contentAlignment = Alignment.Center
        ) {
          if (isNoPhotoNoBio) {
            // State C: Letter 'A' fallback (38sp, font weight 600)
            Text(
              text = "A",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 38.sp,
              color = HavenColors.TextPrimary
            )
          } else {
            // Default & other states: Portrait image
            SubcomposeAsyncImage(
              model = PROFILE_AVATAR_URL,
              contentDescription = displayName,
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
                    text = "A",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 38.sp,
                    color = HavenColors.TextPrimary
                  )
                }
              },
              error = {
                Text(
                  text = "A",
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 38.sp,
                  color = HavenColors.TextPrimary
                )
              }
            )
          }
        }

        // 16px below avatar
        Spacer(modifier = Modifier.height(16.dp))

        // Display Name (22sp, weight 600, tracking -0.015em, lineHeight 1.25)
        Text(
          text = displayName,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 22.sp,
          lineHeight = 27.5.sp,
          letterSpacing = (-0.33).sp,
          color = HavenColors.TextPrimary,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_display_name")
        )

        // Handle & Joined Row (14sp, 4px margin top)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = handleJoined,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 14.sp,
          color = HavenColors.TextSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("profile_handle_joined")
        )

        // Profile Bio (16sp, lineHeight 24sp, 12px margin top)
        // Strict: Zero empty space where bio would be if null
        if (!bioText.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = bioText,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = HavenColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("profile_bio")
          )
        }
      }

      // 28px margin between identity block and links group
      Spacer(modifier = Modifier.height(28.dp))

      // 4-side bordered link group: calc(100% - 32px), 12px radius, 1px solid border
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(HavenColors.SurfaceCard)
          .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
          .testTag("profile_links_group")
      ) {
        ProfileLinkRow(
          icon = ProfileIcons.Storefront,
          label = "My offers",
          showChevron = true,
          showDivider = true,
          testTag = "profile_link_my_offers",
          onClick = {
            currentSubScreen = ProfileSubScreen.MY_OFFERS
          }
        )

        ProfileLinkRow(
          icon = ProfileIcons.FavoriteBorder,
          label = if (savedOffersCount > 0) "Saved offers ($savedOffersCount)" else "Saved offers",
          showChevron = true,
          showDivider = true,
          testTag = "profile_link_saved_offers",
          onClick = {
            currentSubScreen = ProfileSubScreen.SAVED_OFFERS
          }
        )

        ProfileLinkRow(
          icon = ProfileIcons.Group,
          label = "Following",
          showChevron = true,
          showDivider = true,
          testTag = "profile_link_following",
          onClick = {
            currentSubScreen = ProfileSubScreen.FOLLOWING
          }
        )

        ProfileLinkRow(
          icon = ProfileIcons.Visibility,
          label = "View public profile",
          showChevron = true,
          showDivider = true,
          testTag = "profile_link_view_public",
          onClick = {
            snackbarText = "Public profile preview active."
            showSnackbar = true
          }
        )

        ProfileLinkRow(
          icon = ProfileIcons.Edit,
          label = "Edit profile",
          showChevron = true,
          showDivider = true,
          testTag = "profile_link_edit_profile",
          onClick = {
            snackbarText = "Your profile has been updated."
            showSnackbar = true
            specState = ProfileSpecState.STATE_A_UPDATED
          }
        )

        // Appearance row to toggle Light, Dark, or System theme
        ProfileLinkRow(
          icon = ProfileIcons.Appearance,
          label = "Appearance",
          showChevron = true,
          showDivider = false,
          trailingText = ThemeManager.themeMode.title,
          testTag = "profile_link_appearance",
          onClick = {
            showAppearanceDialog = true
          }
        )
      }

      // 16px vertical gap between link rows container and sign out container
      Spacer(modifier = Modifier.height(16.dp))

      // Distinct Sign Out Container (1px solid border, 12px radius, 56dp height)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(HavenColors.SurfaceCard)
          .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
          .testTag("profile_sign_out_group")
      ) {
        ProfileLinkRow(
          icon = ProfileIcons.Logout,
          label = "Sign out",
          showChevron = false,
          showDivider = false,
          testTag = "profile_link_sign_out",
          onClick = {
            showSignOutDialog = true
            specState = ProfileSpecState.STATE_B_SIGN_OUT_DIALOG
          }
        )
      }

      Spacer(modifier = Modifier.height(24.dp))
    }

    // State A: Pinned 48dp Snackbar (16dp above bottom navigation bar, 8dp radius)
    AnimatedVisibility(
      visible = showSnackbar,
      enter = fadeIn() + slideInVertically { it / 2 },
      exit = fadeOut() + slideOutVertically { it / 2 },
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .shadow(8.dp, RoundedCornerShape(8.dp))
          .clip(RoundedCornerShape(8.dp))
          .background(HavenColors.SnackbarBackground)
          .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(8.dp))
          .clickable { showSnackbar = false }
          .padding(horizontal = 16.dp)
          .testTag("profile_updated_snackbar"),
        contentAlignment = Alignment.CenterStart
      ) {
        Text(
          text = snackbarText,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 14.sp,
          color = HavenColors.SnackbarText
        )
      }
    }

    // Appearance Selection Dialog
    if (showAppearanceDialog) {
      AppearanceSelectionDialog(
        currentMode = ThemeManager.themeMode,
        onSelectMode = { newMode ->
          ThemeManager.setMode(newMode, context)
        },
        onDismiss = { showAppearanceDialog = false }
      )
    }

    // State B: Sign Out Confirmation Dialog (320dp width, 28dp radius, 24dp padding)
    if (showSignOutDialog) {
      Dialog(
        onDismissRequest = {
          showSignOutDialog = false
          if (specState == ProfileSpecState.STATE_B_SIGN_OUT_DIALOG) {
            specState = ProfileSpecState.DEFAULT
          }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.40f))
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null
            ) {
              showSignOutDialog = false
              if (specState == ProfileSpecState.STATE_B_SIGN_OUT_DIALOG) {
                specState = ProfileSpecState.DEFAULT
              }
            },
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .width(320.dp)
              .clip(RoundedCornerShape(28.dp))
              .background(HavenColors.SurfaceCard)
              .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(28.dp))
              .shadow(24.dp, RoundedCornerShape(28.dp))
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
              ) { /* Consume clicks inside dialog */ }
              .padding(24.dp)
              .testTag("sign_out_dialog")
          ) {
            Column {
              // Dialog Title: 20sp, weight 600
              Text(
                text = "Sign out?",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 25.sp,
                color = HavenColors.TextPrimary,
                modifier = Modifier.testTag("dialog_title")
              )

              Spacer(modifier = Modifier.height(12.dp))

              // Dialog Message: 16sp, lineHeight 24sp
              Text(
                text = "Are you sure you want to sign out of Haven?",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = HavenColors.TextSecondary,
                modifier = Modifier.testTag("dialog_message")
              )

              Spacer(modifier = Modifier.height(24.dp))

              // Dialog Actions Row (Cancel and Sign out, min touch target 48dp)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                      showSignOutDialog = false
                      if (specState == ProfileSpecState.STATE_B_SIGN_OUT_DIALOG) {
                        specState = ProfileSpecState.DEFAULT
                      }
                    }
                    .padding(horizontal = 16.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "Cancel",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = HavenColors.TextPrimary,
                    modifier = Modifier.testTag("dialog_cancel_button")
                  )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                  modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                      showSignOutDialog = false
                      specState = ProfileSpecState.DEFAULT
                      onSignOut()
                    }
                    .padding(horizontal = 16.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "Sign out",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = HavenColors.TextPrimary,
                    modifier = Modifier.testTag("dialog_confirm_sign_out_button")
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

/**
 * Clean Monastic Appearance Selection Modal Dialog
 */
@Composable
private fun AppearanceSelectionDialog(
  currentMode: ThemeMode,
  onSelectMode: (ThemeMode) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.45f))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) { onDismiss() },
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = Modifier
          .width(320.dp)
          .clip(RoundedCornerShape(28.dp))
          .background(HavenColors.SurfaceCard)
          .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(28.dp))
          .shadow(24.dp, RoundedCornerShape(28.dp))
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
          ) { /* consume clicks */ }
          .padding(24.dp)
          .testTag("appearance_dialog")
      ) {
        Column {
          Text(
            text = "Appearance",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = HavenColors.TextPrimary,
            modifier = Modifier.testTag("appearance_dialog_title")
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Choose your theme preference across Haven",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = HavenColors.TextSecondary
          )

          Spacer(modifier = Modifier.height(20.dp))

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
              .background(HavenColors.SurfaceContainer)
          ) {
            ThemeMode.values().forEachIndexed { index, mode ->
              val isSelected = currentMode == mode
              val isLast = index == ThemeMode.values().size - 1

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    ThemeManager.setMode(mode, context)
                    onSelectMode(mode)
                  }
                  .padding(horizontal = 14.dp, vertical = 13.dp)
                  .testTag("appearance_option_${mode.name.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = mode.title,
                    fontFamily = InterFontFamily,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = HavenColors.TextPrimary
                  )
                  Text(
                    text = mode.subtitle,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = HavenColors.TextSecondary
                  )
                }

                // Radio indicator
                Box(
                  modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(
                      width = if (isSelected) 2.dp else 1.5.dp,
                      color = if (isSelected) HavenColors.Primary else HavenColors.BorderDefault,
                      shape = CircleShape
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  if (isSelected) {
                    Box(
                      modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(HavenColors.Primary)
                    )
                  }
                }
              }

              if (!isLast) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HavenColors.BorderSubtle)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            Box(
              modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onDismiss() }
                .padding(horizontal = 16.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Done",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = HavenColors.TextPrimary,
                modifier = Modifier.testTag("appearance_done_button")
              )
            }
          }
        }
      }
    }
  }
}

/**
 * 56px Link Row with 16px padding and clean 1px hairline dividers
 */
@Composable
private fun ProfileLinkRow(
  icon: ImageVector,
  label: String,
  showChevron: Boolean,
  showDivider: Boolean,
  testTag: String,
  trailingText: String? = null,
  onClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = HavenColors.TextPrimary,
          modifier = Modifier.size(24.dp)
        )
        Text(
          text = label,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 16.sp,
          color = HavenColors.TextPrimary
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (!trailingText.isNullOrBlank()) {
          Text(
            text = trailingText,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = HavenColors.TextSecondary
          )
        }
        if (showChevron) {
          Icon(
            imageVector = ProfileIcons.ChevronRight,
            contentDescription = null,
            tint = HavenColors.TextSecondary,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }

    if (showDivider) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(HavenColors.BorderSubtle)
      )
    }
  }
}

/**
 * Monastic Vector Icons for Profile screen
 */
object ProfileIcons {
  val Storefront: ImageVector = ImageVector.Builder(
    name = "Storefront",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).apply {
    path(fill = SolidColor(Color(0xFF111827))) {
      moveTo(20f, 4f)
      horizontalLineTo(4f)
      verticalLineToRelative(2f)
      horizontalLineToRelative(16f)
      verticalLineTo(4f)
      close()
      moveTo(21f, 14f)
      verticalLineToRelative(-2f)
      lineToRelative(-1f, -5f)
      horizontalLineTo(4f)
      lineToRelative(-1f, 5f)
      verticalLineToRelative(2f)
      horizontalLineToRelative(1f)
      verticalLineToRelative(6f)
      horizontalLineToRelative(10f)
      verticalLineToRelative(-6f)
      horizontalLineToRelative(4f)
      verticalLineToRelative(6f)
      horizontalLineToRelative(2f)
      verticalLineToRelative(-6f)
      horizontalLineToRelative(1f)
      close()
      moveTo(12f, 18f)
      horizontalLineTo(6f)
      verticalLineToRelative(-4f)
      horizontalLineToRelative(6f)
      verticalLineToRelative(4f)
      close()
    }
  }.build()

  val FavoriteBorder: ImageVector = ImageVector.Builder(
    name = "FavoriteBorder",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).apply {
    path(fill = SolidColor(Color(0xFF111827))) {
      moveTo(16.5f, 3f)
      curveToRelative(-1.74f, 0f, -3.41f, 0.81f, -4.5f, 2.09f)
      curveTo(10.91f, 3.81f, 9.24f, 3f, 7.5f, 3f)
      curveTo(4.42f, 3f, 2f, 5.42f, 2f, 8.5f)
      curveToRelative(0f, 3.78f, 3.4f, 6.86f, 8.55f, 11.54f)
      lineTo(12f, 21.35f)
      lineToRelative(1.45f, -1.32f)
      curveTo(18.6f, 15.36f, 22f, 12.28f, 22f, 8.5f)
      curveTo(22f, 5.42f, 19.58f, 3f, 16.5f, 3f)
      close()
      moveTo(12.1f, 18.55f)
      lineToRelative(-0.1f, 0.1f)
      lineToRelative(-0.1f, -0.1f)
      curveTo(7.14f, 14.24f, 4f, 11.39f, 4f, 8.5f)
      curveTo(4f, 6.5f, 5.5f, 5f, 7.5f, 5f)
      curveToRelative(1.54f, 0f, 3.04f, 0.99f, 3.57f, 2.36f)
      horizontalLineToRelative(1.87f)
      curveTo(13.46f, 5.99f, 14.96f, 5f, 16.5f, 5f)
      curveToRelative(2f, 0f, 3.5f, 1.5f, 3.5f, 3.5f)
      curveToRelative(0f, 2.89f, -3.14f, 5.74f, -7.9f, 10.05f)
      close()
    }
  }.build()

  val Group: ImageVector = ImageVector.Builder(
    name = "Group",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).apply {
    path(fill = SolidColor(Color(0xFF111827))) {
      moveTo(16f, 11f)
      curveToRelative(1.66f, 0f, 2.99f, -1.34f, 2.99f, -3f)
      reflectiveCurveTo(17.66f, 5f, 16f, 5f)
      curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
      reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
      close()
      moveTo(8f, 11f)
      curveToRelative(1.66f, 0f, 2.99f, -1.34f, 2.99f, -3f)
      reflectiveCurveTo(9.66f, 5f, 8f, 5f)
      curveTo(6.34f, 5f, 5f, 6.34f, 5f, 8f)
      reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
      close()
      moveTo(8f, 13f)
      curveToRelative(-2.33f, 0f, -7f, 1.17f, -7f, 3.5f)
      verticalLineTo(19f)
      horizontalLineToRelative(14f)
      verticalLineToRelative(-2.5f)
      curveToRelative(0f, -2.33f, -4.67f, -3.5f, -7f, -3.5f)
      close()
      moveTo(16f, 13f)
      curveToRelative(-0.29f, 0f, -0.62f, 0.02f, -0.97f, 0.05f)
      curveToRelative(1.16f, 0.84f, 1.97f, 1.97f, 1.97f, 3.45f)
      verticalLineTo(19f)
      horizontalLineToRelative(6f)
      verticalLineToRelative(-2.5f)
      curveToRelative(0f, -2.33f, -4.67f, -3.5f, -7f, -3.5f)
      close()
    }
  }.build()

  val Visibility: ImageVector = ImageVector.Builder(
    name = "Visibility",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).apply {
    path(fill = SolidColor(Color(0xFF111827))) {
      moveTo(12f, 4.5f)
      curveTo(7f, 4.5f, 2.73f, 7.61f, 1f, 12f)
      curveToRelative(1.73f, 4.39f, 6f, 7.5f, 11f, 7.5f)
      reflectiveCurveToRelative(9.27f, -3.11f, 11f, -7.5f)
      curveToRelative(-1.73f, -4.39f, -6f, -7.5f, -11f, -7.5f)
      close()
      moveTo(12f, 17f)
      curveToRelative(-2.76f, 0f, -5f, -2.24f, -5f, -5f)
      reflectiveCurveToRelative(2.24f, -5f, 5f, -5f)
      reflectiveCurveToRelative(5f, 2.24f, 5f, 5f)
      reflectiveCurveToRelative(-2.24f, 5f, -5f, 5f)
      close()
      moveTo(12f, 9f)
      curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
      reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
      reflectiveCurveToRelative(3f, -1.34f, 3f, -3f)
      reflectiveCurveToRelative(-1.34f, -3f, -3f, -3f)
      close()
    }
  }.build()

  val Edit: ImageVector = ImageVector.Builder(
    name = "Edit",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).apply {
    path(fill = SolidColor(Color(0xFF111827))) {
      moveTo(3f, 17.25f)
      verticalLineTo(21f)
      horizontalLineToRelative(3.75f)
      lineTo(17.81f, 9.94f)
      lineToRelative(-3.75f, -3.75f)
      lineTo(3f, 17.25f)
      close()
      moveTo(20.71f, 7.04f)
      curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0f, -1.41f)
      lineToRelative(-2.34f, -2.34f)
      curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0f)
      lineToRelative(-1.83f, 1.83f)
      lineToRelative(3.75f, 3.75f)
      lineToRelative(1.83f, -1.83f)
      close()
    }
  }.build()

  val Appearance: ImageVector = ImageVector.Builder(
    name = "Appearance",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).apply {
    path(fill = SolidColor(Color(0xFF111827))) {
      moveTo(12f, 3f)
      curveToRelative(-4.97f, 0f, -9f, 4.03f, -9f, 9f)
      reflectiveCurveToRelative(4.03f, 9f, 9f, 9f)
      reflectiveCurveToRelative(9f, -4.03f, 9f, -9f)
      curveToRelative(0f, -0.46f, -0.04f, -0.92f, -0.1f, -1.36f)
      curveToRelative(-0.98f, 1.37f, -2.58f, 2.26f, -4.4f, 2.26f)
      curveToRelative(-2.98f, 0f, -5.4f, -2.42f, -5.4f, -5.4f)
      curveToRelative(0f, -1.81f, 0.89f, -3.42f, 2.26f, -4.4f)
      curveToRelative(-0.44f, -0.06f, -0.9f, -0.1f, -1.36f, -0.1f)
      close()
    }
  }.build()

  val Logout: ImageVector = ImageVector.Builder(
    name = "Logout",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).apply {
    path(fill = SolidColor(Color(0xFF111827))) {
      moveTo(17f, 7f)
      lineToRelative(-1.41f, 1.41f)
      lineTo(18.17f, 11f)
      horizontalLineTo(8f)
      verticalLineToRelative(2f)
      horizontalLineToRelative(10.17f)
      lineToRelative(-2.58f, 2.58f)
      lineTo(17f, 17f)
      lineToRelative(5f, -5f)
      close()
      moveTo(4f, 5f)
      horizontalLineToRelative(8f)
      verticalLineTo(3f)
      horizontalLineTo(4f)
      curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
      verticalLineToRelative(14f)
      curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
      horizontalLineToRelative(8f)
      verticalLineToRelative(-2f)
      horizontalLineTo(4f)
      verticalLineTo(5f)
      close()
    }
  }.build()

  val ChevronRight: ImageVector = ImageVector.Builder(
    name = "ChevronRight",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
  ).apply {
    path(fill = SolidColor(Color(0xFF6B7280))) {
      moveTo(10f, 6f)
      lineTo(8.59f, 7.41f)
      lineTo(13.17f, 12f)
      lineToRelative(-4.58f, 4.59f)
      lineTo(10f, 18f)
      lineToRelative(6f, -6f)
      close()
    }
  }.build()
}
