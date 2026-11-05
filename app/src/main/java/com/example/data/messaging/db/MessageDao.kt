package com.example.data.messaging.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

  @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
  fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

  @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
  suspend fun getMessages(conversationId: String): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE clientMessageId = :clientMessageId LIMIT 1")
  suspend fun getMessageByClientId(clientMessageId: String): MessageEntity?

  @Query("SELECT * FROM messages WHERE serverId = :serverId LIMIT 1")
  suspend fun getMessageByServerId(serverId: String): MessageEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: MessageEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessages(messages: List<MessageEntity>)

  @Query("""
    UPDATE messages 
    SET status = :status,
        errorMessage = :errorMessage,
        serverId = COALESCE(:serverId, serverId),
        retryCount = retryCount + :retryIncrement
    WHERE clientMessageId = :clientMessageId
  """)
  suspend fun updateMessageStatus(
    clientMessageId: String,
    status: String,
    errorMessage: String? = null,
    serverId: String? = null,
    retryIncrement: Int = 0
  )

  @Query("DELETE FROM messages WHERE serverId = :serverId AND clientMessageId != :clientMessageId")
  suspend fun deleteDuplicateDelivery(serverId: String, clientMessageId: String)

  @Query("DELETE FROM messages WHERE clientMessageId = :clientMessageId")
  suspend fun deleteMessage(clientMessageId: String)

  @Query("DELETE FROM messages WHERE conversationId = :conversationId")
  suspend fun deleteMessagesForConversation(conversationId: String)
}
