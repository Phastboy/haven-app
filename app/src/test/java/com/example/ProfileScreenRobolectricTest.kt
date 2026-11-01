package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.ui.components.OfferCardSkeleton
import com.example.ui.components.OffersGridSkeleton
import com.example.ui.model.SessionManager
import com.example.ui.screens.ProfileSpecState
import com.example.ui.theme.HavenTheme
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProfileScreenRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `session manager handles sign in and display name formatting`() {
    SessionManager.signIn("adaeze.obi@haven.ng")
    val session = SessionManager.session.value

    assertTrue(session.isAuthenticated)
    assertEquals("adaeze.obi@haven.ng", session.email)
    assertEquals("Adaeze Obi", session.displayName)
    assertEquals("A", session.initial)
  }

  @Test
  fun `session manager sign out clears user state`() {
    SessionManager.signIn("adaeze.obi@haven.ng")
    assertTrue(SessionManager.session.value.isAuthenticated)

    SessionManager.signOut()
    val session = SessionManager.session.value
    assertFalse(session.isAuthenticated)
    assertEquals(null, session.email)
  }

  @Test
  fun `profile spec states cover all required visual scenarios`() {
    val labels = ProfileSpecState.values().map { it.label }
    assertTrue(labels.contains("Default"))
    assertTrue(labels.contains("State A: Updated"))
    assertTrue(labels.contains("State B: Sign Out"))
    assertTrue(labels.contains("State C: No Photo/Bio"))
    assertTrue(labels.contains("State D: Stress Test"))
  }

  @Test
  fun `theme manager updates modes between system light and dark`() {
    ThemeManager.setMode(ThemeMode.LIGHT)
    assertEquals(ThemeMode.LIGHT, ThemeManager.themeMode)

    ThemeManager.setMode(ThemeMode.DARK)
    assertEquals(ThemeMode.DARK, ThemeManager.themeMode)

    ThemeManager.setMode(ThemeMode.SYSTEM)
    assertEquals(ThemeMode.SYSTEM, ThemeManager.themeMode)
  }

  @Test
  fun `offer card skeleton renders with correct test tag`() {
    composeTestRule.setContent {
      HavenTheme {
        OfferCardSkeleton()
      }
    }

    composeTestRule.onNodeWithTag("offer_card_skeleton").assertExists()
  }

  @Test
  fun `saved offers screen renders with back button and title`() {
    composeTestRule.setContent {
      HavenTheme {
        com.example.ui.screens.SavedOffersScreen(
          onBack = {},
          onBrowseOffers = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("saved_offers_screen").assertExists()
    composeTestRule.onNodeWithTag("saved_offers_back_button").assertExists()
    composeTestRule.onNodeWithTag("saved_offers_title").assertExists()
  }

  @Test
  fun `following screen renders with back button and title`() {
    composeTestRule.setContent {
      HavenTheme {
        com.example.ui.screens.FollowingScreen(
          onBack = {},
          onBrowseOffers = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("following_screen").assertExists()
    composeTestRule.onNodeWithTag("following_back_button").assertExists()
    composeTestRule.onNodeWithTag("following_title").assertExists()
  }

  @Test
  fun `my offers screen renders with back button and create action`() {
    composeTestRule.setContent {
      HavenTheme {
        com.example.ui.screens.MyOffersScreen(
          onBack = {},
          onCreateOffer = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("my_offers_screen").assertExists()
    composeTestRule.onNodeWithTag("my_offers_back_button").assertExists()
    composeTestRule.onNodeWithTag("my_offers_title").assertExists()
    composeTestRule.onNodeWithTag("create_offer_action_button").assertExists()
  }
}
