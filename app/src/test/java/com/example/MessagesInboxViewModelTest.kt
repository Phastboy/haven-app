package com.example

import com.example.data.ConversationInboxPage
import com.example.data.HavenMessagesRepository
import com.example.data.InboxConversation
import com.example.data.MessagesInboxRepository
import com.example.ui.model.MessagesInboxViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MessagesInboxViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Before fun setup() { Dispatchers.setMain(dispatcher) }
  @After fun cleanup() { Dispatchers.resetMain() }

  private fun inbox(id: String) = InboxConversation(
    id, "person-$id", "Real participant", "real-participant", null,
    "A real message from Haven", "2026-11-02T10:44:00Z", true
  )

  private fun fakeInboxRepo(
    globalUnread: Boolean = false,
    fetcher: suspend (String?) -> ConversationInboxPage
  ) = object : MessagesInboxRepository {
    override suspend fun load(after: String?): ConversationInboxPage = fetcher(after)
    override suspend fun getGlobalUnread(): Boolean = globalUnread
  }

  @Test fun `unread badge refresh does not load the private inbox off-tab`() = runTest(dispatcher) {
    var inboxReads = 0
    val model = MessagesInboxViewModel(fakeInboxRepo(globalUnread = true) {
      inboxReads++
      ConversationInboxPage(emptyList(), null)
    })
    model.loadUnread("owner")
    advanceUntilIdle()
    assertEquals(0, inboxReads)
    assertTrue(model.state.value.globalUnread)
    assertTrue(model.state.value.unreadConfirmed)
    model.load("owner")
    advanceUntilIdle()
    assertEquals(1, inboxReads)
  }

  @Test fun `inbox only displays repository data and clears it when account leaves`() = runTest(dispatcher) {
    val model = MessagesInboxViewModel(fakeInboxRepo { ConversationInboxPage(listOf(inbox("one")), null) })
    assertTrue(model.state.value.items.isEmpty())
    model.load("owner")
    advanceUntilIdle()
    assertEquals(listOf("one"), model.state.value.items.map { it.id })
    model.clear()
    assertTrue(model.state.value.items.isEmpty())
    assertNull(model.state.value.error)
  }

  @Test fun `load more uses server cursor and appends server rows`() = runTest(dispatcher) {
    val cursors = mutableListOf<String?>()
    val model = MessagesInboxViewModel(fakeInboxRepo { cursor ->
      cursors += cursor
      if (cursor == null) ConversationInboxPage(listOf(inbox("one")), "next")
      else ConversationInboxPage(listOf(inbox("two")), null)
    })
    model.load("owner")
    advanceUntilIdle()
    model.loadMore()
    advanceUntilIdle()
    assertEquals(listOf(null, "next"), cursors)
    assertEquals(listOf("one", "two"), model.state.value.items.map { it.id })
  }

  @Test fun `inbox failures do not retain sample rows`() = runTest(dispatcher) {
    val model = MessagesInboxViewModel(fakeInboxRepo { error("offline") })
    model.load("owner")
    advanceUntilIdle()
    assertTrue(model.state.value.items.isEmpty())
    assertEquals("Couldn’t load messages. Please try again.", model.state.value.error)
  }

  @Test fun `repository parses the current-person inbox response`() {
    val page = HavenMessagesRepository.parseInbox(
      """{"items":[{"id":"conv-1","other_user_id":"peer-1","display_name":"Ada Real","handle":"ada-real","avatar_url":null,"last_message_body":"Actual server text","last_message_at":"2026-11-02T10:44:00Z","is_unread":true}],"next_cursor":"cursor-2"}"""
    )
    assertEquals("Ada Real", page.items.single().displayName)
    assertEquals("Actual server text", page.items.single().lastMessageBody)
    assertTrue(page.items.single().isUnread)
    assertEquals("cursor-2", page.nextCursor)
  }
  @Test fun unreadFailureRetainsKnownIndicatorButNotFalseConfirmation() = runTest(dispatcher) {
    var fail = false
    val repo = object : MessagesInboxRepository {
      override suspend fun load(after: String?) = ConversationInboxPage(listOf(inbox("cached").copy(isUnread = true)), null)
      override suspend fun getGlobalUnread(): Boolean {
        if (fail) throw java.io.IOException("Offline")
        return true
      }
    }
    val model = MessagesInboxViewModel(repo)
    model.load("owner")
    advanceUntilIdle()
    assertTrue(model.state.value.globalUnread)
    assertTrue(model.state.value.unreadConfirmed)
    fail = true
    model.refreshUnread()
    advanceUntilIdle()
    assertTrue(model.state.value.globalUnread)
    assertEquals(false, model.state.value.unreadConfirmed)
  }

}
