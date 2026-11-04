package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.messaging.HavenLocalFirstMessagingRepository
import com.example.data.messaging.db.ConversationEntity
import com.example.data.messaging.db.HavenDatabase
import com.example.data.messaging.transport.MockMessagingTransport
import com.example.ui.model.ConversationThreadViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
}
