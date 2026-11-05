package com.example.data.messaging.transport

import android.net.Uri
import com.example.auth.HavenAuthClient
import com.example.data.HavenMessagesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Production implementation of [MessagingTransport].
 *
 * Reads and writes the live Haven API only. Inbox parsing reuses the shared
 * paging contract; thread reads and sends map the native JSON routes.
 */
class HavenProductionMessagingTransport(
  private val auth: HavenAuthClient
) : MessagingTransport {

  override suspend fun getConversations(cursor: String?): Result<ConversationInboxPageDto> =
    withContext(Dispatchers.IO) {
      runCatching {
        val query = cursor?.let { "?after=${Uri.encode(it)}" }.orEmpty()
        val parsed = HavenMessagesRepository.parseInbox(
          auth.authenticatedGet("/api/v1/conversations$query")
        )
        ConversationInboxPageDto(
          items = parsed.items.map { item ->
            ConversationSummaryDto(
              id = item.id,
              otherUserId = item.otherUserId,
              displayName = item.displayName,
              handle = item.handle,
              avatarUrl = item.avatarUrl,
              lastMessageBody = item.lastMessageBody,
              lastMessageAt = item.lastMessageAt,
              isUnread = item.isUnread
            )
          },
          nextCursor = parsed.nextCursor
        )
      }
    }

  override suspend fun getThread(
    conversationId: String,
    after: String?
  ): Result<ConversationThreadPageDto> = withContext(Dispatchers.IO) {
    runCatching {
      val query = after?.let { "?after=${Uri.encode(it)}" }.orEmpty()
      parseThread(
        auth.authenticatedGet(
          "/api/v1/conversations/${Uri.encode(conversationId)}$query"
        )
      )
    }
  }

  override suspend fun sendMessage(
    conversationId: String,
    clientMessageId: String,
    text: String
  ): Result<SendMessageAckDto> = mutate(
    path = "/api/v1/conversations/${Uri.encode(conversationId)}/messages",
    text = text,
    clientMessageId = clientMessageId,
    fallbackConversationId = conversationId
  )

  override suspend fun sendOfferInquiry(
    offerSlug: String,
    clientMessageId: String,
    text: String
  ): Result<SendMessageAckDto> = mutate(
    path = "/api/v1/offers/${Uri.encode(offerSlug)}/inquiries",
    text = text,
    clientMessageId = clientMessageId,
    fallbackConversationId = ""
  )

  override suspend fun sendProfileInquiry(
    handle: String,
    clientMessageId: String,
    text: String
  ): Result<SendMessageAckDto> = mutate(
    path = "/api/v1/profiles/${Uri.encode(handle)}/inquiries",
    text = text,
    clientMessageId = clientMessageId,
    fallbackConversationId = ""
  )

  private suspend fun mutate(
    path: String,
    text: String,
    clientMessageId: String,
    fallbackConversationId: String
  ): Result<SendMessageAckDto> = withContext(Dispatchers.IO) {
    runCatching {
      val response = JSONObject(
        auth.authenticatedMutation("POST", path, JSONObject().put("body", text).put("client_message_id", clientMessageId.removePrefix("msg-")))
      )
      val now = System.currentTimeMillis()
      SendMessageAckDto(
        serverId = response.getString("message_id"),
        clientMessageId = clientMessageId,
        conversationId = response.optString("conversation_id")
          .takeIf { it.isNotBlank() }
          ?: fallbackConversationId,
        timestampIso = MessagingTime.formatIso(now),
        timestampMillis = now
      )
    }
  }

  private fun parseThread(body: String): ConversationThreadPageDto {
    val json = JSONObject(body)
    val conversationId = json.getString("conversation_id")
    val other = json.getJSONObject("other_person")
    val otherPerson = RemotePersonDto(
      id = other.getString("id"),
      handle = other.getString("handle"),
      displayName = other.getString("display_name"),
      avatarUrl = other.optionalString("avatar_url")
    )
    val messages = parseMessages(json.getJSONArray("messages"), conversationId, otherPerson.id)
    return ConversationThreadPageDto(
      conversationId = conversationId,
      otherPerson = otherPerson,
      messages = messages,
      nextCursor = json.optionalString("next_cursor"),
      isUnread = json.optBoolean("is_unread", false)
    )
  }

  private fun parseMessages(
    array: JSONArray,
    conversationId: String,
    otherPersonId: String
  ): List<RemoteMessageDto> = List(array.length()) { index ->
    val item = array.getJSONObject(index)
    val createdAt = item.getString("created_at")
    val senderId = item.getString("sender_id")
    val offer = if (item.isNull("offer")) null else item.getJSONObject("offer")
    RemoteMessageDto(
      serverId = item.getString("id"),
      clientMessageId = item.getString("id"),
      conversationId = conversationId,
      senderPersonId = senderId,
      isFromMe = senderId != otherPersonId,
      text = item.getString("body"),
      timestampIso = createdAt,
      timestampMillis = MessagingTime.parseIsoToMillis(createdAt),
      attachedOfferSlug = offer?.optionalString("slug"),
      attachedOfferTitle = offer?.optionalString("title")
    )
  }
}

private fun JSONObject.optionalString(key: String): String? =
  if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
