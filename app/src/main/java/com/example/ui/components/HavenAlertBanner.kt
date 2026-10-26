package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Calm monastic alert banner matching State C:
 * #F9FAFB background, 1px #111827 border, 8px radius, 12px padding, warning alert icon.
 */
@Composable
fun HavenAlertBanner(
  message: String,
  modifier: Modifier = Modifier,
  testTag: String = "auth_error_banner"
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(color = HavenColors.SurfaceContainer, shape = RoundedCornerShape(8.dp))
      .border(width = 1.dp, color = HavenColors.BorderStrong, shape = RoundedCornerShape(8.dp))
      .padding(12.dp)
      .testTag(testTag),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.Top
  ) {
    Icon(
      imageVector = Icons.Outlined.Warning,
      contentDescription = "Warning",
      tint = HavenColors.Primary,
      modifier = Modifier
        .size(20.dp)
        .padding(top = 1.dp)
    )
    Text(
      text = message,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 14.sp,
      lineHeight = 20.sp,
      color = HavenColors.Primary,
      modifier = Modifier.weight(1f)
    )
  }
}
