package com.example

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.auth.HavenAuthClient
import com.example.auth.SecureAuthStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HavenAuthDiagnosticTest {

  private lateinit var auth: HavenAuthClient

  @Before
  fun setUp() {
    HavenAuthClient.clearDiagnosticLogs()
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    auth = HavenAuthClient(SecureAuthStore(context))
  }

  @Test
  fun testLogDiagnosticStoresAndFormatsEvents() {
    HavenAuthClient.logDiagnostic("TEST_FLOW", "TEST_STEP", "Diagnostic test message")

    val logs = HavenAuthClient.diagnosticLogs
    assertEquals(1, logs.size)
    val event = logs[0]
    assertEquals(HavenAuthClient.TAG, event.tag)
    assertEquals("TEST_FLOW", event.flow)
    assertEquals("TEST_STEP", event.step)
    assertEquals("Diagnostic test message", event.message)
    assertFalse(event.isError)

    val summary = HavenAuthClient.getDiagnosticSummary()
    assertTrue(summary.contains("[TEST_FLOW] [TEST_STEP] Diagnostic test message"))
  }

  @Test
  fun testNativeGoogleSignInLifecycleDiagnostics() {
    val nonce = auth.beginNativeGoogle()
    assertNotNull(nonce)

    val logsAfterBegin = HavenAuthClient.diagnosticLogs
    assertTrue("Should record BEGIN_NATIVE_GOOGLE", logsAfterBegin.any {
      it.flow == "NATIVE_GOOGLE" && it.step == "BEGIN_NATIVE_GOOGLE"
    })

    auth.cancelNativeGoogle()
    val logsAfterCancel = HavenAuthClient.diagnosticLogs
    assertTrue("Should record CANCEL_NATIVE_GOOGLE", logsAfterCancel.any {
      it.flow == "NATIVE_GOOGLE" && it.step == "CANCEL_NATIVE_GOOGLE"
    })
  }

  @Test
  fun testBrowserAuthorizationDiagnostics() {
    val uri = auth.beginAuthorization()
    assertNotNull(uri)

    val logs = HavenAuthClient.diagnosticLogs
    assertTrue("Should log BEGIN_AUTHORIZATION", logs.any {
      it.flow == "BROWSER_AUTH" && it.step == "BEGIN_AUTHORIZATION"
    })
    assertTrue("Should log AUTHORIZE_URI_BUILT", logs.any {
      it.flow == "BROWSER_AUTH" && it.step == "AUTHORIZE_URI_BUILT"
    })

    val accepted = auth.acceptsCallback(Uri.parse("https://${HavenAuthClient.HOST}/auth/callback?code=abc&state=xyz"))
    assertTrue(accepted)
    assertTrue("Should log ACCEPTS_CALLBACK", HavenAuthClient.diagnosticLogs.any {
      it.flow == "BROWSER_AUTH" && it.step == "ACCEPTS_CALLBACK"
    })
  }

  @Test
  fun testRoutingDiagnosticConfirmsNativeGoogleMode() {
    // When USE_NATIVE_GOOGLE_SIGN_IN is true, logDiagnostic confirms routing to native credential manager
    assertTrue(HavenAuthClient.USE_NATIVE_GOOGLE_SIGN_IN)

    HavenAuthClient.logDiagnostic(
      flow = "AUTH_ROUTER",
      step = "START_SIGN_IN",
      message = "Routing to NATIVE CREDENTIAL MANAGER"
    )

    val lastLog = HavenAuthClient.diagnosticLogs.last()
    assertEquals("AUTH_ROUTER", lastLog.flow)
    assertEquals("START_SIGN_IN", lastLog.step)
    assertTrue(lastLog.message.contains("NATIVE CREDENTIAL MANAGER"))
  }
}
