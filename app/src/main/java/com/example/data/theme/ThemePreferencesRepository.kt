package com.example.data.theme

import android.content.Context
import com.example.auth.HavenAuthClient
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.ThemeMode

/**
 * Clean Architecture repository contract for Theme & Appearance preferences.
 *
 * Persists selected appearance mode locally via [ThemeManager] and SharedPreferences.
 * If backend user-preference synchronization is deployed in the future (e.g. PATCH /api/v1/me/preferences),
 * it is a minimal single-method change to link the remote call here.
 */
interface ThemePreferencesRepository {
  fun getThemeMode(): ThemeMode
  fun setThemeMode(mode: ThemeMode)
}

class HavenThemePreferencesRepository(
  private val context: Context,
  private val authClient: HavenAuthClient? = null
) : ThemePreferencesRepository {

  override fun getThemeMode(): ThemeMode = ThemeManager.themeMode

  override fun setThemeMode(mode: ThemeMode) {
    ThemeManager.setMode(mode, context)
    // Future backend sync:
    // If backend preference endpoint is live, dispatch asynchronously without blocking UI:
    // authClient?.authenticatedMutation("PATCH", "/api/v1/me/preferences", JSONObject().put("theme_mode", mode.name.lowercase()))
  }

  companion object {
    @Volatile
    private var instance: ThemePreferencesRepository? = null

    fun getInstance(context: Context): ThemePreferencesRepository =
      instance ?: synchronized(this) {
        instance ?: HavenThemePreferencesRepository(
          context.applicationContext,
          HavenAuthClient.shared(context.applicationContext)
        ).also { instance = it }
      }
  }
}
