package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HavenPrimaryButton
import com.example.ui.model.InquiryTarget
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Native first-message composer for offer and profile messaging.
 *
 * Sends through the live API; the caller navigates to the returned conversation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NativeMessageComposerSheet(
  target: InquiryTarget,
  text: String,
  isSending: Boolean,
  error: String?,
  onTextChanged: (String) -> Unit,
  onSend: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val title = when (target) {
    is InquiryTarget.Offer ->
      target.makerName.takeIf { it.isNotBlank() }?.let { "Message $it" } ?: "Message provider"
    is InquiryTarget.Person -> "Message ${target.displayName}"
  }
  val context = when (target) {
    is InquiryTarget.Offer -> target.offerTitle.takeIf { it.isNotBlank() }?.let { "About $it" }
    is InquiryTarget.Person -> null
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = HavenColors.Canvas,
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    modifier = modifier.testTag("message_composer_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .imePadding()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp)
        .padding(top = 4.dp, bottom = 20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        text = title,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = HavenColors.TextPrimary
      )
      if (context != null) {
        Text(
          text = context,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 14.sp,
          color = HavenColors.TextSecondary
        )
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 132.dp)
          .background(HavenColors.SurfaceContainer, RoundedCornerShape(10.dp))
          .border(1.dp, HavenColors.BorderDefault, RoundedCornerShape(10.dp))
          .padding(horizontal = 14.dp, vertical = 12.dp)
      ) {
        if (text.isEmpty()) {
          Text(
            text = "Write your message...",
            fontFamily = InterFontFamily,
            fontSize = 15.sp,
            color = HavenColors.TextMuted
          )
        }
        BasicTextField(
          value = text,
          onValueChange = onTextChanged,
          enabled = !isSending,
          textStyle = TextStyle(
            fontFamily = InterFontFamily,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            color = HavenColors.TextPrimary
          ),
          cursorBrush = SolidColor(HavenColors.Primary),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("message_composer_input")
        )
      }

      if (error != null) {
        Text(
          text = error,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 13.sp,
          color = HavenColors.TextPrimary,
          modifier = Modifier.testTag("message_composer_error")
        )
      }

      Spacer(Modifier.height(2.dp))
      HavenPrimaryButton(
        text = "Send message",
        onClick = onSend,
        enabled = text.isNotBlank(),
        isLoading = isSending,
        testTag = "message_composer_send"
      )
    }
  }
}
