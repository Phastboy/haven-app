package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.messaging.HavenLocalFirstMessagingRepository
import com.example.data.messaging.db.ConversationEntity
import com.example.data.messaging.db.HavenDatabase
import com.example.data.messaging.db.MessageEntity
import com.example.data.messaging.transport.MockMessagingTransport
import com.example.ui.model.ConversationThreadViewModel
import com.example.ui.model.ThreadContentState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationThreadViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var context: Context
  private lateinit var database: HavenDatabase
  private lateinit var transport: MockMessagingTransport
  private lateinit var repository: HavenLocalFirstMessagingRepository
  private lateinit var viewModel: ConversationThreadViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()
    val directExecutor = java.util.concurrent.Executor { it.run() }
    database = HavenDatabase.createInMemory(context, directExecutor)
    transport = MockMessagingTransport(simulatedDelayMs = 0L)
    repository = HavenLocalFirstMessagingRepository(
      database = database,
      transport = transport,
      coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher),
      enforceOwnership = false,
      ioDispatcher = testDispatcher
    )
    viewModel = ConversationThreadViewModel(repository)
  }

  @After
  fun tearDown() {
    viewModel.clear()
    database.close()
    Dispatchers.resetMain()
  }

  @Test
  fun testOpenConversationLoadsCachedSummaryAndMessages() = runTest(testDispatcher) {
    val conv = ConversationEntity(
      id = "conv-open-test",
      ownerPersonId = "owner-1",
      otherUserId = "peer-1",
      displayName = "Kofi Mensah",
      handle = "@kofi",
      avatarUrl = null,
      lastMessageBody = "Ready for the swap?",
      lastMessageAt = "2026-11-04T12:00:00Z",
      lastMessageTimestamp = 1700000000000L,
      isUnread = true
    )
    database.conversationDao().upsertConversation(conv)

    transport.mockConversations = listOf(com.example.data.messaging.transport.ConversationSummaryDto(
      id = conv.id, otherUserId = conv.otherUserId, displayName = conv.displayName,
      handle = conv.handle, avatarUrl = null, lastMessageBody = conv.lastMessageBody,
      lastMessageAt = conv.lastMessageAt, isUnread = true
    ))
    viewModel.openConversation("conv-open-test")
    testScheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNotNull(state.conversation)
    assertEquals("Kofi Mensah", state.conversation?.displayName)
    assertTrue(state.messages.isNotEmpty())

    viewModel.clear()
  }

  @Test
  fun testOptimisticSendClearsInputAndEmitsMessage() = runTest(testDispatcher) {
    val conv = ConversationEntity(
      id = "conv-send-test",
      ownerPersonId = "owner-1",
      otherUserId = "peer-2",
      displayName = "Amina Bello",
      handle = "@amina",
      avatarUrl = null,
      lastMessageBody = "Initial",
      lastMessageAt = "2026-11-04T12:00:00Z",
      lastMessageTimestamp = 1700000000000L,
      isUnread = false
    )
    database.conversationDao().upsertConversation(conv)

    viewModel.openConversation("conv-send-test")
    testScheduler.advanceUntilIdle()

    viewModel.onInputTextChanged("Hello Amina, let's trade!")
    assertEquals("Hello Amina, let's trade!", viewModel.uiState.value.inputText)

    viewModel.sendMessage()
    testScheduler.advanceUntilIdle()

    // Input should be cleared immediately
    assertEquals("", viewModel.uiState.value.inputText)

    // New message should be in state
    val lastMessage = viewModel.uiState.value.messages.lastOrNull()
    assertNotNull(lastMessage)
    assertEquals("Hello Amina, let's trade!", lastMessage?.text)
    assertTrue(lastMessage?.isFromMe == true)

    viewModel.clear()
  }
  @Test
  fun stoppingScreenAndReopeningSameThreadPreservesDraft() = runTest(testDispatcher) {
    viewModel.openConversation("draft-thread", "owner")
    testScheduler.advanceUntilIdle()
    viewModel.onInputTextChanged("Unsent draft survives recreation")
    viewModel.attachOffer("offer", "Offer", "Free")
    viewModel.stopLiveUpdates()
    viewModel.openConversation("draft-thread", "owner")
    testScheduler.advanceUntilIdle()
    assertEquals("Unsent draft survives recreation", viewModel.uiState.value.inputText)
    assertEquals("offer", viewModel.uiState.value.pendingAttachedOfferSlug)
    viewModel.clear()
  }

  /**
   * Runs pending continuations at the current virtual time without advancing
   * past delays, so in-flight work can be asserted deterministically.
   */
  private fun TestScheduler.pump() = repeat(10) { runCurrent() }

  @Test
  fun coldOpenWithoutCachedHistoryReportsRemoteLoadingThenContent() = runTest(testDispatcher) {
    transport.simulatedDelayMs = 500L
    viewModel.openConversation("conv-cold-open")
    // Pump queued continuations without advancing virtual time, so the
    // simulated transport delay stays in flight while the state is asserted.
    testScheduler.pump()
    // Local cache resolved empty and the remote history load is in flight.
    assertEquals(ThreadContentState.LOADING_HISTORY, viewModel.uiState.value.contentState)
    assertTrue(viewModel.uiState.value.isSyncing)
    assertTrue(viewModel.uiState.value.messages.isEmpty())
    testScheduler.advanceUntilIdle()
    assertEquals(ThreadContentState.CONTENT, viewModel.uiState.value.contentState)
    assertTrue(viewModel.uiState.value.messages.isNotEmpty())
    viewModel.clear()
  }

  @Test
  fun coldSyncFailureMarksThreadUnavailableAndRetryRecoversContent() = runTest(testDispatcher) {
    transport.shouldFailNextFetch = true
    viewModel.openConversation("conv-cold-fail")
    testScheduler.advanceUntilIdle()
    assertEquals(ThreadContentState.UNAVAILABLE, viewModel.uiState.value.contentState)
    assertEquals("Couldn't load messages", viewModel.uiState.value.syncError)
    assertTrue(viewModel.uiState.value.messages.isEmpty())

    viewModel.refreshThread()
    testScheduler.advanceUntilIdle()
    assertEquals(ThreadContentState.CONTENT, viewModel.uiState.value.contentState)
    assertEquals(null, viewModel.uiState.value.syncError)
    assertTrue(viewModel.uiState.value.messages.isNotEmpty())
    viewModel.clear()
  }

  @Test
  fun successfulEmptySyncConfirmsEmptyThreadAndQuietPollDoesNotFlashSkeletons() = runTest(testDispatcher) {
    transport.threadPages["latest"] = com.example.data.messaging.transport.ConversationThreadPageDto(
      conversationId = "conv-empty",
      otherPerson = com.example.data.messaging.transport.RemotePersonDto("peer", "peer", "Peer", null),
      messages = emptyList(),
      nextCursor = null,
      isUnread = false
    )
    viewModel.openConversation("conv-empty")
    testScheduler.advanceUntilIdle()
    assertEquals(ThreadContentState.EMPTY, viewModel.uiState.value.contentState)
    assertTrue(viewModel.uiState.value.emptyThreadConfirmed)

    // An ordinary poll refresh on a settled empty thread stays quiet: it must
    // not flip the empty state back to loading skeletons.
    transport.simulatedDelayMs = 500L
    viewModel.refreshThread()
    testScheduler.pump()
    assertTrue(viewModel.uiState.value.isSyncing)
    assertEquals(ThreadContentState.EMPTY, viewModel.uiState.value.contentState)
    testScheduler.advanceUntilIdle()
    assertEquals(ThreadContentState.EMPTY, viewModel.uiState.value.contentState)
    viewModel.clear()
  }

  @Test
  fun warmCachedThreadKeepsContentAndDistinguishesOfflineRefreshFailure() = runTest(testDispatcher) {
    database.conversationDao().upsertConversation(ConversationEntity(
      id = "conv-warm", ownerPersonId = "owner-1", otherUserId = "peer-1",
      displayName = "Chidi Okeke", handle = "@chidi", avatarUrl = null,
      lastMessageBody = "Cached hello", lastMessageAt = "2026-11-04T12:00:00Z",
      lastMessageTimestamp = 1700000000000L, isUnread = false
    ))
    database.messageDao().insertMessage(MessageEntity(
      clientMessageId = "warm-1", conversationId = "conv-warm", senderPersonId = "peer-1",
      isFromMe = false, text = "Cached hello", timestamp = 1700000000000L
    ))
    transport.shouldFailNextFetch = true
    viewModel.openConversation("conv-warm", "owner-1")
    testScheduler.pump()
    // Cached content renders without waiting for the refresh to settle.
    assertEquals(ThreadContentState.CONTENT, viewModel.uiState.value.contentState)
    testScheduler.advanceUntilIdle()
    // A failed refresh with retained content keeps the content.
    assertEquals(ThreadContentState.CONTENT, viewModel.uiState.value.contentState)
    assertEquals("Offline - viewing cached messages", viewModel.uiState.value.syncError)
    assertEquals(1, viewModel.uiState.value.messages.size)
    viewModel.clear()
  }

}
