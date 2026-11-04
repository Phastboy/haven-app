package com.example.data.messaging.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

  @Query("SELECT * FROM conversations WHERE ownerPersonId = :ownerPersonId ORDER BY lastMessageTimestamp DESC")
  fun observeConversations(ownerPersonId: String): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations WHERE ownerPersonId = :ownerPersonId ORDER BY lastMessageTimestamp DESC")
  suspend fun getConversations(ownerPersonId: String): List<ConversationEntity>

  @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
  suspend fun getConversation(id: String): ConversationEntity?

  @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
  fun observeConversation(id: String): Flow<ConversationEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertConversations(conversations: List<ConversationEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertConversation(conversation: ConversationEntity)

  @Query("""
    UPDATE conversations 
    SET lastMessageBody = :lastMessageBody,
        lastMessageAt = :lastMessageAt,
        lastMessageTimestamp = :lastMessageTimestamp,
        isUnread = :isUnread,
        updatedAt = :updatedAt
    WHERE id = :id
  """)
  suspend fun updateLastMessage(
    id: String,
    lastMessageBody: String,
    lastMessageAt: String,
    lastMessageTimestamp: Long,
    isUnread: Boolean,
    updatedAt: Long = System.currentTimeMillis()
  )

  @Query("UPDATE conversations SET isUnread = 0 WHERE id = :id")
  suspend fun markAsRead(id: String)

  @Query("DELETE FROM conversations WHERE ownerPersonId = :ownerPersonId")
  suspend fun deleteForOwner(ownerPersonId: String)

  @Query("DELETE FROM conversations WHERE id = :id")
  suspend fun deleteConversation(id: String)
}
