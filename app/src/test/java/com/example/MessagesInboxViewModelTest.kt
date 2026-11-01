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

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesInboxViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Before fun setup() { Dispatchers.setMain(dispatcher) }
  @After fun cleanup() { Dispatchers.resetMain() }

  private fun inbox(id: String) = InboxConversation(
    id, "person-$id", "Real participant", "real-participant", null,
    "A real message from Haven", "2026-11-02T10:44:00Z", true
  )

  @Test fun `inbox only displays repository data and clears it when account leaves`() = runTest(dispatcher) {
    val model = MessagesInboxViewModel(MessagesInboxRepository { ConversationInboxPage(listOf(inbox("one")), null) })
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
    val model = MessagesInboxViewModel(MessagesInboxRepository { cursor ->
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
    val model = MessagesInboxViewModel(MessagesInboxRepository { error("offline") })
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
}
