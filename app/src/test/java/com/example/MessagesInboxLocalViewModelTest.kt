package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ConversationInboxPage
import com.example.data.MessagesInboxRepository
import com.example.data.messaging.HavenLocalFirstMessagingRepository
import com.example.data.messaging.db.ConversationEntity
import com.example.data.messaging.db.HavenDatabase
import com.example.data.messaging.transport.ConversationSummaryDto
import com.example.data.messaging.transport.MockMessagingTransport
import com.example.ui.model.InboxContentState
import com.example.ui.model.MessagesInboxViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regressions for the local-first inbox path: cache-first rendering, honest
 * empty/failure states, warm-return revalidation, canonical ordering and
 * account isolation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class MessagesInboxLocalViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var context: Context
  private lateinit var database: HavenDatabase
  private lateinit var transport: MockMessagingTransport
  private lateinit var repository: HavenLocalFirstMessagingRepository
  private lateinit var viewModel: MessagesInboxViewModel

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
    viewModel = MessagesInboxViewModel(
      repository = object : MessagesInboxRepository {
        override suspend fun load(after: String?): ConversationInboxPage =
          ConversationInboxPage(emptyList(), null)

        override suspend fun getGlobalUnread(): Boolean = false
      },
      localMessagingRepository = repository
    )
  }

  @After
  fun tearDown() {
    viewModel.clear()
    database.close()
    Dispatchers.resetMain()
  }

  /**
   * Runs pending continuations at the current virtual time without advancing
   * past delays, so in-flight work can be asserted deterministically.
   */
  private fun TestCoroutineScheduler.pump() = repeat(10) { runCurrent() }

  private fun cachedConversation(id: String, owner: String, timestamp: Long) = ConversationEntity(
    id = id, ownerPersonId = owner, otherUserId = "person-$id",
    displayName = "Member $id", handle = "@member-$id", avatarUrl = null,
    lastMessageBody = "Cached update for $id", lastMessageAt = "2026-11-04T12:00:00Z",
    lastMessageTimestamp = timestamp, isUnread = false
  )

  private fun remoteConversation(id: String, atIso: String) = ConversationSummaryDto(
    id = id, otherUserId = "person-$id", displayName = "Member $id", handle = "@member-$id",
    avatarUrl = null, lastMessageBody = "Server update for $id", lastMessageAt = atIso, isUnread = false
  )

  @Test
  fun coldOpenWithoutCachedRowsReportsRemoteLoadingThenContent() = runTest(testDispatcher) {
    transport.simulatedDelayMs = 500L
    viewModel.load("owner-cold")
    testScheduler.pump()
    // Local cache resolved empty and the remote inbox load is in flight.
    assertEquals(InboxContentState.LOADING, viewModel.state.value.contentState)
    assertTrue(viewModel.state.value.loading)
    assertTrue(viewModel.state.value.items.isEmpty())
    testScheduler.advanceUntilIdle()
    assertEquals(InboxContentState.CONTENT, viewModel.state.value.contentState)
    assertTrue(viewModel.state.value.items.isNotEmpty())
    assertFalse(viewModel.state.value.loading)
  }

  @Test
  fun coldFailureMarksInboxUnavailableAndRetryRecoversRows() = runTest(testDispatcher) {
    transport.shouldFailNextFetch = true
    viewModel.load("owner-fail")
    testScheduler.advanceUntilIdle()
    assertEquals(InboxContentState.UNAVAILABLE, viewModel.state.value.contentState)
    assertEquals("Couldn’t load messages. Please try again.", viewModel.state.value.error)
    assertTrue(viewModel.state.value.items.isEmpty())

    viewModel.load("owner-fail", force = true)
    testScheduler.advanceUntilIdle()
    assertEquals(InboxContentState.CONTENT, viewModel.state.value.contentState)
    assertNull(viewModel.state.value.error)
    assertTrue(viewModel.state.value.items.isNotEmpty())
  }

  @Test
  fun loadedEmptyInboxKeepsTruthfulEmptyShellAcrossRevalidation() = runTest(testDispatcher) {
    transport.mockConversations = emptyList()
    viewModel.load("owner-empty")
    testScheduler.advanceUntilIdle()
    assertEquals(InboxContentState.EMPTY, viewModel.state.value.contentState)
    assertTrue(viewModel.state.value.emptyInboxConfirmed)

    // An ordinary forced revalidation (tab re-entry) must not flash the cold
    // loading state over the settled empty shell.
    transport.simulatedDelayMs = 500L
    viewModel.load("owner-empty", force = true)
    testScheduler.pump()
    assertTrue(viewModel.state.value.loading)
    assertEquals(InboxContentState.EMPTY, viewModel.state.value.contentState)
    testScheduler.advanceUntilIdle()
    assertEquals(InboxContentState.EMPTY, viewModel.state.value.contentState)
    assertTrue(viewModel.state.value.emptyInboxConfirmed)

    // A non-forced reload of the same loaded-empty inbox stays quiet.
    viewModel.load("owner-empty")
    testScheduler.advanceUntilIdle()
    assertEquals(InboxContentState.EMPTY, viewModel.state.value.contentState)
    assertFalse(viewModel.state.value.loading)
  }

  @Test
  fun warmCachedRowsRetainContentAndReportOfflineRefreshFailure() = runTest(testDispatcher) {
    database.conversationDao().upsertConversation(cachedConversation("conv-warm", "owner-warm", 1_700_000_000_000L))
    transport.shouldFailNextFetch = true
    viewModel.load("owner-warm")
    testScheduler.pump()
    // Cached rows render while the refresh is still in flight.
    assertEquals(InboxContentState.CONTENT, viewModel.state.value.contentState)
    testScheduler.advanceUntilIdle()
    // A failed refresh with retained rows keeps the content and reports offline.
    assertEquals(InboxContentState.CONTENT, viewModel.state.value.contentState)
    assertEquals("Offline - viewing cached messages", viewModel.state.value.offlineBanner)
    assertNull(viewModel.state.value.error)
    assertEquals(listOf("conv-warm"), viewModel.state.value.items.map { it.id })
  }

  @Test
  fun newActivityOrdersConversationsByLatestMessageWithoutDuplicates() = runTest(testDispatcher) {
    transport.mockConversations = listOf(
      remoteConversation("conv-old", "2026-11-01T10:00:00Z"),
      remoteConversation("conv-mid", "2026-11-02T10:00:00Z"),
      remoteConversation("conv-new", "2026-11-03T10:00:00Z")
    )
    viewModel.load("owner-live")
    testScheduler.advanceUntilIdle()
    assertEquals(InboxContentState.CONTENT, viewModel.state.value.contentState)
    // Canonical latest-message ordering: newest first, stable ids, no duplicates.
    assertEquals(listOf("conv-new", "conv-mid", "conv-old"), viewModel.state.value.items.map { it.id })
    assertEquals(3, viewModel.state.value.items.distinctBy { it.id }.size)

    // A new arrival on a later refresh moves to the top instead of appending.
    transport.mockConversations = listOf(
      remoteConversation("conv-old", "2026-11-01T10:00:00Z"),
      remoteConversation("conv-mid", "2026-11-02T10:00:00Z"),
      remoteConversation("conv-new", "2026-11-03T10:00:00Z"),
      remoteConversation("conv-fresh", "2026-11-04T10:00:00Z")
    )
    viewModel.load("owner-live", force = true)
    testScheduler.advanceUntilIdle()
    assertEquals(
      listOf("conv-fresh", "conv-new", "conv-mid", "conv-old"),
      viewModel.state.value.items.map { it.id }
    )
  }

  @Test
  fun actorSwitchResolvesFreshCacheWithoutLeakingOtherAccountRows() = runTest(testDispatcher) {
    transport.mockConversations = emptyList()
    database.conversationDao().upsertConversation(cachedConversation("conv-a", "owner-a", 1_000L))
    database.conversationDao().upsertConversation(cachedConversation("conv-b", "owner-b", 2_000L))
    viewModel.load("owner-a")
    testScheduler.advanceUntilIdle()
    assertEquals(InboxContentState.CONTENT, viewModel.state.value.contentState)
    assertEquals(listOf("conv-a"), viewModel.state.value.items.map { it.id })

    viewModel.load("owner-b")
    testScheduler.advanceUntilIdle()
    assertEquals(InboxContentState.CONTENT, viewModel.state.value.contentState)
    assertEquals(listOf("conv-b"), viewModel.state.value.items.map { it.id })
  }

  @Test
  fun confirmedEmptyInboxBecomesContentWhenAMessageArrives() = runTest(testDispatcher) {
    transport.mockConversations = emptyList()
    viewModel.load("owner-arrival")
    testScheduler.advanceUntilIdle()
    assertEquals(InboxContentState.EMPTY, viewModel.state.value.contentState)

    transport.mockConversations = listOf(remoteConversation("conv-arrival", "2026-11-05T10:00:00Z"))
    viewModel.load("owner-arrival", force = true)
    testScheduler.advanceUntilIdle()
    assertEquals(InboxContentState.CONTENT, viewModel.state.value.contentState)
    assertEquals(listOf("conv-arrival"), viewModel.state.value.items.map { it.id })
    assertFalse(viewModel.state.value.emptyInboxConfirmed)
  }
}
