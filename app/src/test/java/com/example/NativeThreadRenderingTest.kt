package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.example.data.messaging.db.ConversationEntity
import com.example.data.messaging.db.MessageDeliveryStatus
import com.example.data.messaging.db.MessageEntity
import com.example.ui.model.ConversationThreadUiState
import com.example.ui.model.ThreadContentState
import com.example.ui.screens.NativeConversationThreadScreen
import com.example.ui.theme.HavenTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp")
class NativeThreadRenderingTest {
  @get:Rule val rule = createComposeRule()

  private fun conversation(handle: String = "@kofi") = ConversationEntity(
    id = "conversation", ownerPersonId = "owner", otherUserId = "peer",
    displayName = "Kofi Mensah", handle = handle, avatarUrl = null,
    lastMessageBody = "Ready for the swap?", lastMessageAt = "2026-11-04T12:00:00Z",
    lastMessageTimestamp = 1700000000000L, isUnread = true
  )

  private fun incoming(count: Int, startIndex: Int = 0) = List(count) { offset ->
    val index = startIndex + offset
    MessageEntity(
      clientMessageId = "message-$index", conversationId = "conversation", senderPersonId = "peer",
      isFromMe = false, text = "Message $index", timestamp = index.toLong()
    )
  }

  private fun older(count: Int) = List(count) { index ->
    MessageEntity(
      clientMessageId = "older-$index", conversationId = "conversation", senderPersonId = "peer",
      isFromMe = false, text = "Older $index", timestamp = -100L + index
    )
  }

  private fun setContentWith(state: ConversationThreadUiState, onRefresh: () -> Unit = {}) {
    rule.setContent {
      HavenTheme {
        NativeConversationThreadScreen(
          state = state,
          onBack = {}, onInputChanged = {}, onSendMessage = {}, onRetryMessage = {},
          onOpenProfile = {}, onRefresh = onRefresh
        )
      }
    }
  }

  @Test fun historyControlHasOneStableKeyWhenTheThreadScrolls() {
    val messages = List(20) { index -> MessageEntity(
      clientMessageId = "message-$index", conversationId = "conversation", senderPersonId = "peer",
      isFromMe = false, text = "Message $index", timestamp = index.toLong()
    ) }
    rule.setContent {
      HavenTheme {
        NativeConversationThreadScreen(
          state = ConversationThreadUiState(messages = messages, hasOlder = true),
          onBack = {}, onInputChanged = {}, onSendMessage = {}, onRetryMessage = {}
        )
      }
    }
    rule.onNodeWithTag("thread_messages_list").performScrollToIndex(0)
    rule.onAllNodesWithText("Earlier messages").assertCountEquals(1)
    rule.onNodeWithTag("thread_messages_list").performScrollToIndex(21)
    rule.waitForIdle()
  }

  @Test fun unresolvedLocalCacheRendersBubbleSkeletonsWithoutHistoryControlOrEmptyState() {
    assertEquals(
      ThreadContentState.RESOLVING_CACHE,
      ConversationThreadUiState().contentState
    )
    setContentWith(ConversationThreadUiState())
    rule.onAllNodesWithTag("message_bubble_skeleton").assertCountEquals(6)
    rule.onAllNodesWithText("Earlier messages").assertCountEquals(0)
    rule.onAllNodesWithText("No messages yet").assertCountEquals(0)
    rule.onNodeWithTag("message_input_field").assertExists()
    rule.onNodeWithTag("thread_back_button").assertExists()
  }

  @Test fun remoteLoadingWithNoCachedHistoryRendersBubbleSkeletons() {
    setContentWith(ConversationThreadUiState(localHistoryResolved = true, isSyncing = true))
    rule.onAllNodesWithTag("message_bubble_skeleton").assertCountEquals(6)
    rule.onAllNodesWithText("Earlier messages").assertCountEquals(0)
    rule.onNodeWithTag("message_input_field").assertExists()
  }

  @Test fun confirmedEmptyThreadShowsEmptyStateInsteadOfSkeletonsOrBlankList() {
    setContentWith(ConversationThreadUiState(localHistoryResolved = true, emptyThreadConfirmed = true))
    rule.onNodeWithText("No messages yet").assertExists()
    rule.onAllNodesWithTag("message_bubble_skeleton").assertCountEquals(0)
    rule.onAllNodesWithText("Earlier messages").assertCountEquals(0)
    rule.onNodeWithTag("message_input_field").assertExists()
  }

  @Test fun unavailableThreadShowsHonestFailureStateWithWorkingRetry() {
    var refreshed = false
    setContentWith(
      ConversationThreadUiState(localHistoryResolved = true, syncError = "Couldn't load messages"),
      onRefresh = { refreshed = true }
    )
    rule.onNodeWithText("Couldn't load messages").assertExists()
    rule.onAllNodesWithTag("message_bubble_skeleton").assertCountEquals(0)
    rule.onNodeWithTag("thread_unavailable_state_primary").performClick()
    assertTrue(refreshed)
  }

  @Test fun retainedMessagesAndProfileActionStayVisibleDuringOrdinaryRefresh() {
    setContentWith(
      ConversationThreadUiState(
        conversation = conversation(),
        messages = incoming(3),
        isSyncing = true
      )
    )
    rule.onNodeWithTag("message_bubble_message-2").assertExists()
    rule.onAllNodesWithTag("message_bubble_skeleton").assertCountEquals(0)
    rule.onNodeWithText("Profile").assertExists()
    rule.onAllNodesWithText("Offline - viewing cached messages").assertCountEquals(0)
  }

  @Test fun sendQueueFailureShowsBannerWithoutStealingTheUnavailableRetry() {
    setContentWith(
      ConversationThreadUiState(
        conversation = conversation(),
        messages = incoming(3),
        sendError = "Couldn’t queue your message. Please try again."
      )
    )
    rule.onNodeWithText("Couldn’t queue your message. Please try again.").assertExists()
    rule.onNodeWithTag("thread_unavailable_state_primary").assertDoesNotExist()
  }

  @Test fun initialContentScrollsToTheLatestMessage() {
    setContentWith(ConversationThreadUiState(messages = incoming(30)))
    rule.waitForIdle()
    rule.onNodeWithText("Message 29").assertIsDisplayed()
  }

  @Test fun appendedIncomingMessageDoesNotStealScrollWhileReadingHistory() {
    var uiState by mutableStateOf(ConversationThreadUiState(messages = incoming(30)))
    setContentWith(uiState)
    rule.waitForIdle()
    rule.onNodeWithTag("thread_messages_list").performScrollToIndex(0)
    rule.waitForIdle()
    uiState = uiState.copy(messages = incoming(31))
    rule.waitForIdle()
    rule.onNodeWithText("Message 30").assertDoesNotExist()
    rule.onNodeWithText("Message 0").assertIsDisplayed()
  }

  @Test fun appendedOwnSendingMessageScrollsIntoView() {
    var uiState by mutableStateOf(ConversationThreadUiState(messages = incoming(30)))
    setContentWith(uiState)
    rule.waitForIdle()
    rule.onNodeWithTag("thread_messages_list").performScrollToIndex(0)
    rule.waitForIdle()
    uiState = uiState.copy(
      messages = uiState.messages + MessageEntity(
        clientMessageId = "message-30", conversationId = "conversation", senderPersonId = "owner",
        isFromMe = true, text = "Just sent", timestamp = 30L,
        status = MessageDeliveryStatus.SENDING
      )
    )
    rule.waitForIdle()
    rule.onNodeWithText("Just sent").assertIsDisplayed()
  }

  @Test fun prependingOlderMessagesKeepsTheReadingAnchor() {
    var uiState by mutableStateOf(ConversationThreadUiState(messages = incoming(30)))
    setContentWith(uiState)
    rule.waitForIdle()
    rule.onNodeWithTag("thread_messages_list").performScrollToIndex(6)
    rule.waitForIdle()
    uiState = uiState.copy(messages = older(5) + uiState.messages)
    rule.waitForIdle()
    rule.onNodeWithText("Message 5").assertIsDisplayed()
    rule.onNodeWithText("Older 0").assertDoesNotExist()
  }
}
