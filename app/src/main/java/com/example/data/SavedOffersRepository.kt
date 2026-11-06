package com.example.data

import android.content.Context
import com.example.auth.HavenAuthClient

/**
 * Clean Architecture repository contract for Saved Offers.
 */
interface SavedOffersRepository {
  suspend fun getSavedOffers(after: String? = null): Result<PaginatedOffers>
  suspend fun unsaveOffer(slug: String): Result<Boolean>
}

class HavenSavedOffersRepository(
  private val auth: HavenAuthClient,
  private val nativePagesRepo: NativePagesRepository = HavenNativePagesRepository(auth),
  private val detailRepo: HavenOfferDetailRepository = HavenOfferDetailRepository(auth)
) : SavedOffersRepository {

  override suspend fun getSavedOffers(after: String?): Result<PaginatedOffers> = runCatching {
    nativePagesRepo.getSavedOffers(after)
  }

  override suspend fun unsaveOffer(slug: String): Result<Boolean> = runCatching {
    detailRepo.setSaved(slug, saved = false)
  }

  companion object {
    fun create(context: Context): SavedOffersRepository {
      val auth = HavenAuthClient.shared(context)
      return HavenSavedOffersRepository(auth)
    }
  }
}
