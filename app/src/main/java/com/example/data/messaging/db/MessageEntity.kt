package com.example.data.messaging.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Message delivery status.
 */
object MessageDeliveryStatus {
  const val SENDING = "SENDING"
  const val SENT = "SENT"
  const val FAILED = "FAILED"
}

/**
 * Persisted message record inside a conversation thread.
 * Single source of truth for conversation views.
 */
@Entity(
  tableName = "messages",
  indices = [
    Index(value = ["conversationId", "timestamp"]),
    Index(value = ["serverId"], unique = false)
  ]
)
data class MessageEntity(
  @PrimaryKey
  val clientMessageId: String,
  val conversationId: String,
  val serverId: String? = null,
  val senderPersonId: String,
  val isFromMe: Boolean,
  val text: String,
  val timestamp: Long,
  val formattedTime: String = "",
  val status: String = MessageDeliveryStatus.SENT,
  val errorMessage: String? = null,
  val retryCount: Int = 0,
  val attachedOfferSlug: String? = null,
  val attachedOfferTitle: String? = null,
  val attachedOfferPrice: String? = null,
  val attachedOfferImageResId: Int? = null
)
