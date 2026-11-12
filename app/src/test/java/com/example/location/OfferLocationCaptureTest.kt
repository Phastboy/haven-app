package com.example.location

import com.example.data.OfferPlace
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Test

/** Verifies explicit permission, review, cancellation, failure and late-result boundaries. */
@OptIn(ExperimentalCoroutinesApi::class)
class OfferLocationCaptureTest {
  private val point = CapturedLocation(6.5, 3.4, 25f, false)

  @Test fun `capture requires explicit intent and review before selecting`() = runTest {
    var acquisitions = 0
    var signatures = 0
    var selected: OfferPlace? = null
    val capture = OfferLocationCapture(this, { acquisitions++; point }, { signatures++; OfferPlace("signed", it.publicLabel()) }, { selected = it })
    runCurrent()
    assertEquals(0, acquisitions)
    val request = capture.begin()
    assertEquals(0, acquisitions)
    capture.permissionResult(request, true)
    runCurrent()
    assertEquals(point, capture.state.value.candidate)
    assertNull(selected)
    assertEquals(0, signatures)
    capture.accept()
    runCurrent()
    assertEquals("signed", selected?.token)
    assertEquals(1, signatures)
  }

  @Test fun `denial and cancelled permission results never acquire`() = runTest {
    var acquisitions = 0
    val capture = OfferLocationCapture(this, { acquisitions++; point }, { error("must not sign") }, {})
    capture.permissionResult(capture.begin(), false)
    assertNotNull(capture.state.value.error)
    val oldRequest = capture.begin()
    capture.cancel()
    capture.permissionResult(oldRequest, true)
    runCurrent()
    assertEquals(0, acquisitions)
  }

  @Test fun `manual replacement or leaving rejects late acquisition`() = runTest {
    val gate = CompletableDeferred<CapturedLocation>()
    val capture = OfferLocationCapture(this, { withContext(NonCancellable) { gate.await() } }, { error("must not sign") }, { error("must not select") })
    capture.permissionResult(capture.begin(), true)
    runCurrent()
    capture.cancel()
    gate.complete(point)
    runCurrent()
    assertNull(capture.state.value.candidate)
    assertFalse(capture.state.value.pending)
  }

  @Test fun `late signing cannot replace manual selection or cross editor account`() = runTest {
    val gate = CompletableDeferred<OfferPlace>()
    var selected: OfferPlace? = OfferPlace("manual", "Manual place")
    val capture = OfferLocationCapture(this, { point }, { withContext(NonCancellable) { gate.await() } }, { selected = it })
    capture.permissionResult(capture.begin(), true)
    runCurrent()
    capture.accept()
    runCurrent()
    capture.cancel()
    gate.complete(OfferPlace("late", "Old account location"))
    runCurrent()
    assertEquals("manual", selected?.token)
  }

  @Test fun `failed signing retains candidate and previous draft for retry`() = runTest {
    var fail = true
    var selected = OfferPlace("previous", "Previous place")
    val capture = OfferLocationCapture(this, { point }, { if (fail) error("offline") else OfferPlace("new", it.publicLabel()) }, { selected = it })
    capture.permissionResult(capture.begin(), true)
    runCurrent()
    capture.accept()
    runCurrent()
    assertNotNull(capture.state.value.error)
    assertEquals(point, capture.state.value.candidate)
    assertEquals("previous", selected.token)
    fail = false
    capture.accept()
    runCurrent()
    assertEquals("new", selected.token)
  }

  @Test fun `new request replaces previous candidate and never automatically recaptures`() = runTest {
    var calls = 0
    val capture = OfferLocationCapture(this, { calls++; point }, { OfferPlace("signed", it.publicLabel()) }, {})
    capture.permissionResult(capture.begin(), true)
    runCurrent()
    assertEquals(1, calls)
    capture.cancel()
    runCurrent()
    assertEquals(1, calls)
    capture.permissionResult(capture.begin(), true)
    runCurrent()
    assertEquals(2, calls)
  }

  @Test fun `invalid and stale measurements are rejected and coarse fixes stay approximate`() {
    assertNull(CapturedLocation.fromMeasurement(91.0, 3.0, 10f, 0, true))
    assertNull(CapturedLocation.fromMeasurement(6.0, Double.NaN, 10f, 0, true))
    assertNull(CapturedLocation.fromMeasurement(6.0, 3.0, Float.NaN, 0, true))
    assertNull(CapturedLocation.fromMeasurement(6.0, 3.0, 10f, 120_000_000_001L, true))
    assertNull(CapturedLocation.fromMeasurement(6.0, 3.0, 10f, -1, true))
    val coarse = CapturedLocation.fromMeasurement(6.1234567, 3.1234567, 900f, 0, false)!!
    assertTrue(coarse.approximate)
    assertTrue(coarse.publicLabel().startsWith("Approximate"))
    assertEquals(6.12346, coarse.latitude, 0.00000001)
    assertTrue(CapturedLocation.fromMeasurement(6.0, 3.0, 200f, 0, true)!!.approximate)
  }
}
