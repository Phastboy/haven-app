package com.example

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.OfferPlace
import com.example.location.OfferLocationCaptureState
import com.example.ui.components.OfferLocationEditor
import com.example.ui.model.OfferEditorState
import com.example.ui.theme.HavenTheme
import com.example.ui.theme.ThemeMode
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Focused controls/wording regressions only; owner performs visual acceptance. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp")
class OfferLocationEditorRobolectricTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Test fun `readable selected place permits manual replacement and removal without a review step`() {
    val state = mutableStateOf(OfferEditorState(place = OfferPlace("resolved", "Shopping complex, Yaba"),
      places = listOf(OfferPlace("manual", "Another place"))))
    compose.setContent {
      HavenTheme(ThemeMode.LIGHT) {
        OfferLocationEditor(state.value, "town", {}, OfferLocationCaptureState(),
          {}, {}, {}, { place -> state.value = state.value.copy(place = place) })
      }
    }
    compose.onNodeWithText("Shopping complex, Yaba").assertIsDisplayed()
    compose.onNodeWithText("Review captured location").assertDoesNotExist()
    compose.onNodeWithText("Another place").performClick()
    assertEquals("manual", state.value.place?.token)
    compose.onNodeWithText("Remove location").performClick()
    assertNull(state.value.place)
  }

  @Test fun `pending lookup offers cancellation and continue without location`() {
    var cancelled = 0
    var selected: OfferPlace? = OfferPlace("previous", "Previous place")
    compose.setContent {
      HavenTheme(ThemeMode.LIGHT) {
        OfferLocationEditor(OfferEditorState(), "", {}, OfferLocationCaptureState(pending = true, status = "Finding the place…"),
          {}, { cancelled++ }, {}, { selected = it })
      }
    }
    compose.onNodeWithTag("offer_capture_location").assertIsNotEnabled()
    compose.onNodeWithText("Finding the place…").assertIsDisplayed()
    compose.onNodeWithText("Continue without location").performClick()
    assertEquals(1, cancelled)
    assertNull(selected)
  }
}
