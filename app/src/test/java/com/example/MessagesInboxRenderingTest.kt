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
import com.example.data.InboxConversation
import com.example.ui.model.InboxContentState
import com.example.ui.model.MessagesInboxUiState
import com.example.ui.screens.MessagesInboxScreen
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
class MessagesInboxRenderingTest {
  @get:Rule val rule = createComposeRule()

  private fun conversations(count: Int, startIndex: Int = 0) = List(count) { offset ->
    val index = startIndex + offset
    InboxConversation(
      "conversation-$index", "person-$index", "Conversation $index", "@conversation-$index",
      null, "Latest message $index", "2026-11-04T12:00:00Z", false
    )
  }

  private fun setContentWith(state: MessagesInboxUiState, onRetry: () -> Unit = {}) {
    rule.setContent {
      HavenTheme {
        MessagesInboxScreen(state = state, onRetry = onRetry, onLoadMore = {}, onSelectConversation = {})
      }
    }
  }

  @Test fun unresolvedLocalCacheRendersRowSkeletonsWithoutSpinnerOrEmptyState() {
    assertEquals(
      InboxContentState.RESOLVING_CACHE,
      MessagesInboxUiState().contentState
    )
    setContentWith(MessagesInboxUiState())
    rule.onAllNodesWithTag("conversation_row_skeleton").assertCountEquals(8)
    rule.onNodeWithText("Messages").assertIsDisplayed()
    rule.onAllNodesWithText("No messages yet").assertCountEquals(0)
    rule.onNodeWithTag("messages_loading").assertDoesNotExist()
    rule.onNodeWithTag("messages_list").assertDoesNotExist()
  }

  @Test fun remoteLoadingWithNoCachedRowsRendersRowSkeletons() {
    setContentWith(MessagesInboxUiState(localHistoryResolved = true, loading = true))
    rule.onAllNodesWithTag("conversation_row_skeleton").assertCountEquals(8)
    rule.onNodeWithText("Messages").assertIsDisplayed()
    rule.onAllNodesWithText("No messages yet").assertCountEquals(0)
  }

  @Test fun confirmedEmptyInboxShowsEmptyStateInsteadOfSkeletons() {
    setContentWith(MessagesInboxUiState(localHistoryResolved = true, emptyInboxConfirmed = true))
    rule.onNodeWithText("No messages yet").assertExists()
    rule.onAllNodesWithTag("conversation_row_skeleton").assertCountEquals(0)
    rule.onNodeWithTag("messages_empty_state").assertExists()
  }

  @Test fun unavailableInboxShowsErrorWithWorkingRetry() {
    var retried = false
    setContentWith(
      MessagesInboxUiState(localHistoryResolved = true, error = "Couldn’t load messages. Please try again."),
      onRetry = { retried = true }
    )
    rule.onNodeWithText("Couldn’t load messages. Please try again.").assertExists()
    rule.onAllNodesWithTag("conversation_row_skeleton").assertCountEquals(0)
    rule.onNodeWithTag("messages_retry").performClick()
    assertTrue(retried)
  }

  @Test fun retainedRowsStayVisibleDuringRefreshWithoutSkeletonsOrSpinner() {
    setContentWith(MessagesInboxUiState(items = conversations(3), localHistoryResolved = true, loading = true))
    rule.onNodeWithTag("conversation_conversation-2").assertExists()
    rule.onAllNodesWithTag("conversation_row_skeleton").assertCountEquals(0)
    rule.onNodeWithText("Messages").assertIsDisplayed()
    rule.onNodeWithTag("messages_loading").assertDoesNotExist()
    rule.onAllNodesWithText("Offline - viewing cached messages").assertCountEquals(0)
  }

  @Test fun offlineBannerShowsWithRetainedRowsWithoutStealingTheRetry() {
    setContentWith(
      MessagesInboxUiState(
        items = conversations(3),
        localHistoryResolved = true,
        offlineBanner = "Offline - viewing cached messages"
      )
    )
    rule.onNodeWithText("Offline - viewing cached messages").assertExists()
    rule.onNodeWithTag("conversation_conversation-0").assertExists()
    rule.onNodeWithTag("messages_retry").assertDoesNotExist()
  }

  @Test fun paginationKeepsItsOwnSpinnerAndInlineError() {
    var loadedMore = false
    var uiState by mutableStateOf(
      MessagesInboxUiState(
        items = conversations(3),
        localHistoryResolved = true,
        nextCursor = "next",
        loadingMore = true
      )
    )
    rule.setContent {
      HavenTheme {
        MessagesInboxScreen(
          state = uiState,
          onRetry = {},
          onLoadMore = { loadedMore = true },
          onSelectConversation = {}
        )
      }
    }
    rule.onNodeWithTag("messages_load_more").assertExists()
    uiState = uiState.copy(loadingMore = false, error = "Couldn’t load more messages. Please try again.")
    rule.waitForIdle()
    rule.onNodeWithText("Couldn’t load more messages. Please try again.").assertExists()
    rule.onNodeWithTag("messages_load_more").performClick()
    assertTrue(loadedMore)
  }

  @Test fun newArrivalIsVisibleWhileParkedAtTheTopOfTheInbox() {
    var uiState by mutableStateOf(MessagesInboxUiState(items = conversations(30), localHistoryResolved = true))
    setContentWith(uiState)
    rule.waitForIdle()
    rule.onNodeWithTag("messages_list").performScrollToIndex(0)
    rule.waitForIdle()
    uiState = uiState.copy(
      items = listOf(
        InboxConversation(
          "conversation-30", "person-30", "Fresh conversation", "@fresh",
          null, "A brand-new message", "2026-11-05T12:00:00Z", true
        )
      ) + uiState.items
    )
    rule.waitForIdle()
    rule.onNodeWithText("Fresh conversation").assertIsDisplayed()
  }

  @Test fun newArrivalDoesNotStealScrollWhileReadingDeeperRows() {
    var uiState by mutableStateOf(MessagesInboxUiState(items = conversations(30), localHistoryResolved = true))
    setContentWith(uiState)
    rule.waitForIdle()
    rule.onNodeWithTag("messages_list").performScrollToIndex(10)
    rule.waitForIdle()
    uiState = uiState.copy(
      items = listOf(
        InboxConversation(
          "conversation-30", "person-30", "Fresh conversation", "@fresh",
          null, "A brand-new message", "2026-11-05T12:00:00Z", true
        )
      ) + uiState.items
    )
    rule.waitForIdle()
    rule.onNodeWithText("Conversation 10").assertIsDisplayed()
    rule.onNodeWithText("Fresh conversation").assertDoesNotExist()
  }
}
