package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.messaging.HavenLocalFirstMessagingRepository
import com.example.data.messaging.db.ConversationEntity
import com.example.data.messaging.db.HavenDatabase
import com.example.data.messaging.db.MessageDeliveryStatus
import com.example.data.messaging.transport.ConversationSummaryDto
import com.example.data.messaging.transport.MockMessagingTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class HavenMessagingRepositoryTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var context: Context
  private lateinit var database: HavenDatabase
  private lateinit var transport: MockMessagingTransport
  private lateinit var repository: HavenLocalFirstMessagingRepository

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()
    val directExecutor = java.util.concurrent.Executor { it.run() }
    database = HavenDatabase.createInMemory(context, directExecutor)
    transport = MockMessagingTransport(simulatedDelayMs = 0L)
    repository = HavenLocalFirstMessagingRepository(
      database = database,
      transport = transport,
      coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher),
      enforceOwnership = false,
      ioDispatcher = testDispatcher
    )
  }

  @After
  fun tearDown() {
    database.close()
    Dispatchers.resetMain()
  }

  @Test
  fun simulatorMigrationPreservesPendingAndRealMessages() = runTest(testDispatcher) {
    fun message(id: String, server: String?, status: String) = com.example.data.messaging.db.MessageEntity(
      clientMessageId = id, serverId = server, conversationId = "migration", senderPersonId = "me",
      isFromMe = true, text = "message", timestamp = 1L, status = status
    )
    val dao = database.messageDao()
    dao.insertMessages(listOf(
      message("seed-gen-1", "msg-gen-1", "SENT"),
      message("sent-simulation", "srv-fake", "SENT"),
      message("pending", null, "SENDING"),
      message("real", "00000000-0000-0000-0000-000000000001", "SENT")
    ))
    HavenDatabase.MIGRATION_2_3.migrate(database.openHelper.writableDatabase)
    assertEquals(setOf("pending", "real"), dao.getMessages("migration").map { it.clientMessageId }.toSet())
  }

  @Test
  fun olderHistoryIsFetchedOnePageAtATime() = runTest(testDispatcher) {
    val person = com.example.data.messaging.transport.RemotePersonDto("peer", "peer", "Peer", null)
    fun page(cursor: String?) = com.example.data.messaging.transport.ConversationThreadPageDto(
      conversationId = "paged", otherPerson = person, messages = emptyList(), nextCursor = cursor,
      isUnread = false
    )
    transport.threadPages["latest"] = page("older")
    transport.threadPages["older"] = page(null)
    repository.syncThreadHistory("paged", "owner").getOrThrow()
    assertEquals(listOf<String?>(null), transport.requestedThreadCursors)
    assertFalse(repository.loadOlderMessages("paged").getOrThrow())
    assertEquals(listOf(null, "older"), transport.requestedThreadCursors)
    repository.loadOlderMessages("paged").getOrThrow()
    assertEquals(2, transport.requestedThreadCursors.size)
  }

  @Test
  fun testCachedInboxRendersImmediatelyWithoutNetwork() = runTest(testDispatcher) {
    val cachedConv = ConversationEntity(
      id = "conv-cached-1",
      ownerPersonId = "person-123",
      otherUserId = "user-adaeze",
      displayName = "Adaeze Okafor",
      handle = "@adaeze",
      avatarUrl = null,
      lastMessageBody = "Cached message from local Room",
      lastMessageAt = "2026-11-04T10:00:00Z",
      lastMessageTimestamp = 1700000000000L,
      isUnread = false
    )
    database.conversationDao().upsertConversation(cachedConv)

    // Observe flow - should emit cached item immediately without calling refresh
    val initialList = repository.observeConversations("person-123").first()
    assertEquals(1, initialList.size)
    assertEquals("conv-cached-1", initialList[0].id)
    assertEquals("Cached message from local Room", initialList[0].lastMessageBody)
  }

  @Test
  fun testBackgroundRefreshUpdatesRoomConversations() = runTest(testDispatcher) {
    transport.mockConversations = listOf(
      ConversationSummaryDto(
        id = "conv-remote-1",
        otherUserId = "user-tunde",
        displayName = "Tunde Bakare",
        handle = "@tunde",
        avatarUrl = null,
        lastMessageBody = "Remote update from server",
        lastMessageAt = "2026-11-04T10:30:00Z",
        isUnread = true
      )
    )

    val result = repository.refreshConversations("person-123")
    assertTrue(result.isSuccess)

    val updatedList = repository.observeConversations("person-123").first()
    assertEquals(1, updatedList.size)
    assertEquals("conv-remote-1", updatedList[0].id)
    assertEquals("Remote update from server", updatedList[0].lastMessageBody)
    assertTrue(updatedList[0].isUnread)
  }

  @Test
  fun testOfflinePreservesCachedContent() = runTest(testDispatcher) {
    // Seed Room cache
    val cached = ConversationEntity(
      id = "conv-offline-1",
      ownerPersonId = "person-123",
      otherUserId = "user-chioma",
      displayName = "Chioma Nwosu",
      handle = "@chioma",
      avatarUrl = null,
      lastMessageBody = "Persisted offline message",
      lastMessageAt = "2026-11-04T09:00:00Z",
      lastMessageTimestamp = 1700000000000L,
      isUnread = false
    )
    database.conversationDao().upsertConversation(cached)

    // Simulate network error
    transport.shouldFailNextFetch = true
    val result = repository.refreshConversations("person-123")
    assertFalse(result.isSuccess)

    // Cached content must still exist in Room and remain visible
    val list = repository.observeConversations("person-123").first()
    assertEquals(1, list.size)
    assertEquals("conv-offline-1", list[0].id)
  }

  @Test
  fun testOptimisticMessageSendAndServerAck() = runTest(testDispatcher) {
    val convId = "conv-thread-101"
    val initialConv = ConversationEntity(
      id = convId,
      ownerPersonId = "person-123",
      otherUserId = "user-adaeze",
      displayName = "Adaeze Okafor",
      handle = "@adaeze",
      avatarUrl = null,
      lastMessageBody = "Old preview",
      lastMessageAt = "2026-11-04T08:00:00Z",
      lastMessageTimestamp = 1700000000000L,
      isUnread = false
    )
    database.conversationDao().upsertConversation(initialConv)

    // 1. Send message optimistically
    val clientId = repository.sendMessage(
      conversationId = convId,
      text = "Hi Adaeze, I want to trade my ceramic mug!"
    )
    assertNotNull(clientId)

    // 2. Message should exist immediately in Room
    val messageInDb = database.messageDao().getMessageByClientId(clientId)
    assertNotNull(messageInDb)
    assertEquals("Hi Adaeze, I want to trade my ceramic mug!", messageInDb!!.text)

    // 3. Conversation preview must update immediately
    val convAfterSend = repository.observeConversation(convId).first()
    assertNotNull(convAfterSend)
    assertEquals("Hi Adaeze, I want to trade my ceramic mug!", convAfterSend!!.lastMessageBody)

    // 4. Process outbox (simulate background dispatch completion)
    repository.processOutbox()

    // 5. Message in Room must now be marked SENT with a non-null serverId
    val messageAfterAck = database.messageDao().getMessageByClientId(clientId)
    assertNotNull(messageAfterAck)
    assertEquals(MessageDeliveryStatus.SENT, messageAfterAck!!.status)
    assertNotNull(messageAfterAck.serverId)

    // 6. Outbox should now be empty
    val pendingOutbox = database.outboxDao().getPendingOutbox()
    assertTrue(pendingOutbox.isEmpty())
  }

  @Test
  fun testDeliveryFailureAndRetry() = runTest(testDispatcher) {
    val convId = "conv-thread-fail"
    transport.alwaysFailSend = true

    // Send message while network is failing
    val clientId = repository.sendMessage(
      conversationId = convId,
      text = "Will this fail?"
    )

    // Process outbox to execute dispatch
    repository.processOutbox()

    // Status should be FAILED
    val failedMessage = database.messageDao().getMessageByClientId(clientId)
    assertNotNull(failedMessage)
    assertEquals(MessageDeliveryStatus.FAILED, failedMessage!!.status)
    assertNotNull(failedMessage.errorMessage)

    // Now transport recovers
    transport.alwaysFailSend = false
    repository.retryMessage(clientId)
    repository.processOutbox()

    // Status should transition to SENT
    val recoveredMessage = database.messageDao().getMessageByClientId(clientId)
    assertNotNull(recoveredMessage)
    assertEquals(MessageDeliveryStatus.SENT, recoveredMessage!!.status)
  }

  @Test
  fun testAccountIsolation() = runTest(testDispatcher) {
    val convUserA = ConversationEntity(
      id = "conv-a",
      ownerPersonId = "user_A",
      otherUserId = "peer-1",
      displayName = "Peer One",
      handle = "@peer1",
      avatarUrl = null,
      lastMessageBody = "User A secret trade",
      lastMessageAt = "2026-11-04T10:00:00Z",
      lastMessageTimestamp = 1000L,
      isUnread = false
    )
    val convUserB = ConversationEntity(
      id = "conv-b",
      ownerPersonId = "user_B",
      otherUserId = "peer-2",
      displayName = "Peer Two",
      handle = "@peer2",
      avatarUrl = null,
      lastMessageBody = "User B private thread",
      lastMessageAt = "2026-11-04T10:00:00Z",
      lastMessageTimestamp = 2000L,
      isUnread = false
    )
    database.conversationDao().upsertConversations(listOf(convUserA, convUserB))

    val listA = repository.observeConversations("user_A").first()
    assertEquals(1, listA.size)
    assertEquals("conv-a", listA[0].id)

    val listB = repository.observeConversations("user_B").first()
    assertEquals(1, listB.size)
    assertEquals("conv-b", listB[0].id)
  }

  @Test
  fun testIdempotentSendDeduplication() = runTest(testDispatcher) {
    val convId = "conv-idem"
    val clientId = "msg-fixed-id-123"

    val ack1 = transport.sendMessage(convId, clientId, "Hello").getOrThrow()
    val ack2 = transport.sendMessage(convId, clientId, "Hello").getOrThrow()

    // Same serverId returned, deduplicated
    assertEquals(ack1.serverId, ack2.serverId)
  }
  @Test
  fun pendingOutboxCannotBeSentOrViewedByAnotherAccount() = runTest(testDispatcher) {
    testScheduler.runCurrent()
    val session = com.example.ui.model.SessionManager
    session.signInPerson("owner-a", "A", "owner-a")
    try {
      val scoped = HavenLocalFirstMessagingRepository(database, transport,
        coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher), ioDispatcher = testDispatcher)
      testScheduler.runCurrent()
      database.conversationDao().upsertConversation(ConversationEntity(
        id = "private-thread", ownerPersonId = "owner-a", otherUserId = "peer", displayName = "Peer",
        handle = "peer", avatarUrl = null, lastMessageBody = "", lastMessageAt = "", lastMessageTimestamp = 0, isUnread = false))
      database.messageDao().insertMessage(com.example.data.messaging.db.MessageEntity(
        clientMessageId = "pending-a", conversationId = "private-thread", senderPersonId = "owner-a",
        isFromMe = true, text = "Private draft", timestamp = 1, status = "SENDING"))
      database.outboxDao().insert(com.example.data.messaging.db.OutboxEntity(
        clientMessageId = "pending-a", conversationId = "private-thread", ownerPersonId = "owner-a", text = "Private draft"))
      session.signInPerson("owner-b", "B", "owner-b")
      scoped.processOutbox()
      assertTrue(scoped.observeMessages("private-thread").first().isEmpty())
      assertEquals(1, database.outboxDao().getPendingOutbox().size)
      assertTrue(scoped.retryMessage("pending-a").isFailure)
      session.signInPerson("owner-a", "A", "owner-a")
      scoped.processOutbox()
      assertTrue(database.outboxDao().getPendingOutbox().isEmpty())
      assertEquals("SENT", database.messageDao().getMessageByClientId("pending-a")!!.status)
    } finally { session.signOut() }
  }

  @Test
  fun remoteAcknowledgementDoesNotRestorePendingSnapshot() = runTest(testDispatcher) {
    val local = com.example.data.messaging.db.MessageEntity(
      clientMessageId = "pending-match", conversationId = "ack-thread", senderPersonId = "me",
      isFromMe = true, text = "Delivered", timestamp = 1, status = "SENDING")
    database.messageDao().insertMessage(local)
    transport.threadPages["latest"] = com.example.data.messaging.transport.ConversationThreadPageDto(
      conversationId = "ack-thread",
      otherPerson = com.example.data.messaging.transport.RemotePersonDto("peer", "peer", "Peer", null),
      messages = listOf(com.example.data.messaging.transport.RemoteMessageDto(
        serverId = "ack-server", clientMessageId = "pending-match", conversationId = "ack-thread",
        senderPersonId = "me", isFromMe = true, text = "Delivered", timestampIso = "2026-11-06T00:00:00Z", timestampMillis = 1)),
      nextCursor = null, isUnread = false)
    repository.syncThreadHistory("ack-thread", "owner").getOrThrow()
    val acknowledged = database.messageDao().getMessageByClientId("pending-match")!!
    assertEquals("SENT", acknowledged.status)
    assertEquals("ack-server", acknowledged.serverId)
  }

  @Test
  fun newestRefreshPreservesScrollCursorAndReopensDisconnectedGap() = runTest(testDispatcher) {
    fun page(id: String, cursor: String?) = com.example.data.messaging.transport.ConversationThreadPageDto(
      conversationId = "gap-thread",
      otherPerson = com.example.data.messaging.transport.RemotePersonDto("peer", "peer", "Peer", null),
      messages = listOf(com.example.data.messaging.transport.RemoteMessageDto(
        serverId = id, clientMessageId = id, conversationId = "gap-thread", senderPersonId = "peer",
        isFromMe = false, text = id, timestampIso = "2026-11-06T00:00:00Z", timestampMillis = 1)),
      nextCursor = cursor, isUnread = false)
    transport.threadPages["latest"] = page("known-latest", "older-a")
    transport.threadPages["older-a"] = page("known-older", "older-b")
    repository.syncThreadHistory("gap-thread", "owner").getOrThrow()
    repository.loadOlderMessages("gap-thread").getOrThrow()
    repository.syncThreadHistory("gap-thread", "owner").getOrThrow()
    assertEquals("older-b", database.syncStateDao().getSyncState("thread_gap-thread")!!.nextCursor)
    // More than one newest page arrived while offline: no overlap with cache.
    transport.threadPages["latest"] = page("new-disjoint-page", "reconnect-gap")
    transport.threadPages["reconnect-gap"] = page("missed-middle-message", "older-b")
    repository.syncThreadHistory("gap-thread", "owner").getOrThrow()
    repository.loadOlderMessages("gap-thread").getOrThrow()
    assertEquals(listOf(null, "older-a", null, null, "reconnect-gap"), transport.requestedThreadCursors)
    assertTrue(database.messageDao().getMessages("gap-thread").any { it.text == "missed-middle-message" })
  }

}
