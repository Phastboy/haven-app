package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.ui.components.HavenBottomBarContainer
import com.example.ui.components.HavenTopBarContainer
import com.example.ui.components.havenBottomSafePadding
import com.example.ui.components.havenKeyboardAwareBottomPadding
import com.example.ui.components.havenTopSafePadding
import com.example.ui.model.SampleOffers
import com.example.ui.model.SignInViewModel
import com.example.ui.screens.OfferDetailScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.HavenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class WindowInsetsRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `haven top and bottom bar containers render correctly with safe area infrastructure`() {
    composeTestRule.setContent {
      HavenTheme {
        Box(modifier = Modifier.fillMaxSize()) {
          HavenTopBarContainer(modifier = Modifier.testTag("top_container")) {
            Text("Top Bar Content", modifier = Modifier.testTag("top_content"))
          }
          HavenBottomBarContainer(modifier = Modifier.testTag("bottom_container"), liftWithKeyboard = true) {
            Text("Bottom Bar Content", modifier = Modifier.testTag("bottom_content"))
          }
        }
      }
    }

    composeTestRule.onNodeWithTag("top_container").assertExists()
    composeTestRule.onNodeWithTag("top_content").assertIsDisplayed()
    composeTestRule.onNodeWithTag("bottom_container").assertExists()
    composeTestRule.onNodeWithTag("bottom_content").assertIsDisplayed()
  }

  @Test
  fun `welcome screen renders with wordmark and buttons respecting safe areas`() {
    composeTestRule.setContent {
      HavenTheme {
        WelcomeScreen(
          onNavigateToSignIn = {},
          onNavigateToOffers = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("welcome_screen").assertExists()
    composeTestRule.onNodeWithTag("haven_wordmark").assertExists()
    composeTestRule.onNodeWithTag("browse_offers_button").assertExists()
    composeTestRule.onNodeWithTag("sign_in_nav_button").assertExists()
  }

  @Test
  fun `sign in screen renders top bar and keyboard-aware form`() {
    val viewModel = SignInViewModel(testDispatcher)

    composeTestRule.setContent {
      HavenTheme {
        SignInScreen(
          viewModel = viewModel,
          onNavigateBack = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("sign_in_screen").assertExists()
    composeTestRule.onNodeWithTag("back_button").assertExists()
    composeTestRule.onNodeWithTag("sign_in_title").assertExists()
    composeTestRule.onNodeWithTag("google_sign_in_button").assertExists()
    composeTestRule.onNodeWithTag("email_sign_in_button").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("email_sign_in_note").assertExists()
  }

  @Test
  fun `offer detail screen renders and gates guest message action`() {
    val offer = SampleOffers.defaultOffers.first()
    var messageRequests = 0

    composeTestRule.setContent {
      HavenTheme {
        OfferDetailScreen(
          offer = offer,
          isSaved = false,
          isAuthenticated = false,
          onBack = {},
          onToggleSave = {},
          onMessageProvider = { messageRequests++ },
          onSignInRequested = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("offer_detail_screen").assertExists()
    composeTestRule.onNodeWithTag("offer_detail_back").assertExists()
    composeTestRule.onNodeWithTag("offer_detail_save").assertExists()
    composeTestRule.onNodeWithTag("offer_detail_provider_card").assertExists()
    composeTestRule.onNodeWithTag("offer_detail_message_button").assertIsDisplayed()

    composeTestRule.onNodeWithTag("offer_detail_message_button").performClick()
    composeTestRule.onNodeWithTag("offer_detail_auth_dialog").assertExists()
    assertEquals(0, messageRequests)
  }
}
