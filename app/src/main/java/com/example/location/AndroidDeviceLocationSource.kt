package com.example.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Bounded foreground acquisition using platform providers; no last-known or background tracking. */
class AndroidDeviceLocationSource(context: Context) : DeviceLocationSource {
  private val context = context.applicationContext

  override suspend fun capture(): CapturedLocation {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    check(fine || coarse) { "Location access was denied. Search for a place instead." }
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = listOfNotNull(
      LocationManager.GPS_PROVIDER.takeIf { fine }, LocationManager.NETWORK_PROVIDER,
    ).firstOrNull { manager.isProviderEnabled(it) }
      ?: error("Location services are unavailable. Turn them on or search for a place.")
    return withTimeoutOrNull(20_000L) {
      suspendCancellableCoroutine { continuation ->
        val listener = object : LocationListener {
          override fun onLocationChanged(location: Location) {
            if (!continuation.isActive) return
            val age = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
            if (!location.hasAccuracy()) return
            val candidate = CapturedLocation.fromMeasurement(location.latitude, location.longitude, location.accuracy, age, fine) ?: return
            manager.removeUpdates(this)
            continuation.resume(candidate)
          }
          override fun onProviderDisabled(provider: String) {
            if (continuation.isActive) {
              manager.removeUpdates(this)
              continuation.resumeWithException(IllegalStateException("Location services were turned off. Search for a place instead."))
            }
          }
          override fun onProviderEnabled(provider: String) = Unit
          @Deprecated("Required for platform callbacks before Android 11")
          override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        continuation.invokeOnCancellation { manager.removeUpdates(listener) }
        try {
          // Check again at the platform boundary: access can be revoked during setup.
          if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) throw SecurityException("Location permission revoked")
          manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        } catch (error: SecurityException) {
          manager.removeUpdates(listener)
          if (continuation.isActive) continuation.resumeWithException(IllegalStateException("Location access was revoked. Search for a place instead.", error))
        } catch (error: IllegalArgumentException) {
          manager.removeUpdates(listener)
          if (continuation.isActive) continuation.resumeWithException(error)
        }
      }
    } ?: error("No recent location was available in time. Retry or search for a place.")
  }

}
