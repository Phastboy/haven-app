package com.example.data.messaging.transport

import kotlinx.coroutines.delay
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Test-only transport double.
 *
 * It lives in the unit-test source set so production code cannot fall back to
 * simulated messages or timers.
 */
class MockMessagingTransport(
  var simulatedDelayMs: Long = 0L
) : MessagingTransport {

  // Storage of sent messages keyed by clientMessageId for idempotency.
  private val sentAcks = ConcurrentHashMap<String, SendMessageAckDto>()
  // Conversation thread histories keyed by conversationId.
  private val threadMessages = ConcurrentHashMap<String, MutableList<RemoteMessageDto>>()

  val requestedThreadCursors = mutableListOf<String?>()
  val threadPages = mutableMapOf<String, ConversationThreadPageDto>()
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
      lastMessageAt = MessagingTime.formatIso(System.currentTimeMillis() - 1000 * 60 * 15),
      isUnread = true
    ),
    ConversationSummaryDto(
      id = "conv-tunde-102",
      otherUserId = "user-tunde",
      displayName = "Tunde Bakare",
      handle = "@tunde_leather",
      avatarUrl = null,
      lastMessageBody = "Deal! I will set aside the hand-stitched wallet for you.",
      lastMessageAt = MessagingTime.formatIso(System.currentTimeMillis() - 1000 * 60 * 60 * 2),
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
      ConversationInboxPageDto(items = mockConversations, nextCursor = null)
    )
  }

  override suspend fun getThread(
    conversationId: String,
    after: String?
  ): Result<ConversationThreadPageDto> {
    if (simulatedDelayMs > 0) delay(simulatedDelayMs)
    if (shouldFailNextFetch) {
      shouldFailNextFetch = false
      return Result.failure(failureException)
    }

    requestedThreadCursors.add(after)
    threadPages[after ?: "latest"]?.let { return Result.success(it) }
    val history = threadMessages.computeIfAbsent(conversationId) { id -> seedHistory(id) }
    val summary = mockConversations.firstOrNull { it.id == conversationId }
    return Result.success(
      ConversationThreadPageDto(
        conversationId = conversationId,
        otherPerson = RemotePersonDto(
          id = summary?.otherUserId ?: "peer",
          handle = summary?.handle ?: "@peer",
          displayName = summary?.displayName ?: "Peer",
          avatarUrl = summary?.avatarUrl
        ),
        messages = history.toList(),
        nextCursor = null,
        isUnread = summary?.isUnread ?: false
      )
    )
  }

  override suspend fun sendMessage(
    conversationId: String,
    clientMessageId: String,
    text: String,
    offerId: String?
  ): Result<SendMessageAckDto> {
    if (simulatedDelayMs > 0) delay(simulatedDelayMs)

    if (alwaysFailSend || shouldFailNextSend) {
      shouldFailNextSend = false
      return Result.failure(failureException)
    }

    val existing = sentAcks[clientMessageId]
    if (existing != null) return Result.success(existing)

    val now = System.currentTimeMillis()
    val serverId = "srv-${UUID.randomUUID()}"
    val ack = SendMessageAckDto(
      serverId = serverId,
      clientMessageId = clientMessageId,
      conversationId = conversationId,
      timestampIso = MessagingTime.formatIso(now),
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
        timestampIso = ack.timestampIso,
        timestampMillis = now
      )
    )
    return Result.success(ack)
  }

  override suspend fun sendOfferInquiry(
    offerSlug: String,
    clientMessageId: String,
    text: String
  ): Result<SendMessageAckDto> =
    sendMessage("conv-offer-$offerSlug", clientMessageId, text)

  override suspend fun sendProfileInquiry(
    handle: String,
    clientMessageId: String,
    text: String
  ): Result<SendMessageAckDto> =
    sendMessage("conv-person-$handle", clientMessageId, text)

  private fun seedHistory(conversationId: String): MutableList<RemoteMessageDto> {
    val now = System.currentTimeMillis()
    return mutableListOf(
      RemoteMessageDto(
        serverId = "seed-$conversationId",
        clientMessageId = "seed-$conversationId",
        conversationId = conversationId,
        senderPersonId = "peer",
        isFromMe = false,
        text = "Welcome to Haven! Looking forward to connecting on our trade.",
        timestampIso = MessagingTime.formatIso(now - 1000 * 60 * 30),
        timestampMillis = now - 1000 * 60 * 30
      )
    )
  }
}
