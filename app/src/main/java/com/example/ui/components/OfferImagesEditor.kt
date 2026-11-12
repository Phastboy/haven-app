package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.example.data.EditorImage
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily

/**
 * Stateless photo editor shared by the create and edit offer screens.
 *
 * It owns presentation only: gallery and camera sources are supplied by the
 * caller, and every confirmed image enters the existing ordered image list.
 */
@Composable
fun OfferImagesEditor(
  images: List<EditorImage>,
  enabled: Boolean,
  onAddImages: () -> Unit,
  onTakePhoto: () -> Unit,
  onMoveImage: (index: Int, delta: Int) -> Unit,
  onRemoveImage: (index: Int) -> Unit,
  cameraPending: Boolean = false,
) {
  val canAdd = enabled && images.size < MAX_IMAGES && !cameraPending
  Column(
    modifier = Modifier.fillMaxWidth().testTag("offer_images_editor"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Images", color = HavenColors.TextPrimary, fontFamily = InterFontFamily, fontSize = 16.sp)
      Text("${images.size}/$MAX_IMAGES", color = HavenColors.TextSecondary, fontSize = 14.sp)
    }
    Text(
      "Add up to five photos. The first photo is the cover. A photo is required to publish.",
      color = HavenColors.TextSecondary,
    )
    images.chunked(2).forEachIndexed { row, rowImages ->
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        rowImages.forEachIndexed { column, image ->
          val index = row * 2 + column
          Column(
            Modifier.weight(1f).testTag("offer_photo_$index"),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            SubcomposeAsyncImage(
              model = image.url,
              contentDescription = "Offer photo ${index + 1}",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(HavenColors.SurfaceContainer),
              loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } },
              error = { Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) { Text("Photo preview unavailable", color = HavenColors.TextSecondary) } },
            )
            TextButton(
              onClick = { onMoveImage(index, -1) },
              enabled = enabled && index > 0,
              modifier = Modifier.testTag("offer_photo_earlier_$index"),
            ) { Text("Move earlier") }
            TextButton(
              onClick = { onMoveImage(index, 1) },
              enabled = enabled && index < images.lastIndex,
              modifier = Modifier.testTag("offer_photo_later_$index"),
            ) { Text("Move later") }
            TextButton(
              onClick = { onRemoveImage(index) },
              enabled = enabled,
              modifier = Modifier.testTag("offer_photo_remove_$index"),
            ) { Text("Remove") }
          }
        }
        if (rowImages.size == 1) Spacer(Modifier.weight(1f))
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      OutlinedButton(
        onClick = onTakePhoto,
        enabled = canAdd,
        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("offer_take_photo"),
      ) { Text(if (cameraPending) "Opening camera…" else "Take photo", fontWeight = FontWeight.Medium) }
      OutlinedButton(
        onClick = onAddImages,
        enabled = canAdd,
        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("offer_add_images"),
      ) { Text("Add images", fontWeight = FontWeight.Medium) }
    }
  }
}

/** Maximum photos accepted by the offer API and the editor view model. */
const val MAX_IMAGES = 5
