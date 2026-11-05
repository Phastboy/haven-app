package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.OfferPlace
import com.example.ui.model.OfferEditorState
import com.example.ui.model.OfferEditorViewModel
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

@Composable
fun NativeOfferEditorScreen(model: OfferEditorViewModel, editing: Boolean, onBack: () -> Unit, onSaved: () -> Unit) {
  val state by model.state.collectAsState()
  val context = LocalContext.current
  var confirmLeave by remember { mutableStateOf(false) }
  var placeQuery by rememberSaveable { mutableStateOf("") }
  val leave = {
    if (!state.busy) {
      if (state.dirty) confirmLeave = true else onBack()
    }
  }
  BackHandler(onBack = leave)
  LaunchedEffect(state.saved) { if (state.saved) onSaved() }
  val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
    val retained = uris.filter { uri ->
      try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        true
      } catch (_: SecurityException) { false }
    }
    model.addImages(retained.map { it.toString() })
    if (retained.size != uris.size) model.showError("Some photos could not be opened. Choose them from another source.")
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
        Text(state.error ?: "Loading offer form…", color = HavenColors.TextSecondary)
        if (!state.loading) Button(onClick = model::load) { Text("Retry") }
      }
    } else {
      Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          EditorLabel("Images", "${state.images.size}/5")
          Text("Add up to five photos. The first photo is the cover. A photo is required to publish.", color = HavenColors.TextSecondary)
          state.images.chunked(2).forEachIndexed { row, images ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              images.forEachIndexed { column, image ->
                val index = row * 2 + column
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  SubcomposeAsyncImage(model = image.url, contentDescription = "Offer photo ${index + 1}", contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(HavenColors.SurfaceContainer),
                    loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } },
                    error = { Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) { Text("Photo preview unavailable", color = HavenColors.TextSecondary) } })
                  TextButton(onClick = { model.moveImage(index, -1) }, enabled = !state.busy && index > 0) { Text("Move earlier") }
                  TextButton(onClick = { model.moveImage(index, 1) }, enabled = !state.busy && index < state.images.lastIndex) { Text("Move later") }
                  TextButton(onClick = { model.change { it.copy(images = it.images.filterIndexed { position, _ -> position != index }) } }, enabled = !state.busy) { Text("Remove") }
                }
              }
              if (images.size == 1) Spacer(Modifier.weight(1f))
            }
          }
          OutlinedButton(onClick = { picker.launch(arrayOf("image/jpeg", "image/png", "image/webp")) }, enabled = !state.busy && state.images.size < 5) { Text("Add images") }
        }
        EditorField("Title", state.title, "Name what you are offering.", !state.busy, "${state.title.length}/50") { value -> model.change { it.copy(title = value) } }
        EditorChoice("Category", state.options!!.categories, state.category, !state.busy) { value -> model.change { it.copy(category = value) } }
        EditorField("Description", state.description, "Include useful details and what is included.", !state.busy, "${state.description.length}/2000", multiline = true) { value -> model.change { it.copy(description = value) } }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          EditorField("Price (minor units)", state.price, "For NGN, enter kobo: ₦1,500 = 150000. Enter 0 for free.", !state.busy, numeric = true) { value -> model.change { it.copy(price = value) } }
          EditorChoice("Currency", state.options!!.currencies.map { it to it }, state.currency, !state.busy) { value -> model.change { it.copy(currency = value) } }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          EditorLabel("Offer location (optional)")
          Text("The selected address will be public on this offer. This does not turn on proximity discovery.", color = HavenColors.TextSecondary)
          state.place?.let { place ->
            Text(place.label, color = HavenColors.TextPrimary)
            TextButton(onClick = { model.change { it.copy(place = null) } }, enabled = !state.busy) { Text("Remove location") }
          }
          OutlinedTextField(value = placeQuery, onValueChange = { placeQuery = it }, label = { Text("Search for a place") }, enabled = !state.busy && !state.searching, modifier = Modifier.fillMaxWidth())
          OutlinedButton(onClick = { model.search(placeQuery) }, enabled = !state.busy && !state.searching && placeQuery.trim().length >= 3) { Text(if (state.searching) "Searching…" else "Search places") }
          state.placeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
          val choices = (state.places + state.options!!.recentPlaces).distinctBy { it.token }
          choices.forEach { place ->
            TextButton(onClick = { model.change { it.copy(place = place) } }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(place.label, modifier = Modifier.fillMaxWidth()) }
          }
          if (choices.isEmpty()) Text("You can save this offer without a location.", color = HavenColors.TextSecondary)
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("offer_editor_error")) }
      }
    }
    Column(Modifier.fillMaxWidth().background(HavenColors.Canvas).havenBottomSafePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      state.progress?.let { Text(it, color = HavenColors.TextSecondary) }
      Button(onClick = model::save, enabled = !state.loading && state.options != null && !state.busy,
        colors = ButtonDefaults.buttonColors(containerColor = HavenColors.Primary),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("offer_editor_submit")) {
        Text(if (state.busy) "Saving…" else if (editing) "Save changes" else "Publish offer")
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
