package com.example.data.offers.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.FeedOffer

/**
 * Persisted offer summary for instant, local-first rendering in the feed and detail screens.
 */
@Entity(
  tableName = "feed_offers",
  indices = [
    Index(value = ["slug"], unique = true),
    Index(value = ["category"]),
    Index(value = ["cachedAt"])
  ]
)
data class FeedOfferEntity(
  @PrimaryKey
  val id: String,
  val slug: String,
  val title: String,
  val makerName: String,
  val makerHandle: String,
  val priceDisplay: String,
  val coverUrl: String?,
  val category: String = "",
  val distanceMeters: Double? = null,
  val cachedAt: Long = System.currentTimeMillis(),
  val isSaved: Boolean? = null
) {
  fun toFeedOffer(): FeedOffer = FeedOffer(
    id = id,
    slug = slug,
    title = title,
    makerName = makerName,
    priceDisplay = priceDisplay,
    coverUrl = coverUrl,
    distanceMeters = distanceMeters,
    category = category,
    makerHandle = makerHandle
  )

  companion object {
    fun fromFeedOffer(offer: FeedOffer, isSaved: Boolean? = null): FeedOfferEntity = FeedOfferEntity(
      id = offer.id,
      slug = offer.slug,
      title = offer.title,
      makerName = offer.makerName,
      makerHandle = offer.makerHandle,
      priceDisplay = offer.priceDisplay,
      coverUrl = offer.coverUrl,
      category = offer.category,
      distanceMeters = offer.distanceMeters,
      cachedAt = System.currentTimeMillis(),
      isSaved = isSaved
    )
  }
}
