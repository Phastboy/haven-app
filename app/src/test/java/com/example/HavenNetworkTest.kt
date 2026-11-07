package com.example

import com.example.data.network.HavenNetwork
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.*
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class HavenNetworkTest {
  @Test fun `JSON errors preserve status body and bearer only in the header`() = runTest {
    val client = HttpClient(MockEngine { request ->
      assertEquals("https://haven.test/api/v1/me/profile", request.url.toString())
      assertEquals("Bearer access-test", request.headers[HttpHeaders.Authorization])
      assertEquals(HttpMethod.Post, request.method)
      assertEquals("{\"display_name\":\"Ada\"}", request.body.toByteArray().decodeToString())
      respond("{\"error\":\"handle_taken\"}", HttpStatusCode.Conflict)
    }) { expectSuccess = false; install(HttpTimeout) }
    try {
      val result = HavenNetwork(client, "https://haven.test").request(
        "POST", "/api/v1/me/profile", "{\"display_name\":\"Ada\"}", "access-test")
      assertEquals(409, result.first)
      assertEquals("{\"error\":\"handle_taken\"}", result.second)
    } finally { client.close() }
  }

  @Test fun `multipart preserves field filename mime bytes and stable upload identity`() = runTest {
    val client = HttpClient(MockEngine { request ->
      assertEquals("Bearer test-access", request.headers[HttpHeaders.Authorization])
      assertEquals("upload-identity", request.headers["Idempotency-Key"])
      val multipart = request.body.toByteArray().decodeToString()
      assertTrue(multipart.contains("name=avatar") || multipart.contains("name=\"avatar\""))
      assertTrue(multipart.contains("filename=\"profile.png\""))
      assertTrue(multipart.contains("image/png"))
      assertTrue(multipart.contains("actual-test-image"))
      respond("{\"avatar_url\":\"https://images.test/avatar\"}", HttpStatusCode.OK)
    }) { install(HttpTimeout) }
    try {
      val result = HavenNetwork(client, "https://haven.test").upload("/api/v1/me/avatar", "avatar", "profile.png",
        "actual-test-image".encodeToByteArray(), "image/png", "test-access", "upload-identity")
      assertEquals(200, result.first)
    } finally { client.close() }
  }

  @Test fun `foreground stream is cancellable while waiting for further events`() = runTest {
    val channel = ByteChannel(autoFlush = true)
    val client = HttpClient(MockEngine { request ->
      assertEquals("Bearer stream-access", request.headers[HttpHeaders.Authorization])
      assertEquals("text/event-stream", request.headers[HttpHeaders.Accept])
      assertFalse(request.url.toString().contains("stream-access"))
      respond(channel, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/event-stream"))
    }) { install(HttpTimeout) }
    try {
      val gotLine = CompletableDeferred<Unit>()
      val stream = launch {
        HavenNetwork(client, "https://haven.test").events("/api/v1/conversations/test/events", "stream-access") { line ->
          if (line == "event: changed") gotLine.complete(Unit)
          true
        }
      }
      channel.writeStringUtf8("event: changed\n\n")
      gotLine.await()
      stream.cancelAndJoin()
      assertTrue(stream.isCancelled)
    } finally { channel.cancel(null); client.close() }
  }

  @Test fun `absolute and protocol-relative request paths cannot redirect bearer credentials`() = runTest {
    var requests = 0
    val client = HttpClient(MockEngine { requests++; respond("unexpected") }) { install(HttpTimeout) }
    try {
      val network = HavenNetwork(client, "https://haven.test")
      for (path in listOf("https://elsewhere.test", "//elsewhere.test")) {
        val result = runCatching { network.request("GET", path, bearer = "secret") }
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
      }
      assertEquals(0, requests)
    } finally { client.close() }
  }
}
