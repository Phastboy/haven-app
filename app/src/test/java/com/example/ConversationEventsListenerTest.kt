package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.HavenAuthClient
import com.example.auth.SecureAuthStore
import com.example.data.messaging.ConversationEventsListener
import com.example.data.network.HavenNetwork
import com.example.ui.model.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import javax.crypto.KeyGenerator
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ConversationEventsListenerTest {
  private lateinit var store: SecureAuthStore
  @Before fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("haven_auth", Context.MODE_PRIVATE).edit().clear().commit()
    val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    store = SecureAuthStore(context) { key }
    store.refreshToken = "refresh"
    SessionManager.signOut()
  }
  @After fun cleanup() { SessionManager.signOut() }

  private fun authResponse(path: String): String = when {
    path.endsWith("/refresh") -> """{"access_token":"access","refresh_token":"rotated"}"""
    else -> """{"id":"owner","display_name":"Real person","handle":"real-person"}"""
  }

  @Test fun `heartbeat streams keep reconnecting beyond five cycles and deliver later changes`() = runTest {
    var streams = 0
    var invalidations = 0
    val client = HttpClient(MockEngine { request ->
      if (request.url.encodedPath.endsWith("/events")) {
        streams++
        respond(if (streams <= 6) ": heartbeat\n\n" else "event: changed\ndata: {}\n\n", HttpStatusCode.OK)
      } else respond(authResponse(request.url.encodedPath), HttpStatusCode.OK)
    }) { install(HttpTimeout) }
    try {
      val network = HavenNetwork(client, "https://haven.test")
      val auth = HavenAuthClient(store, network)
      assertTrue(auth.restore())
      val listener = ConversationEventsListener(auth, network, StandardTestDispatcher(testScheduler))
      val job = launch {
        listener.listenToConversationEvents("thread") {
          invalidations++
          currentCoroutineContext().cancel()
        }
      }
      job.join()
      assertEquals(7, streams)
      assertEquals(1, invalidations)
    } finally { client.close() }
  }

  @Test fun `consecutive failed connections still stop at the bounded retry limit`() = runTest {
    var streams = 0
    val client = HttpClient(MockEngine { request ->
      if (request.url.encodedPath.endsWith("/events")) {
        streams++
        respond("unavailable", HttpStatusCode.ServiceUnavailable)
      } else respond(authResponse(request.url.encodedPath), HttpStatusCode.OK)
    }) { expectSuccess = false; install(HttpTimeout) }
    try {
      val network = HavenNetwork(client, "https://haven.test")
      val auth = HavenAuthClient(store, network)
      assertTrue(auth.restore())
      ConversationEventsListener(auth, network, StandardTestDispatcher(testScheduler))
        .listenToConversationEvents("thread") { fail("A failed connection must not invalidate") }
      assertEquals(5, streams)
    } finally { client.close() }
  }
}
