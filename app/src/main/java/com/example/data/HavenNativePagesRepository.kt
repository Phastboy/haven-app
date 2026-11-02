package com.example.data

import android.net.Uri
import com.example.auth.HavenAuthClient
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class Person(
  val id: String,
  val handle: String,
  val displayName: String,
  val bio: String?,
  val joined: String,
  val avatarUrl: String?
)

data class ProviderProfile(
  val person: Person,
  val offers: List<FeedOffer>,
  val nextCursor: String?
)

data class PaginatedOffers(
  val items: List<FeedOffer>,
  val nextCursor: String?
)

data class PaginatedFollowing(
  val items: List<Person>,
  val nextCursor: String?
)

interface NativePagesRepository {
  suspend fun getProviderProfile(handle: String, after: String? = null, authenticated: Boolean = false): ProviderProfile
  suspend fun getMyOffers(after: String? = null): PaginatedOffers
  suspend fun getSavedOffers(after: String? = null): PaginatedOffers
  suspend fun getFollowing(after: String? = null): PaginatedFollowing
}

class HavenNativePagesRepository(private val auth: HavenAuthClient) : NativePagesRepository {
  override suspend fun getProviderProfile(handle: String, after: String?, authenticated: Boolean): ProviderProfile =
    withContext(Dispatchers.IO) {
      val query = if (!after.isNullOrBlank()) "?after=${Uri.encode(after)}" else ""
      val path = "/api/v1/profiles/${Uri.encode(handle)}$query"
      val raw = if (authenticated) auth.authenticatedGet(path) else auth.publicGet(path)
      val json = JSONObject(raw)
      val personJson = json.getJSONObject("person")
      val offersJson = json.getJSONObject("offers")
      val itemsArray = offersJson.getJSONArray("items")

      ProviderProfile(
        person = parsePerson(personJson),
        offers = List(itemsArray.length()) { HavenOffersRepository.parseOffer(itemsArray.getJSONObject(it)) },
        nextCursor = if (offersJson.isNull("next_cursor")) null else offersJson.getString("next_cursor")
      )
    }

  override suspend fun getMyOffers(after: String?): PaginatedOffers = withContext(Dispatchers.IO) {
    val query = if (!after.isNullOrBlank()) "?after=${Uri.encode(after)}" else ""
    val path = "/api/v1/me/offers$query"
    val raw = auth.authenticatedGet(path)
    val json = JSONObject(raw)
    val itemsArray = json.getJSONArray("items")
    PaginatedOffers(
      items = List(itemsArray.length()) { HavenOffersRepository.parseOffer(itemsArray.getJSONObject(it)) },
      nextCursor = if (json.isNull("next_cursor")) null else json.getString("next_cursor")
    )
  }

  override suspend fun getSavedOffers(after: String?): PaginatedOffers = withContext(Dispatchers.IO) {
    val query = if (!after.isNullOrBlank()) "?after=${Uri.encode(after)}" else ""
    val path = "/api/v1/me/saved$query"
    val raw = auth.authenticatedGet(path)
    val json = JSONObject(raw)
    val itemsArray = json.getJSONArray("items")
    PaginatedOffers(
      items = List(itemsArray.length()) { HavenOffersRepository.parseOffer(itemsArray.getJSONObject(it)) },
      nextCursor = if (json.isNull("next_cursor")) null else json.getString("next_cursor")
    )
  }

  override suspend fun getFollowing(after: String?): PaginatedFollowing = withContext(Dispatchers.IO) {
    val query = if (!after.isNullOrBlank()) "?after=${Uri.encode(after)}" else ""
    val path = "/api/v1/me/following$query"
    val raw = auth.authenticatedGet(path)
    val json = JSONObject(raw)
    val itemsArray = json.getJSONArray("items")
    PaginatedFollowing(
      items = List(itemsArray.length()) { parsePerson(itemsArray.getJSONObject(it)) },
      nextCursor = if (json.isNull("next_cursor")) null else json.getString("next_cursor")
    )
  }

  companion object {
    fun parsePerson(json: JSONObject): Person = Person(
      id = json.getString("id"),
      handle = json.getString("handle"),
      displayName = json.getString("display_name"),
      bio = if (json.isNull("bio")) null else json.getString("bio"),
      joined = json.getString("joined"),
      avatarUrl = if (json.isNull("avatar_url")) null else json.getString("avatar_url")
    )
  }
}
