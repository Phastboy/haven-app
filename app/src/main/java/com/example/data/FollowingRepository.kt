package com.example.data

import android.content.Context
import com.example.auth.HavenAuthClient

/**
 * Clean Architecture repository contract for Following.
 */
interface FollowingRepository {
  suspend fun getFollowing(after: String? = null): Result<PaginatedFollowing>
}

class HavenFollowingRepository(
  private val auth: HavenAuthClient,
  private val nativePagesRepo: NativePagesRepository = HavenNativePagesRepository(auth)
) : FollowingRepository {

  override suspend fun getFollowing(after: String?): Result<PaginatedFollowing> = runCatching {
    nativePagesRepo.getFollowing(after)
  }

  companion object {
    fun create(context: Context): FollowingRepository {
      val auth = HavenAuthClient.shared(context)
      return HavenFollowingRepository(auth)
    }
  }
}
