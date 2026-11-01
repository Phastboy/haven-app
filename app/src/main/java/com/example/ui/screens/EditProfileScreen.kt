package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.ui.components.HavenBackButton
import com.example.ui.components.HavenInsets
import com.example.ui.components.HavenTopBarContainer
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily
import kotlinx.coroutines.delay

private const val DEFAULT_AVATAR_URL =
  "https://lh3.googleusercontent.com/aida/AEtjO1XT1RMubL8e8hGOBhw0NvVZyAtLhFXR1IlcXKlP77a0ZvKEXNjhZYJWYiQGy_NxHAEs-n9GJhFMoevM7g5HjrJWBU4NxEXmz-YJXxy_k9A9-crh3u1LNXdTOLbxoETTKILuDGSJC5ZCM01HqG-2e71Ilkdm7DFBWuO6ljlIEjq7PSEAW7L-ddjXkY-fO0VH3GqF89oc8Cu0CWZISQZu8NwYUFWgcU_RSDU4cM2x6Oju8m0IrHNemURl_yzw"

/**
 * Edit Profile Screen (/profile/edit):
 * Implements Frame 01 (Filled form with photo & avatar toast) and Frame 02 (Field error states with zero accent colors)
 * strictly following the Haven Monastic Greyscale Design System.
 */
@Composable
fun EditProfileScreen(
  onBack: () -> Unit,
  onSaveSuccess: () -> Unit = {},
  initialName: String = "Adaeze Obi",
  initialHandle: String = "adaeze-obi",
  initialBio: String = "I weave Aso Oke the traditional way on a narrow loom. I take custom orders for weddings and naming ceremonies, and every piece is made to order.",
  hasPhotoInitial: Boolean = true,
  modifier: Modifier = Modifier
) {
  BackHandler { onBack() }
  val focusManager = LocalFocusManager.current

  var displayName by remember { mutableStateOf(initialName) }
  var handle by remember { mutableStateOf(initialHandle) }
  var bio by remember { mutableStateOf(initialBio) }
  var hasPhoto by remember { mutableStateOf(hasPhotoInitial) }

  var nameError by remember { mutableStateOf<String?>(null) }
  var handleError by remember { mutableStateOf<String?>(null) }
  var bioError by remember { mutableStateOf<String?>(null) }

  var snackbarText by remember { mutableStateOf<String?>(null) }
  var showSnackbar by remember { mutableStateOf(false) }

  LaunchedEffect(showSnackbar) {
    if (showSnackbar) {
      delay(3500)
      showSnackbar = false
    }
  }

  fun validate(): Boolean {
    var isValid = true

    if (displayName.trim().length < 2) {
      nameError = "Display name must be at least 2 characters."
      isValid = false
    } else {
      nameError = null
    }

    val handlePattern = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    if (!handlePattern.matches(handle.trim())) {
      handleError = "Handle must contain only lowercase letters, digits, and single hyphens."
      isValid = false
    } else {
      handleError = null
    }

    if (bio.length > 500) {
      bioError = "Bio cannot exceed 500 characters."
      isValid = false
    } else {
      bioError = null
    }

    return isValid
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .windowInsetsPadding(HavenInsets.bottomWithIme)
      .testTag("edit_profile_screen")
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top App Bar (Back-only)
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
            testTag = "edit_profile_back_button"
          )
        }
      }

      // Scrollable Form Body
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        // Content Title & Subtitle
        Text(
          text = "Edit profile",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 25.sp,
          letterSpacing = (-0.5).sp,
          color = HavenColors.TextPrimary,
          modifier = Modifier.testTag("edit_profile_title")
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Update your public name, handle, bio, and avatar.",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 16.sp,
          lineHeight = 23.sp,
          color = HavenColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Centered 96px Avatar Box
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(96.dp)
              .clip(CircleShape)
              .background(HavenColors.SurfaceContainer)
              .border(1.dp, HavenColors.BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            if (hasPhoto) {
              SubcomposeAsyncImage(
                model = DEFAULT_AVATAR_URL,
                contentDescription = "Profile portrait",
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
                      fontSize = 32.sp,
                      color = HavenColors.TextPrimary
                    )
                  }
                },
                error = {
                  Text(
                    text = "A",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 32.sp,
                    color = HavenColors.TextPrimary
                  )
                }
              )
            } else {
              Text(
                text = "A",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp,
                color = HavenColors.TextPrimary
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Change photo",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp,
              color = HavenColors.TextPrimary,
              textDecoration = TextDecoration.Underline,
              modifier = Modifier
                .clickable {
                  hasPhoto = true
                  snackbarText = "Avatar photo updated successfully."
                  showSnackbar = true
                }
                .padding(vertical = 4.dp)
                .testTag("change_photo_button")
            )

            if (hasPhoto) {
              Text(
                text = "Remove photo",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = HavenColors.TextPrimary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                  .clickable {
                    hasPhoto = false
                    snackbarText = "Avatar photo removed."
                    showSnackbar = true
                  }
                  .padding(vertical = 4.dp)
                  .testTag("remove_photo_button")
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "JPEG, PNG or WebP up to 5 MB. Photo changes are saved right away.",
            fontFamily = InterFontFamily,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = HavenColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Field 1: Display Name
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Display name",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = HavenColors.TextPrimary
          )

          Spacer(modifier = Modifier.height(6.dp))

          val isNameError = nameError != null
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(HavenColors.Canvas)
              .border(
                width = if (isNameError) 2.dp else 1.dp,
                color = if (isNameError) HavenColors.TextPrimary else HavenColors.BorderDefault,
                shape = RoundedCornerShape(8.dp)
              )
              .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
          ) {
            BasicTextField(
              value = displayName,
              onValueChange = {
                displayName = it
                if (nameError != null) validate()
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
                .testTag("display_name_input")
            )
          }

          if (isNameError) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
              modifier = Modifier.padding(top = 5.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = HavenColors.TextPrimary,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = nameError!!,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = HavenColors.TextPrimary
              )
            }
          } else {
            Text(
              text = "The name people see on your offers.",
              fontFamily = InterFontFamily,
              fontSize = 13.sp,
              color = HavenColors.TextSecondary,
              modifier = Modifier.padding(top = 5.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Field 2: Handle (with @ prefix)
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Handle",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = HavenColors.TextPrimary
          )

          Spacer(modifier = Modifier.height(6.dp))

          val isHandleError = handleError != null
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(HavenColors.Canvas)
              .border(
                width = if (isHandleError) 2.dp else 1.dp,
                color = if (isHandleError) HavenColors.TextPrimary else HavenColors.BorderDefault,
                shape = RoundedCornerShape(8.dp)
              ),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "@",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 15.sp,
              color = HavenColors.TextSecondary,
              modifier = Modifier.padding(start = 14.dp, end = 4.dp)
            )

            BasicTextField(
              value = handle,
              onValueChange = {
                handle = it
                if (handleError != null) validate()
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
                .weight(1f)
                .padding(end = 14.dp)
                .testTag("handle_input")
            )
          }

          if (isHandleError) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
              modifier = Modifier.padding(top = 5.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = HavenColors.TextPrimary,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = handleError!!,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = HavenColors.TextPrimary
              )
            }
          } else {
            Text(
              text = "Unique URL handle for your profile (e.g. alex).",
              fontFamily = InterFontFamily,
              fontSize = 13.sp,
              color = HavenColors.TextSecondary,
              modifier = Modifier.padding(top = 5.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Field 3: Bio
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Bio",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = HavenColors.TextPrimary
          )

          Spacer(modifier = Modifier.height(6.dp))

          val isBioError = bioError != null
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(120.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(HavenColors.Canvas)
              .border(
                width = if (isBioError) 2.dp else 1.dp,
                color = if (isBioError) HavenColors.TextPrimary else HavenColors.BorderDefault,
                shape = RoundedCornerShape(8.dp)
              )
              .padding(14.dp),
            contentAlignment = Alignment.TopStart
          ) {
            BasicTextField(
              value = bio,
              onValueChange = {
                bio = it
                if (bioError != null) validate()
              },
              textStyle = TextStyle(
                fontFamily = InterFontFamily,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = HavenColors.TextPrimary
              ),
              cursorBrush = SolidColor(HavenColors.TextPrimary),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
              modifier = Modifier
                .fillMaxSize()
                .testTag("bio_input")
            )
          }

          if (isBioError) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
              modifier = Modifier.padding(top = 5.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = HavenColors.TextPrimary,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = bioError!!,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = HavenColors.TextPrimary
              )
            }
          } else {
            Text(
              text = "A short bio describing what you make or offer (up to 500 characters).",
              fontFamily = InterFontFamily,
              fontSize = 13.sp,
              color = HavenColors.TextSecondary,
              modifier = Modifier.padding(top = 5.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Save Changes Button (52dp Solid Button)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(HavenColors.TextPrimary)
            .clickable {
              focusManager.clearFocus()
              if (validate()) {
                snackbarText = "Your profile has been updated."
                showSnackbar = true
                onSaveSuccess()
              }
            }
            .testTag("save_profile_button"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Save changes",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = HavenColors.TextOnDark
          )
        }

        Spacer(modifier = Modifier.height(32.dp))
      }
    }

    // Pinned Snackbar Toast at bottom
    AnimatedVisibility(
      visible = showSnackbar,
      enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .shadow(8.dp, RoundedCornerShape(8.dp))
          .clip(RoundedCornerShape(8.dp))
          .background(HavenColors.TextPrimary)
          .padding(horizontal = 16.dp, vertical = 14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = snackbarText ?: "Your profile has been updated.",
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
