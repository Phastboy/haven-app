package com.example.data

import android.net.Uri
import com.example.auth.HavenAuthClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class PlaceCandidate(
  val label: String,
  val near: String
)

interface PlacesRepository {
  suspend fun searchPlaces(query: String): Result<List<PlaceCandidate>>
}

class HavenPlacesRepository(
  private val auth: HavenAuthClient
) : PlacesRepository {
  override suspend fun searchPlaces(query: String): Result<List<PlaceCandidate>> = withContext(Dispatchers.IO) {
    runCatching {
      val encoded = Uri.encode(query.trim())
      val path = "/api/v1/places/search?q=$encoded"
      val token = auth.getValidAccessToken()
      val url = URL("https://${HavenAuthClient.HOST}$path")
      val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10_000
        readTimeout = 15_000
        setRequestProperty("Accept", "application/json")
        if (token != null) setRequestProperty("Authorization", "Bearer $token")
      }
      val raw = try {
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        stream?.bufferedReader()?.use { it.readText() } ?: ""
      } finally {
        connection.disconnect()
      }
      val json = JSONObject(raw)
      val items = json.optJSONArray("items") ?: return@runCatching emptyList()
      List(items.length()) { index ->
        val item = items.getJSONObject(index)
        PlaceCandidate(
          label = item.getString("label"),
          near = item.getString("near")
        )
      }
    }
  }
}
