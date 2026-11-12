package com.example.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.images.OfferImageInputs
import com.example.ui.model.SessionManager
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.platform.LocalContext
import com.example.camera.OfferCameraFiles
import com.example.camera.rememberOfferCameraCapture
import com.example.data.EditorImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Connects gallery selection and camera capture to the shared photo editor.
 *
 * Gallery selection keeps its existing persistable read grants. A confirmed
 * camera capture is just another ordered [EditorImage], so count, preview,
 * reorder, removal and upload continue through the existing view model.
 */
@Composable
fun OfferPhotosSection(
  images: List<EditorImage>,
  busy: Boolean,
  onImagesAdded: (uris: List<String>) -> Unit,
  onMoveImage: (index: Int, delta: Int) -> Unit,
  onRemoveImage: (index: Int) -> Unit,
  onError: (message: String) -> Unit,
  captureKey: String = "offer",
  onPendingChanged: (Boolean) -> Unit = {},
) {
  val context = LocalContext.current
  // Retain inputs, not just a transient Job: validation restarts safely after recreation.
  var validationInputs by rememberSaveable(captureKey) { mutableStateOf<List<String>?>(null) }
  var validationActor by rememberSaveable(captureKey) { mutableStateOf<String?>(null) }
  val checking = validationInputs != null
  val latestImages by rememberUpdatedState(images)
  val latestBusy by rememberUpdatedState(busy)
  val latestAdded by rememberUpdatedState(onImagesAdded)
  val latestError by rememberUpdatedState(onError)
  val addChecked: (List<String>) -> Unit = { uris ->
    if (!checking && !busy && uris.isNotEmpty()) {
      validationActor = SessionManager.session.value.personId
      validationInputs = uris.distinct()
    }
  }
  LaunchedEffect(validationInputs, captureKey) {
    val inputs = validationInputs?.toList() ?: return@LaunchedEffect
    val actor = validationActor
    try {
      withContext(Dispatchers.IO) { inputs.forEach { OfferImageInputs.validate(context.contentResolver, it) } }
      // A restored callback may already be present in the persisted draft.
      val additions = inputs.filterNot { uri -> latestImages.any { it.url == uri } }
      if (SessionManager.session.value.personId == actor && !latestBusy && latestImages.size + additions.size <= 5) latestAdded(additions)
      else {
        additions.forEach { OfferCameraFiles.deleteCapture(context, it) }
        if (SessionManager.session.value.personId == actor && !latestBusy && latestImages.size + additions.size > 5)
          latestError("Choose at most five photos in total.")
      }
    } catch (cancelled: CancellationException) {
      // Keep the saved inputs for the replacement composition; do not consume the result.
      throw cancelled
    } catch (_: java.io.IOException) {
      inputs.forEach { OfferCameraFiles.deleteCapture(context, it) }
      if (SessionManager.session.value.personId == actor) latestError("Choose JPEG, PNG or WebP photos up to 5 MB. Your existing photos are unchanged.")
    } catch (_: SecurityException) {
      if (SessionManager.session.value.personId == actor) latestError("Photo access was lost. Choose it again.")
    }
    validationInputs = null
  }
  val camera = rememberOfferCameraCapture(onCaptured = { uri -> addChecked(listOf(uri)) }, captureKey = captureKey, onFailure = onError)
  SideEffect { onPendingChanged(camera.pending || checking) }
  var pickerActor by rememberSaveable { mutableStateOf<String?>(null) }
  var pickerKey by rememberSaveable { mutableStateOf<String?>(null) }
  val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
    if (pickerKey != captureKey || pickerActor != SessionManager.session.value.personId) return@rememberLauncherForActivityResult
    pickerKey = null
    val retained = uris.filter { uri ->
      try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        true
      } catch (_: SecurityException) { false }
    }
    addChecked(retained.map { it.toString() })
    if (retained.size != uris.size) onError("Some photos could not be opened. Choose them from another source.")
  }

  // Remove abandoned captures left by earlier cancel/failure/recreation without
  // touching files still referenced by the retained draft.
  LaunchedEffect(Unit) {
    val keep = images.mapNotNull { OfferCameraFiles.fileName(context, it.url) }.toSet() +
      listOfNotNull(camera.pendingFileName) +
      validationInputs.orEmpty().mapNotNull { OfferCameraFiles.fileName(context, it) }
    withContext(Dispatchers.IO) { OfferCameraFiles.cleanUp(context, keep) }
  }

  OfferImagesEditor(
    images = images,
    enabled = !busy && !checking,
    cameraPending = camera.pending,
    onAddImages = {
      pickerActor = SessionManager.session.value.personId
      pickerKey = captureKey
      picker.launch(arrayOf("image/jpeg", "image/png", "image/webp"))
    },
    onTakePhoto = camera.launch,
    onMoveImage = onMoveImage,
    onRemoveImage = { index ->
      val removed = images.getOrNull(index)
      if (removed != null && images.count { it.url == removed.url } == 1) {
        OfferCameraFiles.deleteCapture(context, removed.url)
      }
      onRemoveImage(index)
    },
  )
}
