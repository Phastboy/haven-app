package com.example.data

import android.content.Context
import com.example.auth.HavenAuthClient
import kotlinx.coroutines.CancellationException

/**
 * Clean Architecture repository contract for Inquired Offers.
 */
interface InquiredRepository {
  suspend fun getInquired(after: String? = null): Result<PaginatedOffers>
}

class HavenInquiredRepository(
  auth: HavenAuthClient,
  private val nativePagesRepo: NativePagesRepository = HavenNativePagesRepository(auth)
) : InquiredRepository {

  override suspend fun getInquired(after: String?): Result<PaginatedOffers> = try {
    Result.success(nativePagesRepo.getInquired(after))
  } catch (cancelled: CancellationException) {
    throw cancelled
  } catch (error: Exception) {
    Result.failure(error)
  }

  companion object {
    fun create(context: Context): InquiredRepository {
      val auth = HavenAuthClient.shared(context)
      return HavenInquiredRepository(auth)
    }
  }
}
