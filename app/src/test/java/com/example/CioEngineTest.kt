package com.example

import com.example.data.network.HavenCioClient
import com.example.data.network.HavenNetwork
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CioEngineTest {
  @Test fun `production CIO configuration performs a real HTTP request`() = runBlocking {
    val server = embeddedServer(CIO, host = "127.0.0.1", port = 0) {
      routing { get("/check") { call.respondText("cio-confirmed") } }
    }.start(wait = false)
    val client = HavenCioClient.createApiClient()
    try {
      val port = server.engine.resolvedConnectors().first().port
      val result = HavenNetwork(client, "http://127.0.0.1:$port").request("GET", "/check")
      assertEquals(200, result.first)
      assertEquals("cio-confirmed", result.second)
    } finally {
      client.close()
      server.stop(0, 1000)
    }
  }
}
