package com.example.data

import com.example.auth.HavenAuthClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ProfileOverview(
  val id: String,
  val handle: String,
  val displayName: String,
  val bio: String?,
  val joined: String,
  val avatarUrl: String?,
  val offerCount: Long,
  val savedCount: Long,
  val followingCount: Long,
  val offers: List<FeedOffer>
)

fun interface ProfileRepository { suspend fun load(): ProfileOverview }

class HavenProfileRepository(private val auth: HavenAuthClient) : ProfileRepository {
  override suspend fun load(): ProfileOverview = withContext(Dispatchers.IO) {
    parseProfile(auth.authenticatedGet("/api/v1/me/profile"))
  }
  companion object {
    fun parseProfile(body: String): ProfileOverview {
      val json = JSONObject(body)
      val counts = json.getJSONObject("counts")
      val offers = json.getJSONArray("offers")
      return ProfileOverview(json.getString("id"), json.getString("handle"), json.getString("display_name"),
        if (json.isNull("bio")) null else json.getString("bio"), json.getString("joined"),
        if (json.isNull("avatar_url")) null else json.getString("avatar_url"),
        counts.getLong("offers"), counts.getLong("saved"), counts.getLong("following"),
        List(offers.length()) { HavenOffersRepository.parseOffer(offers.getJSONObject(it)) })
    }
  }
}
