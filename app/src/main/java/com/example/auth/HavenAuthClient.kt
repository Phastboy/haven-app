package com.example.auth

import android.content.Context
import java.io.IOException
import android.util.Base64
import android.util.Log
import com.example.ui.model.SessionManager
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.util.concurrent.CopyOnWriteArrayList
import org.json.JSONObject

/** The native half of Haven's Google sign-in flow. */
class HavenAuthClient(private val store: SecureAuthStore) {

  data class DiagnosticEvent(
    val timestampMs: Long = System.currentTimeMillis(),
    val tag: String,
    val flow: String,
    val step: String,
    val message: String,
    val isError: Boolean = false
  )

  companion object {
    const val HOST = "haven.apps.mistral.run"
    private const val BASE = "https://$HOST"
    const val TAG = "HavenAuthDiag"

    /** Web OAuth client ID; also the audience of Google ID tokens. */
    const val GOOGLE_WEB_CLIENT_ID =
      "1064336033731-fk1m1vv5q0p0f6j36fhm3iaadcrprtoj.apps.googleusercontent.com"

    private var sharedClient: HavenAuthClient? = null
    private val _diagnosticLogs = CopyOnWriteArrayList<DiagnosticEvent>()

    /** Thread-safe snapshot of all auth execution diagnostic logs. */
    val diagnosticLogs: List<DiagnosticEvent> get() = _diagnosticLogs.toList()

    /** Records a diagnostic event to in-memory history and Logcat. */
    fun logDiagnostic(
      flow: String,
      step: String,
      message: String,
      isError: Boolean = false,
      throwable: Throwable? = null
    ) {
      val fullMessage = if (throwable != null) {
        "$message | ${throwable.javaClass.name}: ${throwable.message}"
      } else {
        message
      }
      val event = DiagnosticEvent(
        tag = TAG,
        flow = flow,
        step = step,
        message = fullMessage,
        isError = isError
      )
      _diagnosticLogs.add(event)
      try {
        if (isError) {
          if (throwable != null) Log.e(TAG, "[$flow] [$step] $fullMessage", throwable)
          else Log.e(TAG, "[$flow] [$step] $fullMessage")
        } else {
          Log.i(TAG, "[$flow] [$step] $fullMessage")
        }
      } catch (_: Throwable) {
        // Fallback for plain host unit-test environments without Android Log implementation
        val level = if (isError) "ERROR" else "INFO"
        println("[$TAG] [$level] [$flow] [$step] $fullMessage")
      }
    }

    /** Returns formatted diagnostic execution trace for debugging and verification. */
    fun getDiagnosticSummary(): String = buildString {
      appendLine("=== Haven Auth Diagnostic Trace (${_diagnosticLogs.size} events) ===")
      _diagnosticLogs.forEach { event ->
        appendLine("[${event.flow}] [${event.step}] ${event.message}${if (event.isError) " (ERROR)" else ""}")
      }
    }

    /** Clears in-memory diagnostic logs (useful for test isolation). */
    fun clearDiagnosticLogs() {
      _diagnosticLogs.clear()
    }

    @Synchronized
    fun shared(context: Context): HavenAuthClient = sharedClient ?: HavenAuthClient(
      SecureAuthStore(context.applicationContext)
    ).also { sharedClient = it }
  }

  private val random = SecureRandom()
  private var accessToken: String? = null

  private fun randomUrlSafe(size: Int): String = ByteArray(size).also(random::nextBytes).let {
    Base64.encodeToString(it, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
  }

  /** Creates and stores the single-use nonce for a native Google request. */
  fun beginNativeGoogle(): String {
    val nonce = randomUrlSafe(24)
    store.saveNativeNonce(nonce)
    logDiagnostic(
      flow = "NATIVE_GOOGLE",
      step = "BEGIN_NATIVE_GOOGLE",
      message = "Generated and saved single-use native nonce (prefix: ${nonce.take(6)}...)"
    )
    return nonce
  }

  /** Drops a pending native nonce after cancellation or failure. */
  fun cancelNativeGoogle() {
    logDiagnostic(
      flow = "NATIVE_GOOGLE",
      step = "CANCEL_NATIVE_GOOGLE",
      message = "Cancelled native Google flow; clearing pending native nonce."
    )
    store.clearNativeNonce()
  }

  /** Returns null on success, or an explicit failure message identifying the failing step. */
  @Synchronized
  fun completeNativeGoogle(idToken: String, nonce: String): String? {
    logDiagnostic(
      flow = "NATIVE_GOOGLE",
      step = "COMPLETE_NATIVE_GOOGLE_START",
      message = "Validating nonce and preparing backend token exchange at POST /api/v1/auth/google."
    )
    val expected = store.consumeNativeNonce()
    if (expected == null) {
      val msg = "Google sign-in request expired. Please try again."
      logDiagnostic("NATIVE_GOOGLE", "NONCE_EXPIRED", msg, isError = true)
      return msg
    }
    if (nonce != expected) {
      val msg = "Google sign-in response did not match this request."
      logDiagnostic("NATIVE_GOOGLE", "NONCE_MISMATCH", msg, isError = true)
      return msg
    }

    logDiagnostic(
      flow = "NATIVE_GOOGLE",
      step = "DISPATCHING_BACKEND_EXCHANGE",
      message = "Dispatching POST /api/v1/auth/google with ID token length=${idToken.length}."
    )
    val response = request("POST", "/api/v1/auth/google", JSONObject()
      .put("id_token", idToken).put("nonce", nonce))
    logDiagnostic(
      flow = "NATIVE_GOOGLE",
      step = "BACKEND_EXCHANGE_RESPONSE",
      message = "Backend POST /api/v1/auth/google returned HTTP ${response.first}."
    )
    if (response.first != 200) {
      val snippet = response.second.take(120).trim()
      val msg = "Google sign-in failed (HTTP ${response.first}): ${if (snippet.isNotEmpty()) snippet else "Unauthorized"}"
      logDiagnostic("NATIVE_GOOGLE", "BACKEND_EXCHANGE_ERROR", msg, isError = true)
      return msg
    }
    return try {
      val success = acceptTokens(JSONObject(response.second))
      if (success) {
        logDiagnostic("NATIVE_GOOGLE", "COMPLETE_SUCCESS", "Native Google sign-in completed and user verified via /api/v1/me.")
        null
      } else {
        val msg = "Identity verification (/api/v1/me) failed."
        logDiagnostic("NATIVE_GOOGLE", "VERIFY_ME_FAILED", msg, isError = true)
        msg
      }
    } catch (e: Exception) {
      val msg = "Failed to parse server authentication response: ${e.message}"
      logDiagnostic("NATIVE_GOOGLE", "PARSE_ERROR", msg, isError = true, throwable = e)
      msg
    }
  }

  /** Restores a session by rotating the stored refresh token, then checking /me. */
  @Synchronized
  fun restore(): Boolean {
    val refresh = store.refreshToken ?: return false
    logDiagnostic("SESSION", "RESTORE_START", "Attempting session restoration with stored refresh token.")
    val response = request("POST", "/api/v1/auth/refresh", JSONObject().put("refresh_token", refresh))
    if (response.first == 401) {
      logDiagnostic("SESSION", "REFRESH_REJECTED_401", "Stored refresh token rejected (HTTP 401). Signing out locally.", isError = true)
      store.refreshToken = null
      SessionManager.signOut()
      return false
    }
    if (response.first != 200) {
      logDiagnostic("SESSION", "REFRESH_FAILED", "Session restoration returned HTTP ${response.first}.", isError = true)
      return false
    }
    val success = acceptTokens(JSONObject(response.second))
    logDiagnostic("SESSION", "RESTORE_RESULT", "Session restoration success=$success.")
    return success
  }

  private fun acceptTokens(body: JSONObject): Boolean {
    val access = body.getString("access_token")
    val refresh = body.getString("refresh_token")
    // Persist the rotated token before any further request. Access stays in memory.
    store.refreshToken = refresh
    accessToken = access
    val me = request("GET", "/api/v1/me", null, access)
    if (me.first != 200) return false
    val profile = JSONObject(me.second)
    SessionManager.signInPerson(
      profile.getString("id"), profile.getString("display_name"), profile.getString("handle")
    )
    return true
  }

  @Synchronized
  fun signOut() {
    logDiagnostic("SESSION", "SIGN_OUT", "User requested sign out. Clearing tokens and session.")
    val refresh = store.refreshToken
    store.refreshToken = null
    accessToken = null
    SessionManager.signOut()
    if (refresh != null) {
      try { request("POST", "/api/v1/auth/revoke", JSONObject().put("refresh_token", refresh)) }
      catch (_: Exception) { /* Local sign-out wins even if offline. */ }
    }
  }

  /** Typed HTTP error with status code and server response body. */
  class HttpException(val statusCode: Int, val responseBody: String) :
    IOException("Request failed (HTTP $statusCode): $responseBody")

  /** Obtains a valid bearer access token, restoring session if needed. Safe for independent streaming or background calls. */
  @Synchronized
  fun getValidAccessToken(): String? {
    if (accessToken == null && !restore()) return null
    return accessToken
  }

  /** Serializes refresh rotation with restore/sign-out; never creates a fake session. */
  @Synchronized
  fun authenticatedGet(path: String): String {
    if (accessToken == null && !restore()) throw IOException("Sign in to continue.")
    var response = request("GET", path, null, accessToken)
    if (response.first == 401 && restore()) response = request("GET", path, null, accessToken)
    if (response.first != 200) throw HttpException(response.first, response.second)
    return response.second
  }

  fun publicGet(path: String): String {
    val response = request("GET", path, null)
    if (response.first != 200) throw HttpException(response.first, response.second)
    return response.second
  }

  @Synchronized
  fun authenticatedMutation(method: String, path: String, body: JSONObject? = null): String {
    require(method == "POST" || method == "DELETE" || method == "PATCH")
    if (accessToken == null && !restore()) throw IOException("Sign in to continue.")
    var response = request(method, path, body, accessToken)
    if (response.first == 401 && restore()) response = request(method, path, body, accessToken)
    if (response.first !in 200..299) throw HttpException(response.first, response.second)
    return response.second
  }

  @Synchronized
  fun authenticatedMultipartUpload(
    path: String,
    fieldName: String,
    filename: String,
    bytes: ByteArray,
    idempotencyKey: String? = null,
    mimeType: String = "image/jpeg"
  ): String {
    if (accessToken == null && !restore()) throw IOException("Sign in to continue.")
    fun upload(): Pair<Int, String> {
      val boundary = "haven-${System.currentTimeMillis()}-${(1000..9999).random()}"
      val connection = (URL(BASE + path).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 15_000
        readTimeout = 60_000
        doOutput = true
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Authorization", "Bearer $accessToken")
        if (idempotencyKey != null) setRequestProperty("Idempotency-Key", idempotencyKey)
        setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
      }
      return try {
        connection.outputStream.use { output ->
          output.write(("--$boundary\r\nContent-Disposition: form-data; name=\"$fieldName\"; filename=\"$filename\"\r\nContent-Type: $mimeType\r\n\r\n").toByteArray())
          output.write(bytes)
          output.write("\r\n--$boundary--\r\n".toByteArray())
        }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        status to (stream?.bufferedReader()?.use { it.readText() } ?: "")
      } finally { connection.disconnect() }
    }
    var response = upload()
    if (response.first == 401 && restore()) response = upload()
    if (response.first !in 200..299) throw HttpException(response.first, response.second)
    return response.second
  }

  @Synchronized
  fun authenticatedImageUpload(path: String, bytes: ByteArray, uploadId: String): String {
    return authenticatedMultipartUpload(
      path = path,
      fieldName = "image",
      filename = "photo",
      bytes = bytes,
      idempotencyKey = uploadId,
      mimeType = "application/octet-stream"
    )
  }

  private fun request(method: String, path: String, body: JSONObject?, bearer: String? = null): Pair<Int, String> {
    val connection = (URL(BASE + path).openConnection() as HttpURLConnection).apply {
      requestMethod = method
      connectTimeout = 10_000
      readTimeout = 10_000
      setRequestProperty("Accept", "application/json")
      if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
      if (body != null) {
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
      }
    }
    return try {
      if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
      val status = connection.responseCode
      val stream = if (status in 200..299) connection.inputStream else connection.errorStream
      status to (stream?.bufferedReader()?.use { it.readText() } ?: "")
    } finally {
      connection.disconnect()
    }
  }
}
