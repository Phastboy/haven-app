package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Haven Text Field matching the exact monastic design specifications:
 * - 52dp height
 * - 8dp corner radius
 * - 14sp Medium label
 * - 15sp text content
 * - Distinct idle, focused (2px #111827), error (1px #111827 + right error icon), and disabled states.
 */
@Composable
fun HavenTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String,
  modifier: Modifier = Modifier,
  helperText: String = "The link works for 15 minutes.",
  errorMessage: String? = null,
  isError: Boolean = errorMessage != null,
  enabled: Boolean = true,
  keyboardOptions: KeyboardOptions = KeyboardOptions(
    keyboardType = KeyboardType.Email,
    imeAction = ImeAction.Done
  ),
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  testTag: String = "email_input"
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  // Monastic border calculation:
  // Error: 1px #111827
  // Focused: 2px #111827
  // Disabled: 1px #E5E7EB
  // Idle: 1px #D1D5DB
  val (borderWidth, borderColor) = when {
    !enabled -> 1.dp to HavenColors.BorderSubtle
    isError -> 1.dp to HavenColors.BorderStrong
    isFocused -> 2.dp to HavenColors.BorderStrong
    else -> 1.dp to HavenColors.BorderDefault
  }

  val backgroundColor = when {
    !enabled -> HavenColors.SurfaceDisabled
    else -> HavenColors.Canvas
  }

  val labelColor = when {
    !enabled -> HavenColors.TextSecondary
    else -> HavenColors.TextPrimary
  }

  val textColor = when {
    !enabled -> HavenColors.TextSecondary
    else -> HavenColors.TextPrimary
  }

  Column(modifier = modifier.fillMaxWidth()) {
    // Input Label
    Text(
      text = label,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 14.sp,
      color = labelColor,
      modifier = Modifier.padding(bottom = 8.dp)
    )

    // Input Box Container
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .background(color = backgroundColor, shape = RoundedCornerShape(8.dp))
        .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(8.dp))
        .padding(horizontal = 14.dp),
      contentAlignment = Alignment.CenterStart
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier.weight(1f),
          contentAlignment = Alignment.CenterStart
        ) {
          if (value.isEmpty()) {
            Text(
              text = placeholder,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Normal,
              fontSize = 15.sp,
              color = HavenColors.TextMuted
            )
          }

          BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            interactionSource = interactionSource,
            textStyle = TextStyle(
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Normal,
              fontSize = 15.sp,
              color = textColor
            ),
            cursorBrush = SolidColor(HavenColors.Primary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier
              .fillMaxWidth()
              .testTag(testTag)
          )
        }

        // Right side icon for validation error state
        if (isError) {
          Spacer(modifier = Modifier.size(8.dp))
          Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = "Validation Error",
            tint = HavenColors.Primary,
            modifier = Modifier
              .size(20.dp)
              .testTag("email_error_icon")
          )
        }
      }
    }

    // Helper text or Error message
    Spacer(modifier = Modifier.height(6.dp))
    if (isError && errorMessage != null) {
      Text(
        text = errorMessage,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        color = HavenColors.TextPrimary,
        modifier = Modifier.testTag("email_error_message")
      )
    } else {
      Text(
        text = helperText,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = if (enabled) HavenColors.TextSecondary else HavenColors.TextMuted,
        modifier = Modifier.testTag("email_helper_text")
      )
    }
  }
}
