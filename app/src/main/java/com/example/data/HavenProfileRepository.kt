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

interface ProfileRepository {
  suspend fun load(): ProfileOverview
  suspend fun updateProfile(displayName: String, handle: String, bio: String?): Result<ProfileOverview>
  suspend fun uploadAvatar(bytes: ByteArray, mimeType: String = "image/jpeg"): Result<ProfileOverview>
  suspend fun removeAvatar(): Result<ProfileOverview>
}

class HavenProfileRepository(private val auth: HavenAuthClient) : ProfileRepository {
  override suspend fun load(): ProfileOverview = withContext(Dispatchers.IO) {
    parseProfile(auth.authenticatedGet("/api/v1/me/profile"))
  }

  override suspend fun updateProfile(
    displayName: String,
    handle: String,
    bio: String?
  ): Result<ProfileOverview> = withContext(Dispatchers.IO) {
    runCatching {
      val payload = JSONObject().apply {
        put("display_name", displayName)
        put("handle", handle)
        if (bio.isNullOrBlank()) {
          put("bio", JSONObject.NULL)
        } else {
          put("bio", bio.trim())
        }
      }
      auth.authenticatedMutation("POST", "/api/v1/me/profile", payload)
      val updated = load()
      com.example.ui.model.SessionManager.signInPerson(updated.id, updated.displayName, updated.handle)
      updated
    }
  }

  override suspend fun uploadAvatar(
    bytes: ByteArray,
    mimeType: String
  ): Result<ProfileOverview> = withContext(Dispatchers.IO) {
    runCatching {
      auth.authenticatedMultipartUpload(
        path = "/api/v1/me/avatar",
        fieldName = "avatar",
        filename = "avatar.jpg",
        bytes = bytes,
        mimeType = mimeType
      )
      load()
    }
  }

  override suspend fun removeAvatar(): Result<ProfileOverview> = withContext(Dispatchers.IO) {
    runCatching {
      auth.authenticatedMutation("DELETE", "/api/v1/me/avatar")
      load()
    }
  }

  companion object {
    fun parseProfile(body: String): ProfileOverview {
      val json = JSONObject(body)
      val counts = json.getJSONObject("counts")
      val offers = json.getJSONArray("offers")
      return ProfileOverview(
        id = json.getString("id"),
        handle = json.getString("handle"),
        displayName = json.getString("display_name"),
        bio = if (json.isNull("bio")) null else json.getString("bio"),
        joined = json.getString("joined"),
        avatarUrl = if (json.isNull("avatar_url")) null else json.getString("avatar_url"),
        offerCount = counts.getLong("offers"),
        savedCount = counts.getLong("saved"),
        followingCount = counts.getLong("following"),
        offers = List(offers.length()) { HavenOffersRepository.parseOffer(offers.getJSONObject(it)) }
      )
    }
  }
}
