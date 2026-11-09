package com.example

import com.example.data.FeedOffer
import com.example.data.InquiredRepository
import com.example.data.PaginatedOffers
import com.example.ui.model.InquiredViewModel
import com.example.ui.model.SessionManager
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InquiredViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  @Before fun setup() {
    Dispatchers.setMain(dispatcher)
    SessionManager.signInPerson("actor", "Person", "person")
  }
  @After fun teardown() {
    SessionManager.signOut()
    Dispatchers.resetMain()
  }
  private fun offer(id: String) = FeedOffer(id, id, "Offer $id", "Provider", "Free", null)
  private fun repository(fetch: suspend (String?) -> PaginatedOffers) = object : InquiredRepository {
    override suspend fun getInquired(after: String?) = try {
      Result.success(fetch(after))
    } catch (error: IOException) { Result.failure(error) }
  }

  @Test fun `paging deduplicates and preserves cursor and retry errors`() = runTest(dispatcher) {
    val requests = mutableListOf<String?>()
    var fail = false
    val model = InquiredViewModel(repository { after ->
      requests += after
      if (fail) throw IOException("offline")
      if (after == null) PaginatedOffers(listOf(offer("a")), "next")
      else PaginatedOffers(listOf(offer("a"), offer("b")), null)
    })
    model.load()
    advanceUntilIdle()
    fail = true
    model.loadMore()
    model.loadMore()
    advanceUntilIdle()
    assertEquals(listOf(null, "next"), requests)
    assertNotNull(model.state.value.pagingError)
    assertEquals(listOf("a"), model.state.value.items.map { it.id })
    fail = false
    model.loadMore()
    advanceUntilIdle()
    assertNull(model.state.value.pagingError)
    assertNull(model.state.value.offers?.nextCursor)
    assertEquals(listOf("a", "b"), model.state.value.items.map { it.id })
  }

  @Test fun `initial and loaded refresh errors remain visible and retry succeeds`() = runTest(dispatcher) {
    var fail = true
    val model = InquiredViewModel(repository {
      if (fail) throw IOException("offline")
      PaginatedOffers(listOf(offer("a")), null)
    })
    model.load()
    advanceUntilIdle()
    assertNotNull(model.state.value.error)
    assertTrue(model.state.value.items.isEmpty())
    fail = false
    model.load()
    advanceUntilIdle()
    assertNull(model.state.value.error)
    fail = true
    model.load()
    advanceUntilIdle()
    assertNotNull(model.state.value.error)
    assertEquals(listOf("a"), model.state.value.items.map { it.id })
  }

  @Test fun `refresh rejects a late page even if cancellation is ignored`() = runTest(dispatcher) {
    val latePage = CompletableDeferred<PaginatedOffers>()
    val model = InquiredViewModel(repository { after ->
      if (after != null) withContext(NonCancellable) { latePage.await() }
      else PaginatedOffers(listOf(offer("fresh")), "next")
    })
    model.load()
    advanceUntilIdle()
    model.loadMore()
    runCurrent()
    model.load()
    runCurrent()
    latePage.complete(PaginatedOffers(listOf(offer("stale")), null))
    advanceUntilIdle()
    assertEquals(listOf("fresh"), model.state.value.items.map { it.id })
    assertEquals("next", model.state.value.offers?.nextCursor)
    assertFalse(model.state.value.loadingMore)
  }

  @Test fun `leaving the screen cancels loading and prevents late data`() = runTest(dispatcher) {
    val latePage = CompletableDeferred<PaginatedOffers>()
    val model = InquiredViewModel(repository { withContext(NonCancellable) { latePage.await() } })
    model.load()
    runCurrent()
    model.cancelLoading()
    latePage.complete(PaginatedOffers(listOf(offer("stale")), null))
    advanceUntilIdle()
    assertTrue(model.state.value.items.isEmpty())
    assertFalse(model.state.value.loading)
  }

  @Test fun `account transition cannot publish a response for the previous person`() = runTest(dispatcher) {
    val latePage = CompletableDeferred<PaginatedOffers>()
    val model = InquiredViewModel(repository { latePage.await() })
    model.load()
    runCurrent()
    SessionManager.signInPerson("another", "Another", "another")
    latePage.complete(PaginatedOffers(listOf(offer("private")), null))
    advanceUntilIdle()
    assertTrue(model.state.value.items.isEmpty())
    model.cancelLoading()
  }
  @Test fun `returning retains accumulated pages cursor and scroll without another first page request`() = runTest(dispatcher) {
    val requests = mutableListOf<String?>()
    val model = InquiredViewModel(repository { after ->
      requests += after
      if (after == null) PaginatedOffers(listOf(offer("a")), "next")
      else PaginatedOffers(listOf(offer("b")), "later")
    })
    model.loadIfNeeded()
    advanceUntilIdle()
    model.loadMore()
    advanceUntilIdle()
    model.updateScrollPosition(1, 48)
    model.cancelLoading()
    model.loadIfNeeded()
    advanceUntilIdle()
    assertEquals(listOf(null, "next"), requests)
    assertEquals(listOf("a", "b"), model.state.value.items.map { it.id })
    assertEquals("later", model.state.value.offers?.nextCursor)
    assertEquals(1, model.scrollIndex)
    assertEquals(48, model.scrollOffset)
  }

  @Test fun `a loaded empty collection stays distinct from never loaded on return`() = runTest(dispatcher) {
    var calls = 0
    val model = InquiredViewModel(repository { calls++; PaginatedOffers(emptyList(), null) })
    model.loadIfNeeded()
    advanceUntilIdle()
    model.cancelLoading()
    model.loadIfNeeded()
    advanceUntilIdle()
    assertEquals(1, calls)
    assertNotNull(model.state.value.offers)
    assertFalse(model.state.value.loading)
    model.load()
    runCurrent()
    advanceUntilIdle()
    assertEquals(2, calls)
  }

}
