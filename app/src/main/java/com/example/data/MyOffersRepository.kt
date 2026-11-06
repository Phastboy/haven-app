package com.example.data

import android.content.Context
import com.example.auth.HavenAuthClient

/**
 * Clean Architecture repository contract for My Published/Private Offers.
 */
interface MyOffersRepository {
  suspend fun getMyOffers(after: String? = null): Result<PaginatedOffers>
}

class HavenMyOffersRepository(
  private val auth: HavenAuthClient,
  private val nativePagesRepo: NativePagesRepository = HavenNativePagesRepository(auth)
) : MyOffersRepository {

  override suspend fun getMyOffers(after: String?): Result<PaginatedOffers> = runCatching {
    nativePagesRepo.getMyOffers(after)
  }

  companion object {
    fun create(context: Context): MyOffersRepository {
      val auth = HavenAuthClient.shared(context)
      return HavenMyOffersRepository(auth)
    }
  }
}
