package com.example.data.offers.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks authoritative server order and pagination cursor per query context.
 *
 * Distinguishes search queries, categories, price bounds, and discovery/proximity scopes.
 * Preserves the exact ranking returned by the backend without client-side reordering.
 */
@Entity(tableName = "feed_query_cache")
data class FeedQueryCacheEntity(
  @PrimaryKey
  val queryKey: String,
  val orderedOfferIdsJson: String, // JSON array of string IDs representing authoritative backend ranking
  val nextCursor: String?,
  val categoriesJson: String = "[]",
  val currenciesJson: String = "[]",
  val lastFetchedAt: Long = System.currentTimeMillis()
)
