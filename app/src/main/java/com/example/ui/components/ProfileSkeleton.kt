package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Placeholder for the remote profile identity (avatar, name and biography)
 * while no cached profile exists.
 *
 * It mirrors the real profile header geometry so a true cache miss reserves the
 * eventual layout instead of collapsing to a page spinner. It is deliberately
 * noninteractive and announced once as a loading state. Never use it to blank a
 * retained profile, the confirmed empty state or the error/retry action.
 */
@Composable
fun ProfileIdentitySkeleton(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .semantics { contentDescription = "Loading profile" }
      .testTag("profile_identity_skeleton"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(Modifier.size(96.dp).shimmerBackground(CircleShape))
    Spacer(Modifier.height(16.dp))
    Box(Modifier.size(width = 176.dp, height = 24.dp).shimmerBackground(RoundedCornerShape(6.dp)))
    Spacer(Modifier.height(12.dp))
    Box(Modifier.fillMaxWidth(0.82f).height(14.dp).shimmerBackground(RoundedCornerShape(4.dp)))
    Spacer(Modifier.height(8.dp))
    Box(Modifier.fillMaxWidth(0.6f).height(14.dp).shimmerBackground(RoundedCornerShape(4.dp)))
  }
}

/**
 * Small shimmer bar reserving space for a remote collection count. Decorative
 * only: it carries no semantics and is never tappable on its own.
 */
@Composable
fun ProfileCountSkeleton(modifier: Modifier = Modifier) {
  Box(modifier.size(width = 44.dp, height = 12.dp).shimmerBackground(RoundedCornerShape(4.dp)))
}
