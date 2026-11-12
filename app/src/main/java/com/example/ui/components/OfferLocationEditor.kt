package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
  onAcceptCapture: () -> Unit,
  onSearch: () -> Unit,
  onSelect: (OfferPlace?) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("offer_location_editor")) {
    Text("Offer location (optional)", color = HavenColors.TextPrimary)
    Text("The selected place and coordinates are public when you submit this offer. Device access does not change discovery proximity. Search for a less precise place if you prefer.", color = HavenColors.TextSecondary)
    state.place?.let { place ->
      Text(place.label, color = HavenColors.TextPrimary)
      TextButton(onClick = { onSelect(null) }, enabled = !state.busy) { Text("Remove location") }
    }
    OutlinedButton(onClick = onCapture, enabled = !state.busy && !captureState.pending,
      modifier = Modifier.testTag("offer_capture_location")) {
      Text(if (captureState.pending) "Getting location…" else if (state.place != null) "Update current location" else "Use my current location")
    }
    captureState.candidate?.let { candidate ->
      Text("Review captured location", color = HavenColors.TextPrimary)
      Text(candidate.publicLabel(), color = HavenColors.TextPrimary)
      Text("This is a measured point, not a verified address. Review it before adding it to your draft. Nothing is published until you submit.", color = HavenColors.TextSecondary)
      TextButton(onClick = onAcceptCapture, enabled = !state.busy && !captureState.pending,
        modifier = Modifier.testTag("offer_accept_location")) { Text("Use this location in draft") }
    }
    if (captureState.pending || captureState.candidate != null) {
      TextButton(onClick = onCancelCapture, enabled = !state.busy) { Text("Cancel location capture") }
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
