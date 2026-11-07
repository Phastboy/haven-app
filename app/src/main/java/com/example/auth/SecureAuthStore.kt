package com.example.auth

import android.content.Context
import androidx.core.content.edit
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Device-only credentials. Android Keystore owns the AES key; backups exclude this file. */
class SecureAuthStore internal constructor(
  context: Context,
  private val keyProvider: (() -> SecretKey)? = null
) {
  private val preferences = context.getSharedPreferences("haven_auth", Context.MODE_PRIVATE)
  private val keyAlias = "haven_device_auth_v1"

  private fun key(): SecretKey {
    keyProvider?.let { return it() }
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (store.getKey(keyAlias, null) as? SecretKey)?.let { return it }
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    generator.init(
      KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .build()
    )
    // Fail closed if Keystore is unavailable. Test keys are injected by test code only.
    return generator.generateKey()
  }

  private fun put(name: String, value: String) {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key())
    val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
    val bytes = cipher.iv + encrypted
    preferences.edit { putString(name, Base64.encodeToString(bytes, Base64.NO_WRAP)) }
  }

  private fun get(name: String): String? {
    val raw = preferences.getString(name, null) ?: return null
    // A transient Keystore failure must propagate without deleting valid ciphertext.
    val encryptionKey = key()
    return try {
      val bytes = Base64.decode(raw, Base64.NO_WRAP)
      if (bytes.size < 13) return null
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, encryptionKey, GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
      String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
    } catch (_: Exception) {
      preferences.edit { remove(name) }
      null
    }
  }

  /** Stores the single-use nonce sent with a native Google ID token request. */
  fun saveNativeNonce(nonce: String) {
    put("native_nonce", nonce)
    preferences.edit { putLong("native_nonce_until", System.currentTimeMillis() + 10 * 60 * 1000) }
  }

  /** Returns the pending native nonce once, or null when absent or expired. */
  fun consumeNativeNonce(): String? {
    val nonce = get("native_nonce")
    val valid = nonce != null &&
      System.currentTimeMillis() <= preferences.getLong("native_nonce_until", 0)
    preferences.edit {
      remove("native_nonce")
      remove("native_nonce_until")
    }
    return if (valid) nonce else null
  }

  fun clearNativeNonce() {
    preferences.edit {
      remove("native_nonce")
      remove("native_nonce_until")
    }
  }

  var refreshToken: String?
    get() = get("refresh_token")
    set(value) {
      if (value == null) preferences.edit { remove("refresh_token") }
      else put("refresh_token", value)
    }
}
