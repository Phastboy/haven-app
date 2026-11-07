package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HavenColors

/** Shared filter/proximity/create geometry, clipping and interaction behavior. */
@Composable
fun FeedActionButton(
  icon: ImageVector,
  description: String,
  tag: String,
  enabled: Boolean = true,
  emphasized: Boolean = false,
  badge: Boolean = false,
  onClick: () -> Unit
) {
  Box(Modifier.size(48.dp).clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    .testTag(tag), contentAlignment = Alignment.Center) {
    Box(Modifier.size(40.dp).clip(CircleShape)
      .background(if (emphasized) HavenColors.Primary else HavenColors.SurfaceContainer)
      .testTag("${tag}_circle"), contentAlignment = Alignment.Center) {
      Icon(icon, description, Modifier.size(18.dp),
        tint = if (emphasized) HavenColors.TextOnDark else HavenColors.TextPrimary)
      if (badge) Box(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp)
        .size(7.dp).clip(CircleShape).background(HavenColors.Primary))
    }
  }
}
