package com.example.data.messaging.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {

  @Query("SELECT * FROM message_outbox WHERE status IN ('PENDING', 'UNCONFIRMED') ORDER BY createdAt ASC")
  suspend fun getPendingOutbox(): List<OutboxEntity>

  @Query("SELECT COUNT(*) FROM message_outbox")
  fun observePendingCount(): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(entry: OutboxEntity)

  @Query("""
    UPDATE message_outbox 
    SET status = :status,
        retryCount = retryCount + 1,
        lastAttemptAt = :attemptTime
    WHERE clientMessageId = :clientMessageId
  """)
  suspend fun markAttempt(
    clientMessageId: String,
    status: String,
    attemptTime: Long = System.currentTimeMillis()
  )

  @Query("DELETE FROM message_outbox WHERE clientMessageId = :clientMessageId")
  suspend fun delete(clientMessageId: String)

  @Query("DELETE FROM message_outbox WHERE conversationId = :conversationId")
  suspend fun deleteForConversation(conversationId: String)
}
