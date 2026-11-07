package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.testTag
import com.example.ui.components.FeedHeaderLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.data.FeedOffer
import com.example.ui.components.FeedActionButton
import com.example.ui.components.FeedOfferCard
import com.example.ui.model.StartupState
import com.example.ui.screens.StartupGate
import com.example.ui.theme.HavenColors
import com.example.ui.theme.HavenTheme
import com.example.ui.theme.ThemeMode
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MinimalFeedRobolectricTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
  private val offer = FeedOffer("compact", "chair", "Ergonomic office chair with a very long title",
    "Ade Office Supplies with a long provider name", "NGN 1,250,000", null, 2400.0)

  private fun assertOneLine(tag: String) {
    val layouts = mutableListOf<TextLayoutResult>()
    compose.onNodeWithTag(tag, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
      it(layouts)
    }
    assertEquals("$tag must occupy exactly one row", 1, layouts.single().lineCount)
  }

  private fun screenshot(name: String): Bitmap {
    compose.waitForIdle()
    val view = compose.activity.window.decorView
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    compose.runOnUiThread { view.draw(Canvas(bitmap)) }
    // Robolectric's manually drawn view starts Coil's crossfade on this first draw.
    ShadowSystemClock.advanceBy(Duration.ofMillis(500))
    compose.mainClock.advanceTimeBy(32)
    compose.runOnUiThread { view.draw(Canvas(bitmap)) }
    val file = File("build/reports/screenshots/$name.png")
    file.parentFile?.mkdirs()
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return bitmap
  }

  @Test fun `card keeps three single-line rows at narrow widths and enlarged text in both themes`() {
    val mode = mutableStateOf(ThemeMode.LIGHT)
    val width = mutableStateOf(158)
    val scale = mutableStateOf(1f)
    val currentOffer = mutableStateOf(offer)
    compose.setContent {
      HavenTheme(mode.value) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, scale.value)) {
          Column(Modifier.width((width.value + 32).dp).background(HavenColors.Canvas).padding(16.dp)) {
            FeedOfferCard(currentOffer.value, showDistance = true, onOpen = {})
          }
        }
      }
    }
    for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
      for (cardWidth in listOf(158, 173)) {
        for (fontScale in listOf(1f, 2f)) {
          compose.runOnIdle { mode.value = theme; width.value = cardWidth; scale.value = fontScale }
          for (part in listOf("title", "price", "maker")) assertOneLine("offer_${part}_compact")
          val price = compose.onNodeWithTag("offer_price_compact", true).fetchSemanticsNode().boundsInRoot
          val provider = compose.onNodeWithTag("offer_maker_compact", true).fetchSemanticsNode().boundsInRoot
          assertTrue("provider stays below the price row", provider.top >= price.bottom)
          val distanceNode = compose.onAllNodesWithTag("offer_distance_compact", true).fetchSemanticsNodes().singleOrNull()
          if (distanceNode != null && distanceNode.boundsInRoot.width > 0f) {
            val distance = distanceNode.boundsInRoot
            assertEquals("distance cannot move below price", price.top, distance.top, 0.1f)
            assertTrue("distance cannot overlap price", distance.width == 0f || distance.left >= price.right)
          }
          if (fontScale == 1f) compose.onNodeWithTag("offer_distance_compact", true).assertIsDisplayed()
          screenshot("minimal-card-${theme.name.lowercase()}-$cardWidth-$fontScale")
        }
      }
    }
    compose.runOnIdle { currentOffer.value = offer.copy(priceDisplay = "NGN 999,999,999,999,999,999,999") }
    assertOneLine("offer_price_compact")
  }

  @Test fun `loaded photograph renders in both themes with the same card geometry`() {
    val mode = mutableStateOf(ThemeMode.LIGHT)
    val saved = mutableStateOf(false)
    val photoOffer = offer.copy(
      title = "Handcrafted reading lamp", makerName = "Ade Home Supplies", priceDisplay = "NGN 85,000",
      coverUrl = "android.resource://${compose.activity.packageName}/${R.drawable.lamp_craft}"
    )
    compose.setContent { HavenTheme(mode.value) {
      Column(Modifier.width(205.dp).background(HavenColors.Canvas).padding(16.dp)) {
        FeedOfferCard(photoOffer, showDistance = true, saved = saved.value, onOpen = {})
      }
    } }
    compose.waitUntil(10_000) {
      compose.onAllNodesWithTag("offer_image_compact", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
    for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
      compose.runOnIdle { mode.value = theme; saved.value = theme == ThemeMode.DARK }
      compose.onNodeWithTag("offer_image_compact", true).assertIsDisplayed()
      // Successful decoding precedes the end of Coil's crossfade animation.
      compose.mainClock.advanceTimeBy(500)
      val bitmap = screenshot("minimal-card-photo-${theme.name.lowercase()}")
      val bounds = compose.onNodeWithTag("offer_image_compact", true).fetchSemanticsNode().boundsInWindow
      val colors = listOf(0.25f, 0.5f, 0.75f).flatMap { x ->
        listOf(0.25f, 0.5f, 0.75f).map { y ->
          bitmap.getPixel((bounds.left + bounds.width * x).toInt(), (bounds.top + bounds.height * y).toInt())
        }
      }
      assertTrue("Loaded photo must be visible, not a flat placeholder", colors.distinct().size > 1)
    }
  }

  @Test fun `price keeps its full width before distance is truncated`() {
    val active = mutableStateOf(false)
    compose.setContent { HavenTheme {
      Column(Modifier.width(158.dp)) {
        FeedOfferCard(offer.copy(priceDisplay = "NGN 1,250,000,000"), showDistance = active.value, onOpen = {})
      }
    } }
    val price = compose.onNodeWithTag("offer_price_compact", true)
    val before = price.fetchSemanticsNode().boundsInRoot.width
    compose.runOnIdle { active.value = true }
    val layouts = mutableListOf<TextLayoutResult>()
    price.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
    assertFalse("distance must not truncate a price that fits alone", layouts.single().isLineEllipsized(0))
    assertEquals(before, price.fetchSemanticsNode().boundsInRoot.width, 0.1f)
  }

  @Test fun `narrow header preserves full search width without shrinking action targets`() {
    val width = mutableStateOf(320)
    compose.setContent { HavenTheme {
      Column(Modifier.width(width.value.dp)) {
        FeedHeaderLayout(search = { modifier -> Box(modifier.height(40.dp).testTag("header_search")) }, actions = {
          FeedActionButton(Icons.Outlined.Tune, "Filters", "filter", onClick = {})
          FeedActionButton(Icons.Outlined.LocationOn, "Proximity", "near", onClick = {})
          FeedActionButton(Icons.Outlined.Add, "Create", "create", onClick = {})
        })
      }
    } }
    compose.onNodeWithTag("header_search").assertWidthIsEqualTo(288.dp)
    val search = compose.onNodeWithTag("header_search").fetchSemanticsNode().boundsInRoot
    val filter = compose.onNodeWithTag("filter").fetchSemanticsNode().boundsInRoot
    assertTrue(search.bottom <= filter.top)
    for (tag in listOf("filter", "near", "create")) compose.onNodeWithTag(tag).assertWidthIsEqualTo(48.dp)
    compose.runOnIdle { width.value = 360 }
    val wideSearch = compose.onNodeWithTag("header_search").fetchSemanticsNode().boundsInRoot
    val wideFilter = compose.onNodeWithTag("filter").fetchSemanticsNode().boundsInRoot
    assertTrue(wideSearch.right <= wideFilter.left)
    assertTrue(wideFilter.top < wideSearch.bottom)
  }

  @Test fun `general discovery hides server distance and saved heart keeps working`() {
    var saves = 0
    compose.setContent { HavenTheme {
      Column(Modifier.width(158.dp)) {
        FeedOfferCard(offer, saved = true, onSave = { saves++ }, onOpen = {})
      }
    } }
    compose.onNodeWithTag("offer_distance_compact", true).assertDoesNotExist()
    compose.onNodeWithContentDescription("Unsave offer").performClick()
    assertEquals(1, saves)
    compose.onNodeWithText(offer.makerName, useUnmergedTree = true).assertExists()
  }

  @Test fun `filter proximity and create have identical circles and separate accessible targets`() {
    var clicks = 0
    compose.setContent { HavenTheme {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FeedActionButton(Icons.Outlined.Tune, "Filters", "filter", onClick = { clicks++ })
        FeedActionButton(Icons.Outlined.LocationOn, "Proximity", "near", emphasized = true, onClick = { clicks++ })
        FeedActionButton(Icons.Outlined.Add, "Create", "create", emphasized = true, onClick = { clicks++ })
      }
    } }
    for (tag in listOf("filter", "near", "create")) {
      compose.onNodeWithTag(tag).assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp).performClick()
      compose.onNodeWithTag("${tag}_circle", true).assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp)
    }
    assertEquals(3, clicks)
    screenshot("feed-controls")
  }

  @Test fun `startup does not compose welcome or feed before restoration and retry is actionable`() {
    val state = mutableStateOf(StartupState.RESTORING)
    var retries = 0
    var destination: Boolean? = null
    compose.setContent { HavenTheme {
      StartupGate(state.value, { retries++ }) { destination = it }
    } }
    compose.onNodeWithTag("startup_screen").assertIsDisplayed()
    assertNull(destination)
    compose.runOnIdle { state.value = StartupState.RETRY }
    compose.onNodeWithTag("startup_retry").performClick()
    assertEquals(1, retries)
    assertNull(destination)
    compose.runOnIdle { state.value = StartupState.SIGNED_OUT }
    compose.waitForIdle()
    assertEquals(false, destination)
    compose.runOnIdle { state.value = StartupState.AUTHENTICATED }
    compose.waitForIdle()
    assertEquals(true, destination)
  }
}
