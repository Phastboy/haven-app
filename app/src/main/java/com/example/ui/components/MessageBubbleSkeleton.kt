package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Bubble-shaped placeholder for a conversation thread's missing initial history.
 *
 * Mirrors the alignment, corner radii, maximum width and timestamp row of the
 * rendered message bubble so the loading state reads as chat content. It is
 * deliberately noninteractive: it never offers tap affordances and must only be
 * used while initial content is missing, never to blank out retained messages
 * or the older-messages pagination control.
 */
@Composable
fun MessageBubbleSkeleton(
  fromMe: Boolean,
  bubbleWidth: Dp,
  bubbleHeight: Dp,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("message_bubble_skeleton"),
    horizontalAlignment = if (fromMe) Alignment.End else Alignment.Start
  ) {
    Box(
      modifier = Modifier
        .widthIn(max = 295.dp)
        .width(bubbleWidth)
        .height(bubbleHeight)
        .shimmerBackground(
          RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (fromMe) 16.dp else 4.dp,
            bottomEnd = if (fromMe) 4.dp else 16.dp
          )
        )
    )
    Spacer(modifier = Modifier.height(3.dp))
    Box(
      modifier = Modifier
        .width(56.dp)
        .height(11.dp)
        .shimmerBackground(RoundedCornerShape(4.dp))
    )
  }
}
