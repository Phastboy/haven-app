package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

@Composable
fun HavenSpinner(
  modifier: Modifier = Modifier,
  color: Color = HavenColors.TextPrimary,
  size: Dp = 24.dp,
  strokeWidth: Dp = 2.5.dp
) {
  CircularProgressIndicator(
    modifier = modifier.size(size),
    color = color,
    strokeWidth = strokeWidth
  )
}

@Composable
fun HavenPrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isLoading: Boolean = false,
  testTag: String = ""
) {
  Button(
    onClick = onClick,
    modifier = modifier
      .fillMaxWidth()
      .height(52.dp)
      .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it },
    enabled = enabled && !isLoading,
    shape = RoundedCornerShape(26.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = HavenColors.Primary,
      contentColor = HavenColors.TextOnDark,
      disabledContainerColor = HavenColors.ButtonDisabled,
      disabledContentColor = HavenColors.ButtonDisabledText
    ),
    contentPadding = PaddingValues(horizontal = 24.dp)
  ) {
    if (isLoading) {
      HavenSpinner(color = HavenColors.TextOnDark, size = 20.dp, strokeWidth = 2.dp)
    } else {
      Text(
        text = text,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
      )
    }
  }
}

@Composable
fun HavenSecondaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  testTag: String = ""
) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier
      .fillMaxWidth()
      .height(52.dp)
      .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it },
    enabled = enabled,
    shape = RoundedCornerShape(26.dp),
    border = BorderStroke(1.dp, HavenColors.BorderDefault),
    colors = ButtonDefaults.outlinedButtonColors(
      contentColor = HavenColors.TextPrimary,
      disabledContentColor = HavenColors.TextSecondary
    ),
    contentPadding = PaddingValues(horizontal = 24.dp)
  ) {
    Text(
      text = text,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 15.sp
    )
  }
}

@Composable
fun HavenGoogleButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  testTag: String = ""
) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier
      .fillMaxWidth()
      .height(52.dp)
      .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it },
    enabled = enabled,
    shape = RoundedCornerShape(26.dp),
    border = BorderStroke(1.dp, HavenColors.BorderDefault),
    colors = ButtonDefaults.outlinedButtonColors(
      contentColor = HavenColors.TextPrimary
    ),
    contentPadding = PaddingValues(horizontal = 20.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically
    ) {
      Image(
        painter = painterResource(R.drawable.ic_google_monochrome),
        contentDescription = "Google logo",
        modifier = Modifier.size(18.dp)
      )
      Spacer(Modifier.width(12.dp))
      Text(
        text = "Continue with Google",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp
      )
    }
  }
}
