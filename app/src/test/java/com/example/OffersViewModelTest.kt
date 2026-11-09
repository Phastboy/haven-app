package com.example

import com.example.data.FeedCategory
import com.example.data.FeedOffer
import com.example.data.HavenOffersRepository
import com.example.data.OffersPage
import com.example.data.OffersQuery
import com.example.data.OffersRepository
import com.example.ui.model.OffersViewModel
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class OffersViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  @Before fun setup() { Dispatchers.setMain(dispatcher) }
  @After fun teardown() { Dispatchers.resetMain() }

  private fun offer(id: String) = FeedOffer(id, "offer-$id", "Server title $id", "Real maker", "Free", null)
  private fun page(items: List<FeedOffer>, next: String? = null) = OffersPage(items,
    listOf(FeedCategory("food-groceries", "Food & groceries")), listOf("NGN"), next)

  @Test fun `server order and cursors are preserved without local search filtering`() = runTest(dispatcher) {
    val requests = mutableListOf<OffersQuery>()
    val model = OffersViewModel(OffersRepository { query ->
      requests += query
      if (query.after == null) page(listOf(offer("2"), offer("1")), "next-page")
      else page(listOf(offer("3")))
    })
    advanceUntilIdle()
    assertEquals(listOf("2", "1"), model.uiState.value.items.map { it.id })
    model.loadMore()
    advanceUntilIdle()
    assertEquals("next-page", requests.last().after)
    assertEquals(listOf("2", "1", "3"), model.uiState.value.items.map { it.id })
    model.onSearchQueryChange("another title")
    advanceUntilIdle()
    assertEquals("another title", requests.last().search)
    assertNull(requests.last().after)
    assertEquals(2, model.uiState.value.items.size)
  }

  @Test fun `a failed request leaves no sample results and can be retried`() = runTest(dispatcher) {
    var fail = true
    val model = OffersViewModel(OffersRepository {
      if (fail) throw IOException("offline")
      page(listOf(offer("real")))
    })
    advanceUntilIdle()
    assertTrue(model.uiState.value.items.isEmpty())
    assertNotNull(model.uiState.value.error)
    assertFalse(model.uiState.value.isLoading)
    fail = false
    model.refresh()
    advanceUntilIdle()
    assertEquals("real", model.uiState.value.items.single().id)
    assertNull(model.uiState.value.error)
  }

  @Test fun `changing criteria cancels the old request and resets paging`() = runTest(dispatcher) {
    val oldPage = CompletableDeferred<OffersPage>()
    val model = OffersViewModel(OffersRepository { query ->
      if (query.category.isEmpty()) oldPage.await() else page(listOf(offer("new")))
    })
    testScheduler.runCurrent()
    model.selectCategory("food-groceries")
    advanceUntilIdle()
    oldPage.complete(page(listOf(offer("stale"))))
    advanceUntilIdle()
    assertEquals("new", model.uiState.value.items.single().id)
  }

  @Test fun `invalid price range never makes a request or claims filtered results`() = runTest(dispatcher) {
    var calls = 0
    val model = OffersViewModel(OffersRepository { calls++; page(emptyList()) })
    advanceUntilIdle()
    model.onDraftCurrencyChange("NGN")
    model.onDraftMinPriceChange("20")
    model.onDraftMaxPriceChange("10")
    model.applyFilters()
    advanceUntilIdle()
    assertEquals(1, calls)
    assertNotNull(model.uiState.value.error)
    assertNull(model.uiState.value.query.minPrice)
  }

  @Test fun `parser uses backend price display and accepts a missing cover without dummy images`() {
    val page = HavenOffersRepository.parsePage("""{"items":[{"id":"real","slug":"real-offer","title":"An offer","maker_name":"A person","price_display":"100 NGN","cover_url":null}],"categories":[],"currencies":["NGN"],"next_cursor":null}""")
    assertEquals("100 NGN", page.items.single().priceDisplay)
    assertNull(page.items.single().coverUrl)
    assertNull(page.nextCursor)
  }
  @Test fun profileEditDuringRequestDoesNotLeaveFeedLoading() = runTest(dispatcher) {
    com.example.ui.model.SessionManager.signInPerson("actor", "Before", "before")
    try {
      val model = OffersViewModel(OffersRepository {
        com.example.ui.model.SessionManager.updateProfileIfCurrent("actor", "After", "after")
        page(listOf(offer("confirmed")))
      })
      advanceUntilIdle()
      assertEquals("confirmed", model.uiState.value.items.single().id)
      assertFalse(model.uiState.value.isLoading)
      assertFalse(model.uiState.value.isRefreshing)
    } finally { com.example.ui.model.SessionManager.signOut() }
  }

  @Test fun `Following retains facets resets cursor and disables proximity`() = runTest(dispatcher) {
    val requests = mutableListOf<OffersQuery>()
    val model = OffersViewModel(OffersRepository { query ->
      requests += query
      page(listOf(offer("followed")), "next-page")
    })
    advanceUntilIdle()
    model.selectCategory("food-groceries")
    model.onSearchQueryChange("bread")
    model.applyReference(com.example.data.PlaceCandidate("Selected place", "explicit-reference"))
    advanceUntilIdle()
    model.setScope("following")
    advanceUntilIdle()
    assertEquals("following", requests.last().scope)
    assertEquals("food-groceries", requests.last().category)
    assertEquals("bread", requests.last().search)
    assertEquals("off", requests.last().near)
    assertNull(requests.last().after)
    assertNull(model.uiState.value.activeReference)
    model.loadMore()
    advanceUntilIdle()
    assertEquals("following", requests.last().scope)
    assertEquals("next-page", requests.last().after)
    model.applyReference(com.example.data.PlaceCandidate("Another place", "new-reference"))
    advanceUntilIdle()
    assertEquals("public", requests.last().scope)
    assertEquals("new-reference", requests.last().near)
  }

  @Test fun `unsupported feed scopes never reach the repository`() = runTest(dispatcher) {
    var calls = 0
    val model = OffersViewModel(OffersRepository { calls++; page(emptyList()) })
    advanceUntilIdle()
    for (scope in listOf("saved", "inquired", "unknown", "")) model.setScope(scope)
    advanceUntilIdle()
    assertEquals(1, calls)
    assertEquals("public", model.uiState.value.query.scope)
  }

  @Test fun `server rejection is preserved for Following and paging and retry succeeds`() = runTest(dispatcher) {
    var reject = true
    val explanation = "This feed request was rejected. Check your filters and try again."
    val model = OffersViewModel(OffersRepository { query ->
      if (query.scope == "following" && reject) {
        throw com.example.data.OffersRequestException(explanation, IOException("HTTP 400"))
      }
      page(listOf(offer("real")), "next-page")
    })
    advanceUntilIdle()
    model.setScope("following")
    advanceUntilIdle()
    assertEquals(explanation, model.uiState.value.error)
    reject = false
    model.refresh()
    advanceUntilIdle()
    assertNull(model.uiState.value.error)
    assertEquals("real", model.uiState.value.items.single().id)
    reject = true
    model.loadMore()
    advanceUntilIdle()
    assertEquals(explanation, model.uiState.value.error)
    assertEquals("real", model.uiState.value.items.single().id)
  }

  @Test fun `feed exposes sign-in action only for an authentication requirement`() = runTest(dispatcher) {
    var signInRequired = true
    val model = OffersViewModel(OffersRepository { query ->
      if (query.scope == "following") throw com.example.data.OffersRequestException(
        if (signInRequired) "Please sign in to view this collection." else "Couldn’t refresh your session. Please try again.",
        IOException("request failure"), requiresSignIn = signInRequired)
      page(emptyList())
    })
    advanceUntilIdle()
    model.setScope("following")
    advanceUntilIdle()
    assertTrue(model.uiState.value.requiresSignIn)
    signInRequired = false
    model.refresh()
    advanceUntilIdle()
    assertFalse(model.uiState.value.requiresSignIn)
    assertEquals("Couldn’t refresh your session. Please try again.", model.uiState.value.error)
  }

}
