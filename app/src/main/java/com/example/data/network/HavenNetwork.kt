package com.example.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.utils.io.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/** One explicitly selected Kotlin CIO engine. No implicit Java/OkHttp engine discovery. */
object HavenCioClient {
  val client: HttpClient by lazy { createApiClient() }
  // Public image/CDN redirects never inherit an API bearer token.
  val imageClient: HttpClient by lazy { create(redirects = true) }

  internal fun createApiClient(): HttpClient = create(redirects = false)

  private fun create(redirects: Boolean): HttpClient {
    val engine = CIO.create { endpoint.connectAttempts = 1 }
    return HttpClient(engine) {
      expectSuccess = false
      followRedirects = redirects
      install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 15_000
        requestTimeoutMillis = 60_000
      }
    }
  }
}

/** Same-origin, suspending transport shared by the API and foreground invalidation stream. */
class HavenNetwork(
  private val client: HttpClient,
  private val baseUrl: String = "https://haven.apps.mistral.run"
) {
  companion object {
    val shared: HavenNetwork by lazy { HavenNetwork(HavenCioClient.client) }
  }

  private fun url(path: String): String {
    require(path.startsWith('/') && !path.startsWith("//") && '\r' !in path && '\n' !in path)
    return baseUrl.trimEnd('/') + path
  }

  suspend fun request(method: String, path: String, body: String? = null, bearer: String? = null): Pair<Int, String> {
    val response = client.request(url(path)) {
      this.method = HttpMethod.parse(method)
      accept(ContentType.Application.Json)
      timeout { requestTimeoutMillis = 20_000 }
      if (bearer != null) bearerAuth(bearer)
      if (body != null) {
        contentType(ContentType.Application.Json)
        setBody(body)
      }
    }
    return response.status.value to response.bodyAsText()
  }

  suspend fun upload(
    path: String, field: String, filename: String, bytes: ByteArray,
    mimeType: String, bearer: String, idempotencyKey: String?
  ): Pair<Int, String> {
    require(field.all { it.isLetterOrDigit() || it == '_' })
    require(filename.none { it == '\r' || it == '\n' || it == '"' })
    val response = client.request(url(path)) {
      method = HttpMethod.Post
      accept(ContentType.Application.Json)
      bearerAuth(bearer)
      if (idempotencyKey != null) header("Idempotency-Key", idempotencyKey)
      timeout { connectTimeoutMillis = 15_000; socketTimeoutMillis = 60_000; requestTimeoutMillis = 75_000 }
      setBody(MultiPartFormDataContent(formData {
        append(field, bytes, Headers.build {
          append(HttpHeaders.ContentType, mimeType)
          append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
        })
      }))
    }
    return response.status.value to response.bodyAsText()
  }

  /** Ktor cancels the response channel/socket when its owning coroutine leaves the thread. */
  suspend fun events(path: String, bearer: String, onLine: suspend (String) -> Boolean): Int =
    client.prepareGet(url(path)) {
      accept(ContentType.Text.EventStream)
      bearerAuth(bearer)
      timeout { requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS; socketTimeoutMillis = 40_000 }
    }.execute { response ->
      if (response.status.value == 200) {
        val channel = response.bodyAsChannel()
        while (currentCoroutineContext().isActive) {
          val line = channel.readUTF8Line(64 * 1024) ?: break
          if (!onLine(line)) break
        }
      }
      response.status.value
    }
}
