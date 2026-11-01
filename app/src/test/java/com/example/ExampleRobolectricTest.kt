package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.model.MessagesViewModel
import com.example.ui.model.OffersTab
import com.example.ui.model.OffersViewModel
import com.example.ui.model.SampleOffers
import com.example.ui.model.SessionManager
import com.example.ui.model.SignInViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class ExampleRobolectricTest {

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
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Haven", appName)
  }

  @Test
  fun `email validation correctly identifies valid and invalid emails`() {
    assertTrue(SignInViewModel.isValidEmail("adaeze@crafts.ng"))
    assertTrue(SignInViewModel.isValidEmail("name@example.com"))
    assertTrue(SignInViewModel.isValidEmail("user.name+tag@sub.domain.co"))

    assertFalse(SignInViewModel.isValidEmail(""))
    assertFalse(SignInViewModel.isValidEmail("   "))
    assertFalse(SignInViewModel.isValidEmail("adaeze@not-an-email"))
    assertFalse(SignInViewModel.isValidEmail("plainaddress"))
    assertFalse(SignInViewModel.isValidEmail("@missingusername.com"))
    assertFalse(SignInViewModel.isValidEmail("user@.com"))
  }

  @Test
  fun `submitting invalid email displays validation error`() {
    val viewModel = SignInViewModel(testDispatcher)
    viewModel.onEmailChange("adaeze@not-an-email")
    viewModel.onSendMagicLink()

    val state = viewModel.uiState.value
    assertTrue(state.isError)
    assertEquals("Enter a valid email address.", state.emailError)
    assertFalse(state.isSending)
    assertFalse(state.isSuccess)
  }

  @Test
  fun `typing in email clears previous validation error`() {
    val viewModel = SignInViewModel(testDispatcher)
    viewModel.onEmailChange("invalid")
    viewModel.onSendMagicLink()
    assertTrue(viewModel.uiState.value.isError)

    viewModel.onEmailChange("adaeze@crafts.ng")
    assertFalse(viewModel.uiState.value.isError)
    assertNull(viewModel.uiState.value.emailError)
  }

  @Test
  fun `browser handoff never authenticates without an API exchange`() {
    SessionManager.signOut()
    val viewModel = SignInViewModel(testDispatcher)
    viewModel.onEmailChange("adaeze@crafts.ng")
    assertTrue(viewModel.onSendMagicLink())
    viewModel.onGoogleSignInClicked()
    assertFalse(SessionManager.session.value.isAuthenticated)
    assertNull(viewModel.uiState.value.googleErrorBanner)
  }

  @Test
  fun `offers feed initializes with 6 items and filters by search keyword`() = runTest(testDispatcher) {
    val viewModel = OffersViewModel()
    testScheduler.advanceTimeBy(700)

    val state = viewModel.uiState.value
    assertEquals(6, state.allOffers.size)
    assertEquals(6, state.filteredOffers.size)

    // Search by title keyword
    viewModel.onSearchQueryChange("Aso Oke")
    assertEquals(1, viewModel.uiState.value.filteredOffers.size)
    assertEquals("Hand-woven Aso Oke fabric, 6 yards", viewModel.uiState.value.filteredOffers.first().title)

    // Search by creator name
    viewModel.onSearchQueryChange("Tunde")
    assertEquals(1, viewModel.uiState.value.filteredOffers.size)
    assertEquals("Tunde Bakare", viewModel.uiState.value.filteredOffers.first().creatorName)

    // Clear search
    viewModel.onSearchQueryChange("")
    assertEquals(6, viewModel.uiState.value.filteredOffers.size)
  }

  @Test
  fun `offers currency and price filters work correctly`() = runTest(testDispatcher) {
    val viewModel = OffersViewModel()
    testScheduler.advanceTimeBy(700)

    // Filter by USD currency with max price 20
    viewModel.onDraftCurrencyChange("USD")
    viewModel.onDraftMinPriceChange("0")
    viewModel.onDraftMaxPriceChange("20")
    viewModel.applyFilters()
    testScheduler.advanceTimeBy(500)

    val filtered = viewModel.uiState.value.filteredOffers
    // Should include 18 USD ceramic mugs and Free textbooks
    assertTrue(filtered.any { it.title.contains("ceramic mugs") })
    assertTrue(filtered.any { it.title.contains("textbooks") })
    assertFalse(filtered.any { it.title.contains("Guitar") }) // 25 USD > 20 USD
    assertFalse(filtered.any { it.title.contains("Aso Oke") }) // NGN

    // Clear filters restores all items
    viewModel.clearFilters()
    testScheduler.advanceTimeBy(500)
    assertEquals(6, viewModel.uiState.value.filteredOffers.size)
  }

  @Test
  fun `toggling favorite updates offer state`() = runTest(testDispatcher) {
    val viewModel = OffersViewModel()
    testScheduler.advanceTimeBy(700)

    val offerId = "offer-2"
    val initialFavorite = viewModel.uiState.value.allOffers.first { it.id == offerId }.isFavorite
    viewModel.onToggleFavorite(offerId)

    val updatedFavorite = viewModel.uiState.value.allOffers.first { it.id == offerId }.isFavorite
    assertEquals(!initialFavorite, updatedFavorite)
  }

  @Test
  fun `session manager handles sign in and sign out correctly`() {
    SessionManager.signOut()
    assertFalse(SessionManager.session.value.isAuthenticated)
    assertNull(SessionManager.session.value.email)
    assertEquals("Guest Member", SessionManager.session.value.displayName)

    SessionManager.signInPerson("person-1", "Adaeze Obi", "adaeze")
    assertTrue(SessionManager.session.value.isAuthenticated)
    assertEquals("person-1", SessionManager.session.value.personId)
    assertEquals("Adaeze Obi", SessionManager.session.value.displayName)
    assertEquals("A", SessionManager.session.value.initial)

    SessionManager.signOut()
    assertFalse(SessionManager.session.value.isAuthenticated)
  }

  @Test
  fun `offers tab navigation switches active tabs`() {
    val viewModel = OffersViewModel()
    assertEquals(OffersTab.OFFERS, viewModel.uiState.value.activeTab)

    viewModel.selectTab(OffersTab.MESSAGES)
    assertEquals(OffersTab.MESSAGES, viewModel.uiState.value.activeTab)

    viewModel.selectTab(OffersTab.PROFILE)
    assertEquals(OffersTab.PROFILE, viewModel.uiState.value.activeTab)

    viewModel.selectTab(OffersTab.OFFERS)
    assertEquals(OffersTab.OFFERS, viewModel.uiState.value.activeTab)
  }

  @Test
  fun `messages view model manages conversation thread and offer attachments`() = runTest(testDispatcher) {
    val viewModel = MessagesViewModel()
    val initialConversations = viewModel.uiState.value.conversations
    assertTrue(initialConversations.isNotEmpty())

    // Selecting a conversation opens thread and marks as read
    val targetConv = initialConversations.first()
    viewModel.selectConversation(targetConv.id)
    assertEquals(targetConv.id, viewModel.uiState.value.selectedConversationId)
    assertNotNull(viewModel.uiState.value.selectedConversation)

    // Attaching an offer
    val offerToAttach = SampleOffers.defaultOffers[1] // Lamp
    viewModel.selectOfferToAttach(offerToAttach)
    assertEquals(offerToAttach, viewModel.uiState.value.pendingAttachedOffer)

    // Sending a message with the attached offer
    viewModel.onMessageInputChange("Would you accept this brass lamp in trade?")
    viewModel.sendMessage()

    // Input and pending attachment are cleared
    assertTrue(viewModel.uiState.value.messageInputText.isEmpty())
    assertNull(viewModel.uiState.value.pendingAttachedOffer)

    // The sent message contains the attached offer
    val updatedConv = viewModel.uiState.value.selectedConversation!!
    val lastMsg = updatedConv.messages.last()
    assertTrue(lastMsg.isFromMe)
    assertEquals("Would you accept this brass lamp in trade?", lastMsg.text)
    assertEquals(offerToAttach, lastMsg.attachedOffer)

    // Closing thread returns to conversation list
    viewModel.closeConversationThread()
    assertNull(viewModel.uiState.value.selectedConversationId)
  }

  @Test
  fun `startConversationForOffer pre-attaches offer to thread`() {
    val viewModel = MessagesViewModel()
    val offer = SampleOffers.defaultOffers[0] // Aso Oke by Adaeze Obi

    viewModel.startConversationForOffer(offer)
    assertNotNull(viewModel.uiState.value.selectedConversationId)
    assertEquals(offer, viewModel.uiState.value.pendingAttachedOffer)
  }

  @Test
  fun `toggling favorite keeps the open offer detail in sync`() = runTest(testDispatcher) {
    val viewModel = OffersViewModel()
    testScheduler.advanceTimeBy(700)

    val offer = viewModel.uiState.value.allOffers.first()
    viewModel.selectOffer(offer)
    val initialFavorite = viewModel.uiState.value.selectedOffer!!.isFavorite

    viewModel.onToggleFavorite(offer.id)

    val updatedDetail = viewModel.uiState.value.selectedOffer!!
    val updatedListOffer = viewModel.uiState.value.allOffers.first { it.id == offer.id }
    assertEquals(!initialFavorite, updatedDetail.isFavorite)
    assertEquals(!initialFavorite, updatedListOffer.isFavorite)
  }
}
