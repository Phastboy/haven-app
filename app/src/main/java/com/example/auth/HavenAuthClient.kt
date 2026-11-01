package com.example.auth

import android.content.Context
import java.io.IOException
import android.net.Uri
import android.util.Base64
import com.example.ui.model.SessionManager
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import org.json.JSONObject

/** The native half of Haven's browser sign-in and PKCE code exchange. */
class HavenAuthClient(private val store: SecureAuthStore) {
  companion object {
    const val HOST = "haven-odysseon-d48990aa.koyeb.app"
    private const val BASE = "https://$HOST"
    private var sharedClient: HavenAuthClient? = null

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

  fun beginAuthorization(): Uri {
    val verifier = randomUrlSafe(32)
    val state = randomUrlSafe(24)
    val challenge = Base64.encodeToString(
      MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)),
      Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
    )
    store.savePending(verifier, state)
    return Uri.parse("$BASE/api/v1/auth/authorize").buildUpon()
      .appendQueryParameter("code_challenge", challenge)
      .appendQueryParameter("state", state)
      .build()
  }

  fun acceptsCallback(uri: Uri?): Boolean = uri?.scheme == "https" &&
    uri.host == HOST && uri.path == "/auth/callback"

  /** Returns null on success, or an explicit failure message identifying the exact failing step. */
  @Synchronized
  fun completeAuthorization(uri: Uri): String? {
    if (!acceptsCallback(uri)) {
      return "Invalid callback URL: ${uri.scheme}://${uri.host}${uri.path}"
    }
    uri.getQueryParameter("error")?.let { err ->
      return when (err) {
        "google_cancelled" -> "Google sign-in was cancelled."
        "access_denied" -> "Access was denied by Google."
        else -> "Authentication failed: $err"
      }
    }
    val state = uri.getQueryParameter("state")
      ?: return "Callback missing OAuth state parameter."
    val verifier = store.consumePending(state)
      ?: return "OAuth state mismatch or expired request. Please try again."
    val code = uri.getQueryParameter("code")
      ?: return "Callback missing authorization code."

    val exchangeResponse = request("POST", "/api/v1/auth/exchange", JSONObject()
      .put("code", code).put("code_verifier", verifier))
    if (exchangeResponse.first != 200) {
      val snippet = exchangeResponse.second.take(120).trim()
      return "PKCE token exchange failed (HTTP ${exchangeResponse.first}): ${if (snippet.isNotEmpty()) snippet else "Unauthorized"}"
    }

    return try {
      val tokens = JSONObject(exchangeResponse.second)
      val access = tokens.getString("access_token")
      val refresh = tokens.getString("refresh_token")
      store.refreshToken = refresh
      accessToken = access

      val me = request("GET", "/api/v1/me", null, access)
      if (me.first != 200) {
        val snippet = me.second.take(120).trim()
        return "Identity verification (/api/v1/me) failed (HTTP ${me.first}): ${if (snippet.isNotEmpty()) snippet else "Unauthorized"}"
      }
      val profile = JSONObject(me.second)
      SessionManager.signInPerson(
        profile.getString("id"), profile.getString("display_name"), profile.getString("handle")
      )
      null
    } catch (e: Exception) {
      "Failed to parse server authentication response: ${e.message}"
    }
  }

  /** Returns true only after the code is exchanged and /me confirms the person. */
  fun finishAuthorization(uri: Uri): Boolean = completeAuthorization(uri) == null

  /** Restores a session by rotating the stored refresh token, then checking /me. */
  @Synchronized
  fun restore(): Boolean {
    val refresh = store.refreshToken ?: return false
    val response = request("POST", "/api/v1/auth/refresh", JSONObject().put("refresh_token", refresh))
    if (response.first == 401) {
      store.refreshToken = null
      SessionManager.signOut()
      return false
    }
    if (response.first != 200) return false
    return acceptTokens(JSONObject(response.second))
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
    val refresh = store.refreshToken
    store.refreshToken = null
    accessToken = null
    SessionManager.signOut()
    if (refresh != null) {
      try { request("POST", "/api/v1/auth/revoke", JSONObject().put("refresh_token", refresh)) }
      catch (_: Exception) { /* Local sign-out wins even if offline. */ }
    }
  }

  /** Serializes refresh rotation with restore/sign-out; never creates a fake session. */
  @Synchronized
  fun authenticatedGet(path: String): String {
    if (accessToken == null && !restore()) throw IOException("Sign in to continue.")
    var response = request("GET", path, null, accessToken)
    if (response.first == 401 && restore()) response = request("GET", path, null, accessToken)
    if (response.first != 200) throw IOException("Request failed (HTTP ${response.first}).")
    return response.second
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
