package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HavenAlertBanner
import com.example.ui.components.HavenGoogleButton
import com.example.ui.components.HavenInsets
import com.example.ui.components.HavenPrimaryButton
import com.example.ui.components.HavenTextField
import com.example.ui.components.HavenTopBarContainer
import com.example.ui.model.SignInViewModel
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Screen 2: Sign In Screen
 * Faithfully implements all specifications:
 * - Default Idle State
 * - State A: Keyboard Open / Typing with keyboard avoidance and dynamic insets
 * - Error banner for browser sign-in failures
 * - Invalid email validation before browser handoff
 *
 * Centralized Insets:
 * - Top bar extends behind the status bar and display cutouts to the top edge.
 * - Interactive form lifts cleanly above the IME virtual keyboard.
 * - Respects gesture navigation handle and 3-button navigation when keyboard is dismissed.
 */
@Composable
fun SignInScreen(
  viewModel: SignInViewModel,
  onNavigateBack: () -> Unit,
  onStartBrowserSignIn: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val focusManager = LocalFocusManager.current
  val scrollState = rememberScrollState()

  // Handle Android physical/gesture back button to return to Welcome
  BackHandler {
    onNavigateBack()
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .pointerInput(Unit) {
        detectTapGestures(onTap = { focusManager.clearFocus() })
      }
      .testTag("sign_in_screen")
  ) {
    // 56px Top Bar with Back Chevron & Status Bar / Notch Inset integration
    HavenTopBarContainer(
      backgroundColor = HavenColors.Canvas,
      showBottomBorder = true,
      borderColor = HavenColors.BorderSubtle
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
      ) {
        IconButton(
          onClick = onNavigateBack,
          enabled = !uiState.isSending,
          modifier = Modifier
            .size(48.dp)
            .testTag("back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Go back",
            tint = if (uiState.isSending) HavenColors.TextMuted else HavenColors.Primary,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }

    // Main Content Area with natural scroll and keyboard avoidance
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .windowInsetsPadding(HavenInsets.bottomWithIme.only(WindowInsetsSides.Bottom))
        .padding(horizontal = 24.dp)
        .verticalScroll(scrollState),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Spacer(modifier = Modifier.height(24.dp))

        // Heading & Subtitle
        Text(
          text = "Sign in",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 25.sp,
          lineHeight = 30.sp,
          letterSpacing = (-0.3).sp,
          color = HavenColors.TextPrimary,
          modifier = Modifier.testTag("sign_in_title")
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Welcome to Haven.",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 16.sp,
          color = HavenColors.TextSecondary,
          modifier = Modifier.testTag("sign_in_subtitle")
        )

        Spacer(modifier = Modifier.height(24.dp))

          // State C: Auth Error Banner (when Google auth was tapped)
          AnimatedVisibility(
            visible = uiState.googleErrorBanner != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut()
          ) {
            uiState.googleErrorBanner?.let { message ->
              HavenAlertBanner(
                message = message,
                modifier = Modifier.padding(bottom = 16.dp),
                testTag = "google_error_banner"
              )
            }
          }

          // Google Auth Button
          HavenGoogleButton(
            onClick = {
              focusManager.clearFocus()
              viewModel.onGoogleSignInClicked()
              onStartBrowserSignIn()
            },
            enabled = !uiState.isInputsDisabled,
            testTag = "google_sign_in_button"
          )

          // Divider: 1px line with "or" in center
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
          ) {
            HorizontalDivider(
              color = HavenColors.BorderSubtle,
              thickness = 1.dp,
              modifier = Modifier.fillMaxWidth()
            )
            Text(
              text = "or",
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Normal,
              fontSize = 13.sp,
              color = if (uiState.isSending) HavenColors.TextMuted else HavenColors.TextSecondary,
              modifier = Modifier
                .background(HavenColors.Canvas)
                .padding(horizontal = 12.dp)
            )
          }

          // Email Input Form Group
          HavenTextField(
            value = uiState.email,
            onValueChange = { viewModel.onEmailChange(it) },
            label = "Email",
            placeholder = "name@example.com",
            errorMessage = uiState.emailError,
            isError = uiState.isError,
            enabled = !uiState.isInputsDisabled,
            keyboardActions = KeyboardActions(
              onDone = {
                focusManager.clearFocus()
                if (viewModel.onSendMagicLink()) onStartBrowserSignIn()
              }
            ),
            testTag = "email_input_field"
          )

          Spacer(modifier = Modifier.height(20.dp))

          // Primary Submit Button ("Send magic link" or "Sending...")
          HavenPrimaryButton(
            text = "Continue with email",
            onClick = {
              focusManager.clearFocus()
              if (viewModel.onSendMagicLink()) onStartBrowserSignIn()
            },
            enabled = !uiState.isInputsDisabled,
            isLoading = uiState.isSending,
            testTag = "send_magic_link_button"
          )
      }

      // Account creation note
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 28.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "New to Haven? We'll create your account the first time you sign in.",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 13.sp,
          lineHeight = 18.sp,
          color = if (uiState.isSending) HavenColors.TextMuted else HavenColors.TextSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .fillMaxWidth(0.9f)
            .testTag("account_creation_note")
        )

      }
    }
  }
}
