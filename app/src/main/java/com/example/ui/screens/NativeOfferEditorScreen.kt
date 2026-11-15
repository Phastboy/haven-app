package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HavenTopBarContainer
import com.example.ui.components.OfferLocationSection
import com.example.ui.components.OfferPhotosSection
import com.example.ui.components.havenBottomSafePadding
import com.example.ui.model.OfferEditorState
import com.example.ui.model.OfferEditorViewModel
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

@Composable
fun NativeOfferEditorScreen(model: OfferEditorViewModel, editing: Boolean, onBack: () -> Unit, onSaved: (String?) -> Unit) {
  val state by model.state.collectAsState()
  var confirmLeave by remember { mutableStateOf(false) }
  var locationPending by remember { mutableStateOf(false) }
  var photoPending by remember { mutableStateOf(false) }
  var cancelLocation by remember { mutableStateOf(0) }
  var cancelPhotos by remember { mutableStateOf(0) }
  val context = LocalContext.current
  val leave = {
    if (!state.busy) {
      if (state.dirty) confirmLeave = true else onBack()
    }
  }
  BackHandler(onBack = leave)
  LaunchedEffect(state.saved) {
    if (state.saved) {
      android.widget.Toast.makeText(context, state.savedMessage ?: "Offer saved", android.widget.Toast.LENGTH_LONG).show()
      onSaved(state.savedSlug)
    }
  }
  Column(Modifier.fillMaxSize().background(HavenColors.Canvas).imePadding().testTag("native_offer_editor")) {
    HavenTopBarContainer {
      Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = leave, enabled = !state.busy) { Icon(Icons.Default.Close, "Close", tint = HavenColors.TextPrimary) }
        Text(if (editing) "Edit offer" else "Create offer", Modifier.weight(1f), style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp), color = HavenColors.TextPrimary)
      }
    }
    if (state.loading || state.options == null) {
      Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (state.loading) CircularProgressIndicator(color = HavenColors.Primary)
        Text(if (state.loading) "Loading offer form…" else "The offer form is unavailable. Use Retry below.", color = HavenColors.TextSecondary)
      }
    } else {
      Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          OfferPhotosSection(
            images = state.images,
            captureKey = model.captureKey,
            busy = state.busy,
            onImagesAdded = model::addImages,
            onMoveImage = model::moveImage,
            onRemoveImage = { index -> model.change { it.copy(images = it.images.filterIndexed { position, _ -> position != index }) } },
            onError = model::showError,
            onPendingChanged = { photoPending = it },
            cancelRequest = cancelPhotos,
          )
        }
        EditorField("Title", state.title, "Name what you are offering.", !state.busy, "${state.title.length}/50") { value -> model.change { it.copy(title = value) } }
        EditorChoice("Category", state.options!!.categories, state.category, !state.busy) { value -> model.change { it.copy(category = value) } }
        EditorField("Description", state.description, "Include useful details and what is included.", !state.busy, "${state.description.length}/2000", multiline = true) { value -> model.change { it.copy(description = value) } }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          EditorField("Price (minor units)", state.price, "For NGN, enter kobo: ₦1,500 = 150000. Enter 0 for free.", !state.busy, numeric = true) { value -> model.change { it.copy(price = value) } }
          EditorChoice("Currency", state.options!!.currencies.map { it to it }, state.currency, !state.busy) { value -> model.change { it.copy(currency = value) } }
        }
        OfferLocationSection(
          editorKey = model, state = state,
          onSelect = { place -> model.change { it.copy(place = place) } },
          onSearch = model::search, sign = model::capturedPlace,
          onPendingChanged = { locationPending = it },
          cancelRequest = cancelLocation,
        )
      }
    }
    Column(Modifier.fillMaxWidth().background(HavenColors.Canvas).havenBottomSafePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      state.error?.let { Text(it, color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("offer_editor_error").semantics { liveRegion = LiveRegionMode.Polite }) }
      state.progress?.let { Text(it, color = HavenColors.TextSecondary,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
      val blocker = OfferEditorViewModel.submissionBlocker(state, locationPending, photoPending)
      blocker?.let { Text(it, color = HavenColors.TextSecondary,
        modifier = Modifier.testTag("offer_editor_blocker").semantics { liveRegion = LiveRegionMode.Polite }) }
      if (locationPending) TextButton(onClick = { cancelLocation++ }, enabled = !state.busy) { Text("Cancel location lookup") }
      if (photoPending) TextButton(onClick = { cancelPhotos++ }, enabled = !state.busy) { Text("Cancel photo preparation") }
      if (state.busy) TextButton(onClick = model::cancelSave) { Text("Stop saving and keep draft") }
      if (!state.loading && state.options == null) TextButton(onClick = model::load) { Text("Retry loading form") }
      Button(onClick = model::save, enabled = !state.loading && state.options != null && !state.busy && !locationPending && !photoPending,
        colors = ButtonDefaults.buttonColors(containerColor = HavenColors.Primary),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("offer_editor_submit")) {
        if (state.busy) {
          CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
          Spacer(Modifier.width(8.dp))
        }
        Text(if (state.busy) "Saving…" else if (state.allowPrivateSave && state.images.isEmpty()) "Save private offer" else if (editing) "Save changes" else "Publish offer")
      }
    }
  }
  if (confirmLeave) AlertDialog(onDismissRequest = { confirmLeave = false }, title = { Text("Leave this offer?") },
    text = { Text("Your unsaved entries will be discarded. Changes saved during an earlier attempt remain on your offer.") },
    confirmButton = { TextButton(onClick = onBack) { Text("Discard entries") } },
    dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Keep editing") } })
}

@Composable
private fun EditorLabel(label: String, counter: String = "") {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, color = HavenColors.TextPrimary, fontFamily = InterFontFamily, fontSize = 16.sp)
    if (counter.isNotEmpty()) Text(counter, color = HavenColors.TextSecondary, fontSize = 14.sp)
  }
}

@Composable
private fun EditorField(label: String, value: String, help: String, enabled: Boolean, counter: String = "", multiline: Boolean = false, numeric: Boolean = false, onChange: (String) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    EditorLabel(label, counter)
    OutlinedTextField(value = value, onValueChange = onChange, enabled = enabled, singleLine = !multiline, minLines = if (multiline) 5 else 1,
      textStyle = TextStyle(fontFamily = InterFontFamily, fontSize = 16.sp, color = HavenColors.TextPrimary),
      keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
      modifier = Modifier.fillMaxWidth(), label = { Text(label) })
    Text(help, color = HavenColors.TextSecondary, fontSize = 13.sp)
  }
}

@Composable
private fun EditorChoice(label: String, choices: List<Pair<String, String>>, selected: String, enabled: Boolean, onSelect: (String) -> Unit) {
  var expanded by remember { mutableStateOf(false) }
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    EditorLabel(label)
    Box {
      OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        Text(choices.firstOrNull { it.first == selected }?.second ?: "Choose a ${label.lowercase()}", modifier = Modifier.fillMaxWidth())
      }
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        choices.forEach { (value, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(value); expanded = false }) }
      }
    }
  }
}
