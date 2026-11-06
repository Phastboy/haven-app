package com.example.data.messaging.transport

/**
 * Transport contract between the local Haven messaging repository and the live
 * Haven API.
 *
 * The production implementation talks only to server state. There is no
 * simulated fallback: a failed call is reported as a failure so the caller can
 * offer a retry instead of inventing a local success.
 */
interface MessagingTransport {

  /** Loads a page of conversation summaries. */
  suspend fun getConversations(cursor: String?): Result<ConversationInboxPageDto>

  /**
   * Fetches the newest page of a conversation thread.
   *
   * Pass [after] with a previous [ConversationThreadPageDto.nextCursor] to fetch
   * older messages.
   */
  suspend fun getThread(
    conversationId: String,
    after: String? = null
  ): Result<ConversationThreadPageDto>

  /** Appends a message to an existing conversation. */
  suspend fun sendMessage(
    conversationId: String,
    clientMessageId: String,
    text: String,
    offerId: String? = null
  ): Result<SendMessageAckDto>

  /** Starts or continues a conversation about a published offer. */
  suspend fun sendOfferInquiry(
    offerSlug: String,
    clientMessageId: String,
    text: String
  ): Result<SendMessageAckDto>

  /** Starts or continues a direct conversation with another person. */
  suspend fun sendProfileInquiry(
    handle: String,
    clientMessageId: String,
    text: String
  ): Result<SendMessageAckDto>
}
