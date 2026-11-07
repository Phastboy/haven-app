package com.example

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.OfferDetail
import com.example.ui.screens.NativeOfferDetailScreen
import com.example.ui.screens.requestedSavedState
import com.example.ui.theme.HavenTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NativeOfferDetailReviewTest {
  @get:Rule val rule = createComposeRule()
  private val paid = OfferDetail("id", "paid-offer", "Paid offer", "An actual description",
    "Other", "45000 NGN", "Ada", "ada", listOf("https://example.test/offer.jpg"), null, false)

  @Test fun `paid detail keeps formatted price and provider opens real handle`() {
    var opened: String? = null
    rule.setContent { HavenTheme {
      NativeOfferDetailScreen(paid, false, null, true, {}, {}, {}, {}, {}, { opened = it })
    } }
    rule.onNodeWithText("45000 NGN").assertExists()
    rule.onNodeWithText("Free").assertDoesNotExist()
    rule.onNodeWithTag("offer_detail_provider_card").performScrollTo().performClick()
    rule.runOnIdle { assertEquals("ada", opened) }
  }

  @Test fun `free detail keeps backend display`() {
    rule.setContent { HavenTheme {
      NativeOfferDetailScreen(paid.copy(priceDisplay = "Free"), false, null, false, {}, {}, {}, {}, {}, {})
    } }
    rule.onNodeWithText("Free").assertExists()
  }

  private fun checkBack(loading: Boolean) {
    var backs = 0
    var dispatcher: OnBackPressedDispatcher? = null
    rule.mainClock.autoAdvance = false
    rule.setContent { HavenTheme {
      dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
      NativeOfferDetailScreen(null, loading, "Offline", false, { backs++ }, {}, {}, {}, {}, {})
    } }
    rule.onNodeWithTag("offer_detail_back").performClick()
    rule.runOnIdle { assertEquals(1, backs); dispatcher!!.onBackPressed() }
    rule.runOnIdle { assertEquals(2, backs) }
  }

  @Test fun `loading detail supports visible and system back`() = checkBack(true)
  @Test fun `failed detail supports visible and system back`() = checkBack(false)

  @Test fun `outlined save never silently unsaves unknown server state`() {
    assertTrue(requestedSavedState(null))
    assertTrue(requestedSavedState(false))
    assertFalse(requestedSavedState(true))
  }
}
