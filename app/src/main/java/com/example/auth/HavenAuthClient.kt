package com.example.auth

import android.content.Context
import java.io.IOException
import android.util.Base64
import android.util.Log
import com.example.ui.model.SessionManager
import java.security.SecureRandom
import java.util.concurrent.CopyOnWriteArrayList
import org.json.JSONObject
import com.example.data.network.HavenNetwork
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The native half of Haven's Google sign-in flow. */
class HavenAuthClient(
  private val store: SecureAuthStore,
  private val network: HavenNetwork = HavenNetwork.shared
) {
  private val sessionMutex = Mutex()

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
  suspend fun completeNativeGoogle(idToken: String, nonce: String): String? = sessionMutex.withLock {
    currentCoroutineContext().ensureActive()
    withContext(NonCancellable) { completeNativeGoogleLocked(idToken, nonce) }
  }

  private suspend fun completeNativeGoogleLocked(idToken: String, nonce: String): String? {
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

  /** Presence is not proof of authentication; it distinguishes retry from signed out. */
  @Synchronized
  fun hasSavedSession(): Boolean = store.refreshToken != null

  /** Restores a session by rotating the stored refresh token, then checking /me. */
  suspend fun restore(): Boolean = sessionMutex.withLock {
    currentCoroutineContext().ensureActive()
    withContext(NonCancellable) { restoreLocked() }
  }

  private suspend fun restoreLocked(): Boolean {
    currentCoroutineContext().ensureActive()
    return withContext(NonCancellable) { rotateRefresh() }
  }

  private suspend fun rotateRefresh(): Boolean {
    val refresh = store.refreshToken ?: return false
    logDiagnostic("SESSION", "RESTORE_START", "Attempting session restoration with stored refresh token.")
    val response = request("POST", "/api/v1/auth/refresh", JSONObject().put("refresh_token", refresh))
    if (response.first == 401) {
      logDiagnostic("SESSION", "REFRESH_REJECTED_401", "Stored refresh token rejected (HTTP 401). Signing out locally.", isError = true)
      store.refreshToken = null
      accessToken = null
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

  private suspend fun acceptTokens(body: JSONObject): Boolean {
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

  suspend fun signOut(): Unit = sessionMutex.withLock {
    signOutLocked()
  }

  private suspend fun signOutLocked(): Unit {
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
  suspend fun getValidAccessToken(): String? = sessionMutex.withLock {
    getValidAccessTokenLocked()
  }

  private suspend fun getValidAccessTokenLocked(): String? {
    if (accessToken == null && !restoreLocked()) return null
    return accessToken
  }

  /** Serializes refresh rotation with restore/sign-out; never creates a fake session. */
  suspend fun authenticatedGet(path: String, expectedPersonId: String? = SessionManager.session.value.personId ?: "<restore-session>"): String = sessionMutex.withLock {
    authenticatedGetLocked(path, expectedPersonId)
  }

  private suspend fun authenticatedGetLocked(path: String, expectedPersonId: String? = SessionManager.session.value.personId ?: "<restore-session>"): String {
    if (accessToken == null && !restoreLocked()) throw IOException("Sign in to continue.")
    val expectedOwner = if (expectedPersonId == "<restore-session>") SessionManager.session.value.personId else expectedPersonId
    check(expectedOwner != null && SessionManager.session.value.personId == expectedOwner) { "Account changed" }
    var response = request("GET", path, null, accessToken)
    if (response.first == 401 && restoreLocked()) {
      check(SessionManager.session.value.personId == expectedOwner) { "Account changed" }
      response = request("GET", path, null, accessToken)
    }
    if (response.first != 200) throw HttpException(response.first, response.second)
    return response.second
  }

  suspend fun optionalGet(path: String): String {
    val person = SessionManager.session.value.personId
    return if (person != null) authenticatedGet(path, expectedPersonId = person) else publicGet(path)
  }

  suspend fun publicGet(path: String): String {
    val response = request("GET", path, null)
    if (response.first != 200) throw HttpException(response.first, response.second)
    return response.second
  }

  suspend fun authenticatedMutation(method: String, path: String, body: JSONObject? = null, expectedPersonId: String? = SessionManager.session.value.personId ?: "<restore-session>"): String = sessionMutex.withLock {
    authenticatedMutationLocked(method, path, body, expectedPersonId)
  }

  private suspend fun authenticatedMutationLocked(method: String, path: String, body: JSONObject? = null, expectedPersonId: String? = SessionManager.session.value.personId ?: "<restore-session>"): String {
    require(method == "POST" || method == "DELETE" || method == "PATCH")
    if (accessToken == null && !restoreLocked()) throw IOException("Sign in to continue.")
    val expectedOwner = if (expectedPersonId == "<restore-session>") SessionManager.session.value.personId else expectedPersonId
    check(expectedOwner != null && SessionManager.session.value.personId == expectedOwner) { "Account changed" }
    var response = request(method, path, body, accessToken)
    if (response.first == 401 && restoreLocked()) {
      check(SessionManager.session.value.personId == expectedOwner) { "Account changed" }
      response = request(method, path, body, accessToken)
    }
    if (response.first !in 200..299) throw HttpException(response.first, response.second)
    return response.second
  }

  suspend fun authenticatedMultipartUpload(
    path: String,
    fieldName: String,
    filename: String,
    bytes: ByteArray,
    idempotencyKey: String? = null,
    mimeType: String = "image/jpeg",
    expectedPersonId: String? = SessionManager.session.value.personId ?: "<restore-session>"
  ): String = sessionMutex.withLock {
    if (accessToken == null && !restoreLocked()) throw IOException("Sign in to continue.")
    val expectedOwner = if (expectedPersonId == "<restore-session>") SessionManager.session.value.personId else expectedPersonId
    check(expectedOwner != null && SessionManager.session.value.personId == expectedOwner) { "Account changed" }
    suspend fun upload() = network.upload(path, fieldName, filename, bytes, mimeType,
      checkNotNull(accessToken), idempotencyKey)
    var response = upload()
    if (response.first == 401 && restoreLocked()) {
      check(SessionManager.session.value.personId == expectedOwner) { "Account changed" }
      response = upload()
    }
    if (response.first !in 200..299) throw HttpException(response.first, response.second)
    response.second
  }

  suspend fun authenticatedImageUpload(path: String, bytes: ByteArray, uploadId: String, expectedPersonId: String? = SessionManager.session.value.personId ?: "<restore-session>"): String {
    return authenticatedMultipartUpload(
      path = path,
      fieldName = "image",
      filename = "photo",
      bytes = bytes,
      idempotencyKey = uploadId,
      mimeType = "application/octet-stream",
      expectedPersonId = expectedPersonId
    )
  }

  private suspend fun request(method: String, path: String, body: JSONObject?, bearer: String? = null): Pair<Int, String> =
    network.request(method, path, body?.toString(), bearer)
}
