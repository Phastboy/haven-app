package com.example

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.FeedOffer
import com.example.data.FollowingRepository
import com.example.data.MyOffersRepository
import com.example.data.PaginatedFollowing
import com.example.data.PaginatedOffers
import com.example.data.Person
import com.example.data.ProfileOverview
import com.example.data.SavedOffersRepository
import com.example.data.theme.HavenThemePreferencesRepository
import com.example.ui.model.FollowingViewModel
import com.example.ui.model.MyOffersViewModel
import com.example.ui.model.ProfileUiState
import com.example.ui.model.SavedOffersViewModel
import com.example.ui.screens.FollowingScreen
import com.example.ui.screens.MyOffersScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SavedOffersScreen
import com.example.ui.theme.HavenTheme
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
@OptIn(ExperimentalCoroutinesApi::class)
class PagesAndAppearanceCleanArchitectureTest {

  @get:Rule
  val compose = createComposeRule()

  private val sampleOffer = FeedOffer(
    id = "saved-1",
    slug = "handcrafted-pot",
    title = "Handcrafted Ceramic Pot",
    makerName = "Olumide Adeleke",
    priceDisplay = "12,000 NGN",
    coverUrl = null,
    category = "pottery"
  )

  private val samplePerson = Person(
    id = "person-1",
    handle = "olumide",
    displayName = "Olumide Adeleke",
    bio = "Potter",
    joined = "Nov 2026",
    avatarUrl = null
  )

  @Test
  fun appearanceModalDisplaysRadioButtonsAndSwitchesMode() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val repo = HavenThemePreferencesRepository(context)
    repo.setThemeMode(ThemeMode.LIGHT)
    assertEquals(ThemeMode.LIGHT, repo.getThemeMode())

    compose.setContent {
      HavenTheme {
        ProfileScreen(
          state = ProfileUiState(
            profile = ProfileOverview(
              id = "test-user",
              handle = "testuser",
              displayName = "Test User",
              bio = null,
              joined = "Nov 2026",
              avatarUrl = null,
              offerCount = 1,
              savedCount = 2,
              followingCount = 3,
              offers = emptyList()
            )
          ),
          onRetry = {},
          onOpenWeb = { _ -> },
          onSignOut = {}
        )
      }
    }

    // Open Appearance modal
    compose.onNodeWithTag("profile_link_appearance").performScrollTo().performClick()
    compose.waitForIdle()

    // Verify radio buttons exist
    compose.onNodeWithTag("appearance_radio_light").assertIsSelected()

    // Click dark option
    compose.onNodeWithTag("appearance_option_dark").performClick()
    compose.waitForIdle()
    assertEquals(ThemeMode.DARK, repo.getThemeMode())
    assertEquals(ThemeMode.DARK, ThemeManager.themeMode)
  }

  @Test
  fun savedOffersViewModelLoadsItemsAndHandlesUnsave() = runTest {
    var unsavedSlug: String? = null
    val fakeRepo = object : SavedOffersRepository {
      override suspend fun getSavedOffers(after: String?): Result<PaginatedOffers> =
        Result.success(PaginatedOffers(listOf(sampleOffer), null))

      override suspend fun unsaveOffer(slug: String): Result<Boolean> {
        unsavedSlug = slug
        return Result.success(true)
      }
    }

    val viewModel = SavedOffersViewModel(fakeRepo)
    advanceUntilIdle()

    assertEquals(1, viewModel.state.value.items.size)
    assertEquals("handcrafted-pot", viewModel.state.value.items.first().slug)

    viewModel.toggleSave("handcrafted-pot")
    advanceUntilIdle()

    assertEquals("handcrafted-pot", unsavedSlug)
    assertEquals(0, viewModel.state.value.items.size)
  }

  @Test
  fun savedOffersScreenRendersWithViewModel() {
    val fakeRepo = object : SavedOffersRepository {
      override suspend fun getSavedOffers(after: String?): Result<PaginatedOffers> =
        Result.success(PaginatedOffers(listOf(sampleOffer), null))

      override suspend fun unsaveOffer(slug: String): Result<Boolean> = Result.success(true)
    }

    val viewModel = SavedOffersViewModel(fakeRepo)

    compose.setContent {
      HavenTheme {
        SavedOffersScreen(
          viewModel = viewModel,
          onBack = {},
          onOpenOffer = {},
          onBrowseOffers = {}
        )
      }
    }

    compose.onNodeWithText("Saved offers").assertExists()
    compose.onNodeWithText("Handcrafted Ceramic Pot").assertExists()
    compose.onNodeWithText("1 saved").assertExists()
  }

  @Test
  fun followingViewModelLoadsFollowedUsers() = runTest {
    val fakeRepo = object : FollowingRepository {
      override suspend fun getFollowing(after: String?): Result<PaginatedFollowing> =
        Result.success(PaginatedFollowing(listOf(samplePerson), null))
      override suspend fun unfollow(handle: String): Result<Boolean> =
        Result.success(true)
    }

    val viewModel = FollowingViewModel(fakeRepo)
    advanceUntilIdle()

    assertEquals(1, viewModel.state.value.items.size)
    assertEquals("olumide", viewModel.state.value.items.first().handle)
  }

  @Test
  fun followingScreenRendersWithViewModel() {
    val fakeRepo = object : FollowingRepository {
      override suspend fun getFollowing(after: String?): Result<PaginatedFollowing> =
        Result.success(PaginatedFollowing(listOf(samplePerson), null))
      override suspend fun unfollow(handle: String): Result<Boolean> =
        Result.success(true)
    }

    val viewModel = FollowingViewModel(fakeRepo)

    compose.setContent {
      HavenTheme {
        FollowingScreen(
          viewModel = viewModel,
          onBack = {},
          onOpenProvider = {},
          onOpenWebFollow = {},
          onDiscoverOffers = {}
        )
      }
    }

    compose.waitForIdle()
    compose.onNodeWithTag("following_title").assertExists()
    compose.onNodeWithText("Olumide Adeleke").assertExists()
  }

  @Test
  fun myOffersViewModelLoadsOffersAndHandlesPagination() = runTest {
    val fakeRepo = object : MyOffersRepository {
      override suspend fun getMyOffers(after: String?): Result<PaginatedOffers> =
        Result.success(PaginatedOffers(listOf(sampleOffer), "cursor-2"))
      override suspend fun deleteOffer(slug: String): Result<Boolean> =
        Result.success(true)
    }

    val viewModel = MyOffersViewModel(fakeRepo)
    advanceUntilIdle()

    assertEquals(1, viewModel.state.value.items.size)
    assertEquals("cursor-2", viewModel.state.value.offers?.nextCursor)
  }

  @Test
  fun myOffersScreenRendersWithViewModel() {
    val fakeRepo = object : MyOffersRepository {
      override suspend fun getMyOffers(after: String?): Result<PaginatedOffers> =
        Result.success(PaginatedOffers(listOf(sampleOffer), null))
      override suspend fun deleteOffer(slug: String): Result<Boolean> =
        Result.success(true)
    }

    val viewModel = MyOffersViewModel(fakeRepo)

    compose.setContent {
      HavenTheme {
        MyOffersScreen(
          viewModel = viewModel,
          onBack = {},
          onOpenOffer = {},
          onCreateOffer = {},
          onEditOffer = {}
        )
      }
    }

    compose.onNodeWithText("My offers").assertExists()
    compose.onNodeWithText("Handcrafted Ceramic Vase").let {
      compose.onNodeWithText("Handcrafted Ceramic Pot").assertExists()
    }
  }
  @Test
  fun deletionDoesNotClaimSuccessBeforeServerConfirmation() = runTest {
    val confirmation = kotlinx.coroutines.CompletableDeferred<Result<Boolean>>()
    val fakeRepo = object : MyOffersRepository {
      override suspend fun getMyOffers(after: String?) = Result.success(PaginatedOffers(listOf(sampleOffer), null))
      override suspend fun deleteOffer(slug: String): Result<Boolean> = confirmation.await()
    }
    val model = MyOffersViewModel(fakeRepo)
    advanceUntilIdle()
    model.deleteOffer(sampleOffer.slug)
    assertEquals(1, model.state.value.items.size)
    assertEquals(null, model.state.value.actionMessage)
    confirmation.complete(Result.success(true))
    advanceUntilIdle()
    assertEquals(0, model.state.value.items.size)
    assertEquals("Deleted “${sampleOffer.title}”.", model.state.value.actionMessage)
  }

}
