package com.example.data

import android.content.Context
import com.example.auth.HavenAuthClient
import kotlinx.coroutines.CancellationException

/**
 * Clean Architecture repository contract for Inquired Offers.
 */
interface InquiredRepository {
  /** Fetch one server-ordered page of the authenticated person’s collection. */
  suspend fun getInquired(after: String? = null): Result<PaginatedOffers>
}

/** Delegate Inquired reads to native pages without swallowing coroutine cancellation. */
class HavenInquiredRepository(
  auth: HavenAuthClient,
  private val nativePagesRepo: NativePagesRepository = HavenNativePagesRepository(auth)
) : InquiredRepository {

  /** Return a real native page or a request failure; cancellation remains cancellation. */
  override suspend fun getInquired(after: String?): Result<PaginatedOffers> = try {
    Result.success(nativePagesRepo.getInquired(after))
  } catch (cancelled: CancellationException) {
    throw cancelled
  } catch (error: Exception) {
    Result.failure(error)
  }

  companion object {
    /** Use the application’s shared authenticated client for collection requests. */
    fun create(context: Context): InquiredRepository {
      val auth = HavenAuthClient.shared(context)
      return HavenInquiredRepository(auth)
    }
  }
}
