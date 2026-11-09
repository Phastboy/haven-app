package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.HavenAuthClient
import com.example.auth.SecureAuthStore
import com.example.data.network.HavenNetwork
import com.example.ui.model.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.*
import javax.crypto.KeyGenerator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineStart
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
class HavenAuthTransportTest {
  private lateinit var store: SecureAuthStore
  @Before fun setup() {
    SessionManager.signOut()
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("haven_auth", Context.MODE_PRIVATE).edit().clear().commit()
    val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    store = SecureAuthStore(context) { key }
    store.refreshToken = "refresh-0"
  }
  @After fun cleanup() { SessionManager.signOut() }
  private fun client(engine: MockEngine) = HttpClient(engine) { expectSuccess = false; install(HttpTimeout) }
  private val person = """{"id":"owner","display_name":"Real person","handle":"real-person"}"""

  @Test fun `refresh rotation persists before verification and concurrent 401s do not replay old tokens`() = runTest {
    var rotations = 0
    val client = client(MockEngine { request ->
      when (request.url.encodedPath) {
        "/api/v1/auth/refresh" -> {
          assertTrue(request.body.toByteArray().decodeToString().contains("refresh-$rotations"))
          rotations++
          respond("""{"access_token":"access-$rotations","refresh_token":"refresh-$rotations"}""", HttpStatusCode.OK)
        }
        "/api/v1/me" -> {
          assertEquals("refresh-$rotations", store.refreshToken)
          respond(person, HttpStatusCode.OK)
        }
        else -> if (request.headers[HttpHeaders.Authorization] == "Bearer access-1") respond("expired", HttpStatusCode.Unauthorized)
          else respond("confirmed", HttpStatusCode.OK)
      }
    })
    try {
      val auth = HavenAuthClient(store, HavenNetwork(client, "https://haven.test"))
      assertTrue(auth.restore())
      val first = async { auth.authenticatedGet("/private/first", "owner") }
      val second = async { auth.authenticatedGet("/private/second", "owner") }
      assertEquals("confirmed", first.await())
      assertEquals("confirmed", second.await())
      assertEquals(2, rotations)
      assertEquals("refresh-2", store.refreshToken)
    } finally { client.close() }
  }

  @Test fun `cancelling refresh still persists its already-dispatched rotation`() = runTest {
    val dispatched = CompletableDeferred<Unit>()
    val reply = CompletableDeferred<Unit>()
    val client = client(MockEngine { request ->
      if (request.url.encodedPath.endsWith("/refresh")) {
        dispatched.complete(Unit)
        reply.await()
        respond("""{"access_token":"rotated-access","refresh_token":"rotated-refresh"}""", HttpStatusCode.OK)
      } else respond(person, HttpStatusCode.OK)
    })
    try {
      val auth = HavenAuthClient(store, HavenNetwork(client, "https://haven.test"))
      val restore = launch { auth.restore() }
      dispatched.await()
      restore.cancel()
      reply.complete(Unit)
      restore.join()
      assertEquals("rotated-refresh", store.refreshToken)
      assertTrue(SessionManager.session.value.isAuthenticated)
    } finally { client.close() }
  }

  @Test fun `network failure keeps credentials while a confirmed refresh rejection clears them`() = runTest {
    var status = HttpStatusCode.ServiceUnavailable
    val client = client(MockEngine { respond("not accepted", status) })
    try {
      val auth = HavenAuthClient(store, HavenNetwork(client, "https://haven.test"))
      assertFalse(auth.restore())
      assertEquals("refresh-0", store.refreshToken)
      status = HttpStatusCode.Unauthorized
      assertFalse(auth.restore())
      assertNull(store.refreshToken)
      assertFalse(SessionManager.session.value.isAuthenticated)
    } finally { client.close() }
  }

  @Test fun `sign out waiting on rotation revokes the rotated token and cannot resurrect the person`() = runTest {
    val dispatched = CompletableDeferred<Unit>()
    val reply = CompletableDeferred<Unit>()
    var revoked: String? = null
    val client = client(MockEngine { request ->
      when {
        request.url.encodedPath.endsWith("/refresh") -> {
          dispatched.complete(Unit); reply.await()
          respond("""{"access_token":"new-access","refresh_token":"new-refresh"}""", HttpStatusCode.OK)
        }
        request.url.encodedPath.endsWith("/revoke") -> {
          revoked = request.body.toByteArray().decodeToString()
          respond("{}", HttpStatusCode.OK)
        }
        else -> respond(person, HttpStatusCode.OK)
      }
    })
    try {
      val auth = HavenAuthClient(store, HavenNetwork(client, "https://haven.test"))
      val restoring = launch { auth.restore() }
      dispatched.await()
      val signOut = launch(start = CoroutineStart.UNDISPATCHED) { auth.signOut() }
      // The request has entered signOut and is waiting behind the rotating refresh.
      signOut.cancel()
      reply.complete(Unit)
      restoring.join(); signOut.join()
      assertNull(store.refreshToken)
      assertFalse(SessionManager.session.value.isAuthenticated)
      assertTrue(revoked.orEmpty().contains("new-refresh"))
    } finally { client.close() }
  }

  @Test fun `unavailable keystore cannot retain a signed in session during sign out`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val unreadable = SecureAuthStore(context) { throw java.io.IOException("Keystore unavailable") }
    SessionManager.signInPerson("owner", "Real person", "real-person")
    var requests = 0
    val client = client(MockEngine { requests++; respond("{}", HttpStatusCode.OK) })
    try {
      HavenAuthClient(unreadable, HavenNetwork(client, "https://haven.test")).signOut()
      assertFalse(SessionManager.session.value.isAuthenticated)
      assertNull(store.refreshToken)
      assertEquals(0, requests)
    } finally { client.close() }
  }

  @Test fun `cancellation during revocation still completes server sign out`() = runTest {
    val dispatched = CompletableDeferred<Unit>()
    val reply = CompletableDeferred<Unit>()
    var revoked = false
    val client = client(MockEngine {
      dispatched.complete(Unit)
      reply.await()
      revoked = true
      respond("{}", HttpStatusCode.OK)
    })
    try {
      val auth = HavenAuthClient(store, HavenNetwork(client, "https://haven.test"))
      val signOut = launch(start = CoroutineStart.UNDISPATCHED) { auth.signOut() }
      dispatched.await()
      signOut.cancel()
      reply.complete(Unit)
      signOut.join()
      assertTrue(revoked)
      assertNull(store.refreshToken)
      assertFalse(SessionManager.session.value.isAuthenticated)
    } finally { client.close() }
  }
  @Test fun `Following sends bearer facets and cursor to the offers endpoint and preserves HTTP errors`() = runTest {
    var status = HttpStatusCode.OK
    val client = client(MockEngine { request ->
      when (request.url.encodedPath) {
        "/api/v1/auth/refresh" -> respond(
          """{"access_token":"feed-access","refresh_token":"feed-refresh"}""", HttpStatusCode.OK)
        "/api/v1/me" -> respond(person, HttpStatusCode.OK)
        else -> {
          assertEquals("/api/v1/offers", request.url.encodedPath)
          assertEquals("Bearer feed-access", request.headers[HttpHeaders.Authorization])
          assertEquals("following", request.url.parameters["scope"])
          assertEquals("bread", request.url.parameters["q"])
          assertEquals("food-groceries", request.url.parameters["category"])
          assertEquals("NGN", request.url.parameters["currency"])
          assertEquals("0", request.url.parameters["min_price"])
          assertEquals("100", request.url.parameters["max_price"])
          assertEquals("cursor+/=", request.url.parameters["after"])
          assertEquals("off", request.url.parameters["near"])
          respond("""{"items":[],"categories":[],"currencies":[],"next_cursor":null}""", status)
        }
      }
    })
    try {
      val auth = HavenAuthClient(store, HavenNetwork(client, "https://haven.test"))
      assertTrue(auth.restore())
      val repository = com.example.data.HavenOffersRepository(auth)
      val query = com.example.data.OffersQuery(search = "bread", category = "food-groceries",
        currency = "NGN", minPrice = 0, maxPrice = 100, after = "cursor+/=", scope = "following", near = "off")
      assertTrue(repository.fetch(query).items.isEmpty())
      status = HttpStatusCode.BadRequest
      val error = runCatching { repository.fetch(query) }.exceptionOrNull()
      assertTrue(error is com.example.data.OffersRequestException)
      assertEquals("This feed request was rejected. Check your filters and try again.", error?.message)
      assertFalse(error?.message.orEmpty().contains("connection"))
    } finally { client.close() }
  }

  @Test fun `Inquired is a bearer profile collection with encoded paging`() = runTest {
    val client = client(MockEngine { request ->
      when (request.url.encodedPath) {
        "/api/v1/auth/refresh" -> respond(
          """{"access_token":"collection-access","refresh_token":"collection-refresh"}""", HttpStatusCode.OK)
        "/api/v1/me" -> respond(person, HttpStatusCode.OK)
        else -> {
          assertEquals("/api/v1/me/inquired", request.url.encodedPath)
          assertEquals("Bearer collection-access", request.headers[HttpHeaders.Authorization])
          assertEquals("cursor+/=", request.url.parameters["after"])
          assertNull(request.url.parameters["scope"])
          respond("""{"items":[{"id":"offer-id","slug":"real-offer","title":"Real offer","maker_name":"Person","price_display":"Free","cover_url":null}],"next_cursor":"next"}""", HttpStatusCode.OK)
        }
      }
    })
    try {
      val auth = HavenAuthClient(store, HavenNetwork(client, "https://haven.test"))
      assertTrue(auth.restore())
      val page = com.example.data.HavenInquiredRepository(auth).getInquired("cursor+/=").getOrThrow()
      assertEquals("offer-id", page.items.single().id)
      assertEquals("next", page.nextCursor)
    } finally { client.close() }
  }

}
