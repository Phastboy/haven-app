package com.example.data.offers.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedOfferDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertOffers(offers: List<FeedOfferEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertOffer(offer: FeedOfferEntity)

  @Query("SELECT * FROM feed_offers WHERE id = :id LIMIT 1")
  suspend fun getOfferById(id: String): FeedOfferEntity?

  @Query("SELECT * FROM feed_offers WHERE slug = :slug LIMIT 1")
  suspend fun getOfferBySlug(slug: String): FeedOfferEntity?

  @Query("SELECT * FROM feed_offers WHERE slug = :slug LIMIT 1")
  fun observeOfferBySlug(slug: String): Flow<FeedOfferEntity?>

  @Query("SELECT * FROM feed_offers WHERE id IN (:ids)")
  suspend fun getOffersByIds(ids: List<String>): List<FeedOfferEntity>

  @Query("SELECT * FROM feed_offers WHERE id IN (:ids)")
  fun observeOffersByIds(ids: List<String>): Flow<List<FeedOfferEntity>>

  @Query("UPDATE feed_offers SET isSaved = :isSaved WHERE slug = :slug")
  suspend fun updateSavedStatus(slug: String, isSaved: Boolean)

  @Query("DELETE FROM feed_offers WHERE cachedAt < :thresholdMillis")
  suspend fun pruneOffersOlderThan(thresholdMillis: Long)

  @Query("DELETE FROM feed_offers")
  suspend fun clearAll()
}
