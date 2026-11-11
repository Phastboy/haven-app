package com.example

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.ProfileOverview
import com.example.ui.model.ProfileUiState
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.HavenTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Issue #31: a genuinely missing remote profile shows the static shell plus
 * reusable skeletons instead of a page spinner, while retained or failed content
 * never collapses into skeletons.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProfileShellRobolectricTest {

  @get:Rule
  val compose = createComposeRule()

  private fun loadedProfile() = ProfileOverview(
    id = "person", handle = "real-person", displayName = "Real person", bio = null,
    joined = "Joined November 2026", avatarUrl = null,
    offerCount = 3, savedCount = 2, followingCount = 1, offers = emptyList())

  @Test
  fun `cold start renders the shell and reusable skeletons instead of a spinner`() {
    compose.setContent {
      HavenTheme {
        ProfileScreen(ProfileUiState(loading = true), {}, {}, {})
      }
    }

    compose.onNodeWithTag("profile_screen").assertExists()
    // Identity + offer geometry reserved by reusable skeletons, not a page spinner.
    compose.onNodeWithTag("profile_identity_skeleton").assertExists()
    compose.onNodeWithContentDescription("Loading profile").assertExists()
    compose.onNodeWithTag("profile_offers_skeleton").assertExists()
    compose.onAllNodesWithTag("offer_card_skeleton").assertCountEquals(4)
    compose.onNodeWithTag("profile_loading").assertDoesNotExist()
    // No fake counts: the count rows show placeholders, not zeros.
    compose.onNodeWithText("0 offers").assertDoesNotExist()
  }

  @Test
  fun `profile dependent controls stay disabled until identity loads`() {
    compose.setContent {
      HavenTheme {
        ProfileScreen(ProfileUiState(loading = true), {}, {}, {})
      }
    }

    compose.onNodeWithTag("profile_link_my_offers").assertIsNotEnabled()
    compose.onNodeWithTag("profile_link_saved").assertIsNotEnabled()
    compose.onNodeWithTag("profile_link_inquired").assertIsNotEnabled()
    compose.onNodeWithTag("profile_link_following").assertIsNotEnabled()
    compose.onNodeWithTag("profile_edit_button").assertIsNotEnabled()
  }

  @Test
  fun `static appearance control works before remote data arrives`() {
    compose.setContent {
      HavenTheme {
        ProfileScreen(ProfileUiState(loading = true), {}, {}, {})
      }
    }

    compose.onNodeWithTag("profile_link_appearance").assertIsEnabled()
    compose.onNodeWithTag("profile_link_appearance").performScrollTo().performClick()
    compose.onNodeWithTag("appearance_radio_light").assertExists()
  }

  @Test
  fun `retained profile stays visible while refreshing`() {
    compose.setContent {
      HavenTheme {
        ProfileScreen(ProfileUiState(profile = loadedProfile(), refreshing = true), {}, {}, {})
      }
    }

    compose.onNodeWithTag("profile_display_name").assertIsDisplayed()
    compose.onNodeWithTag("profile_refreshing").assertExists()
    compose.onNodeWithTag("profile_identity_skeleton").assertDoesNotExist()
    compose.onNodeWithTag("profile_offers_skeleton").assertDoesNotExist()
  }

  @Test
  fun `failed refresh keeps content with a nonblocking retry`() {
    var retried = 0
    compose.setContent {
      HavenTheme {
        ProfileScreen(
          state = ProfileUiState(profile = loadedProfile(), error = "Couldn’t load your profile. Please try again."),
          onRetry = { retried++ },
          onOpenWeb = {},
          onSignOut = {}
        )
      }
    }

    compose.onNodeWithTag("profile_display_name").assertIsDisplayed()
    compose.onNodeWithTag("profile_error").assertExists()
    compose.onNodeWithTag("profile_identity_skeleton").assertDoesNotExist()
    compose.onNodeWithTag("profile_retry").performScrollTo().performClick()
    assertEquals(1, retried)
  }

  @Test
  fun `cold start failure reserves geometry and keeps retry available`() {
    compose.setContent {
      HavenTheme {
        ProfileScreen(ProfileUiState(error = "Couldn’t load your profile. Please try again."), {}, {}, {})
      }
    }

    compose.onNodeWithTag("profile_error").assertExists()
    compose.onNodeWithTag("profile_retry").assertExists()
    compose.onNodeWithTag("profile_identity_skeleton").assertExists()
    compose.onNodeWithTag("profile_link_my_offers").assertIsNotEnabled()
    compose.onNodeWithTag("profile_link_appearance").assertIsEnabled()
  }
}
