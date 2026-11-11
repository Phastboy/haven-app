package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.data.FeedOffer
import com.example.ui.components.FeedOfferCard
import com.example.ui.components.SaveOfferButton
import com.example.ui.components.SaveOfferButtonDefaults
import com.example.ui.model.SampleOffers
import com.example.ui.screens.OfferDetailScreen
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
 * Verifies the single shared save control (issue #34) end to end:
 * distinct visible sizes over one 48dp touch target, centralized Save/Unsave
 * semantics, pending/disabled presentation, theme-aware glass contrast over a
 * busy cover, and both call sites (feed card + offer detail) using it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SaveOfferButtonRobolectricTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private val offer = FeedOffer("save", "chair", "Visible offer title", "Real person", "100 NGN", null)

  private fun captureRootBitmap(): Bitmap {
    compose.waitForIdle()
    val view = compose.activity.window.decorView
    val width = view.width.takeIf { it > 0 } ?: 1080
    val height = view.height.takeIf { it > 0 } ?: 2340
    if (view.width <= 0 || view.height <= 0) {
      view.measure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
      )
      view.layout(0, 0, width, height)
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bitmap))
    return bitmap
  }

  private fun captureNodeBitmap(tag: String): Bitmap {
    val root = captureRootBitmap()
    val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
    val left = bounds.left.toInt().coerceIn(0, root.width - 1)
    val top = bounds.top.toInt().coerceIn(0, root.height - 1)
    val width = bounds.width.toInt().coerceIn(1, root.width - left)
    val height = bounds.height.toInt().coerceIn(1, root.height - top)
    return Bitmap.createBitmap(root, left, top, width, height)
  }

  private fun saveScreenshot(name: String) {
    val file = File("build/reports/screenshots/$name")
    file.parentFile?.mkdirs()
    file.outputStream().use { captureRootBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
  }

  /** Deterministic mid-gray stripes standing in for a busy, unpredictable cover image. */
  private fun busyStripes(): Modifier = Modifier.drawBehind {
    val stripe = size.height / 6f
    for (i in 0..5) {
      drawRect(
        color = if (i % 2 == 0) Color(0xFF6B7280) else Color(0xFF9CA3AF),
        topLeft = Offset(0f, i * stripe),
        size = Size(size.width, stripe)
      )
    }
  }

  private fun centerLuminance(tag: String): Float {
    val pixels = captureNodeBitmap(tag).asImageBitmap().toPixelMap()
    return pixels[pixels.width / 2, pixels.height / 2].luminance()
  }

  private fun luminanceRange(tag: String): Pair<Float, Float> {
    val pixels = captureNodeBitmap(tag).asImageBitmap().toPixelMap()
    var darkest = 1f
    var brightest = 0f
    for (x in 0 until pixels.width) for (y in 0 until pixels.height) {
      val value = pixels[x, y].luminance()
      darkest = minOf(darkest, value)
      brightest = maxOf(brightest, value)
    }
    return darkest to brightest
  }

  @Test fun `visible sizes stay distinct while every touch target is at least 48dp`() {
    compose.setContent {
      HavenTheme(ThemeMode.LIGHT) {
        Column(Modifier.size(120.dp)) {
          SaveOfferButton(saved = false, onClick = {}, testTag = "card_save")
          SaveOfferButton(
            saved = true, onClick = {}, testTag = "detail_save",
            containerSize = SaveOfferButtonDefaults.DetailContainerSize,
            iconSize = SaveOfferButtonDefaults.DetailIconSize
          )
        }
      }
    }
    for (tag in listOf("card_save", "detail_save")) {
      compose.onNodeWithTag(tag).assertIsDisplayed().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }
    compose.onNodeWithTag("card_save_glass", true).assertWidthIsAtLeast(28.dp)
    compose.onNodeWithTag("detail_save_glass", true).assertWidthIsAtLeast(44.dp)
    assertEquals(28.dp, SaveOfferButtonDefaults.CardContainerSize)
    assertEquals(44.dp, SaveOfferButtonDefaults.DetailContainerSize)
    assertEquals(48.dp, SaveOfferButtonDefaults.MinTouchTarget)
  }

  @Test fun `semantics describe saved unsaved and pending states`() {
    val saved = mutableStateOf(false)
    val saving = mutableStateOf(false)
    compose.setContent {
      HavenTheme(ThemeMode.LIGHT) {
        SaveOfferButton(saved = saved.value, saving = saving.value, onClick = {}, testTag = "state_save")
      }
    }
    compose.onNodeWithContentDescription("Save offer").assertIsDisplayed()
    compose.runOnIdle { saved.value = true }
    compose.onNodeWithContentDescription("Unsave offer").assertIsDisplayed()
    compose.runOnIdle { saving.value = true }
    compose.onNodeWithContentDescription("Saving offer").assertIsDisplayed()
    compose.runOnIdle { saving.value = false }
    compose.onNodeWithContentDescription("Unsave offer").assertIsDisplayed()
  }

  @Test fun `enabled button fires exactly once`() {
    var clicks = 0
    compose.setContent {
      HavenTheme(ThemeMode.LIGHT) {
        SaveOfferButton(saved = false, onClick = { clicks++ }, testTag = "enabled_save")
      }
    }
    compose.onNodeWithTag("enabled_save").assertIsEnabled().performClick()
    assertEquals(1, clicks)
  }

  @Test fun `disabled and pending buttons ignore clicks and are not enabled`() {
    var clicks = 0
    val enabled = mutableStateOf(false)
    val saving = mutableStateOf(false)
    compose.setContent {
      HavenTheme(ThemeMode.LIGHT) {
        SaveOfferButton(
          saved = false, onClick = { clicks++ }, enabled = enabled.value, saving = saving.value,
          testTag = "gated_save"
        )
      }
    }
    compose.onNodeWithTag("gated_save").assertIsNotEnabled().performClick()
    assertEquals(0, clicks)
    compose.runOnIdle { enabled.value = true; saving.value = true }
    compose.onNodeWithContentDescription("Saving offer").assertIsNotEnabled().performClick()
    assertEquals(0, clicks)
    compose.runOnIdle { saving.value = false }
    compose.onNodeWithTag("gated_save").assertIsEnabled().performClick()
    assertEquals(1, clicks)
  }

  @Test fun `glass keeps the heart readable over a busy cover in both themes`() {
    val mode = mutableStateOf(ThemeMode.LIGHT)
    val saved = mutableStateOf(false)
    compose.setContent {
      HavenTheme(mode.value) {
        Box(Modifier.size(96.dp).then(busyStripes()), contentAlignment = Alignment.Center) {
          SaveOfferButton(saved = saved.value, onClick = {}, testTag = "busy_save")
        }
      }
    }
    for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
      compose.runOnIdle { mode.value = theme; saved.value = false }
      val (unsavedDarkest, unsavedBrightest) = luminanceRange("busy_save")
      val unsavedCenter = centerLuminance("busy_save")
      compose.runOnIdle { saved.value = true }
      val savedCenter = centerLuminance("busy_save")
      if (theme == ThemeMode.LIGHT) {
        assertTrue("unsaved glass scrim stays bright", unsavedCenter > 0.5f)
        assertTrue("unsaved outline is visible", unsavedDarkest < 0.2f)
        assertTrue("saved fill is dark", savedCenter < 0.15f)
      } else {
        assertTrue("unsaved glass scrim stays dark", unsavedCenter < 0.2f)
        assertTrue("unsaved outline is visible", unsavedBrightest > 0.5f)
        assertTrue("saved fill is light", savedCenter > 0.5f)
      }
      saveScreenshot("save-button-busy-${theme.name.lowercase()}-unsaved.png")
    }
    compose.runOnIdle { mode.value = ThemeMode.LIGHT; saved.value = true }
    saveScreenshot("save-button-busy-light-saved.png")
    compose.runOnIdle { mode.value = ThemeMode.DARK }
    saveScreenshot("save-button-busy-dark-saved.png")
  }

  /** Regression: the scrim must follow the theme, never an unconditional white card tint. */
  @Test fun `glass scrim follows the theme rather than a fixed white overlay`() {
    val mode = mutableStateOf(ThemeMode.LIGHT)
    val background = mutableStateOf(Color.Black)
    compose.setContent {
      HavenTheme(mode.value) {
        Box(Modifier.size(96.dp).background(background.value), contentAlignment = Alignment.Center) {
          SaveOfferButton(saved = false, onClick = {}, testTag = "theme_save")
        }
      }
    }
    compose.runOnIdle { mode.value = ThemeMode.LIGHT; background.value = Color.Black }
    assertTrue("light scrim stays light over a dark cover", centerLuminance("theme_save") > 0.6f)
    compose.runOnIdle { mode.value = ThemeMode.DARK; background.value = Color.White }
    assertTrue("dark scrim stays dark over a bright cover", centerLuminance("theme_save") < 0.3f)
  }

  @Test fun `feed card renders the shared 48dp control`() {
    var opened = false
    compose.setContent {
      HavenTheme(ThemeMode.LIGHT) {
        Box(Modifier.size(390.dp)) {
          FeedOfferCard(offer, saved = true, onOpen = { opened = true })
        }
      }
    }
    compose.onNodeWithTag("offer_save_save").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    compose.onNodeWithTag("offer_save_save_glass", true).assertWidthIsAtLeast(28.dp)
    compose.onNodeWithContentDescription("Unsave offer").performClick()
    assertTrue("card heart still opens when no save callback is wired", opened)
    saveScreenshot("save-button-card-light.png")
  }

  @Test fun `offer detail renders the shared 48dp control`() {
    val mode = mutableStateOf(ThemeMode.LIGHT)
    compose.setContent {
      HavenTheme(mode.value) {
        OfferDetailScreen(
          offer = SampleOffers.defaultOffers.first().copy(imageUrl = ""),
          isSaved = false,
          isAuthenticated = false,
          onBack = {},
          onToggleSave = {},
          onMessageProvider = {},
          onSignInRequested = {}
        )
      }
    }
    compose.onNodeWithTag("offer_detail_save").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    compose.onNodeWithTag("offer_detail_save_glass", true).assertWidthIsAtLeast(44.dp)
    compose.onNodeWithContentDescription("Save offer").assertIsDisplayed()
    saveScreenshot("save-button-detail-light.png")
    compose.runOnIdle { mode.value = ThemeMode.DARK }
    saveScreenshot("save-button-detail-dark.png")
  }
}
