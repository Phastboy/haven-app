package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.SubcomposeAsyncImage
import com.example.data.ProfileOverview
import com.example.ui.components.HavenBackButton
import com.example.ui.components.HavenInsets
import com.example.ui.components.HavenTopBarContainer
import com.example.ui.model.EditProfileViewModel
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily
import kotlinx.coroutines.delay

@Composable
fun EditProfileScreen(
  onBack: () -> Unit,
  onSaveSuccess: () -> Unit = {},
  initialProfile: ProfileOverview? = null,
  viewModel: EditProfileViewModel = viewModel(factory = EditProfileViewModel.factory(LocalContext.current)),
  modifier: Modifier = Modifier
) {
  LaunchedEffect(initialProfile) {
    viewModel.initialize(initialProfile)
  }

  val state by viewModel.state.collectAsState()
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) viewModel.selectAvatar(context, uri)
  }

  BackHandler { onBack() }

  LaunchedEffect(state.saveSuccessMessage) {
    if (state.saveSuccessMessage != null) {
      delay(3500)
      viewModel.dismissSuccessMessage()
    }
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
        backgroundColor = HavenColors.Canvas
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
            val initialLetter = state.displayName.firstOrNull()?.uppercase() ?: "H"
            val initialComposable: @Composable () -> Unit = {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(HavenColors.SurfaceContainer),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = initialLetter,
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 32.sp,
                  color = HavenColors.TextPrimary
                )
              }
            }

            if (state.uploadingAvatar || state.removingAvatar) {
              CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = HavenColors.Primary
              )
            } else if (!state.avatarUrl.isNullOrBlank()) {
              SubcomposeAsyncImage(
                model = state.avatarUrl,
                contentDescription = "Profile portrait",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { initialComposable() },
                error = { initialComposable() }
              )
            } else {
              initialComposable()
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
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                }
                .padding(vertical = 4.dp)
                .testTag("change_photo_button")
            )

            if (!state.avatarUrl.isNullOrBlank()) {
              Text(
                text = "Remove photo",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = HavenColors.TextPrimary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                  .clickable {
                    viewModel.removeAvatar()
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
            color = HavenColors.TextSecondary,
            textAlign = TextAlign.Center
          )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Display Name Field
        val isNameError = state.nameError != null
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Display name",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = HavenColors.TextPrimary
          )
          Spacer(modifier = Modifier.height(6.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(HavenColors.SurfaceCard)
              .border(
                width = if (isNameError) 2.dp else 1.dp,
                color = if (isNameError) HavenColors.TextPrimary else HavenColors.BorderDefault,
                shape = RoundedCornerShape(8.dp)
              )
              .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
          ) {
            BasicTextField(
              value = state.displayName,
              onValueChange = viewModel::onDisplayNameChange,
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
                text = state.nameError!!,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = HavenColors.TextPrimary,
                modifier = Modifier.testTag("display_name_error")
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Handle Field (with haven.market/ prefix)
        val isHandleError = state.handleError != null
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Handle",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = HavenColors.TextPrimary
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(HavenColors.SurfaceCard)
              .border(
                width = if (isHandleError) 2.dp else 1.dp,
                color = if (isHandleError) HavenColors.TextPrimary else HavenColors.BorderDefault,
                shape = RoundedCornerShape(8.dp)
              ),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .background(HavenColors.SurfaceContainer)
                .padding(horizontal = 12.dp)
                .height(48.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "haven.market/",
                fontFamily = InterFontFamily,
                fontSize = 14.sp,
                color = HavenColors.TextSecondary
              )
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
              contentAlignment = Alignment.CenterStart
            ) {
              BasicTextField(
                value = state.handle,
                onValueChange = viewModel::onHandleChange,
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
                  .testTag("handle_input")
              )
            }
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
                text = state.handleError!!,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = HavenColors.TextPrimary,
                modifier = Modifier.testTag("handle_error")
              )
            }
          } else {
            Text(
              text = "Letters, numbers, and single hyphens. 3 to 30 characters.",
              fontFamily = InterFontFamily,
              fontSize = 13.sp,
              color = HavenColors.TextSecondary,
              modifier = Modifier.padding(top = 5.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bio Field (Multline, 108dp min height)
        val isBioError = state.bioError != null
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Bio",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp,
              color = HavenColors.TextPrimary
            )
            Text(
              text = "${state.bio.length} / 500",
              fontFamily = InterFontFamily,
              fontSize = 12.sp,
              color = if (state.bio.length > 500) HavenColors.TextPrimary else HavenColors.TextSecondary
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(108.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(HavenColors.SurfaceCard)
              .border(
                width = if (isBioError) 2.dp else 1.dp,
                color = if (isBioError) HavenColors.TextPrimary else HavenColors.BorderDefault,
                shape = RoundedCornerShape(8.dp)
              )
              .padding(14.dp),
            contentAlignment = Alignment.TopStart
          ) {
            BasicTextField(
              value = state.bio,
              onValueChange = viewModel::onBioChange,
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
                text = state.bioError!!,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = HavenColors.TextPrimary,
                modifier = Modifier.testTag("bio_error")
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

        state.generalError?.let { generalErr ->
          Spacer(modifier = Modifier.height(16.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.ErrorOutline,
              contentDescription = null,
              tint = HavenColors.TextPrimary,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = generalErr,
              fontFamily = InterFontFamily,
              fontSize = 14.sp,
              color = HavenColors.TextPrimary,
              modifier = Modifier.testTag("edit_profile_general_error")
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
            .background(if (state.saving) HavenColors.TextSecondary else HavenColors.TextPrimary)
            .clickable(enabled = !state.saving) {
              focusManager.clearFocus()
              viewModel.save {
                onSaveSuccess()
              }
            }
            .testTag("save_profile_button"),
          contentAlignment = Alignment.Center
        ) {
          if (state.saving) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = HavenColors.TextOnDark
            )
          } else {
            Text(
              text = "Save changes",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 16.sp,
              color = HavenColors.TextOnDark
            )
          }
        }

        Spacer(modifier = Modifier.height(32.dp))
      }
    }

    // Pinned Snackbar Toast at bottom
    AnimatedVisibility(
      visible = state.saveSuccessMessage != null,
      enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
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
            text = state.saveSuccessMessage ?: "Your profile has been updated.",
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
