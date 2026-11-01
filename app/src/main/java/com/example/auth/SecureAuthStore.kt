package com.example.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Device-only credentials. Android Keystore owns the AES key; backups exclude this file. */
class SecureAuthStore(context: Context) {
  private val preferences = context.getSharedPreferences("haven_auth", Context.MODE_PRIVATE)
  private val keyAlias = "haven_device_auth_v1"

  private fun key(): SecretKey {
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (store.getKey(keyAlias, null) as? SecretKey)?.let { return it }
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    generator.init(
      KeyGenParameterSpec.Builder(
        keyAlias,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .build()
    )
    return generator.generateKey()
  }

  private fun put(name: String, value: String) {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key())
    val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
    val bytes = cipher.iv + encrypted
    preferences.edit().putString(name, Base64.encodeToString(bytes, Base64.NO_WRAP)).apply()
  }

  private fun get(name: String): String? {
    val raw = preferences.getString(name, null) ?: return null
    return try {
      val bytes = Base64.decode(raw, Base64.NO_WRAP)
      if (bytes.size < 13) return null
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
      String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
    } catch (_: Exception) {
      preferences.edit().remove(name).apply()
      null
    }
  }

  fun savePending(verifier: String, state: String) {
    put("pkce_verifier", verifier)
    put("oauth_state", state)
    preferences.edit().putLong("pending_until", System.currentTimeMillis() + 10 * 60 * 1000).apply()
  }

  fun consumePending(incomingState: String): String? {
    val state = get("oauth_state")
    val valid = state != null && state == incomingState &&
      System.currentTimeMillis() <= preferences.getLong("pending_until", 0)
    val verifier = if (valid) get("pkce_verifier") else null
    preferences.edit().remove("pkce_verifier").remove("oauth_state").remove("pending_until").apply()
    return verifier
  }

  var refreshToken: String?
    get() = get("refresh_token")
    set(value) {
      if (value == null) preferences.edit().remove("refresh_token").apply()
      else put("refresh_token", value)
    }
}
