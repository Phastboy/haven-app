package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.FeedOffer
import com.example.data.HavenNativePagesRepository
import com.example.data.PaginatedFollowing
import com.example.data.PaginatedOffers
import com.example.data.Person
import com.example.data.ProviderProfile
import com.example.ui.components.FeedOfferCard
import com.example.ui.screens.FollowingScreen
import com.example.ui.screens.MyOffersScreen
import com.example.ui.screens.ProviderProfileScreen
import com.example.ui.screens.SavedOffersScreen
import com.example.ui.theme.HavenTheme
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp")
class NativePagesTest {
  @get:Rule val rule = createComposeRule()

  private val testPerson = Person(
    id = "person-123",
    handle = "olumide",
    displayName = "Olumide Adeleke",
    bio = "Master ceramicist crafting earthenware vessels.",
    joined = "Nov 2026",
    avatarUrl = null
  )

  private val testOffer = FeedOffer(
    id = "offer-456",
    slug = "handcrafted-vase",
    title = "Handcrafted Ceramic Vase",
    makerName = "Olumide Adeleke",
    priceDisplay = "15,000 NGN",
    coverUrl = null,
    category = "Art & Crafts"
  )

  @Test
  fun `provider profile screen renders person details and offers`() {
    var backPressed = false
    var openedOfferSlug: String? = null
    var messagedHandle: String? = null

    val profile = ProviderProfile(
      person = testPerson,
      offers = listOf(testOffer),
      nextCursor = null
    )

    rule.setContent {
      HavenTheme {
        ProviderProfileScreen(
          profile = profile,
          loading = false,
          error = null,
          onBack = { backPressed = true },
          onRetry = {},
          onOpenOffer = { openedOfferSlug = it },
          onMessageProvider = { messagedHandle = it },
          onFollowProvider = {}
        )
      }
    }

    rule.onNodeWithTag("provider_name").assertExists()
    rule.onNodeWithTag("provider_joined").assertExists()
    rule.onNodeWithTag("provider_bio").assertExists()
    rule.onNodeWithText("Handcrafted Ceramic Vase").assertExists()
    rule.onNodeWithText("15,000 NGN").assertExists()

    rule.onNodeWithTag("provider_profile_back").performClick()
    rule.runOnIdle { assertEquals(true, backPressed) }

    rule.onNodeWithTag("provider_message_button").performClick()
    rule.runOnIdle { assertEquals("olumide", messagedHandle) }

    rule.onNodeWithTag("provider_offer_${testOffer.id}").performClick()
    rule.runOnIdle { assertEquals("handcrafted-vase", openedOfferSlug) }
  }

  @Test
  fun `my offers screen renders list and handles edit and create actions`() {
    var created = false
    var editedId: String? = null
    var openedSlug: String? = null

    val paginated = PaginatedOffers(
      items = listOf(testOffer),
      nextCursor = null
    )

    rule.setContent {
      HavenTheme {
        MyOffersScreen(
          offers = paginated,
          loading = false,
          error = null,
          onBack = {},
          onRetry = {},
          onOpenOffer = { openedSlug = it },
          onCreateOffer = { created = true },
          onEditOffer = { editedId = it }
        )
      }
    }

    rule.onNodeWithText("My offers").assertExists()
    rule.onNodeWithText("1 offer").assertExists()
    rule.onNodeWithText("Handcrafted Ceramic Vase").assertExists()

    rule.onNodeWithTag("my_offers_create_button").performClick()
    rule.runOnIdle { assertEquals(true, created) }

    rule.onNodeWithTag("my_offer_edit_${testOffer.id}").performClick()
    rule.runOnIdle { assertEquals(testOffer.id, editedId) }
  }

  @Test
  fun `private offers open the editor instead of unavailable public detail`() {
    var editedId: String? = null
    var openedSlug: String? = null
    rule.setContent {
      HavenTheme {
        MyOffersScreen(
          offers = PaginatedOffers(listOf(testOffer.copy(isPublic = false)), null),
          loading = false, error = null, onBack = {}, onRetry = {},
          onOpenOffer = { openedSlug = it }, onCreateOffer = {}, onEditOffer = { editedId = it }
        )
      }
    }
    rule.onNodeWithText("Private · add a photo to publish").assertExists()
    rule.onNodeWithText(testOffer.title).performClick()
    rule.runOnIdle { assertEquals(testOffer.id, editedId); assertNull(openedSlug) }
  }

  @Test
  fun `saved offers screen renders items`() {
    val paginated = PaginatedOffers(
      items = listOf(testOffer),
      nextCursor = null
    )

    rule.setContent {
      HavenTheme {
        SavedOffersScreen(
          offers = paginated,
          loading = false,
          error = null,
          savingSlugs = emptySet(),
          onBack = {},
          onRetry = {},
          onOpenOffer = {},
          onUnsaveOffer = {},
          onBrowseOffers = {}
        )
      }
    }

    rule.onNodeWithText("Saved offers").assertExists()
    rule.onNodeWithText("1 saved").assertExists()
    rule.onNodeWithText("Handcrafted Ceramic Vase").assertExists()
  }

  @Test
  fun `saved offers screen renders empty state and handles browse CTA`() {
    var browsed = false

    rule.setContent {
      HavenTheme {
        SavedOffersScreen(
          offers = PaginatedOffers(emptyList(), null),
          loading = false,
          error = null,
          savingSlugs = emptySet(),
          onBack = {},
          onRetry = {},
          onOpenOffer = {},
          onUnsaveOffer = {},
          onBrowseOffers = { browsed = true }
        )
      }
    }

    rule.onNodeWithText("No saved offers yet").assertExists()
    rule.onNodeWithTag("saved_offers_empty_browse").performClick()
    rule.runOnIdle { assertEquals(true, browsed) }
  }

  @Test
  fun `following screen renders rows`() {
    var openedProvider: String? = null

    val following = PaginatedFollowing(
      items = listOf(testPerson),
      nextCursor = null
    )

    rule.setContent {
      HavenTheme {
        FollowingScreen(
          following = following,
          loading = false,
          error = null,
          onBack = {},
          onRetry = {},
          onOpenProvider = { openedProvider = it },
          onOpenWebFollow = {},
          onDiscoverOffers = {}
        )
      }
    }

    rule.onAllNodesWithText("Following")[0].assertExists()
    rule.onNodeWithText("1 person").assertExists()
    rule.onNodeWithText("Olumide Adeleke").assertExists()
    rule.onNodeWithTag("following_button_${testPerson.id}").assertExists()

    rule.onNodeWithTag("following_row_${testPerson.id}").performClick()
    rule.runOnIdle { assertEquals("olumide", openedProvider) }
  }

  @Test
  fun `following screen renders empty state and handles discover CTA`() {
    var discovered = false

    rule.setContent {
      HavenTheme {
        FollowingScreen(
          following = PaginatedFollowing(emptyList(), null),
          loading = false,
          error = null,
          onBack = {},
          onRetry = {},
          onOpenProvider = {},
          onOpenWebFollow = {},
          onDiscoverOffers = { discovered = true }
        )
      }
    }

    rule.onNodeWithText("Not following anyone yet").assertExists()
    rule.onNodeWithTag("following_empty_discover").performClick()
    rule.runOnIdle { assertEquals(true, discovered) }
  }

  @Test
  fun `feed offer card triggers onOpenProvider when provider text is clicked`() {
    var openedProviderHandle: String? = null
    var openedOffer = false
    val offerWithHandle = testOffer.copy(makerHandle = "olumide-adeleke")

    rule.setContent {
      HavenTheme {
        FeedOfferCard(
          offer = offerWithHandle,
          onOpenProvider = { openedProviderHandle = it },
          onOpen = { openedOffer = true }
        )
      }
    }

    rule.onNodeWithTag("offer_maker_${offerWithHandle.id}").performClick()
    rule.runOnIdle {
      assertEquals("olumide-adeleke", openedProviderHandle)
      assertEquals(false, openedOffer)
    }
  }

  @Test
  fun `provider profile screen renders category rail and filters offers`() {
    val offer1 = testOffer.copy(id = "o-1", slug = "slug-1", title = "Aso Oke Textile", category = "Craft & Making")
    val offer2 = testOffer.copy(id = "o-2", slug = "slug-2", title = "Handcrafted Clay Pot", category = "Home & Living")
    val profile = ProviderProfile(
      person = testPerson,
      offers = listOf(offer1, offer2),
      nextCursor = null
    )

    rule.setContent {
      HavenTheme {
        ProviderProfileScreen(
          profile = profile,
          loading = false,
          error = null,
          onBack = {},
          onRetry = {},
          onOpenOffer = {},
          onMessageProvider = {},
          onFollowProvider = {}
        )
      }
    }

    rule.onNodeWithTag("provider_category_chip_All").assertExists()
    rule.onNodeWithTag("provider_category_chip_Craft & Making").assertExists()
    rule.onNodeWithTag("provider_category_chip_Home & Living").assertExists()

    // Both offers initially exist in the list
    rule.onNodeWithTag("provider_offer_${offer1.id}").performScrollTo().assertExists()
    rule.onNodeWithTag("provider_offer_${offer2.id}").performScrollTo().assertExists()

    // Filter by Craft & Making
    rule.onNodeWithTag("provider_category_chip_Craft & Making").performClick()
    rule.runOnIdle {
      rule.onNodeWithTag("provider_offer_${offer1.id}").performScrollTo().assertExists()
      rule.onNodeWithTag("provider_offer_${offer2.id}").assertDoesNotExist()
    }
  }

  @Test
  fun `parsePerson parses json correctly`() {
    val json = JSONObject("""{
      "id": "p-1",
      "handle": "amaka",
      "display_name": "Amaka Eze",
      "bio": null,
      "joined": "Oct 2026",
      "avatar_url": "https://example.com/avatar.jpg"
    }""")
    val person = HavenNativePagesRepository.parsePerson(json)
    assertEquals("p-1", person.id)
    assertEquals("amaka", person.handle)
    assertEquals("Amaka Eze", person.displayName)
    assertNull(person.bio)
    assertEquals("Oct 2026", person.joined)
    assertEquals("https://example.com/avatar.jpg", person.avatarUrl)
  }
}
