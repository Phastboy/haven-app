package com.example

import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Legacy launch resources must retain the OS qualifier for switching back to System. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "night")
class LegacyStartupThemeTest {
  @Test fun savedLightDoesNotOverrideSystemNightQualifier() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    ThemeManager.setMode(ThemeMode.LIGHT, context)
    try {
      // Attach only: no network/session restoration or Activity onCreate is needed.
      val activity = Robolectric.buildActivity(MainActivity::class.java).get()
      ThemeManager.setMode(ThemeMode.SYSTEM, context)
      assertEquals(Configuration.UI_MODE_NIGHT_YES,
        activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
      assertEquals(ThemeMode.SYSTEM, ThemeManager.themeMode)
    } finally {
      ThemeManager.setMode(ThemeMode.SYSTEM, context)
    }
  }
}
