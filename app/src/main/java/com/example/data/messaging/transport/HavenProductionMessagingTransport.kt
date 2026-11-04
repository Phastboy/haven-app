package com.example.data.messaging.transport

import android.net.Uri
import com.example.auth.HavenAuthClient
import com.example.data.HavenMessagesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Production implementation of [MessagingTransport].
 *
 * Integrates directly with the live Haven backend (`GET /api/v1/conversations`)
 * while delegating pending thread history and sending operations to a robust
 * transport simulator until the Haven backend deploys the native message routes.
 */
class HavenProductionMessagingTransport(
  private val auth: HavenAuthClient,
  private val fallbackSimulator: MockMessagingTransport = MockMessagingTransport(simulatedDelayMs = 250L)
) : MessagingTransport {

  override suspend fun getConversations(cursor: String?): Result<ConversationInboxPageDto> = withContext(Dispatchers.IO) {
    try {
      val query = cursor?.let { "?after=${Uri.encode(it)}" }.orEmpty()
      val responseBody = auth.authenticatedGet("/api/v1/conversations$query")
      val parsed = HavenMessagesRepository.parseInbox(responseBody)
      val dtos = parsed.items.map { item ->
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
      }
      Result.success(ConversationInboxPageDto(items = dtos, nextCursor = parsed.nextCursor))
    } catch (e: Exception) {
      // If server unreachable or error, let caller handle with Room cache fallback
      Result.failure(e)
    }
  }

  override suspend fun getThreadHistory(
    conversationId: String,
    beforeTimestamp: Long?,
    limit: Int
  ): Result<List<RemoteMessageDto>> {
    // Native thread history route (/api/v1/conversations/{id}/messages) is not yet exposed by backend
    return fallbackSimulator.getThreadHistory(conversationId, beforeTimestamp, limit)
  }

  override suspend fun sendMessage(
    conversationId: String,
    clientMessageId: String,
    text: String,
    attachedOfferSlug: String?
  ): Result<SendMessageAckDto> {
    // Native message dispatch route (POST /api/v1/conversations/{id}/messages) is not yet exposed by backend
    return fallbackSimulator.sendMessage(conversationId, clientMessageId, text, attachedOfferSlug)
  }

  override fun observeEvents(): Flow<MessagingEvent> {
    return fallbackSimulator.observeEvents()
  }
}
