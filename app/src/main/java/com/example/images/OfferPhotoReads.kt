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

/** A broken document provider must not hold the editor coroutine indefinitely.
 * At most two blocking reads can remain in flight; cancellation drops late results
 * and interrupts the worker, without creating an unbounded queue or more threads.
 */
object OfferPhotoReads {
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
      if (continuation.isActive) continuation.resumeWithException(IOException("Photo preparation is unavailable after stalled reads. Keep your draft and try again later, or reopen the app."))
    }
  }
}
