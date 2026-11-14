package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.OfferPlace
import com.example.ui.model.OfferEditorState
import com.example.ui.theme.HavenColors

/** Stateless Create/Edit location controls; capture, manual selection and removal share one draft. */
@Composable
fun OfferLocationEditor(
  state: OfferEditorState,
  query: String,
  onQueryChange: (String) -> Unit,
  captureState: com.example.location.OfferLocationCaptureState,
  onCapture: () -> Unit,
  onCancelCapture: () -> Unit,
  onSearch: () -> Unit,
  onSelect: (OfferPlace?) -> Unit,
) {
  val context = LocalContext.current
  Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("offer_location_editor")) {
    Text("Offer location (optional)", color = HavenColors.TextPrimary)
    Text("Your chosen place is shown with your offer. You can change or remove it before publishing, or edit it later. This does not change discovery proximity.", color = HavenColors.TextSecondary)
    state.place?.let { place ->
      Text(place.label, color = HavenColors.TextPrimary)
      place.attribution?.let { Text(it, color = HavenColors.TextSecondary) }
      if (place.sourceUrl == "https://www.geoapify.com/") {
        TextButton(onClick = {
          try { context.startActivity(Intent(Intent.ACTION_VIEW, "https://www.geoapify.com/".toUri())) }
          catch (_: android.content.ActivityNotFoundException) { android.widget.Toast.makeText(context, "No browser is available to open this link.", android.widget.Toast.LENGTH_SHORT).show() }
        }) { Text("Powered by Geoapify") }
      }
      TextButton(onClick = { onSelect(null) }, enabled = !state.busy) { Text("Remove location") }
    }
    OutlinedButton(onClick = onCapture, enabled = !state.busy && !captureState.pending,
      modifier = Modifier.testTag("offer_capture_location")) {
      Text(if (captureState.pending) captureState.status ?: "Finding your location…" else if (state.place != null) "Update current location" else "Use my current location")
    }
    if (captureState.pending) {
      Text("Finding a readable place. You can continue without waiting.", color = HavenColors.TextSecondary)
      TextButton(onClick = onCancelCapture, enabled = !state.busy) { Text("Cancel location lookup") }
    }
    if (captureState.pending || captureState.error != null) {
      TextButton(onClick = { onCancelCapture(); onSelect(null) }, enabled = !state.busy) { Text("Continue without location") }
    }
    captureState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    OutlinedTextField(value = query, onValueChange = onQueryChange, label = { Text("Search for a place") },
      enabled = !state.busy && !state.searching, modifier = Modifier.fillMaxWidth())
    OutlinedButton(onClick = onSearch, enabled = !state.busy && !state.searching && query.trim().length >= 3) {
      Text(if (state.searching) "Searching…" else "Search places")
    }
    state.placeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    val choices = (state.places + state.options?.recentPlaces.orEmpty()).distinctBy { it.token }
    choices.forEach { place ->
      TextButton(onClick = { onSelect(place) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
        Text(place.label, modifier = Modifier.fillMaxWidth())
      }
    }
    if (choices.isEmpty()) Text("You can save this offer without a location.", color = HavenColors.TextSecondary)
  }
}
