package com.example.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.camera.OfferImageInputs
import com.example.ui.model.SessionManager
import kotlinx.coroutines.launch
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
  val scope = rememberCoroutineScope()
  var checking by remember { mutableStateOf(false) }
  val latestImages by rememberUpdatedState(images)
  val latestBusy by rememberUpdatedState(busy)
  val addChecked: (List<String>) -> Unit = { uris ->
    if (!checking && !busy) {
      val actor = SessionManager.session.value.personId
      checking = true
      scope.launch {
        try {
          withContext(Dispatchers.IO) { uris.forEach { OfferImageInputs.validate(context.contentResolver, it) } }
          if (SessionManager.session.value.personId == actor && !latestBusy && latestImages.size + uris.size <= 5) onImagesAdded(uris)
          else uris.forEach { OfferCameraFiles.deleteCapture(context, it) }
        } catch (_: java.io.IOException) {
          uris.forEach { OfferCameraFiles.deleteCapture(context, it) }
          if (SessionManager.session.value.personId == actor) onError("Choose JPEG, PNG or WebP photos up to 5 MB. Your existing photos are unchanged.")
        } catch (_: SecurityException) {
          if (SessionManager.session.value.personId == actor) onError("Photo access was lost. Choose it again.")
        } finally { checking = false }
      }
    }
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
      listOfNotNull(camera.pendingFileName)
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
