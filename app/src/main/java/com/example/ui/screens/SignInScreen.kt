package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HavenAlertBanner
import com.example.ui.components.HavenGoogleButton
import com.example.ui.components.HavenPrimaryButton
import com.example.ui.components.HavenSecondaryButton
import com.example.ui.components.HavenTextField
import com.example.ui.model.SignInViewModel
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Screen 2: Sign In Screen
 * Faithfully implements:
 * - Default Idle State
 * - State A: Keyboard Open / Typing with keyboard avoidance
 * - State B: Loading (Sending...)
 * - State C: Error Banner ("Google sign-in was cancelled.")
 * - State D: Invalid Email Validation ("Enter a valid email address.")
 * - Success State: "Check your email"
 */
@Composable
fun SignInScreen(
  viewModel: SignInViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val focusManager = LocalFocusManager.current
  val scrollState = rememberScrollState()

  // Handle Android physical/gesture back button to return to Welcome
  BackHandler {
    onNavigateBack()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(HavenColors.Canvas)
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) {
        focusManager.clearFocus()
      }
      .testTag("sign_in_screen")
  ) {
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      // 56px Top Bar with Back Chevron
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .border(width = 1.dp, color = HavenColors.BorderSubtle)
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

      // Main Content Area with natural scroll
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
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

          if (uiState.isSuccess) {
            // Success View ("Check your email")
            SuccessMessageCard(
              email = uiState.successEmail,
              onResend = { viewModel.onSendMagicLink() },
              onUseDifferentEmail = { viewModel.onResetSuccess() }
            )
          } else {
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
                  viewModel.onSendMagicLink()
                }
              ),
              testTag = "email_input_field"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Submit Button ("Send magic link" or "Sending...")
            HavenPrimaryButton(
              text = "Send magic link",
              onClick = {
                focusManager.clearFocus()
                viewModel.onSendMagicLink()
              },
              enabled = !uiState.isInputsDisabled,
              isLoading = uiState.isSending,
              testTag = "send_magic_link_button"
            )
          }
        }

        // Bottom Footer: centered account creation note & quick state inspector
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 12.dp),
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

          Spacer(modifier = Modifier.height(16.dp))

          // Subtle Spec State Selector Chips (allows testing States A, B, C, D instantly)
          StateInspectorRow(
            onSelectIdle = { viewModel.setIdlePreset() },
            onSelectInvalid = { viewModel.setInvalidEmailPreset() },
            onSelectSending = { viewModel.setLoadingPreset() },
            onSelectGoogleError = { viewModel.setGoogleErrorPreset() }
          )
        }
      }
    }
  }
}

/**
 * Success view matching the spec: "Check your email"
 */
@Composable
private fun SuccessMessageCard(
  email: String,
  onResend: () -> Unit,
  onUseDifferentEmail: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(HavenColors.SurfaceContainer, RoundedCornerShape(12.dp))
      .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(12.dp))
      .padding(20.dp)
      .testTag("success_message_card"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(HavenColors.Canvas)
        .border(1.dp, HavenColors.BorderSubtle, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Outlined.MailOutline,
        contentDescription = "Email sent",
        tint = HavenColors.Primary,
        modifier = Modifier.size(24.dp)
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "Check your email",
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 20.sp,
      color = HavenColors.TextPrimary
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "We sent a magic sign-in link to:\n$email\n\nThe link works for 15 minutes.",
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 14.sp,
      lineHeight = 20.sp,
      color = HavenColors.TextSecondary,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(20.dp))

    HavenSecondaryButton(
      text = "Sign in with a different email",
      onClick = onUseDifferentEmail,
      modifier = Modifier.fillMaxWidth()
    )
  }
}

/**
 * Optional spec inspection bar providing 1-click test toggles for all documented auxiliary states
 */
@Composable
private fun StateInspectorRow(
  onSelectIdle: () -> Unit,
  onSelectInvalid: () -> Unit,
  onSelectSending: () -> Unit,
  onSelectGoogleError: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(top = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "Interactive Spec States",
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 11.sp,
      letterSpacing = 0.5.sp,
      color = HavenColors.TextMuted,
      modifier = Modifier.padding(bottom = 6.dp)
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
      InspectorPill(label = "Default", onClick = onSelectIdle)
      InspectorPill(label = "Invalid Email", onClick = onSelectInvalid)
      InspectorPill(label = "Sending", onClick = onSelectSending)
      InspectorPill(label = "Google Alert", onClick = onSelectGoogleError)
    }
  }
}

@Composable
private fun InspectorPill(
  label: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(HavenColors.SurfaceContainer)
      .border(1.dp, HavenColors.BorderSubtle, RoundedCornerShape(6.dp))
      .clickable { onClick() }
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Text(
      text = label,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 11.sp,
      color = HavenColors.TextSecondary
    )
  }
}
