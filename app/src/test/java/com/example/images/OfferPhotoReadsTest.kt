package com.example.images

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** A document provider ignoring thread interruption cannot strand the editor. */
@OptIn(ExperimentalCoroutinesApi::class)
class OfferPhotoReadsTest {
  @Test fun `deadline returns without waiting for an uncooperative provider and ignores late result`() = runTest {
    val started = CountDownLatch(1)
    val release = CountDownLatch(1)
    val finished = CountDownLatch(1)
    var published = false
    val result = async {
      withTimeoutOrNull(100L) {
        OfferPhotoReads.run {
          started.countDown()
          try {
            while (release.count > 0) {
              try { release.await() } catch (_: InterruptedException) { /* deliberately uncooperative fixture */ }
            }
            "late bytes"
          } finally { finished.countDown() }
        }
        published = true
      }
    }
    try {
      runCurrent()
      assertTrue(started.await(2, TimeUnit.SECONDS))
      advanceTimeBy(101L); runCurrent()
      assertTrue(result.isCompleted)
      assertNull(result.await())
      assertFalse(published)
    } finally {
      release.countDown()
      assertTrue(finished.await(2, TimeUnit.SECONDS))
    }
  }

  @Test fun `saturation is typed and a user retry succeeds after workers are released`() = runTest {
    val pool = PhotoReadWorkers()
    val started = CountDownLatch(2)
    val release = CountDownLatch(1)
    val finished = CountDownLatch(2)
    val readers = List(2) {
      launch {
        pool.run {
          started.countDown()
          try {
            while (release.count > 0) {
              try { release.await() } catch (_: InterruptedException) { /* capacity stays occupied */ }
            }
          } finally { finished.countDown() }
        }
      }
    }
    try {
      runCurrent()
      assertTrue(started.await(2, TimeUnit.SECONDS))
      try {
        pool.run { "must not run while saturated" }
        fail("Expected capacity exhaustion")
      } catch (expected: PhotoReadSaturatedException) {
        assertTrue(expected.message!!.contains("kept for retry"))
      }
    } finally {
      readers.forEach { it.cancel() }
      release.countDown()
      assertTrue(finished.await(2, TimeUnit.SECONDS))
      readers.forEach { it.cancelAndJoin() }
    }
    // A completed read releases its worker just after its finally block. This
    // bounded test-only wait models a later user retry, not a production loop.
    var retried: String? = null
    repeat(100) {
      if (retried == null) {
        try { retried = pool.run { "retry succeeded" } }
        catch (_: PhotoReadSaturatedException) { Thread.sleep(10L) }
      }
    }
    assertEquals("retry succeeded", retried)
  }

}
