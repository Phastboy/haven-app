package com.example.data.messaging.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistent outbox for queued/in-flight outgoing messages.
 * Ensures optimistic writes survive app restarts and are delivered reliably.
 */
@Entity(
  tableName = "message_outbox",
  indices = [
    Index(value = ["createdAt"])
  ]
)
data class OutboxEntity(
  @PrimaryKey
  val clientMessageId: String,
  val conversationId: String,
  val ownerPersonId: String? = null,
  val text: String,
  val attachedOfferSlug: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val retryCount: Int = 0,
  val lastAttemptAt: Long = 0L,
  val status: String = "PENDING" // PENDING, IN_FLIGHT, FAILED
)
