package com.example.data.messaging

import androidx.room.withTransaction
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import com.example.ui.model.SessionManager
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
  suspend fun loadOlderMessages(conversationId: String): Result<Boolean>
  suspend fun hasOlderMessages(conversationId: String): Boolean = true
  /**
   * One-shot read of the currently persisted rows for a thread, bypassing the
   * reactive flow. Used to decide settled states after a sync: the transaction
   * has committed by the time callers run this, so it reflects what the flow
   * is about to emit.
   */
  suspend fun snapshotMessages(conversationId: String): List<MessageEntity>
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
  private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
  private val enforceOwnership: Boolean = true
) : MessagingRepository {

  private val conversationDao: ConversationDao = database.conversationDao()
  private val messageDao: MessageDao = database.messageDao()
  private val outboxDao: OutboxDao = database.outboxDao()
  private val syncStateDao: SyncStateDao = database.syncStateDao()

  private val outboxMutex = Mutex()
  private val threadMutex = Mutex()
  private val boundOwnerPersonId = SessionManager.session.value.personId

  init {
    // Deliver messages that were never attempted (for example the app stopped
    // mid-flight). FAILED rows stay put until the person retries explicitly.
    coroutineScope.launch {
      processOutbox()
    }
  }

  override fun observeConversations(ownerPersonId: String): Flow<List<ConversationEntity>> {
    return conversationDao.observeConversations(ownerPersonId).combine(SessionManager.session) { rows, session ->
      if (!enforceOwnership || session.personId == ownerPersonId) rows else emptyList()
    }.distinctUntilChanged()
  }

  override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> {
    return combine(messageDao.observeMessages(conversationId), conversationDao.observeConversation(conversationId), SessionManager.session) { rows, conversation, session ->
      if (!enforceOwnership) rows
      else if (session.personId == null || conversation?.ownerPersonId != session.personId) emptyList()
      else rows.filter { it.status == MessageDeliveryStatus.SENT || it.senderPersonId == session.personId }
        .map { it.copy(isFromMe = it.senderPersonId == session.personId) }
    }.distinctUntilChanged()
  }

  override fun observeConversation(conversationId: String): Flow<ConversationEntity?> {
    return conversationDao.observeConversation(conversationId).combine(SessionManager.session) { row, session ->
      if (!enforceOwnership || session.personId != null && row?.ownerPersonId == session.personId) row else null
    }.distinctUntilChanged()
  }

  override suspend fun refreshConversations(ownerPersonId: String): Result<Unit> = withContext(ioDispatcher) {
    val actor = SessionManager.session.value.personId
    if (enforceOwnership && (actor == null || actor != ownerPersonId)) return@withContext Result.failure(IllegalStateException("Account changed"))
    val result = transport.getConversations(cursor = null)
    if (enforceOwnership && SessionManager.session.value.personId != actor) return@withContext Result.failure(IllegalStateException("Account changed"))
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

    val actor = SessionManager.session.value.personId
    if (enforceOwnership && (actor == null || actor != ownerPersonId)) return@withContext Result.failure(IllegalStateException("Account changed"))
    val result = transport.getConversations(cursor)
    if (enforceOwnership && SessionManager.session.value.personId != actor) return@withContext Result.failure(IllegalStateException("Account changed"))
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
  ): Result<Unit> = threadMutex.withLock { syncLatestPage(conversationId, ownerPersonId) }

  private suspend fun syncLatestPage(
    conversationId: String,
    ownerPersonId: String?
  ): Result<Unit> = withContext(ioDispatcher) {
    val actor = SessionManager.session.value.personId
    val owner = ownerPersonId ?: conversationDao.getConversation(conversationId)?.ownerPersonId
    if (enforceOwnership && (actor == null || actor != owner)) return@withContext Result.failure(IllegalStateException("Account changed"))
    processOutbox()
    val result = transport.getThread(conversationId)
    if (enforceOwnership && SessionManager.session.value.personId != actor) return@withContext Result.failure(IllegalStateException("Account changed"))
    if (result.isFailure) {
      return@withContext Result.failure(
        result.exceptionOrNull() ?: RuntimeException("Failed to sync thread")
      )
    }

    val page = result.getOrThrow()
    val previousCursor = syncStateDao.getSyncState("thread_$conversationId")
    val knownServerIds = messageDao.getMessages(conversationId).mapNotNull { it.serverId }.toSet()
    // Keep scroll progress when newest data overlaps known history. A disjoint
    // page means a reconnect gap: page back from there instead of skipping it.
    val overlaps = page.messages.any { it.serverId in knownServerIds }
    if (previousCursor == null || knownServerIds.isEmpty() || !overlaps) {
      syncStateDao.upsertSyncState(SyncStateEntity("thread_$conversationId", page.nextCursor))
    }
    mergeThreadPage(conversationId, page)
    val existing = messageDao.getMessages(conversationId)
    upsertConversationFromThread(conversationId, owner, page, existing)
    Result.success(Unit)
  }

  override suspend fun hasOlderMessages(conversationId: String): Boolean = withContext(ioDispatcher) {
    syncStateDao.getSyncState("thread_$conversationId")?.nextCursor != null
  }

  override suspend fun snapshotMessages(conversationId: String): List<MessageEntity> = withContext(ioDispatcher) {
    messageDao.getMessages(conversationId)
  }

  override suspend fun loadOlderMessages(conversationId: String): Result<Boolean> = threadMutex.withLock {
    loadOlderPage(conversationId)
  }

  private suspend fun loadOlderPage(conversationId: String): Result<Boolean> = withContext(ioDispatcher) {
    val actor = SessionManager.session.value.personId
    if (enforceOwnership && (actor == null || conversationDao.getConversation(conversationId)?.ownerPersonId != actor))
      return@withContext Result.failure(IllegalStateException("Account changed"))
    val cursor = syncStateDao.getSyncState("thread_$conversationId")?.nextCursor
      ?: return@withContext Result.success(false)
    val result = transport.getThread(conversationId, cursor)
    if (enforceOwnership && SessionManager.session.value.personId != actor)
      return@withContext Result.failure(IllegalStateException("Account changed"))
    result.map { page ->
      mergeThreadPage(conversationId, page)
      syncStateDao.upsertSyncState(SyncStateEntity("thread_$conversationId", page.nextCursor))
      page.nextCursor != null
    }
  }

  private suspend fun mergeThreadPage(conversationId: String, page: ConversationThreadPageDto) = database.withTransaction {
    val existing = messageDao.getMessages(conversationId)
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

    // Upserting server rows never deletes other local sends. Do not restore a
    // stale SENDING snapshot over an acknowledgement committed in the meantime.
    messageDao.insertMessages(entitiesToInsert)
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
    val summaryBody = if (keepLocal) current.lastMessageBody else lastRemote.text
    val summaryAt = if (keepLocal) current.lastMessageAt else lastRemote.timestampIso
    val summaryTimestamp =
      if (keepLocal) current.lastMessageTimestamp else lastRemote.timestampMillis

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
    val actor = SessionManager.session.value.personId
    check(!enforceOwnership || actor != null && conversationDao.getConversation(conversationId)?.ownerPersonId == actor) { "Sign in to this conversation before sending" }
    require(text.isNotBlank()) { "Write a message first" }
    val now = System.currentTimeMillis()
    val clientMessageId = "msg-${UUID.randomUUID()}"

    // 1. Optimistic write: insert MessageEntity immediately with status = SENDING
    val optimisticMessage = MessageEntity(
      clientMessageId = clientMessageId,
      conversationId = conversationId,
      serverId = null,
      senderPersonId = if (enforceOwnership) actor!! else senderPersonId,
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
    database.withTransaction {
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
        ownerPersonId = actor,
        text = text,
        attachedOfferSlug = attachedOfferSlug,
        createdAt = now,
        status = "PENDING"
      )
    )

    }

    // 4. Trigger Outbox processing in background
    coroutineScope.launch {
      processOutbox()
    }

    clientMessageId
  }

  override suspend fun retryMessage(clientMessageId: String): Result<Unit> = withContext(ioDispatcher) {
    val message = messageDao.getMessageByClientId(clientMessageId)
      ?: return@withContext Result.failure(IllegalArgumentException("Message not found"))

    val actor = SessionManager.session.value.personId
    if (enforceOwnership && (actor == null || message.senderPersonId != actor))
      return@withContext Result.failure(IllegalStateException("This message belongs to another account"))
    messageDao.updateMessageStatus(
      clientMessageId = clientMessageId,
      status = MessageDeliveryStatus.SENDING,
      errorMessage = null
    )

    outboxDao.insert(
      OutboxEntity(
        clientMessageId = clientMessageId,
        conversationId = message.conversationId,
        ownerPersonId = actor,
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
        val actor = SessionManager.session.value.personId
        if (enforceOwnership && (actor == null || actor != boundOwnerPersonId || entry.ownerPersonId != actor)) continue
        val result = transport.sendMessage(
          conversationId = entry.conversationId,
          clientMessageId = entry.clientMessageId,
          text = entry.text
        )
        if (enforceOwnership && SessionManager.session.value.personId != actor) continue
        if (result.isSuccess) {
          val ack = result.getOrThrow()
          database.withTransaction {
          messageDao.deleteDuplicateDelivery(ack.serverId, entry.clientMessageId)
          // Mark message SENT with serverId
          messageDao.updateMessageStatus(
            clientMessageId = entry.clientMessageId,
            status = MessageDeliveryStatus.SENT,
            serverId = ack.serverId,
            errorMessage = null
          )
          outboxDao.delete(entry.clientMessageId)
          }
        } else {
          // Delivery failed
          val error = result.exceptionOrNull()
          val unconfirmed = error is java.io.IOException &&
            (error !is com.example.auth.HavenAuthClient.HttpException || error.statusCode >= 500)
          val errorMsg = if (unconfirmed) "Delivery unconfirmed; checking when connected" else error?.localizedMessage ?: "Failed to send message"
          messageDao.updateMessageStatus(
            clientMessageId = entry.clientMessageId,
            status = if (unconfirmed) MessageDeliveryStatus.SENDING else MessageDeliveryStatus.FAILED,
            errorMessage = errorMsg, retryIncrement = 1)
          outboxDao.markAttempt(clientMessageId = entry.clientMessageId,
            status = if (unconfirmed) "UNCONFIRMED" else "FAILED")
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
