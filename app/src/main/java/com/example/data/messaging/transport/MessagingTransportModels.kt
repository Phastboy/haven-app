package com.example.data.messaging.transport

/**
 * Transport data transfer object for inbox conversation summaries.
 */
data class ConversationSummaryDto(
  val id: String,
  val otherUserId: String,
  val displayName: String,
  val handle: String,
  val avatarUrl: String?,
  val lastMessageBody: String,
  val lastMessageAt: String,
  val isUnread: Boolean
)

/**
 * Page response for conversations inbox.
 */
data class ConversationInboxPageDto(
  val items: List<ConversationSummaryDto>,
  val nextCursor: String?
)

/**
 * Remote message data model.
 */
data class RemoteMessageDto(
  val serverId: String,
  val clientMessageId: String,
  val conversationId: String,
  val senderPersonId: String,
  val isFromMe: Boolean,
  val text: String,
  val timestampIso: String,
  val timestampMillis: Long,
  val attachedOfferSlug: String? = null,
  val attachedOfferTitle: String? = null,
  val attachedOfferPrice: String? = null
)

/**
 * Server acknowledgment returned after a message is successfully delivered.
 */
data class SendMessageAckDto(
  val serverId: String,
  val clientMessageId: String,
  val conversationId: String,
  val timestampIso: String,
  val timestampMillis: Long
)

/**
 * Realtime messaging events stream.
 */
sealed interface MessagingEvent {
  data class NewMessageReceived(val message: RemoteMessageDto) : MessagingEvent
  data class ConversationUpdated(val conversation: ConversationSummaryDto) : MessagingEvent
  data class MessageDeliveryAck(val ack: SendMessageAckDto) : MessagingEvent
}
