package com.example.data

import android.net.Uri
import com.example.auth.HavenAuthClient
import java.io.IOException
import com.example.data.network.HavenNetwork
import io.ktor.http.Parameters
import io.ktor.http.formUrlEncode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class FeedOffer(
  val id: String,
  val slug: String,
  val title: String,
  val makerName: String,
  val priceDisplay: String,
  val coverUrl: String?,
  val distanceMeters: Double? = null,
  val category: String = "",
  val makerHandle: String = "",
  val isPublic: Boolean = true,
  val isSaved: Boolean? = null,
  val location: String? = null
)

data class OfferDetail(
  val id: String, val slug: String, val title: String, val description: String,
  val category: String, val priceDisplay: String, val makerName: String,
  val makerHandle: String, val images: List<String>, val location: String?, val isSaved: Boolean
)

class HavenOfferDetailRepository(private val auth: HavenAuthClient) {
  suspend fun load(slug: String, authenticated: Boolean = true): OfferDetail = withContext(Dispatchers.IO) {
    val path = "/api/v1/offers/${Uri.encode(slug)}"
    val json = JSONObject(if (authenticated) auth.authenticatedGet(path) else auth.publicGet(path))
    val images = json.getJSONArray("images")
    OfferDetail(
      id = json.getString("id"),
      slug = json.getString("slug"),
      title = json.getString("title"),
      description = json.getString("description"),
      category = json.getString("category"),
      priceDisplay = json.getString("price_display"),
      makerName = json.getString("maker_name"),
      makerHandle = json.getString("maker_handle"),
      images = List(images.length()) { images.getString(it) },
      location = if (json.isNull("location")) null else json.getString("location"),
      isSaved = json.getBoolean("is_saved")
    )
  }

  suspend fun setSaved(slug: String, saved: Boolean): Boolean = withContext(Dispatchers.IO) {
    val method = if (saved) "POST" else "DELETE"
    val body = JSONObject(auth.authenticatedMutation(method, "/api/v1/offers/${Uri.encode(slug)}/saved"))
    body.getBoolean("is_saved")
  }
}

data class FeedCategory(val slug: String, val label: String)
data class OffersQuery(
  val search: String = "",
  val category: String = "",
  val currency: String = "",
  val minPrice: Int? = null,
  val maxPrice: Int? = null,
  val after: String? = null,
  val scope: String = "public",
  val near: String? = null
)
data class OffersPage(
  val items: List<FeedOffer>,
  val categories: List<FeedCategory>,
  val currencies: List<String>,
  val nextCursor: String?
)

fun interface OffersRepository {
  suspend fun fetch(query: OffersQuery): OffersPage
}

/** A server rejection with a safe, user-facing explanation. */
class OffersRequestException(message: String, cause: Throwable) : IOException(message, cause)

/** Public discovery; the server owns filtering, visibility, and result order. */
class HavenOffersRepository(
  private val auth: HavenAuthClient? = null
) : OffersRepository {
  override suspend fun fetch(query: OffersQuery): OffersPage = withContext(Dispatchers.IO) {
    val parameters = Parameters.build {
      if (query.search.isNotBlank()) append("q", query.search.trim())
      if (query.category.isNotBlank()) append("category", query.category)
      if (query.currency.isNotBlank()) append("currency", query.currency)
      query.minPrice?.let { append("min_price", it.toString()) }
      query.maxPrice?.let { append("max_price", it.toString()) }
      query.after?.let { append("after", it) }
      if (query.scope != "public" && query.scope.isNotBlank()) append("scope", query.scope)
      if (!query.near.isNullOrBlank()) append("near", query.near)
    }.formUrlEncode()
    val path = "/api/v1/offers" + if (parameters.isBlank()) "" else "?$parameters"
    try {
      val raw = if (auth != null) auth.optionalGet(path) else {
        val response = HavenNetwork.shared.request("GET", path)
        if (response.first != 200) throw HavenAuthClient.HttpException(response.first, response.second)
        response.second
      }
      parsePage(raw)
    } catch (error: HavenAuthClient.HttpException) {
      throw OffersRequestException(when (error.statusCode) {
        400 -> "This feed request was rejected. Check your filters and try again."
        401 -> "Please sign in to view this collection."
        else -> "Couldn’t load offers. Please try again."
      }, error)
    }
  }

  companion object {
    fun parseOffer(item: JSONObject) = FeedOffer(
      id = item.getString("id"),
      slug = item.getString("slug"),
      title = item.getString("title"),
      makerName = item.getString("maker_name"),
      priceDisplay = item.getString("price_display"),
      coverUrl = if (item.isNull("cover_url")) null else item.getString("cover_url"),
      distanceMeters = if (item.isNull("distance_meters")) null else item.getDouble("distance_meters"),
      category = if (item.isNull("category")) "" else item.getString("category"),
      makerHandle = if (item.isNull("maker_handle")) "" else item.getString("maker_handle"),
      isPublic = item.optBoolean("is_public", true),
      isSaved = if (item.has("is_saved") && !item.isNull("is_saved")) item.getBoolean("is_saved") else null,
      location = if (item.isNull("location")) null else item.getString("location")
    )

    fun parsePage(body: String): OffersPage {
      val json = JSONObject(body)
      val items = json.getJSONArray("items")
      val categories = json.getJSONArray("categories")
      val currencies = json.getJSONArray("currencies")
      return OffersPage(
        items = List(items.length()) { parseOffer(items.getJSONObject(it)) },
        categories = List(categories.length()) { index ->
          val category = categories.getJSONObject(index)
          FeedCategory(category.getString("slug"), category.getString("label"))
        },
        currencies = List(currencies.length()) { currencies.getString(it) },
        nextCursor = if (json.isNull("next_cursor")) null else json.getString("next_cursor")
      )
    }
  }
}
