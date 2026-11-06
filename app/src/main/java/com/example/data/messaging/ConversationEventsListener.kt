package com.example.data.messaging

import android.net.Uri
import com.example.auth.HavenAuthClient
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class ConversationEventsListener(
  private val auth: HavenAuthClient
) {
  suspend fun listenToConversationEvents(
    conversationId: String,
    onInvalidation: suspend () -> Unit
  ) = withContext(Dispatchers.IO) {
    var backoffMs = 1000L
    val maxBackoffMs = 15000L

    while (isActive) {
      val token = auth.getValidAccessToken() ?: break
      val url = URL("https://${HavenAuthClient.HOST}/api/v1/conversations/${Uri.encode(conversationId)}/events")
      var connection: HttpURLConnection? = null
      try {
        connection = (url.openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          connectTimeout = 15_000
          readTimeout = 40_000 // Keepalive comments sent every 20s
          setRequestProperty("Accept", "text/event-stream")
          setRequestProperty("Authorization", "Bearer $token")
        }

        val status = connection.responseCode
        if (status != 200) {
          if (status == 401) {
            auth.restore()
          }
          delay(backoffMs)
          backoffMs = (backoffMs * 2).coerceAtMost(maxBackoffMs)
          continue
        }

        backoffMs = 1000L // Reset backoff on successful connect
        val reader = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
        var currentEvent: String? = null

        while (isActive) {
          val line = reader.readLine() ?: break // EOF
          if (line.startsWith("event:")) {
            currentEvent = line.substring(6).trim()
          } else if (line.startsWith("data:")) {
            if (currentEvent == "resync" || currentEvent == "changed") {
              onInvalidation()
            }
          } else if (line.isEmpty()) {
            currentEvent = null
          }
        }
      } catch (e: Exception) {
        if (!isActive) break
      } finally {
        try { connection?.disconnect() } catch (_: Exception) {}
      }

      if (isActive) {
        delay(backoffMs)
        backoffMs = (backoffMs * 2).coerceAtMost(maxBackoffMs)
      }
    }
  }
}
