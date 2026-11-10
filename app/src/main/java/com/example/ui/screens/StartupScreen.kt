package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.HavenPrimaryButton
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.model.StartupState
import com.example.ui.theme.HavenColors

/** Hold navigation until credentials are checked; retry keeps the saved session. */
@Composable
fun StartupGate(state: StartupState, onRetry: () -> Unit, content: @Composable (Boolean) -> Unit) {
  when (state) {
    StartupState.AUTHENTICATED -> content(true)
    StartupState.SIGNED_OUT -> content(false)
    else -> Column(
      Modifier.fillMaxSize().background(HavenColors.Canvas).safeDrawingPadding()
        .padding(24.dp).testTag("startup_screen"),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
      if (state == StartupState.RESTORING) {
        Icon(painterResource(R.drawable.haven_launch_logo), contentDescription = "Haven",
          tint = HavenColors.TextPrimary, modifier = Modifier.size(288.dp))
      } else {
        Text("Couldn’t restore your session. Check your connection and try again.", color = HavenColors.TextPrimary)
        HavenPrimaryButton("Try again", onRetry, testTag = "startup_retry")
      }
    }
  }
}
