package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.data.EditorImage
import com.example.ui.components.OfferImagesEditor
import com.example.ui.components.OfferPhotosSection
import com.example.ui.theme.HavenColors
import com.example.ui.theme.HavenTheme
import com.example.ui.theme.ThemeMode
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Verifies offer photo sources in both light and dark themes: the explicit
 * camera action, the retained gallery action, count limits, ordering/removal
 * callbacks, and the pending camera state that keeps the draft untouched.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OfferPhotoSourcesRobolectricTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private fun images(count: Int): List<EditorImage> =
    List(count) { index -> EditorImage(id = "photo-$index", url = "android.resource://android/drawable/ic_menu_camera") }

  private fun captureRootBitmap(): Bitmap {
    compose.waitForIdle()
    val view = compose.activity.window.decorView
    val width = view.width.takeIf { it > 0 } ?: 1080
    val height = view.height.takeIf { it > 0 } ?: 2340
    if (view.width <= 0 || view.height <= 0) {
      view.measure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
      )
      view.layout(0, 0, width, height)
    }
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
  }

  private fun saveScreenshot(name: String) {
    val file = File("build/reports/screenshots", name)
    file.parentFile?.mkdirs()
    file.outputStream().use { captureRootBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
  }

  @Test fun `camera and gallery actions are explicit and enabled while room remains`() {
    var gallery = 0
    var camera = 0
    compose.setContent {
      HavenTheme(ThemeMode.LIGHT) {
        Box(Modifier.fillMaxSize().background(HavenColors.Canvas).padding(16.dp)) {
          OfferImagesEditor(
            images = images(4),
            enabled = true,
            onAddImages = { gallery++ },
            onTakePhoto = { camera++ },
            onMoveImage = { _, _ -> },
            onRemoveImage = { },
          )
        }
      }
    }
    compose.onNodeWithTag("offer_take_photo").assertIsDisplayed().assertIsEnabled().performClick()
    compose.onNodeWithTag("offer_add_images").assertIsDisplayed().assertIsEnabled().performClick()
    assertEquals(1, camera)
    assertEquals(1, gallery)
    compose.onNodeWithText("4/5").assertIsDisplayed()
  }

  @Test fun `both sources stop at five photos and while saving`() {
    val enabled = mutableStateOf(true)
    val photos = mutableStateOf(5)
    compose.setContent {
      HavenTheme(ThemeMode.LIGHT) {
        OfferImagesEditor(
          images = images(photos.value),
          enabled = enabled.value,
          onAddImages = { },
          onTakePhoto = { },
          onMoveImage = { _, _ -> },
          onRemoveImage = { },
        )
      }
    }
    compose.onNodeWithTag("offer_take_photo").assertIsNotEnabled()
    compose.onNodeWithTag("offer_add_images").assertIsNotEnabled()
    compose.onNodeWithText("5/5").assertIsDisplayed()
    compose.runOnIdle { photos.value = 4 }
    compose.onNodeWithTag("offer_take_photo").assertIsEnabled()
    compose.onNodeWithTag("offer_add_images").assertIsEnabled()
    compose.runOnIdle { enabled.value = false }
    compose.onNodeWithTag("offer_take_photo").assertIsNotEnabled()
    compose.onNodeWithTag("offer_add_images").assertIsNotEnabled()
  }

  @Test fun `busy editing disables every photo action`() {
    compose.setContent {
      HavenTheme(ThemeMode.LIGHT) {
        OfferImagesEditor(
          images = images(2),
          enabled = false,
          onAddImages = { },
          onTakePhoto = { },
          onMoveImage = { _, _ -> },
          onRemoveImage = { },
        )
      }
    }
    for (tag in listOf("offer_take_photo", "offer_add_images", "offer_photo_remove_0", "offer_photo_remove_1")) {
      compose.onNodeWithTag(tag).assertIsNotEnabled()
    }
    compose.onNodeWithTag("offer_photo_earlier_0").assertIsNotEnabled()
  }

  @Test fun `pending capture disables sources and preserves the draft`() {
    compose.setContent {
      HavenTheme(ThemeMode.LIGHT) {
        OfferImagesEditor(
          images = images(1),
          enabled = true,
          cameraPending = true,
          onAddImages = { },
          onTakePhoto = { },
          onMoveImage = { _, _ -> },
          onRemoveImage = { },
        )
      }
    }
    compose.onNodeWithText("Opening camera…").assertIsDisplayed()
    compose.onNodeWithTag("offer_take_photo").assertIsNotEnabled()
    compose.onNodeWithTag("offer_add_images").assertIsNotEnabled()
    compose.onNodeWithText("1/5").assertIsDisplayed()
  }

  @Test fun `confirmed photos keep the existing order move and remove callbacks`() {
    val moves = mutableListOf<Pair<Int, Int>>()
    val removes = mutableListOf<Int>()
    compose.setContent {
      HavenTheme(ThemeMode.LIGHT) {
        OfferImagesEditor(
          images = images(3),
          enabled = true,
          onAddImages = { },
          onTakePhoto = { },
          onMoveImage = { index, delta -> moves += index to delta },
          onRemoveImage = { index -> removes += index },
        )
      }
    }
    compose.onNodeWithTag("offer_photo_earlier_0").assertIsNotEnabled()
    compose.onNodeWithTag("offer_photo_later_0").performClick()
    compose.onNodeWithTag("offer_photo_earlier_2").performClick()
    compose.onNodeWithTag("offer_photo_later_2").assertIsNotEnabled()
    compose.onNodeWithTag("offer_photo_remove_1").performClick()
    assertEquals(listOf(0 to 1, 2 to -1), moves)
    assertEquals(listOf(1), removes)
  }

  @Test fun `section wires both sources into the shared editor`() {
    compose.setContent {
      HavenTheme(ThemeMode.LIGHT) {
        OfferPhotosSection(
          images = emptyList(),
          busy = false,
          onImagesAdded = { },
          onMoveImage = { _, _ -> },
          onRemoveImage = { },
          onError = { },
        )
      }
    }
    compose.onNodeWithTag("offer_images_editor").assertIsDisplayed()
    compose.onNodeWithTag("offer_take_photo").assertIsDisplayed().assertIsEnabled()
    compose.onNodeWithTag("offer_add_images").assertIsDisplayed().assertIsEnabled()
    compose.onNodeWithText("0/5").assertIsDisplayed()
  }

  @Test fun `photo editor renders in light and dark themes`() {
    val mode = mutableStateOf(ThemeMode.LIGHT)
    compose.setContent {
      HavenTheme(mode.value) {
        Box(Modifier.fillMaxSize().background(HavenColors.Canvas).padding(16.dp)) {
          OfferImagesEditor(
            images = images(2),
            enabled = true,
            onAddImages = { },
            onTakePhoto = { },
            onMoveImage = { _, _ -> },
            onRemoveImage = { },
          )
        }
      }
    }
    saveScreenshot("offer-photo-sources-light.png")
    compose.runOnIdle { mode.value = ThemeMode.DARK }
    saveScreenshot("offer-photo-sources-dark.png")
    assertTrue(File("build/reports/screenshots/offer-photo-sources-light.png").exists())
    assertTrue(File("build/reports/screenshots/offer-photo-sources-dark.png").exists())
  }
}
