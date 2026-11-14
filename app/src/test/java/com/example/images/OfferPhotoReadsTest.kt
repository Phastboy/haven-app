package com.example.images

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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
}
