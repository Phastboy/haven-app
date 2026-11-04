package com.example.data.messaging.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted conversation summary for the Messages Inbox.
 * Single source of truth for the inbox list.
 *
 * Scoped by [ownerPersonId] to guarantee user account isolation.
 */
@Entity(
  tableName = "conversations",
  indices = [
    Index(value = ["ownerPersonId", "lastMessageTimestamp"]),
    Index(value = ["ownerPersonId", "id"], unique = true)
  ]
)
data class ConversationEntity(
  @PrimaryKey
  val id: String,
  val ownerPersonId: String,
  val otherUserId: String,
  val displayName: String,
  val handle: String,
  val avatarUrl: String?,
  val lastMessageBody: String,
  val lastMessageAt: String,
  val lastMessageTimestamp: Long,
  val isUnread: Boolean,
  val unreadCount: Int = if (isUnread) 1 else 0,
  val updatedAt: Long = System.currentTimeMillis()
)
