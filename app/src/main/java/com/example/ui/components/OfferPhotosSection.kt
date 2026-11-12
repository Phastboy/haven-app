package com.example.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
) {
  val context = LocalContext.current
  val camera = rememberOfferCameraCapture(onCaptured = { uri -> onImagesAdded(listOf(uri)) }, onFailure = onError)
  val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
    val retained = uris.filter { uri ->
      try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        true
      } catch (_: SecurityException) { false }
    }
    onImagesAdded(retained.map { it.toString() })
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
    enabled = !busy,
    cameraPending = camera.pending,
    onAddImages = { picker.launch(arrayOf("image/jpeg", "image/png", "image/webp")) },
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
