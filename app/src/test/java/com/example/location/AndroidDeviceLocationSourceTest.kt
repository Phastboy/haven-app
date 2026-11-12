package com.example.location

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Real adapter regressions for network fallback and platform-listener cleanup. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidDeviceLocationSourceTest {
  private val app: Application get() = ApplicationProvider.getApplicationContext()
  private val manager: LocationManager get() = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager

  private fun enableProviders() {
    shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    shadowOf(manager).setLocationEnabled(true)
    shadowOf(manager).setProviderEnabled(LocationManager.GPS_PROVIDER, true)
    shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
  }

  private fun networkFix() = Location(LocationManager.NETWORK_PROVIDER).apply {
    latitude = 6.5
    longitude = 3.4
    accuracy = 250f
    time = System.currentTimeMillis()
    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
  }

  @Suppress("DEPRECATION") // Shadow listener inventory verifies platform resource cleanup.
  @Test fun networkCanCompleteWhenGpsIsEnabledButSilent() = runTest {
    enableProviders()
    val result = async { AndroidDeviceLocationSource(app).capture() }
    runCurrent()
    shadowOf(manager).simulateLocation(networkFix())
    shadowOf(Looper.getMainLooper()).idle()
    runCurrent()
    val point = result.await()
    assertEquals(6.5, point.latitude, 0.00001)
    assertTrue(point.approximate)
    assertTrue(shadowOf(manager).getLocationUpdateListeners().isEmpty())
  }

  @Suppress("DEPRECATION") // Both providers must be detached on cancellation.
  @Test fun cancellationRemovesBothProviderSubscriptions() = runTest {
    enableProviders()
    val result = async { AndroidDeviceLocationSource(app).capture() }
    runCurrent()
    assertFalse(shadowOf(manager).getLocationUpdateListeners().isEmpty())
    result.cancel()
    runCurrent()
    assertTrue(shadowOf(manager).getLocationUpdateListeners().isEmpty())
  }
}
