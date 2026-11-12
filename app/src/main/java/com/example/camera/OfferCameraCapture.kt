package com.example.camera

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** State and trigger for the system camera capture contract. */
@Immutable
data class OfferCameraCapture(
  /** True from launch until the system camera returns, including after recreation. */
  val pending: Boolean,
  /** Name of the in-flight capture file, retained across recreation. */
  val pendingFileName: String?,
  val launch: () -> Unit,
)

/**
 * Wraps [ActivityResultContracts.TakePicture] for offer photos.
 *
 * The contract asks the system camera app to write into a private provider URI,
 * so the app declares no [`android.Manifest.permission.CAMERA`] permission and
 * requests no storage permission. Cancellation, launch failure and unusable
 * output delete their temporary file and leave the draft untouched.
 */
@Composable
fun rememberOfferCameraCapture(
  onCaptured: (uri: String) -> Unit,
  onFailure: (message: String) -> Unit,
): OfferCameraCapture {
  val context = LocalContext.current
  // Only the file name is saved. The provider URI is rebuilt on return, so a
  // recreated activity can still consume the capture without a stale grant.
  var pendingName by rememberSaveable { mutableStateOf<String?>(null) }

  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { confirmed ->
    val name = pendingName ?: return@rememberLauncherForActivityResult
    pendingName = null
    if (!confirmed) {
      OfferCameraFiles.cancel(context, name)
      onFailure("Photo was not taken. Your offer draft is unchanged.")
      return@rememberLauncherForActivityResult
    }
    val uri = OfferCameraFiles.consume(context, name)
    if (uri == null) {
      onFailure("The camera did not return a photo. Try again or add one from your gallery.")
    } else {
      onCaptured(uri)
    }
  }

  return OfferCameraCapture(pending = pendingName != null, pendingFileName = pendingName, launch = {
    if (pendingName == null) {
      try {
        val file = OfferCameraFiles.create(context)
        try {
          val target = OfferCameraFiles.uri(context, file)
          pendingName = file.name
          launcher.launch(target)
        } catch (_: Exception) {
          pendingName = null
          OfferCameraFiles.cancel(context, file.name)
          onFailure("The camera is unavailable. Add a photo from your gallery instead.")
        }
      } catch (_: Exception) {
        onFailure("Could not open the camera. Add a photo from your gallery instead.")
      }
    }
  })
}
