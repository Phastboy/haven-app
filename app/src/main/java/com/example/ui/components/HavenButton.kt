package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Haven Primary Action Button (52dp height, 8dp radius, near-black action)
 */
@Composable
fun HavenPrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isLoading: Boolean = false,
  testTag: String = "primary_button"
) {
  Button(
    elevation = null,
    onClick = onClick,
    enabled = enabled && !isLoading,
    modifier = modifier
      .fillMaxWidth()
      .height(52.dp)
      .testTag(testTag),
    shape = RoundedCornerShape(8.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = HavenColors.Primary,
      contentColor = HavenColors.TextOnDark,
      disabledContainerColor = HavenColors.ButtonDisabled,
      disabledContentColor = HavenColors.ButtonDisabledText
    ),
    contentPadding = PaddingValues(horizontal = 16.dp)
  ) {
    if (isLoading) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        HavenSpinner(
          modifier = Modifier.size(20.dp),
          color = HavenColors.TextOnDark
        )
        Text(
          text = "Sending...",
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 16.sp,
          color = HavenColors.TextOnDark
        )
      }
    } else {
      Text(
        text = text,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
      )
    }
  }
}

/**
 * Haven Secondary Button (52dp height, 8dp radius, flat theme background, no decorative border)
 */
@Composable
fun HavenSecondaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  testTag: String = "secondary_button"
) {
  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier
      .fillMaxWidth()
      .height(52.dp)
      .testTag(testTag),
    shape = RoundedCornerShape(8.dp),
    border = null,
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = HavenColors.Canvas,
      contentColor = HavenColors.Primary
    ),
    contentPadding = PaddingValues(horizontal = 16.dp)
  ) {
    Text(
      text = text,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 16.sp,
      color = HavenColors.Primary
    )
  }
}

/**
 * Haven Google sign-in button matching the sign-in prototype.
 */
@Composable
fun HavenGoogleButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  testTag: String = "google_sign_in_button"
) {
  val containerColor = if (enabled) HavenColors.Canvas else HavenColors.SurfaceDisabled
  val contentColor = if (enabled) HavenColors.Primary else HavenColors.TextMuted

  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier
      .fillMaxWidth()
      .height(52.dp)
      .testTag(testTag),
    shape = RoundedCornerShape(8.dp),
    border = null,
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = containerColor,
      contentColor = contentColor,
      disabledContainerColor = containerColor,
      disabledContentColor = contentColor
    ),
    contentPadding = PaddingValues(horizontal = 16.dp)
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        painter = painterResource(id = R.drawable.ic_google_monochrome),
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = Color.Unspecified
      )
      Text(
        text = "Continue with Google",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        color = contentColor
      )
    }
  }
}

/**
 * Smooth spinning circular progress spinner matching monastic style
 */
@Composable
fun HavenSpinner(
  modifier: Modifier = Modifier,
  color: Color = HavenColors.TextPrimary
) {
  val transition = rememberInfiniteTransition(label = "spinner_transition")
  val rotation by transition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 800, easing = LinearEasing)
    ),
    label = "spinner_rotation"
  )

  Box(
    modifier = modifier.rotate(rotation),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.size(20.dp)) {
      val strokeWidth = 2.5.dp.toPx()
      // Background faint ring
      drawCircle(
        color = color.copy(alpha = 0.25f),
        radius = (size.minDimension - strokeWidth) / 2,
        style = Stroke(width = strokeWidth)
      )
      // Active spinning arc
      drawArc(
        color = color.copy(alpha = 0.85f),
        startAngle = 0f,
        sweepAngle = 100f,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
      )
    }
  }
}
