package com.example.location

import com.example.data.OfferPlace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Only active work blocks submission; a location is selected automatically once resolved. */
data class OfferLocationCaptureState(
  val pending: Boolean = false,
  val status: String? = null,
  val error: String? = null,
)

/** One explicit capture and resolution, bounded together and isolated from stale callbacks. */
class OfferLocationCapture(
  private val scope: CoroutineScope,
  private val source: DeviceLocationSource,
  private val resolve: suspend (CapturedLocation) -> OfferPlace,
  private val onSelected: (OfferPlace) -> Unit,
  private val timeoutMillis: Long = 20_000L,
  private val permissionTimeoutMillis: Long = 30_000L,
) {
  private val mutable = MutableStateFlow(OfferLocationCaptureState())
  val state = mutable.asStateFlow()
  private var generation = 0L
  private var job: Job? = null
  private var awaitingPermission = false

  fun begin(): Long {
    cancel()
    val request = generation
    awaitingPermission = true
    mutable.value = OfferLocationCaptureState(pending = true, status = "Waiting for location permission…")
    job = scope.launch {
      delay(permissionTimeoutMillis)
      if (request == generation) {
        awaitingPermission = false
        mutable.value = OfferLocationCaptureState(error = "Location permission did not finish. Retry, search for a place, or continue without location.")
      }
    }
    return request
  }

  fun permissionResult(request: Long, granted: Boolean) {
    if (request != generation || !mutable.value.pending || !awaitingPermission) return
    awaitingPermission = false
    job?.cancel()
    if (!granted) {
      mutable.value = OfferLocationCaptureState(error = "Location access was denied. Search for a place or continue without location.")
      return
    }
    job = scope.launch {
      try {
        val place = withTimeout(timeoutMillis) {
          mutable.value = OfferLocationCaptureState(pending = true, status = "Getting your location…")
          val point = source.capture()
          if (request != generation) throw CancellationException("Location request replaced")
          mutable.value = OfferLocationCaptureState(pending = true, status = "Finding the place…")
          resolve(point)
        }
        check(place.token.isNotBlank() && place.label.isNotBlank()) { "No readable place was returned" }
        if (generation == request) {
          mutable.value = OfferLocationCaptureState()
          onSelected(place)
        }
      } catch (_: TimeoutCancellationException) {
        if (generation == request) mutable.value = OfferLocationCaptureState(
          error = "Finding your location took too long. Retry, search, or continue without location. Your previous selection is unchanged.")
      } catch (cancelled: CancellationException) { throw cancelled }
      catch (_: Exception) {
        if (generation == request) mutable.value = OfferLocationCaptureState(
          error = "Could not find a readable place. Retry, search, or continue without location. Your previous selection is unchanged.")
      }
    }
  }

  /** Cancels permission, capture and lookup without changing the draft location. */
  fun cancel() {
    generation++
    awaitingPermission = false
    job?.cancel()
    job = null
    mutable.value = OfferLocationCaptureState()
  }
}
