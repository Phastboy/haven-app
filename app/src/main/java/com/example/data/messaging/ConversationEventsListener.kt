package com.example.data.messaging

import android.net.Uri
import com.example.auth.HavenAuthClient
import com.example.ui.model.SessionManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class ConversationEventsListener(private val auth: HavenAuthClient) {
  suspend fun listenToConversationEvents(
    conversationId: String,
    onInvalidation: suspend () -> Unit
  ) = withContext(Dispatchers.IO) {
    val actor = SessionManager.session.value.personId ?: return@withContext
    var failures = 0
    var refreshed = false
    while (isActive && failures < 5 && SessionManager.session.value.personId == actor) {
      try {
        val token = auth.getValidAccessToken() ?: break
        val request = Request.Builder()
          .url("https://${HavenAuthClient.HOST}/api/v1/conversations/${Uri.encode(conversationId)}/events")
          .header("Accept", "text/event-stream")
          .header("Authorization", "Bearer $token").build()
        val status = coroutineScope {
          val call = client.newCall(request)
          // Socket cancellation interrupts blocking reads immediately on leaving/background.
          val cancellation = launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
            try { awaitCancellation() } finally { call.cancel() }
          }
          try {
            call.execute().use { response ->
              if (response.code == 200) {
                failures = 0
                val reader = response.body?.charStream()?.buffered()
                var event: String? = null
                while (isActive && SessionManager.session.value.personId == actor) {
                  val line = reader?.readLine() ?: break
                  when {
                    line.startsWith("event:") -> event = line.substring(6).trim()
                    line.isEmpty() -> {
                      if (event == "resync" || event == "changed") onInvalidation()
                      event = null
                    }
                  }
                }
              }
              response.code
            }
          } finally { cancellation.cancel() }
        }
        if (status == 403 || status == 404) break
        if (status == 401) {
          if (refreshed || !auth.restore()) break
          refreshed = true
        }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        // Foreground polling remains available after bounded stream failures.
      }
      failures++
      delay((1000L shl (failures - 1)).coerceAtMost(15_000L))
    }
  }

  companion object {
    private val client = OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(40, TimeUnit.SECONDS)
      .build()
  }
}
