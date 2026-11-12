package com.example.location

import java.util.Locale

/** One measured point, never an address lookup or a claim of physical presence. */
data class CapturedLocation(
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Float,
  val approximate: Boolean,
) {
  companion object {
    /** Rejects stale/invalid measurements and rounds coordinates to the displayed precision. */
    fun fromMeasurement(latitude: Double, longitude: Double, accuracy: Float, ageNanos: Long, precisePermission: Boolean): CapturedLocation? {
      if (!latitude.isFinite() || latitude !in -90.0..90.0 || !longitude.isFinite() || longitude !in -180.0..180.0 ||
        !accuracy.isFinite() || accuracy < 0f || ageNanos !in 0L..120_000_000_000L) return null
      fun rounded(value: Double) = kotlin.math.round(value * 100_000.0) / 100_000.0
      return CapturedLocation(rounded(latitude), rounded(longitude), accuracy, !precisePermission || accuracy > 100f)
    }
  }

  /** Public label includes coordinates and measurement uncertainty rather than inventing an address. */
  fun publicLabel(): String = String.format(
    Locale.US, "%s: %.5f, %.5f (accuracy about %.0f m)",
    if (approximate) "Approximate current location" else "Current location",
    latitude, longitude, accuracyMeters.coerceAtLeast(1f),
  )
}

/** Captures only when invoked; implementations must cancel their platform request on cancellation. */
fun interface DeviceLocationSource {
  suspend fun capture(): CapturedLocation
}
