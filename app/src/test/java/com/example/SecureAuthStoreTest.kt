package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.SecureAuthStore
import java.io.IOException
import javax.crypto.KeyGenerator
import android.security.keystore.KeyPermanentlyInvalidatedException
import java.security.UnrecoverableKeyException
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SecureAuthStoreTest {
  private val context: Context get() = ApplicationProvider.getApplicationContext()
  @Before fun reset() { context.getSharedPreferences("haven_auth", Context.MODE_PRIVATE).edit().clear().commit() }

  @Test fun `keystore write failure cannot silently encrypt with a fallback key`() {
    val store = SecureAuthStore(context) { throw IOException("Keystore unavailable") }
    assertThrows(IOException::class.java) { store.refreshToken = "test-refresh" }
    assertNull(context.getSharedPreferences("haven_auth", Context.MODE_PRIVATE).getString("refresh_token", null))
  }

  @Test fun `default keystore path fails closed when its provider is unavailable`() {
    val provider = java.security.Security.getProvider("AndroidKeyStore")
    java.security.Security.removeProvider("AndroidKeyStore")
    try {
      assertThrows(java.security.KeyStoreException::class.java) {
        SecureAuthStore(context).refreshToken = "must-not-be-stored"
      }
      assertNull(context.getSharedPreferences("haven_auth", Context.MODE_PRIVATE).getString("refresh_token", null))
    } finally {
      if (provider != null) java.security.Security.addProvider(provider)
    }
  }

  @Test fun `temporary key failure preserves ciphertext for a later retry`() {
    val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    var unavailable = false
    val store = SecureAuthStore(context) {
      if (unavailable) throw IOException("Keystore unavailable") else key
    }
    store.refreshToken = "test-refresh"
    val prefs = context.getSharedPreferences("haven_auth", Context.MODE_PRIVATE)
    val encrypted = prefs.getString("refresh_token", null)
    assertNotNull(encrypted)
    assertNotEquals("test-refresh", encrypted)
    unavailable = true
    assertThrows(IOException::class.java) { store.refreshToken }
    assertEquals(encrypted, prefs.getString("refresh_token", null))
    unavailable = false
    assertEquals("test-refresh", store.refreshToken)
  }

  @Test fun `confirmed invalid key clears credentials and permits a new sign in`() {
    var key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    var invalid = false
    var resets = 0
    val store = SecureAuthStore(context, keyReset = {
      resets++
      key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
      invalid = false
    }) {
      if (invalid) throw UnrecoverableKeyException("Invalid key").apply {
        initCause(KeyPermanentlyInvalidatedException())
      }
      key
    }
    store.refreshToken = "old-refresh"
    store.saveNativeNonce("old-nonce")
    invalid = true
    assertNull(store.refreshToken)
    assertEquals(1, resets)
    assertNull(store.consumeNativeNonce())
    store.saveNativeNonce("new-nonce")
    store.refreshToken = "new-refresh"
    assertEquals("new-nonce", store.consumeNativeNonce())
    assertEquals("new-refresh", store.refreshToken)
  }

  @Test fun `unclassified unrecoverable key failure is retryable without deleting credentials`() {
    val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    var unavailable = false
    var resets = 0
    val store = SecureAuthStore(context, keyReset = { resets++ }) {
      if (unavailable) throw UnrecoverableKeyException("Provider unavailable")
      key
    }
    store.refreshToken = "saved-refresh"
    unavailable = true
    assertThrows(UnrecoverableKeyException::class.java) { store.refreshToken }
    assertEquals(0, resets)
    unavailable = false
    assertEquals("saved-refresh", store.refreshToken)
  }
}
