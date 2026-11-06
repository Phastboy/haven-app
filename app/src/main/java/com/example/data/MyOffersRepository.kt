package com.example.data

import android.content.Context
import android.net.Uri
import com.example.auth.HavenAuthClient
import org.json.JSONObject

/**
 * Clean Architecture repository contract for My Published/Private Offers.
 */
interface MyOffersRepository {
  suspend fun getMyOffers(after: String? = null): Result<PaginatedOffers>
  suspend fun deleteOffer(slug: String): Result<Boolean>
}

class HavenMyOffersRepository(
  private val auth: HavenAuthClient,
  private val ownerPersonId: String? = com.example.ui.model.SessionManager.session.value.personId,
  private val nativePagesRepo: NativePagesRepository = HavenNativePagesRepository(auth)
) : MyOffersRepository {

  override suspend fun getMyOffers(after: String?): Result<PaginatedOffers> = runCatching {
    nativePagesRepo.getMyOffers(after)
  }

  override suspend fun deleteOffer(slug: String): Result<Boolean> = runCatching {
    val raw = auth.authenticatedMutation("DELETE", "/api/v1/offers/${Uri.encode(slug)}", expectedPersonId = ownerPersonId)
    JSONObject(raw).getBoolean("deleted")
  }

  companion object {
    fun create(context: Context): MyOffersRepository {
      val auth = HavenAuthClient.shared(context)
      return HavenMyOffersRepository(auth)
    }
  }
}
