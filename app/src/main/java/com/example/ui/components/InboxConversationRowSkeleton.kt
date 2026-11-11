package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Row-shaped placeholder for the messages inbox's missing initial history.
 *
 * Mirrors the avatar circle, name, time and preview-line layout of the real
 * conversation row so the loading state reads as inbox content. It is
 * deliberately noninteractive: it never offers tap affordances and must only
 * be used while no cached row exists to display, never to blank out retained
 * conversations, the confirmed empty state or the pagination action.
 */
@Composable
fun InboxConversationRowSkeleton(
  nameWidth: Dp,
  previewWidth: Dp,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(80.dp)
      .padding(horizontal = 16.dp)
      .testTag("conversation_row_skeleton"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(modifier = Modifier.size(48.dp).shimmerBackground(CircleShape))
    Spacer(modifier = Modifier.size(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .width(nameWidth)
            .height(14.dp)
            .shimmerBackground(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.size(8.dp))
        Box(
          modifier = Modifier
            .width(36.dp)
            .height(11.dp)
            .shimmerBackground(RoundedCornerShape(4.dp))
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Box(
        modifier = Modifier
          .width(previewWidth)
          .height(12.dp)
          .shimmerBackground(RoundedCornerShape(4.dp))
      )
    }
    Spacer(modifier = Modifier.size(20.dp))
  }
}
