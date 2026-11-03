package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Reusable contextual empty and recovery state component.
 * Follows Monastic theme with clear hierarchy: visual anchor, concise title,
 * contextual guidance, and max 1 primary + 1 secondary recovery action.
 */
@Composable
fun HavenEmptyState(
  icon: ImageVector,
  title: String,
  body: String,
  modifier: Modifier = Modifier,
  primaryActionLabel: String? = null,
  onPrimaryAction: (() -> Unit)? = null,
  secondaryActionLabel: String? = null,
  onSecondaryAction: (() -> Unit)? = null,
  testTag: String = "haven_empty_state"
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp, vertical = 32.dp)
      .testTag(testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = HavenColors.TextMuted,
      modifier = Modifier.size(44.dp)
    )
    Spacer(Modifier.height(16.dp))
    Text(
      text = title,
      fontFamily = InterFontFamily,
      fontSize = 17.sp,
      fontWeight = FontWeight.SemiBold,
      color = HavenColors.TextPrimary,
      textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(6.dp))
    Text(
      text = body,
      fontFamily = InterFontFamily,
      fontSize = 14.sp,
      lineHeight = 20.sp,
      color = HavenColors.TextSecondary,
      textAlign = TextAlign.Center,
      modifier = Modifier.widthIn(max = 300.dp)
    )
    if (primaryActionLabel != null && onPrimaryAction != null) {
      Spacer(Modifier.height(20.dp))
      Button(
        onClick = onPrimaryAction,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = HavenColors.Primary,
          contentColor = HavenColors.TextOnDark
        ),
        modifier = Modifier
          .heightIn(min = 42.dp)
          .testTag("${testTag}_primary")
      ) {
        Text(
          text = primaryActionLabel,
          fontFamily = InterFontFamily,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }
    if (secondaryActionLabel != null && onSecondaryAction != null) {
      Spacer(Modifier.height(8.dp))
      OutlinedButton(
        onClick = onSecondaryAction,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = HavenColors.Primary
        ),
        modifier = Modifier
          .heightIn(min = 40.dp)
          .testTag("${testTag}_secondary")
      ) {
        Text(
          text = secondaryActionLabel,
          fontFamily = InterFontFamily,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}
