package com.example.data.messaging.transport

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Replaceable mock transport for the Haven messaging system.
 * Simulates network latency, idempotency checking, server message ID generation,
 * thread history generation, and realtime inbound events.
 */
class MockMessagingTransport(
  private val simulatedDelayMs: Long = 0L
) : MessagingTransport {

  private val _events = MutableSharedFlow<MessagingEvent>(extraBufferCapacity = 64)
  override fun observeEvents(): Flow<MessagingEvent> = _events.asSharedFlow()

  // Storage of sent messages keyed by clientMessageId for idempotency
  private val sentAcks = ConcurrentHashMap<String, SendMessageAckDto>()
  // Conversation thread histories keyed by conversationId
  private val threadMessages = ConcurrentHashMap<String, MutableList<RemoteMessageDto>>()

  var alwaysFailSend: Boolean = false
  var shouldFailNextSend: Boolean = false
  var shouldFailNextFetch: Boolean = false
  var failureException: Exception = RuntimeException("Simulated network failure")

  var mockConversations: List<ConversationSummaryDto> = listOf(
    ConversationSummaryDto(
      id = "conv-adaeze-101",
      otherUserId = "user-adaeze",
      displayName = "Adaeze Okafor",
      handle = "@adaeze_crafts",
      avatarUrl = null,
      lastMessageBody = "I love the handcrafted bowl! Can we meet at the community center on Saturday?",
      lastMessageAt = formatIso(System.currentTimeMillis() - 1000 * 60 * 15),
      isUnread = true
    ),
    ConversationSummaryDto(
      id = "conv-tunde-102",
      otherUserId = "user-tunde",
      displayName = "Tunde Bakare",
      handle = "@tunde_leather",
      avatarUrl = null,
      lastMessageBody = "Deal! I will set aside the hand-stitched wallet for you.",
      lastMessageAt = formatIso(System.currentTimeMillis() - 1000 * 60 * 60 * 2),
      isUnread = false
    ),
    ConversationSummaryDto(
      id = "conv-chioma-103",
      otherUserId = "user-chioma",
      displayName = "Chioma Nwosu",
      handle = "@chioma_textiles",
      avatarUrl = null,
      lastMessageBody = "Let me check my fabric stock and get back to you shortly.",
      lastMessageAt = formatIso(System.currentTimeMillis() - 1000 * 60 * 60 * 24),
      isUnread = false
    )
  )

  override suspend fun getConversations(cursor: String?): Result<ConversationInboxPageDto> {
    if (simulatedDelayMs > 0) delay(simulatedDelayMs)
    if (shouldFailNextFetch) {
      shouldFailNextFetch = false
      return Result.failure(failureException)
    }
    return Result.success(
      ConversationInboxPageDto(
        items = mockConversations,
        nextCursor = null
      )
    )
  }

  override suspend fun getThreadHistory(
    conversationId: String,
    beforeTimestamp: Long?,
    limit: Int
  ): Result<List<RemoteMessageDto>> {
    if (simulatedDelayMs > 0) delay(simulatedDelayMs)
    if (shouldFailNextFetch) {
      shouldFailNextFetch = false
      return Result.failure(failureException)
    }

    val history = threadMessages.computeIfAbsent(conversationId) { id ->
      // Seed initial history if none exists
      seedHistory(id)
    }

    val filtered = if (beforeTimestamp != null) {
      history.filter { it.timestampMillis < beforeTimestamp }
    } else {
      history
    }
    return Result.success(filtered.takeLast(limit))
  }

  override suspend fun sendMessage(
    conversationId: String,
    clientMessageId: String,
    text: String,
    attachedOfferSlug: String?
  ): Result<SendMessageAckDto> {
    if (simulatedDelayMs > 0) delay(simulatedDelayMs)

    if (alwaysFailSend || shouldFailNextSend) {
      shouldFailNextSend = false
      return Result.failure(failureException)
    }

    // Idempotency: return existing ack if already received
    val existing = sentAcks[clientMessageId]
    if (existing != null) {
      return Result.success(existing)
    }

    val now = System.currentTimeMillis()
    val serverId = "srv-${UUID.randomUUID()}"
    val ack = SendMessageAckDto(
      serverId = serverId,
      clientMessageId = clientMessageId,
      conversationId = conversationId,
      timestampIso = formatIso(now),
      timestampMillis = now
    )
    sentAcks[clientMessageId] = ack

    val history = threadMessages.computeIfAbsent(conversationId) { id -> seedHistory(id) }
    history.add(
      RemoteMessageDto(
        serverId = serverId,
        clientMessageId = clientMessageId,
        conversationId = conversationId,
        senderPersonId = "me",
        isFromMe = true,
        text = text,
        timestampIso = formatIso(now),
        timestampMillis = now,
        attachedOfferSlug = attachedOfferSlug
      )
    )

    return Result.success(ack)
  }

  suspend fun emitSimulatedIncomingMessage(
    conversationId: String,
    senderPersonId: String,
    text: String
  ): RemoteMessageDto {
    val now = System.currentTimeMillis()
    val msg = RemoteMessageDto(
      serverId = "srv-${UUID.randomUUID()}",
      clientMessageId = "inbound-${UUID.randomUUID()}",
      conversationId = conversationId,
      senderPersonId = senderPersonId,
      isFromMe = false,
      text = text,
      timestampIso = formatIso(now),
      timestampMillis = now
    )
    val history = threadMessages.computeIfAbsent(conversationId) { id -> seedHistory(id) }
    history.add(msg)
    _events.emit(MessagingEvent.NewMessageReceived(msg))
    return msg
  }

  private fun seedHistory(conversationId: String): MutableList<RemoteMessageDto> {
    val now = System.currentTimeMillis()
    val list = mutableListOf<RemoteMessageDto>()
    when (conversationId) {
      "conv-adaeze-101" -> {
        list.add(
          RemoteMessageDto(
            serverId = "msg-ad-1",
            clientMessageId = "seed-ad-1",
            conversationId = conversationId,
            senderPersonId = "user-adaeze",
            isFromMe = false,
            text = "Hello! I saw your barter offer for the pottery set.",
            timestampIso = formatIso(now - 1000 * 60 * 60 * 3),
            timestampMillis = now - 1000 * 60 * 60 * 3
          )
        )
        list.add(
          RemoteMessageDto(
            serverId = "msg-ad-2",
            clientMessageId = "seed-ad-2",
            conversationId = conversationId,
            senderPersonId = "me",
            isFromMe = true,
            text = "Hi Adaeze! Yes, I would love to trade for one of your hand-carved trays.",
            timestampIso = formatIso(now - 1000 * 60 * 60 * 2),
            timestampMillis = now - 1000 * 60 * 60 * 2
          )
        )
        list.add(
          RemoteMessageDto(
            serverId = "msg-ad-3",
            clientMessageId = "seed-ad-3",
            conversationId = conversationId,
            senderPersonId = "user-adaeze",
            isFromMe = false,
            text = "I love the handcrafted bowl! Can we meet at the community center on Saturday?",
            timestampIso = formatIso(now - 1000 * 60 * 15),
            timestampMillis = now - 1000 * 60 * 15
          )
        )
      }
      "conv-tunde-102" -> {
        list.add(
          RemoteMessageDto(
            serverId = "msg-tu-1",
            clientMessageId = "seed-tu-1",
            conversationId = conversationId,
            senderPersonId = "user-tunde",
            isFromMe = false,
            text = "Deal! I will set aside the hand-stitched wallet for you.",
            timestampIso = formatIso(now - 1000 * 60 * 60 * 2),
            timestampMillis = now - 1000 * 60 * 60 * 2
          )
        )
      }
      else -> {
        list.add(
          RemoteMessageDto(
            serverId = "msg-gen-1",
            clientMessageId = "seed-gen-1",
            conversationId = conversationId,
            senderPersonId = "peer",
            isFromMe = false,
            text = "Welcome to Haven! Looking forward to connecting on our trade.",
            timestampIso = formatIso(now - 1000 * 60 * 30),
            timestampMillis = now - 1000 * 60 * 30
          )
        )
      }
    }
    return list
  }

  companion object {
    fun formatIso(epochMillis: Long): String {
      return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
      }.format(Date(epochMillis))
    }
  }
}
