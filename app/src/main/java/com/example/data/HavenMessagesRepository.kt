package com.example.data

import android.net.Uri
import com.example.auth.HavenAuthClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class InboxConversation(
  val id: String,
  val otherUserId: String,
  val displayName: String,
  val handle: String,
  val avatarUrl: String?,
  val lastMessageBody: String,
  val lastMessageAt: String,
  val isUnread: Boolean
)

data class ConversationInboxPage(val items: List<InboxConversation>, val nextCursor: String?)

fun interface MessagesInboxRepository {
  suspend fun load(after: String?): ConversationInboxPage
}

class HavenMessagesRepository(private val auth: HavenAuthClient) : MessagesInboxRepository {
  override suspend fun load(after: String?): ConversationInboxPage = withContext(Dispatchers.IO) {
    val query = after?.let { "?after=${Uri.encode(it)}" }.orEmpty()
    parseInbox(auth.authenticatedGet("/api/v1/conversations$query"))
  }

  companion object {
    fun parseInbox(body: String): ConversationInboxPage {
      val json = JSONObject(body)
      val items = json.getJSONArray("items")
      return ConversationInboxPage(
        items = List(items.length()) { index ->
          val item = items.getJSONObject(index)
          InboxConversation(
            id = item.getString("id"),
            otherUserId = item.getString("other_user_id"),
            displayName = item.getString("display_name"),
            handle = item.getString("handle"),
            avatarUrl = if (item.isNull("avatar_url")) null else item.getString("avatar_url"),
            lastMessageBody = item.getString("last_message_body"),
            lastMessageAt = item.getString("last_message_at"),
            isUnread = item.getBoolean("is_unread")
          )
        },
        nextCursor = if (json.isNull("next_cursor")) null else json.getString("next_cursor")
      )
    }
  }
}
