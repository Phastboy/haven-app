package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.SecureAuthStore
import java.io.IOException
import javax.crypto.KeyGenerator
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
}
