package com.example.data

import android.net.Uri
import com.example.auth.HavenAuthClient
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class FeedOffer(
  val id: String,
  val slug: String,
  val title: String,
  val makerName: String,
  val priceDisplay: String,
  val coverUrl: String?
)

data class FeedCategory(val slug: String, val label: String)
data class OffersQuery(
  val search: String = "",
  val category: String = "",
  val currency: String = "",
  val minPrice: Int? = null,
  val maxPrice: Int? = null,
  val after: String? = null
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

/** Public discovery; the server owns filtering, visibility, and result order. */
class HavenOffersRepository : OffersRepository {
  override suspend fun fetch(query: OffersQuery): OffersPage = withContext(Dispatchers.IO) {
    val uri = Uri.parse("https://${HavenAuthClient.HOST}/api/v1/offers").buildUpon()
    if (query.search.isNotBlank()) uri.appendQueryParameter("q", query.search.trim())
    if (query.category.isNotBlank()) uri.appendQueryParameter("category", query.category)
    if (query.currency.isNotBlank()) uri.appendQueryParameter("currency", query.currency)
    query.minPrice?.let { uri.appendQueryParameter("min_price", it.toString()) }
    query.maxPrice?.let { uri.appendQueryParameter("max_price", it.toString()) }
    query.after?.let { uri.appendQueryParameter("after", it) }
    val connection = (URL(uri.build().toString()).openConnection() as HttpURLConnection).apply {
      connectTimeout = 10_000
      readTimeout = 15_000
      setRequestProperty("Accept", "application/json")
    }
    try {
      when (connection.responseCode) {
        200 -> connection.inputStream.bufferedReader().use { parsePage(it.readText()) }
        400 -> throw IOException("Check your search and price filters, then try again.")
        else -> throw IOException("Couldn’t load offers. Please try again.")
      }
    } finally {
      connection.disconnect()
    }
  }

  companion object {
    fun parsePage(body: String): OffersPage {
      val json = JSONObject(body)
      val items = json.getJSONArray("items")
      val categories = json.getJSONArray("categories")
      val currencies = json.getJSONArray("currencies")
      return OffersPage(
        items = List(items.length()) { index ->
          val item = items.getJSONObject(index)
          FeedOffer(item.getString("id"), item.getString("slug"), item.getString("title"),
            item.getString("maker_name"), item.getString("price_display"),
            if (item.isNull("cover_url")) null else item.getString("cover_url"))
        },
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
