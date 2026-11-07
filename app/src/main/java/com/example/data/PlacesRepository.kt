package com.example.data

import io.ktor.http.encodeURLParameter
import com.example.auth.HavenAuthClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

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
      val encoded = query.trim().encodeURLParameter()
      val path = "/api/v1/places/search?q=$encoded"
      val raw = auth.optionalGet(path)
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
