package com.example.location

import com.example.data.OfferPlace
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Acquisition/review state is separate from the persisted editor draft. */
data class OfferLocationCaptureState(
  val pending: Boolean = false,
  val candidate: CapturedLocation? = null,
  val error: String? = null,
)

/** Owns one explicit request and rejects late results after cancellation or replacement. */
class OfferLocationCapture(
  private val scope: CoroutineScope,
  private val source: DeviceLocationSource,
  private val sign: suspend (CapturedLocation) -> OfferPlace,
  private val onSelected: (OfferPlace) -> Unit,
) {
  private val mutable = MutableStateFlow(OfferLocationCaptureState())
  val state = mutable.asStateFlow()
  private var generation = 0L
  private var job: Job? = null

  /** Starts permission intent; a returned result is valid only for this request. */
  fun begin(): Long {
    cancel()
    mutable.value = OfferLocationCaptureState(pending = true)
    return generation
  }

  /** Continues only the tap that requested permission, never a restored or cancelled request. */
  fun permissionResult(request: Long, granted: Boolean) {
    if (request != generation || !mutable.value.pending) return
    if (!granted) {
      mutable.value = OfferLocationCaptureState(error = "Location access was denied. Search for a place instead.")
      return
    }
    job = scope.launch {
      try {
        val candidate = source.capture()
        if (generation == request) mutable.value = OfferLocationCaptureState(candidate = candidate)
      } catch (cancelled: CancellationException) { throw cancelled }
      catch (error: Exception) {
        if (generation == request) mutable.value = OfferLocationCaptureState(error = error.message ?: "Location is unavailable. Retry or search for a place.")
      }
    }
  }

  /** Signs a reviewed candidate; this does not submit or publish an offer. */
  fun accept() {
    val candidate = mutable.value.candidate ?: return
    if (mutable.value.pending) return
    val request = generation
    mutable.value = mutable.value.copy(pending = true, error = null)
    job = scope.launch {
      try {
        val place = sign(candidate)
        if (generation == request) {
          mutable.value = OfferLocationCaptureState()
          onSelected(place)
        }
      } catch (cancelled: CancellationException) { throw cancelled }
      catch (_: Exception) {
        if (generation == request) mutable.value = OfferLocationCaptureState(candidate = candidate,
          error = "Could not prepare this location. Check your connection and retry, or search for a place. Your previous location is unchanged.")
      }
    }
  }

  /** Cancels permission, platform and signing results without altering the selected draft place. */
  fun cancel() {
    generation++
    job?.cancel()
    job = null
    mutable.value = OfferLocationCaptureState()
  }
}
