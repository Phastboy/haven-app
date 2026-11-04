package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.FeedCategory
import com.example.data.FeedOffer
import com.example.data.HavenOffersRepository
import com.example.data.OffersPage
import com.example.data.OffersQuery
import com.example.data.OffersRepository
import com.example.data.messaging.db.HavenDatabase
import com.example.data.offers.HavenLocalFirstOffersRepository
import com.example.data.offers.db.FeedOfferEntity
import com.example.ui.model.OffersViewModel
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class HavenLocalFirstOffersRepositoryTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var context: Context
  private lateinit var database: HavenDatabase

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()
    val directExecutor = java.util.concurrent.Executor { it.run() }
    database = HavenDatabase.createInMemory(context, directExecutor)
  }

  @After
  fun tearDown() {
    database.close()
    Dispatchers.resetMain()
  }

  private fun sampleOffer(id: String, slug: String = "offer-$id") = FeedOffer(
    id = id,
    slug = slug,
    title = "Ceramic Bowl $id",
    makerName = "Adaeze Okafor",
    priceDisplay = "Free",
    coverUrl = "https://haven.app/covers/$id.jpg",
    category = "pottery",
    makerHandle = "@adaeze"
  )

  @Test
  fun testCachedFeedOpeningReturnsImmediatelyFromRoom() = runTest(testDispatcher) {
    val query = OffersQuery()
    val localRepo = HavenLocalFirstOffersRepository(
      database = database,
      remoteRepository = OffersRepository { OffersPage(emptyList(), emptyList(), emptyList(), null) },
      ioDispatcher = testDispatcher
    )

    // Pre-populate Room cache with authoritative order: ["2", "1"]
    val entities = listOf(sampleOffer("2"), sampleOffer("1")).map { FeedOfferEntity.fromFeedOffer(it) }
    database.feedOfferDao().upsertOffers(entities)
    val queryKey = HavenLocalFirstOffersRepository.computeQueryKey(query)
    database.feedQueryCacheDao().upsertCache(
      com.example.data.offers.db.FeedQueryCacheEntity(
        queryKey = queryKey,
        orderedOfferIdsJson = "[\"2\",\"1\"]",
        nextCursor = "cursor-next",
        categoriesJson = "[{\"slug\":\"pottery\",\"label\":\"Pottery\"}]",
        currenciesJson = "[\"NGN\"]"
      )
    )

    // Read cache without making any network call
    val cachedPage = localRepo.getCachedPage(query)
    assertNotNull(cachedPage)
    assertEquals(listOf("2", "1"), cachedPage!!.items.map { it.id })
    assertEquals("cursor-next", cachedPage.nextCursor)
    assertEquals("pottery", cachedPage.categories.first().slug)
  }

  @Test
  fun testColdStartWithNoCacheFetchesAndPersistsToRoom() = runTest(testDispatcher) {
    val query = OffersQuery()
    var remoteCalled = false
    val remoteRepo = OffersRepository {
      remoteCalled = true
      OffersPage(
        items = listOf(sampleOffer("alpha"), sampleOffer("beta")),
        categories = listOf(FeedCategory("woodwork", "Woodwork")),
        currencies = listOf("USD"),
        nextCursor = "cursor-2"
      )
    }
    val localRepo = HavenLocalFirstOffersRepository(
      database = database,
      remoteRepository = remoteRepo,
      ioDispatcher = testDispatcher
    )

    // Initially Room has nothing
    assertNull(localRepo.getCachedPage(query))

    // Fetch and cache
    val result = localRepo.fetchAndCache(query)
    assertTrue(result.isSuccess)
    assertTrue(remoteCalled)

    // Room should now persist offers and query cache
    val cachedAfter = localRepo.getCachedPage(query)
    assertNotNull(cachedAfter)
    assertEquals(listOf("alpha", "beta"), cachedAfter!!.items.map { it.id })
    assertEquals("cursor-2", cachedAfter.nextCursor)
  }

  @Test
  fun testOfflineOpeningPreservesCachedOffers() = runTest(testDispatcher) {
    val query = OffersQuery()
    val remoteRepo = OffersRepository { throw IOException("No network connection") }
    val localRepo = HavenLocalFirstOffersRepository(
      database = database,
      remoteRepository = remoteRepo,
      ioDispatcher = testDispatcher
    )

    // Pre-populate Room
    val entities = listOf(sampleOffer("cached-1")).map { FeedOfferEntity.fromFeedOffer(it) }
    database.feedOfferDao().upsertOffers(entities)
    database.feedQueryCacheDao().upsertCache(
      com.example.data.offers.db.FeedQueryCacheEntity(
        queryKey = HavenLocalFirstOffersRepository.computeQueryKey(query),
        orderedOfferIdsJson = "[\"cached-1\"]",
        nextCursor = null
      )
    )

    // Attempt network fetch which fails
    val result = localRepo.fetchAndCache(query)
    assertTrue(result.isFailure)

    // Cache remains intact and available to the user
    val cachedPage = localRepo.getCachedPage(query)
    assertNotNull(cachedPage)
    assertEquals("cached-1", cachedPage!!.items.single().id)
  }

  @Test
  fun testDistinctQueriesPreserveSeparateCaches() = runTest(testDispatcher) {
    val queryGeneral = OffersQuery()
    val queryPottery = OffersQuery(category = "pottery")
    val querySearch = OffersQuery(search = "table")

    val localRepo = HavenLocalFirstOffersRepository(
      database = database,
      remoteRepository = OffersRepository { OffersPage(emptyList(), emptyList(), emptyList(), null) },
      ioDispatcher = testDispatcher
    )

    val keyGeneral = HavenLocalFirstOffersRepository.computeQueryKey(queryGeneral)
    val keyPottery = HavenLocalFirstOffersRepository.computeQueryKey(queryPottery)
    val keySearch = HavenLocalFirstOffersRepository.computeQueryKey(querySearch)

    // Ensure query keys distinguish filters and search
    assertTrue(keyGeneral != keyPottery)
    assertTrue(keyGeneral != keySearch)
    assertTrue(keyPottery != keySearch)
  }

  @Test
  fun testPaginationAppendsAndDeduplicates() = runTest(testDispatcher) {
    val query = OffersQuery()
    val remoteRepo = OffersRepository {
      OffersPage(
        items = listOf(sampleOffer("page2-1"), sampleOffer("existing-1")), // includes duplicate
        categories = emptyList(),
        currencies = emptyList(),
        nextCursor = null
      )
    }
    val localRepo = HavenLocalFirstOffersRepository(
      database = database,
      remoteRepository = remoteRepo,
      ioDispatcher = testDispatcher
    )

    val currentIds = listOf("existing-1", "existing-2")
    database.feedOfferDao().upsertOffers(
      listOf(sampleOffer("existing-1"), sampleOffer("existing-2")).map { FeedOfferEntity.fromFeedOffer(it) }
    )
    val result = localRepo.fetchMoreAndCache(query, currentIds)
    assertTrue(result.isSuccess)

    val cached = localRepo.getCachedPage(query)
    assertNotNull(cached)
    // Combined without duplicates: existing-1, existing-2, page2-1
    assertEquals(listOf("existing-1", "existing-2", "page2-1"), cached!!.items.map { it.id })
  }

  @Test
  fun testInstantOfferDetailSummaryAvailableFromFeedCache() = runTest(testDispatcher) {
    val localRepo = HavenLocalFirstOffersRepository(
      database = database,
      remoteRepository = OffersRepository { OffersPage(emptyList(), emptyList(), emptyList(), null) },
      ioDispatcher = testDispatcher
    )

    val offer = sampleOffer("detail-1", slug = "handmade-ceramic-bowl")
    database.feedOfferDao().upsertOffer(FeedOfferEntity.fromFeedOffer(offer))

    val retrieved = localRepo.getCachedOfferBySlug("handmade-ceramic-bowl")
    assertNotNull(retrieved)
    assertEquals("detail-1", retrieved!!.id)
    assertEquals("Ceramic Bowl detail-1", retrieved.title)
    assertEquals("Free", retrieved.priceDisplay)
  }

  @Test
  fun testViewModelInstantCacheDisplayAndScrollRetention() = runTest(testDispatcher) {
    val localRepo = HavenLocalFirstOffersRepository(
      database = database,
      remoteRepository = OffersRepository { OffersPage(listOf(sampleOffer("net-1")), emptyList(), emptyList(), null) },
      ioDispatcher = testDispatcher
    )

    // Pre-populate Room
    val entities = listOf(sampleOffer("cached-first")).map { FeedOfferEntity.fromFeedOffer(it) }
    database.feedOfferDao().upsertOffers(entities)
    database.feedQueryCacheDao().upsertCache(
      com.example.data.offers.db.FeedQueryCacheEntity(
        queryKey = HavenLocalFirstOffersRepository.computeQueryKey(OffersQuery()),
        orderedOfferIdsJson = "[\"cached-first\"]",
        nextCursor = null
      )
    )

    val viewModel = OffersViewModel(
      repository = HavenOffersRepository(),
      localOffersRepository = localRepo
    )

    // Update scroll coordinates
    viewModel.updateScrollPosition(index = 8, offset = 120)
    assertEquals(8, viewModel.scrollIndex)
    assertEquals(120, viewModel.scrollOffset)

    advanceUntilIdle()

    // Items should be populated
    assertTrue(viewModel.uiState.value.items.isNotEmpty())
  }
}
