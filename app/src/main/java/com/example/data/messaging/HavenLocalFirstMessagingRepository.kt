package com.example.data.messaging

import com.example.data.messaging.db.ConversationDao
import com.example.data.messaging.db.ConversationEntity
import com.example.data.messaging.db.HavenDatabase
import com.example.data.messaging.db.MessageDao
import com.example.data.messaging.db.MessageDeliveryStatus
import com.example.data.messaging.db.MessageEntity
import com.example.data.messaging.db.OutboxDao
import com.example.data.messaging.db.OutboxEntity
import com.example.data.messaging.db.SyncStateDao
import com.example.data.messaging.db.SyncStateEntity
import com.example.data.messaging.transport.ConversationSummaryDto
import com.example.data.messaging.transport.ConversationThreadPageDto
import com.example.data.messaging.transport.MessagingTime
import com.example.data.messaging.transport.MessagingTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Interface defining the messaging repository contract.
 */
interface MessagingRepository {
  fun observeConversations(ownerPersonId: String): Flow<List<ConversationEntity>>
  fun observeMessages(conversationId: String): Flow<List<MessageEntity>>
  fun observeConversation(conversationId: String): Flow<ConversationEntity?>
  suspend fun refreshConversations(ownerPersonId: String): Result<Unit>
  suspend fun loadMoreConversations(ownerPersonId: String): Result<Boolean>
  suspend fun syncThreadHistory(conversationId: String, ownerPersonId: String? = null): Result<Unit>
  suspend fun sendMessage(
    conversationId: String,
    text: String,
    senderPersonId: String = "me",
    attachedOfferSlug: String? = null,
    attachedOfferTitle: String? = null,
    attachedOfferPrice: String? = null,
    attachedOfferImageResId: Int? = null
  ): String
  suspend fun startOfferInquiry(offerSlug: String, text: String): Result<String>
  suspend fun startProfileInquiry(handle: String, text: String): Result<String>
  suspend fun retryMessage(clientMessageId: String): Result<Unit>
  suspend fun markConversationRead(conversationId: String)
  suspend fun clearForOwner(ownerPersonId: String)
}

/**
 * Local-First single source of truth for Haven messaging.
 * Uses Room for persistent storage and reactive flows, with optimistic writes,
 * outbox retry queue, and background transport synchronization.
 */
class HavenLocalFirstMessagingRepository(
  private val database: HavenDatabase,
  private val transport: MessagingTransport,
  private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
  private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) : MessagingRepository {

  private val conversationDao: ConversationDao = database.conversationDao()
  private val messageDao: MessageDao = database.messageDao()
  private val outboxDao: OutboxDao = database.outboxDao()
  private val syncStateDao: SyncStateDao = database.syncStateDao()

  private val outboxMutex = Mutex()

  init {
    // Deliver messages that were never attempted (for example the app stopped
    // mid-flight). FAILED rows stay put until the person retries explicitly.
    coroutineScope.launch {
      processOutbox()
    }
  }

  override fun observeConversations(ownerPersonId: String): Flow<List<ConversationEntity>> {
    return conversationDao.observeConversations(ownerPersonId).distinctUntilChanged()
  }

  override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> {
    return messageDao.observeMessages(conversationId).distinctUntilChanged()
  }

  override fun observeConversation(conversationId: String): Flow<ConversationEntity?> {
    return conversationDao.observeConversation(conversationId).distinctUntilChanged()
  }

  override suspend fun refreshConversations(ownerPersonId: String): Result<Unit> = withContext(ioDispatcher) {
    val result = transport.getConversations(cursor = null)
    if (result.isSuccess) {
      val page = result.getOrThrow()
      val entities = page.items.map { dto -> dto.toEntity(ownerPersonId) }
      conversationDao.upsertConversations(entities)
      syncStateDao.upsertSyncState(
        SyncStateEntity(
          syncKey = "inbox_$ownerPersonId",
          nextCursor = page.nextCursor,
          lastSyncedAt = System.currentTimeMillis()
        )
      )
      Result.success(Unit)
    } else {
      Result.failure(result.exceptionOrNull() ?: RuntimeException("Failed to refresh conversations"))
    }
  }

  override suspend fun loadMoreConversations(ownerPersonId: String): Result<Boolean> = withContext(ioDispatcher) {
    val syncKey = "inbox_$ownerPersonId"
    val syncState = syncStateDao.getSyncState(syncKey)
    val cursor = syncState?.nextCursor ?: return@withContext Result.success(false)

    val result = transport.getConversations(cursor)
    if (result.isSuccess) {
      val page = result.getOrThrow()
      val entities = page.items.map { dto -> dto.toEntity(ownerPersonId) }
      conversationDao.upsertConversations(entities)
      syncStateDao.upsertSyncState(
        SyncStateEntity(
          syncKey = syncKey,
          nextCursor = page.nextCursor,
          lastSyncedAt = System.currentTimeMillis()
        )
      )
      Result.success(page.nextCursor != null)
    } else {
      Result.failure(result.exceptionOrNull() ?: RuntimeException("Failed to load more conversations"))
    }
  }

  override suspend fun syncThreadHistory(
    conversationId: String,
    ownerPersonId: String?
  ): Result<Unit> = withContext(ioDispatcher) {
    val result = transport.getThread(conversationId)
    if (result.isFailure) {
      return@withContext Result.failure(
        result.exceptionOrNull() ?: RuntimeException("Failed to sync thread")
      )
    }

    val page = result.getOrThrow()
    val existing = messageDao.getMessages(conversationId)
    // Preserve any local messages currently in SENDING or FAILED state.
    val localSendingOrFailed = existing.filter {
      it.status == MessageDeliveryStatus.SENDING || it.status == MessageDeliveryStatus.FAILED
    }.associateBy { it.clientMessageId }

    val entitiesToInsert = page.messages.map { remote ->
      // If this matches an existing local client ID, keep clientMessageId.
      val existingMatch = existing.firstOrNull {
        it.serverId == remote.serverId || it.clientMessageId == remote.clientMessageId
      }
      val clientId = existingMatch?.clientMessageId ?: remote.clientMessageId
      MessageEntity(
        clientMessageId = clientId,
        conversationId = remote.conversationId,
        serverId = remote.serverId,
        senderPersonId = remote.senderPersonId,
        isFromMe = remote.isFromMe,
        text = remote.text,
        timestamp = remote.timestampMillis,
        formattedTime = formatTimeLabel(remote.timestampMillis),
        status = MessageDeliveryStatus.SENT,
        errorMessage = null,
        attachedOfferSlug = remote.attachedOfferSlug,
        attachedOfferTitle = remote.attachedOfferTitle,
        attachedOfferPrice = remote.attachedOfferPrice
      )
    }

    // Reconcile and save.
    messageDao.insertMessages(entitiesToInsert)

    // Re-insert unacknowledged local messages so a server fetch never wipes them out.
    if (localSendingOrFailed.isNotEmpty()) {
      messageDao.insertMessages(localSendingOrFailed.values.toList())
    }

    upsertConversationFromThread(conversationId, ownerPersonId, page, existing)
    Result.success(Unit)
  }

  /**
   * Keeps the inbox summary in sync with the thread the person just opened.
   *
   * The owner comes from the caller when known; otherwise an existing local row
   * supplies it. Without an owner the summary is skipped rather than guessed.
   */
  private suspend fun upsertConversationFromThread(
    conversationId: String,
    ownerPersonId: String?,
    page: ConversationThreadPageDto,
    existing: List<MessageEntity>
  ) {
    val current = conversationDao.getConversation(conversationId)
    val owner = ownerPersonId ?: current?.ownerPersonId ?: return
    val lastRemote = page.messages.lastOrNull() ?: return

    // Do not move the summary backwards if a newer optimistic send is pending.
    val pendingNewest = existing.maxOfOrNull { it.timestamp } ?: 0L
    val keepLocal = current != null &&
      current.lastMessageTimestamp > lastRemote.timestampMillis &&
      current.lastMessageTimestamp >= pendingNewest
    val summaryBody = if (keepLocal) current?.lastMessageBody ?: lastRemote.text else lastRemote.text
    val summaryAt = if (keepLocal) current?.lastMessageAt ?: lastRemote.timestampIso else lastRemote.timestampIso
    val summaryTimestamp =
      if (keepLocal) current?.lastMessageTimestamp ?: lastRemote.timestampMillis else lastRemote.timestampMillis

    conversationDao.upsertConversation(
      ConversationEntity(
        id = conversationId,
        ownerPersonId = owner,
        otherUserId = page.otherPerson.id,
        displayName = page.otherPerson.displayName,
        handle = page.otherPerson.handle,
        avatarUrl = page.otherPerson.avatarUrl,
        lastMessageBody = summaryBody,
        lastMessageAt = summaryAt,
        lastMessageTimestamp = summaryTimestamp,
        isUnread = false,
        unreadCount = 0,
        updatedAt = System.currentTimeMillis()
      )
    )
  }

  override suspend fun startOfferInquiry(
    offerSlug: String,
    text: String
  ): Result<String> = startInquiry(text) { clientMessageId ->
    transport.sendOfferInquiry(offerSlug, clientMessageId, text)
  }

  override suspend fun startProfileInquiry(
    handle: String,
    text: String
  ): Result<String> = startInquiry(text) { clientMessageId ->
    transport.sendProfileInquiry(handle, clientMessageId, text)
  }

  private suspend fun startInquiry(
    text: String,
    send: suspend (String) -> Result<com.example.data.messaging.transport.SendMessageAckDto>
  ): Result<String> = withContext(ioDispatcher) {
    val clientMessageId = "msg-${UUID.randomUUID()}"
    send(clientMessageId).map { it.conversationId }
  }

  override suspend fun sendMessage(
    conversationId: String,
    text: String,
    senderPersonId: String,
    attachedOfferSlug: String?,
    attachedOfferTitle: String?,
    attachedOfferPrice: String?,
    attachedOfferImageResId: Int?
  ): String = withContext(ioDispatcher) {
    val now = System.currentTimeMillis()
    val clientMessageId = "msg-${UUID.randomUUID()}"

    // 1. Optimistic write: insert MessageEntity immediately with status = SENDING
    val optimisticMessage = MessageEntity(
      clientMessageId = clientMessageId,
      conversationId = conversationId,
      serverId = null,
      senderPersonId = senderPersonId,
      isFromMe = true,
      text = text,
      timestamp = now,
      formattedTime = "Just now",
      status = MessageDeliveryStatus.SENDING,
      errorMessage = null,
      retryCount = 0,
      attachedOfferSlug = attachedOfferSlug,
      attachedOfferTitle = attachedOfferTitle,
      attachedOfferPrice = attachedOfferPrice,
      attachedOfferImageResId = attachedOfferImageResId
    )
    messageDao.insertMessage(optimisticMessage)

    // 2. Optimistically update Conversation preview in Room
    val previewText = when {
      attachedOfferTitle != null && text.isNotBlank() -> "Attached: $attachedOfferTitle — $text"
      attachedOfferTitle != null -> "Attached offer: $attachedOfferTitle"
      else -> text
    }
    conversationDao.updateLastMessage(
      id = conversationId,
      lastMessageBody = previewText,
      lastMessageAt = formatIso(now),
      lastMessageTimestamp = now,
      isUnread = false
    )

    // 3. Queue into Outbox
    outboxDao.insert(
      OutboxEntity(
        clientMessageId = clientMessageId,
        conversationId = conversationId,
        text = text,
        attachedOfferSlug = attachedOfferSlug,
        createdAt = now,
        status = "PENDING"
      )
    )

    // 4. Trigger Outbox processing in background
    coroutineScope.launch {
      processOutbox()
    }

    clientMessageId
  }

  override suspend fun retryMessage(clientMessageId: String): Result<Unit> = withContext(ioDispatcher) {
    val message = messageDao.getMessageByClientId(clientMessageId)
      ?: return@withContext Result.failure(IllegalArgumentException("Message not found"))

    messageDao.updateMessageStatus(
      clientMessageId = clientMessageId,
      status = MessageDeliveryStatus.SENDING,
      errorMessage = null
    )

    outboxDao.insert(
      OutboxEntity(
        clientMessageId = clientMessageId,
        conversationId = message.conversationId,
        text = message.text,
        attachedOfferSlug = message.attachedOfferSlug,
        createdAt = System.currentTimeMillis(),
        status = "PENDING"
      )
    )

    coroutineScope.launch {
      processOutbox()
    }

    Result.success(Unit)
  }

  override suspend fun markConversationRead(conversationId: String) = withContext(ioDispatcher) {
    conversationDao.markAsRead(conversationId)
  }

  override suspend fun clearForOwner(ownerPersonId: String) = withContext(ioDispatcher) {
    conversationDao.deleteForOwner(ownerPersonId)
  }

  suspend fun processOutbox() = withContext(ioDispatcher) {
    outboxMutex.withLock {
      val pending = outboxDao.getPendingOutbox()
      for (entry in pending) {
        val result = transport.sendMessage(
          conversationId = entry.conversationId,
          clientMessageId = entry.clientMessageId,
          text = entry.text
        )
        if (result.isSuccess) {
          val ack = result.getOrThrow()
          // Mark message SENT with serverId
          messageDao.updateMessageStatus(
            clientMessageId = entry.clientMessageId,
            status = MessageDeliveryStatus.SENT,
            serverId = ack.serverId,
            errorMessage = null
          )
          outboxDao.delete(entry.clientMessageId)
        } else {
          // Delivery failed
          val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Failed to send message"
          messageDao.updateMessageStatus(
            clientMessageId = entry.clientMessageId,
            status = MessageDeliveryStatus.FAILED,
            errorMessage = errorMsg,
            retryIncrement = 1
          )
          outboxDao.markAttempt(
            clientMessageId = entry.clientMessageId,
            status = "FAILED"
          )
        }
      }
    }
  }

  companion object {
    fun parseIsoToMillis(isoString: String): Long = MessagingTime.parseIsoToMillis(isoString)

    fun formatIso(epochMillis: Long): String = MessagingTime.formatIso(epochMillis)

    fun formatTimeLabel(epochMillis: Long): String = MessagingTime.formatTimeLabel(epochMillis)
  }

  private fun ConversationSummaryDto.toEntity(ownerPersonId: String): ConversationEntity {
    return ConversationEntity(
      id = id,
      ownerPersonId = ownerPersonId,
      otherUserId = otherUserId,
      displayName = displayName,
      handle = handle,
      avatarUrl = avatarUrl,
      lastMessageBody = lastMessageBody,
      lastMessageAt = lastMessageAt,
      lastMessageTimestamp = parseIsoToMillis(lastMessageAt),
      isUnread = isUnread,
      unreadCount = if (isUnread) 1 else 0,
      updatedAt = System.currentTimeMillis()
    )
  }
}
