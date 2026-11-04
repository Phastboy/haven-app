package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.model.MessagesViewModel
import com.example.ui.model.SampleOffers
import com.example.ui.model.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
@Config(sdk = [35])
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

}
