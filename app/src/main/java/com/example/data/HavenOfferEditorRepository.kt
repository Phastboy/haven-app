package com.example.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.example.auth.HavenAuthClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import com.example.images.OfferPhotoReads
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class OfferPlace(val token: String, val label: String, val attribution: String? = null, val sourceUrl: String? = null)
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

/** Offer editing boundary shared by the screen and state-transition tests. */
interface OfferEditorRepository {
  suspend fun options(): OfferFormOptions
  suspend fun load(id: String): EditableOffer
  suspend fun save(id: String?, input: JSONObject): EditableOffer
  suspend fun search(query: String): List<OfferPlace>
  suspend fun capturedPlace(point: com.example.location.CapturedLocation): OfferPlace
  suspend fun upload(id: String, image: EditorImage): EditableOffer
  suspend fun remove(id: String, imageId: String): EditableOffer
  suspend fun reorder(id: String, images: List<EditorImage>): EditableOffer
}

class HavenOfferEditorRepository(private val auth: HavenAuthClient, private val context: Context, private val ownerPersonId: String? = com.example.ui.model.SessionManager.session.value.personId) : OfferEditorRepository {
  override suspend fun options(): OfferFormOptions = withContext(Dispatchers.IO) {
    val json = JSONObject(auth.authenticatedGet("/api/v1/me/offer-form", expectedPersonId = ownerPersonId))
    val categories = json.getJSONArray("categories")
    val currencies = json.getJSONArray("currencies")
    OfferFormOptions(
      List(categories.length()) { categories.getJSONObject(it).let { value -> value.getString("slug") to value.getString("label") } },
      List(currencies.length()) { currencies.getString(it) },
      places(json.getJSONArray("recent_places")), json.getString("default_place_token")
    )
  }
  override suspend fun load(id: String): EditableOffer = withContext(Dispatchers.IO) {
    parse(JSONObject(auth.authenticatedGet(path(id), expectedPersonId = ownerPersonId)))
  }
  override suspend fun save(id: String?, input: JSONObject): EditableOffer = withContext(Dispatchers.IO) {
    parse(JSONObject(auth.authenticatedMutation("POST", if (id == null) "/api/v1/me/offer-management" else path(id), input, expectedPersonId = ownerPersonId)))
  }
  override suspend fun search(query: String): List<OfferPlace> = withContext(Dispatchers.IO) {
    places(JSONObject(auth.authenticatedMutation("POST", "/api/v1/me/offer-places/search", JSONObject().put("query", query), expectedPersonId = ownerPersonId)).getJSONArray("items"))
  }
  /** Resolves an explicit device point to a readable signed place; writes no offer. */
  override suspend fun capturedPlace(point: com.example.location.CapturedLocation): OfferPlace = withContext(Dispatchers.IO) {
    val json = JSONObject(auth.authenticatedMutation("POST", "/api/v1/me/offer-places/resolve",
      JSONObject().put("latitude", point.latitude).put("longitude", point.longitude),
      expectedPersonId = ownerPersonId))
    OfferPlace(json.getString("token"), json.getString("label"), json.optString("attribution").takeIf { it.isNotBlank() }, json.optString("source_url").takeIf { it == "https://www.geoapify.com/" })
  }
  override suspend fun upload(id: String, image: EditorImage): EditableOffer = withContext(Dispatchers.IO) {
    val bytes = withTimeout(15_000L) { OfferPhotoReads.run { context.contentResolver.openInputStream(image.url.toUri())?.use { stream ->
      val buffer = java.io.ByteArrayOutputStream()
      val chunk = ByteArray(8192)
      while (true) {
        val count = stream.read(chunk)
        if (count < 0) break
        if (buffer.size().toLong() + count > com.example.images.OfferImageInputs.MAX_IMAGE_BYTES) throw IOException("Choose photos up to 5 MB.")
        buffer.write(chunk, 0, count)
      }
      buffer.toByteArray()
    } ?: throw IOException("Could not open this photo. Choose it again.") } }
    parse(JSONObject(auth.authenticatedImageUpload(path(id) + "/images", bytes, image.uploadId, expectedPersonId = ownerPersonId)))
  }
  override suspend fun remove(id: String, imageId: String): EditableOffer = withContext(Dispatchers.IO) {
    parse(JSONObject(auth.authenticatedMutation("POST", path(id) + "/images/remove", JSONObject().put("image_id", imageId), expectedPersonId = ownerPersonId)))
  }
  override suspend fun reorder(id: String, images: List<EditorImage>): EditableOffer = withContext(Dispatchers.IO) {
    parse(JSONObject(auth.authenticatedMutation("POST", path(id) + "/images/reorder", JSONObject().put("image_ids", JSONArray(images.map { it.id })), expectedPersonId = ownerPersonId)))
  }
  private fun path(id: String) = "/api/v1/me/offer-management/${Uri.encode(id)}"
  companion object {
    fun places(array: JSONArray): List<OfferPlace> = List(array.length()) {
      array.getJSONObject(it).let { value -> OfferPlace(value.getString("token"), value.getString("label"), value.optString("attribution").takeIf { it.isNotBlank() }, value.optString("source_url").takeIf { it == "https://www.geoapify.com/" }) }
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
