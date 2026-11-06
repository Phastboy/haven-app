package com.example.data.offers

import androidx.room.withTransaction
import com.example.ui.model.SessionManager
import kotlinx.coroutines.CancellationException
import com.example.data.FeedCategory
import com.example.data.FeedOffer
import com.example.data.OffersPage
import com.example.data.OffersQuery
import com.example.data.OffersRepository
import com.example.data.messaging.db.HavenDatabase
import com.example.data.offers.db.FeedOfferDao
import com.example.data.offers.db.FeedOfferEntity
import com.example.data.offers.db.FeedQueryCacheDao
import com.example.data.offers.db.FeedQueryCacheEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface LocalFirstOffersRepository {
  suspend fun getCachedPage(query: OffersQuery): OffersPage?
  fun observeCachedPage(query: OffersQuery): Flow<OffersPage?>
  suspend fun fetchAndCache(query: OffersQuery): Result<OffersPage>
  suspend fun fetchMoreAndCache(query: OffersQuery, currentOfferIds: List<String>): Result<OffersPage>
  suspend fun getCachedOfferBySlug(slug: String): FeedOffer?
  suspend fun updateSavedStatus(slug: String, isSaved: Boolean)
  suspend fun clearCacheForQuery(query: OffersQuery)
}

/**
 * Local-First single source of truth for the Haven Offers Feed.
 *
 * Implements deterministic preservation of backend ranking order,
 * query-scoped caching, instant offline rendering, and background revalidation.
 */
class HavenLocalFirstOffersRepository(
  private val database: HavenDatabase,
  private val remoteRepository: OffersRepository,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LocalFirstOffersRepository {

  private val offerDao: FeedOfferDao = database.feedOfferDao()
  private val queryCacheDao: FeedQueryCacheDao = database.feedQueryCacheDao()

  override suspend fun getCachedPage(query: OffersQuery): OffersPage? = withContext(ioDispatcher) {
    val actor = SessionManager.session.value
    val key = computeQueryKey(query, actor.personId)
    val cache = queryCacheDao.getCache(key) ?: return@withContext null
    val page = buildOffersPageFromCache(cache)
    if (SessionManager.session.value != actor) null else page
  }

  override fun observeCachedPage(query: OffersQuery): Flow<OffersPage?> {
    val actor = SessionManager.session.value
    val key = computeQueryKey(query, actor.personId)
    return queryCacheDao.observeCache(key).combine(SessionManager.session) { cache, session ->
      if (cache == null || session != actor) null else buildOffersPageFromCache(cache)
    }.distinctUntilChanged()
  }

  override suspend fun fetchAndCache(query: OffersQuery): Result<OffersPage> = withContext(ioDispatcher) {
    try {
      val actor = SessionManager.session.value
      val key = computeQueryKey(query, actor.personId)
      val freshPage = remoteRepository.fetch(query.copy(after = null))
      if (SessionManager.session.value != actor) throw CancellationException("Account changed")

      // 1. Upsert offer records into Room
      val entities = freshPage.items.map { FeedOfferEntity.fromFeedOffer(it.copy(isSaved = null, distanceMeters = null)) }

      // 2. Persist authoritative backend ranking order
      val orderedIds = JSONArray().apply { freshPage.items.forEach { put(it.id) } }.toString()
      val categoriesJson = serializeCategories(freshPage.categories)
      val currenciesJson = JSONArray(freshPage.currencies).toString()

      val cacheEntry = FeedQueryCacheEntity(
        queryKey = key,
        orderedOfferIdsJson = orderedIds,
        nextCursor = freshPage.nextCursor,
        categoriesJson = categoriesJson,
        currenciesJson = currenciesJson,
        contextJson = serializeContext(freshPage.items),
        lastFetchedAt = System.currentTimeMillis()
      )
      database.withTransaction {
        if (SessionManager.session.value != actor) throw CancellationException("Account changed")
        offerDao.upsertOffers(entities)
        queryCacheDao.upsertCache(cacheEntry)
      }

      Result.success(freshPage)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun fetchMoreAndCache(
    query: OffersQuery,
    currentOfferIds: List<String>
  ): Result<OffersPage> = withContext(ioDispatcher) {
    try {
      val actor = SessionManager.session.value
      val key = computeQueryKey(query, actor.personId)
      val freshPage = remoteRepository.fetch(query)
      if (SessionManager.session.value != actor) throw CancellationException("Account changed")

      // 1. Upsert newly fetched offers
      val entities = freshPage.items.map { FeedOfferEntity.fromFeedOffer(it.copy(isSaved = null, distanceMeters = null)) }

      // 2. Append new IDs without duplicating
      val combinedIds = (currentOfferIds + freshPage.items.map { it.id }).distinct()
      val orderedIds = JSONArray().apply { combinedIds.forEach { put(it) } }.toString()
      val categoriesJson = serializeCategories(freshPage.categories)
      val currenciesJson = JSONArray(freshPage.currencies).toString()

      val cacheEntry = FeedQueryCacheEntity(
        queryKey = key,
        orderedOfferIdsJson = orderedIds,
        nextCursor = freshPage.nextCursor,
        categoriesJson = categoriesJson,
        currenciesJson = currenciesJson,
        contextJson = mergeContext(queryCacheDao.getCache(key)?.contextJson, freshPage.items),
        lastFetchedAt = System.currentTimeMillis()
      )
      database.withTransaction {
        if (SessionManager.session.value != actor) throw CancellationException("Account changed")
        offerDao.upsertOffers(entities)
        queryCacheDao.upsertCache(cacheEntry)
      }

      Result.success(freshPage)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun getCachedOfferBySlug(slug: String): FeedOffer? = withContext(ioDispatcher) {
    offerDao.getOfferBySlug(slug)?.toFeedOffer()?.copy(isSaved = null, distanceMeters = null)
  }

  override suspend fun updateSavedStatus(slug: String, isSaved: Boolean) = withContext(ioDispatcher) {
    // A confirmed save changes membership as well as heart state. Revalidate pages.
    queryCacheDao.clearAll()
  }

  override suspend fun clearCacheForQuery(query: OffersQuery) = withContext(ioDispatcher) {
    queryCacheDao.clearCache(computeQueryKey(query))
  }

  private suspend fun buildOffersPageFromCache(cache: FeedQueryCacheEntity): OffersPage {
    val idList = parseIdsJson(cache.orderedOfferIdsJson)
    val offersMap = offerDao.getOffersByIds(idList).associateBy { it.id }

    // Preserve the exact order dictated by the backend
    val context = JSONObject(cache.contextJson)
    val orderedOffers = idList.mapNotNull { id ->
      offersMap[id]?.toFeedOffer()?.let { offer ->
        val values = context.optJSONObject(id)
        offer.copy(
          isSaved = if (values == null || values.isNull("saved")) null else values.getBoolean("saved"),
          distanceMeters = if (values == null || values.isNull("distance")) null else values.getDouble("distance")
        )
      }
    }
    val categories = parseCategoriesJson(cache.categoriesJson)
    val currencies = parseCurrenciesJson(cache.currenciesJson)

    return OffersPage(
      items = orderedOffers,
      categories = categories,
      currencies = currencies,
      nextCursor = cache.nextCursor
    )
  }

  companion object {
    fun computeQueryKey(query: OffersQuery, actor: String? = com.example.ui.model.SessionManager.session.value.personId): String {
      val user = actor ?: "guest"
      val search = query.search.trim().lowercase()
      val category = query.category.trim()
      val currency = query.currency.trim()
      val min = query.minPrice ?: ""
      val max = query.maxPrice ?: ""
      val scope = query.scope.trim().lowercase()
      val near = query.near?.trim() ?: "none"
      // Structured encoding prevents search text from impersonating another facet.
      return JSONArray(listOf(user, scope, near, search, category, currency, min, max)).toString()
    }

    private fun serializeContext(offers: List<FeedOffer>): String = mergeContext(null, offers)

    private fun mergeContext(previous: String?, offers: List<FeedOffer>): String {
      val context = JSONObject(previous ?: "{}")
      offers.forEach { offer ->
        context.put(offer.id, JSONObject()
          .put("saved", offer.isSaved ?: JSONObject.NULL)
          .put("distance", offer.distanceMeters ?: JSONObject.NULL))
      }
      return context.toString()
    }

    private fun serializeCategories(categories: List<FeedCategory>): String {
      val array = JSONArray()
      for (cat in categories) {
        val obj = JSONObject().apply {
          put("slug", cat.slug)
          put("label", cat.label)
        }
        array.put(obj)
      }
      return array.toString()
    }

    private fun parseCategoriesJson(jsonStr: String): List<FeedCategory> {
      return runCatching {
        val array = JSONArray(jsonStr)
        List(array.length()) { index ->
          val obj = array.getJSONObject(index)
          FeedCategory(obj.getString("slug"), obj.getString("label"))
        }
      }.getOrDefault(emptyList())
    }

    private fun parseCurrenciesJson(jsonStr: String): List<String> {
      return runCatching {
        val array = JSONArray(jsonStr)
        List(array.length()) { array.getString(it) }
      }.getOrDefault(emptyList())
    }

    private fun parseIdsJson(jsonStr: String): List<String> {
      return runCatching {
        val array = JSONArray(jsonStr)
        List(array.length()) { array.getString(it) }
      }.getOrDefault(emptyList())
    }
  }
}
