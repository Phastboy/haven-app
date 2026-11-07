package com.example

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import com.example.data.messaging.db.MessageEntity
import com.example.ui.model.ConversationThreadUiState
import com.example.ui.screens.NativeConversationThreadScreen
import com.example.ui.theme.HavenTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp")
class NativeThreadRenderingTest {
  @get:Rule val rule = createComposeRule()

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
}
