package com.example.data.messaging

import android.net.Uri
import com.example.auth.HavenAuthClient
import com.example.data.network.HavenNetwork
import com.example.ui.model.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class ConversationEventsListener(
  private val auth: HavenAuthClient,
  private val network: HavenNetwork = HavenNetwork.shared
) {
  suspend fun listenToConversationEvents(conversationId: String, onInvalidation: suspend () -> Unit) =
    withContext(Dispatchers.IO) {
      val actor = SessionManager.session.value.personId ?: return@withContext
      var failures = 0
      var refreshed = false
      while (isActive && failures < 5 && SessionManager.session.value.personId == actor) {
        try {
          // Acquire credentials, then release the auth mutex before opening the stream.
          val token = auth.getValidAccessToken() ?: break
          var event: String? = null
          val status = network.events("/api/v1/conversations/${Uri.encode(conversationId)}/events", token) { line ->
            if (SessionManager.session.value.personId != actor) false else {
              when {
                line.startsWith("event:") -> event = line.substring(6).trim()
                line.isEmpty() -> {
                  if (event == "resync" || event == "changed") {
                    failures = 0
                    onInvalidation()
                  }
                  event = null
                }
              }
              true
            }
          }
          if (status == 403 || status == 404) break
          if (status == 401) {
            if (refreshed || !auth.restore()) break
            refreshed = true
          }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Foreground polling survives bounded stream failures. */ }
        failures++
        delay((1000L shl (failures - 1)).coerceAtMost(15_000L))
      }
    }
}
