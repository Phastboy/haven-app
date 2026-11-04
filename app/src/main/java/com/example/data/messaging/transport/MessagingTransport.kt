package com.example.data.messaging.transport

import kotlinx.coroutines.flow.Flow

/**
 * Transport contract defining communications between the local Haven messaging repository
 * and remote services.
 *
 * Designed to cleanly swap between mock transport, staging, and future production
 * REST/WebSocket endpoints without altering Room schemas, ViewModels, or Composables.
 */
interface MessagingTransport {

  /**
   * Loads a page of conversation summaries.
   */
  suspend fun getConversations(cursor: String?): Result<ConversationInboxPageDto>

  /**
   * Fetches historical messages for an individual conversation thread.
   */
  suspend fun getThreadHistory(
    conversationId: String,
    beforeTimestamp: Long? = null,
    limit: Int = 50
  ): Result<List<RemoteMessageDto>>

  /**
   * Dispatches an outgoing message with an idempotency key ([clientMessageId]).
   */
  suspend fun sendMessage(
    conversationId: String,
    clientMessageId: String,
    text: String,
    attachedOfferSlug: String? = null
  ): Result<SendMessageAckDto>

  /**
   * Observes live incoming messages and thread events (via WebSocket / SSE / simulated stream).
   */
  fun observeEvents(): Flow<MessagingEvent>
}
