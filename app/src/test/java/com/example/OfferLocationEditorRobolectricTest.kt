package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.OfferPlace
import com.example.location.CapturedLocation
import com.example.location.OfferLocationCaptureState
import com.example.ui.components.OfferLocationEditor
import com.example.ui.model.OfferEditorState
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
import org.robolectric.annotation.GraphicsMode

/** Checks honest review/public-visibility wording and the retained manual path in both themes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OfferLocationEditorRobolectricTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Test fun `review is explicit and manual replacement removal remain available in both themes`() {
    val mode = mutableStateOf(ThemeMode.LIGHT)
    val state = mutableStateOf(OfferEditorState(place = OfferPlace("old", "Old public place"),
      places = listOf(OfferPlace("manual", "Less precise town"))))
    var accepted = 0
    val point = CapturedLocation(6.5, 3.4, 900f, true)
    compose.setContent {
      HavenTheme(mode.value) {
        Column(Modifier.fillMaxSize().background(HavenColors.Canvas).verticalScroll(rememberScrollState())) {
          OfferLocationEditor(state.value, "town", {}, OfferLocationCaptureState(candidate = point),
            {}, {}, { accepted++ }, {}, { place -> state.value = state.value.copy(place = place) })
        }
      }
    }
    compose.onNodeWithText("Old public place").assertIsDisplayed()
    compose.onNodeWithText(point.publicLabel()).assertIsDisplayed()
    assertEquals(0, accepted)
    compose.onNodeWithTag("offer_accept_location").performScrollTo().performClick()
    assertEquals(1, accepted)
    compose.onNodeWithText("Less precise town").performScrollTo().performClick()
    assertEquals("manual", state.value.place?.token)
    compose.onNodeWithText("Remove location").performScrollTo().performClick()
    assertNull(state.value.place)
    for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
      compose.runOnIdle { mode.value = theme }
      compose.onNodeWithTag("offer_capture_location").performScrollTo()
      compose.waitForIdle()
      val view = compose.activity.window.decorView
      val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
      view.draw(Canvas(bitmap))
      val file = File("build/reports/screenshots", "offer-location-${theme.name.lowercase()}.png")
      file.parentFile?.mkdirs()
      file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
  }

  @Test fun `pending capture blocks repeated capture but offers explicit cancellation`() {
    var cancelled = false
    compose.setContent {
      HavenTheme(ThemeMode.LIGHT) {
        OfferLocationEditor(OfferEditorState(), "", {}, OfferLocationCaptureState(pending = true),
          {}, { cancelled = true }, {}, {}, {})
      }
    }
    compose.onNodeWithTag("offer_capture_location").assertIsNotEnabled()
    compose.onNodeWithText("Cancel location capture").performClick()
    assertTrue(cancelled)
  }
}
