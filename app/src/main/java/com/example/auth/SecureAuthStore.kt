package com.example.auth

import android.content.Context
import android.os.Build
import android.security.KeyStoreException
import androidx.core.content.edit
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
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
  private val keyReset: (() -> Unit)? = null,
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
    try {
      cipher.init(Cipher.ENCRYPT_MODE, key())
    } catch (failure: Exception) {
      if (!recoverPermanentKey(failure)) throw failure
      cipher.init(Cipher.ENCRYPT_MODE, key())
    }
    val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
    val bytes = cipher.iv + encrypted
    preferences.edit { putString(name, Base64.encodeToString(bytes, Base64.NO_WRAP)) }
  }

  private fun get(name: String): String? {
    val raw = preferences.getString(name, null) ?: return null
    val bytes = try { Base64.decode(raw, Base64.NO_WRAP) }
    catch (_: IllegalArgumentException) {
      preferences.edit { remove(name) }
      return null
    }
    if (bytes.size < 28) {
      preferences.edit { remove(name) }
      return null
    }
    // Provider/key initialization errors are retryable; they do not prove corrupt ciphertext.
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    try {
      cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
    } catch (failure: Exception) {
      if (recoverPermanentKey(failure)) return null
      throw failure
    }
    return try {
      String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
    } catch (_: javax.crypto.BadPaddingException) {
      preferences.edit { remove(name) }
      null
    } catch (_: javax.crypto.IllegalBlockSizeException) {
      preferences.edit { remove(name) }
      null
    }
  }

  /** Only explicit permanent failures justify deleting a device key and its credentials. */
  private fun recoverPermanentKey(failure: Exception): Boolean {
    val permanent = generateSequence<Throwable>(failure) { it.cause }.any { cause ->
      cause is KeyPermanentlyInvalidatedException ||
        (Build.VERSION.SDK_INT >= 33 && cause is KeyStoreException &&
          cause.numericErrorCode in setOf(KeyStoreException.ERROR_KEY_CORRUPTED, KeyStoreException.ERROR_KEY_DOES_NOT_EXIST))
    }
    if (!permanent) return false
    preferences.edit { clear() }
    if (keyReset != null) keyReset.invoke()
    else KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(keyAlias) }
    return true
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
