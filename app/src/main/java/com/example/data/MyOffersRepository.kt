package com.example.data

import android.content.Context
import android.net.Uri
import com.example.auth.HavenAuthClient
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Clean Architecture repository contract for My Published/Private Offers.
 */
interface MyOffersRepository {
  suspend fun getMyOffers(after: String? = null): Result<PaginatedOffers>
  suspend fun deleteOffer(slug: String): Result<Boolean>
}

class HavenMyOffersRepository(
  private val auth: HavenAuthClient,
  private val ownerPersonId: String? = com.example.ui.model.SessionManager.session.value.personId
) : MyOffersRepository {

  override suspend fun getMyOffers(after: String?): Result<PaginatedOffers> = withContext(Dispatchers.IO) {
    runCatching {
      val query = after?.let { "?after=${Uri.encode(it)}" }.orEmpty()
      val json = JSONObject(auth.authenticatedGet("/api/v1/me/offer-management$query", expectedPersonId = ownerPersonId))
      val items = json.getJSONArray("items")
      PaginatedOffers(List(items.length()) { HavenOffersRepository.parseOffer(items.getJSONObject(it)) },
        if (json.isNull("next_cursor")) null else json.getString("next_cursor"))
    }
  }

  override suspend fun deleteOffer(slug: String): Result<Boolean> = withContext(Dispatchers.IO) {
    runCatching {
      val raw = auth.authenticatedMutation("DELETE", "/api/v1/offers/${Uri.encode(slug)}", expectedPersonId = ownerPersonId)
      JSONObject(raw).getBoolean("deleted")
    }
  }

  companion object {
    fun create(context: Context, ownerPersonId: String? = com.example.ui.model.SessionManager.session.value.personId): MyOffersRepository {
      val auth = HavenAuthClient.shared(context)
      return HavenMyOffersRepository(auth, ownerPersonId)
    }
  }
}
