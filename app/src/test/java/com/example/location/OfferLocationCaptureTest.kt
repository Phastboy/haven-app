package com.example.location

import com.example.data.OfferPlace
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Test

/** Explicit acquisition, automatic readable selection, deadlines and stale-result guards. */
@OptIn(ExperimentalCoroutinesApi::class)
class OfferLocationCaptureTest {
  private val point = CapturedLocation(6.5, 3.4, 25f, false)

  @Test fun `explicit permission intent resolves and selects readable place without another tap`() = runTest {
    var acquisitions = 0
    var resolutions = 0
    var selected: OfferPlace? = null
    val capture = OfferLocationCapture(this, { acquisitions++; point }, {
      resolutions++
      OfferPlace("signed", "Yaba Shopping Complex, Lagos")
    }, { selected = it })
    runCurrent()
    assertEquals(0, acquisitions)
    val request = capture.begin()
    assertEquals(0, acquisitions)
    capture.permissionResult(request, true)
    runCurrent()
    assertEquals("Yaba Shopping Complex, Lagos", selected?.label)
    assertEquals(1, resolutions)
    assertFalse(capture.state.value.pending)
  }

  @Test fun `denial and cancelled permission results never acquire`() = runTest {
    var acquisitions = 0
    val capture = OfferLocationCapture(this, { acquisitions++; point }, { error("must not resolve") }, {})
    capture.permissionResult(capture.begin(), false)
    assertNotNull(capture.state.value.error)
    assertFalse(capture.state.value.pending)
    val oldRequest = capture.begin()
    capture.cancel()
    capture.permissionResult(oldRequest, true)
    runCurrent()
    assertEquals(0, acquisitions)
  }

  @Test fun `manual replacement or leaving rejects late acquisition`() = runTest {
    val gate = CompletableDeferred<CapturedLocation>()
    val capture = OfferLocationCapture(this, { withContext(NonCancellable) { gate.await() } }, { error("must not resolve") }, { error("must not select") })
    capture.permissionResult(capture.begin(), true)
    runCurrent()
    capture.cancel()
    gate.complete(point)
    runCurrent()
    assertFalse(capture.state.value.pending)
  }

  @Test fun `late resolution cannot replace manual selection or cross editor accounts`() = runTest {
    val gate = CompletableDeferred<OfferPlace>()
    var selected = OfferPlace("manual", "Manual place")
    val capture = OfferLocationCapture(this, { point }, { withContext(NonCancellable) { gate.await() } }, { selected = it })
    capture.permissionResult(capture.begin(), true)
    runCurrent()
    assertEquals("Finding the place…", capture.state.value.status)
    capture.cancel()
    gate.complete(OfferPlace("late", "Old account place"))
    runCurrent()
    assertEquals("manual", selected.token)
  }

  @Test fun `failed resolution releases publish and preserves selected draft place`() = runTest {
    var fail = true
    var selected = OfferPlace("previous", "Previous place")
    val capture = OfferLocationCapture(this, { point }, { if (fail) error("offline") else OfferPlace("new", "Readable place") }, { selected = it })
    capture.permissionResult(capture.begin(), true)
    runCurrent()
    assertNotNull(capture.state.value.error)
    assertFalse(capture.state.value.pending)
    assertEquals("previous", selected.token)
    fail = false
    capture.permissionResult(capture.begin(), true)
    runCurrent()
    assertEquals("new", selected.token)
  }

  @Test fun `capture and resolution share a finite total deadline`() = runTest {
    var selected = OfferPlace("previous", "Previous place")
    val capture = OfferLocationCapture(this, { point }, { CompletableDeferred<OfferPlace>().await() }, { selected = it })
    capture.permissionResult(capture.begin(), true)
    runCurrent()
    advanceTimeBy(20_001L)
    runCurrent()
    assertFalse(capture.state.value.pending)
    assertTrue(capture.state.value.error!!.contains("took too long"))
    assertEquals("previous", selected.token)
  }

  @Test fun `lost permission callback does not block forever`() = runTest {
    val capture = OfferLocationCapture(this, { error("must not acquire") }, { error("must not resolve") }, {})
    capture.begin()
    advanceTimeBy(30_001L)
    runCurrent()
    assertFalse(capture.state.value.pending)
    assertNotNull(capture.state.value.error)
  }

  @Test fun `invalid and stale measurements are rejected and coarse fixes stay approximate`() {
    assertNull(CapturedLocation.fromMeasurement(91.0, 3.0, 10f, 0, true))
    assertNull(CapturedLocation.fromMeasurement(6.0, Double.NaN, 10f, 0, true))
    assertNull(CapturedLocation.fromMeasurement(6.0, 3.0, Float.NaN, 0, true))
    assertNull(CapturedLocation.fromMeasurement(6.0, 3.0, 10f, 120_000_000_001L, true))
    assertNull(CapturedLocation.fromMeasurement(6.0, 3.0, 10f, -1, true))
    val coarse = CapturedLocation.fromMeasurement(6.1234567, 3.1234567, 900f, 0, false)!!
    assertTrue(coarse.approximate)
    assertEquals(6.12346, coarse.latitude, 0.00000001)
    assertTrue(CapturedLocation.fromMeasurement(6.0, 3.0, 200f, 0, true)!!.approximate)
  }
}
