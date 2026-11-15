package com.example.images

import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.util.concurrent.FutureTask
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Capacity exhaustion is recoverable and is not evidence of an invalid photo. */
class PhotoReadSaturatedException : IOException(
  "Photo preparation is busy after stalled reads. Your new photos are kept for retry. Try again when reads finish, or reopen the app."
)

/** A broken document provider must not hold the editor coroutine indefinitely.
 * At most two blocking reads can remain in flight; cancellation drops late results
 * and interrupts the worker, without creating an unbounded queue or more threads.
 */
object OfferPhotoReads {
  private val workers = PhotoReadWorkers()
  suspend fun <T> run(read: () -> T): T = workers.run(read)
}

/** Isolated worker ownership also lets tests exercise a fresh saturated pool. */
internal class PhotoReadWorkers {
  private val workers = ThreadPoolExecutor(0, 2, 30L, TimeUnit.SECONDS, SynchronousQueue<Runnable>(),
    { task -> Thread(task, "haven-photo-read").apply { isDaemon = true } })

  suspend fun <T> run(read: () -> T): T = suspendCancellableCoroutine { continuation ->
    val task = FutureTask<Unit> {
      try {
        val value = read()
        if (continuation.isActive) continuation.resume(value)
      } catch (error: Exception) {
        if (continuation.isActive) continuation.resumeWithException(error)
      }
    }
    continuation.invokeOnCancellation { task.cancel(true) }
    try { workers.execute(task) }
    catch (_: RejectedExecutionException) {
      if (continuation.isActive) continuation.resumeWithException(PhotoReadSaturatedException())
    }
  }
}
