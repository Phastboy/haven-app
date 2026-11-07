package com.example.data

import android.content.Context
import android.net.Uri
import com.example.auth.HavenAuthClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class OfferPlace(val token: String, val label: String)
data class EditorImage(val id: String? = null, val url: String, val uploadId: String = java.util.UUID.randomUUID().toString())
data class EditableOffer(
  val id: String, val slug: String, val title: String, val description: String,
  val category: String, val price: String, val currency: String,
  val place: OfferPlace?, val images: List<EditorImage>, val isPublic: Boolean
)
data class OfferFormOptions(
  val categories: List<Pair<String, String>>, val currencies: List<String>,
  val recentPlaces: List<OfferPlace>, val defaultPlaceToken: String
)

class HavenOfferEditorRepository(private val auth: HavenAuthClient, private val context: Context, private val ownerPersonId: String? = com.example.ui.model.SessionManager.session.value.personId) {
  suspend fun options(): OfferFormOptions = withContext(Dispatchers.IO) {
    val json = JSONObject(auth.authenticatedGet("/api/v1/me/offer-form", expectedPersonId = ownerPersonId))
    val categories = json.getJSONArray("categories")
    val currencies = json.getJSONArray("currencies")
    OfferFormOptions(
      List(categories.length()) { categories.getJSONObject(it).let { value -> value.getString("slug") to value.getString("label") } },
      List(currencies.length()) { currencies.getString(it) },
      places(json.getJSONArray("recent_places")), json.getString("default_place_token")
    )
  }
  suspend fun load(id: String): EditableOffer = withContext(Dispatchers.IO) {
    parse(JSONObject(auth.authenticatedGet(path(id), expectedPersonId = ownerPersonId)))
  }
  suspend fun save(id: String?, input: JSONObject): EditableOffer = withContext(Dispatchers.IO) {
    parse(JSONObject(auth.authenticatedMutation("POST", if (id == null) "/api/v1/me/offer-management" else path(id), input, expectedPersonId = ownerPersonId)))
  }
  suspend fun search(query: String): List<OfferPlace> = withContext(Dispatchers.IO) {
    places(JSONObject(auth.authenticatedMutation("POST", "/api/v1/me/offer-places/search", JSONObject().put("query", query), expectedPersonId = ownerPersonId)).getJSONArray("items"))
  }
  suspend fun upload(id: String, image: EditorImage): EditableOffer = withContext(Dispatchers.IO) {
    val bytes = context.contentResolver.openInputStream(Uri.parse(image.url))?.use { stream ->
      val buffer = java.io.ByteArrayOutputStream()
      val chunk = ByteArray(8192)
      while (true) {
        val count = stream.read(chunk)
        if (count < 0) break
        if (buffer.size() + count > 5 * 1024 * 1024) throw IOException("Choose photos up to 5 MB.")
        buffer.write(chunk, 0, count)
      }
      buffer.toByteArray()
    } ?: throw IOException("Could not open this photo. Choose it again.")
    parse(JSONObject(auth.authenticatedImageUpload(path(id) + "/images", bytes, image.uploadId, expectedPersonId = ownerPersonId)))
  }
  suspend fun remove(id: String, imageId: String): EditableOffer = withContext(Dispatchers.IO) {
    parse(JSONObject(auth.authenticatedMutation("POST", path(id) + "/images/remove", JSONObject().put("image_id", imageId), expectedPersonId = ownerPersonId)))
  }
  suspend fun reorder(id: String, images: List<EditorImage>): EditableOffer = withContext(Dispatchers.IO) {
    parse(JSONObject(auth.authenticatedMutation("POST", path(id) + "/images/reorder", JSONObject().put("image_ids", JSONArray(images.map { it.id })), expectedPersonId = ownerPersonId)))
  }
  private fun path(id: String) = "/api/v1/me/offer-management/${Uri.encode(id)}"
  companion object {
    fun places(array: JSONArray): List<OfferPlace> = List(array.length()) {
      array.getJSONObject(it).let { value -> OfferPlace(value.getString("token"), value.getString("label")) }
    }
    fun parse(json: JSONObject): EditableOffer {
      val location = json.optJSONObject("location")
      val images = json.getJSONArray("images")
      return EditableOffer(
        json.getString("id"), json.getString("slug"), json.getString("title"), json.getString("description"),
        json.getString("category"), json.getInt("price").toString(), json.getString("currency"),
        location?.let { OfferPlace(it.getString("token"), it.getString("label")) },
        List(images.length()) { images.getJSONObject(it).let { image -> EditorImage(id = image.getString("id"), url = image.getString("url")) } },
        json.getBoolean("is_public")
      )
    }
  }
}
