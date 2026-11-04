package com.example.data.messaging.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks synchronization metadata (cursors, timestamps) per sync key.
 * e.g., "inbox_{ownerPersonId}" or "thread_{conversationId}".
 */
@Entity(tableName = "sync_states")
data class SyncStateEntity(
  @PrimaryKey
  val syncKey: String,
  val nextCursor: String?,
  val lastSyncedAt: Long = System.currentTimeMillis()
)
