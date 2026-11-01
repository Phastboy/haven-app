package com.example.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Supported Theme Modes in Haven:
 * - SYSTEM: Follows system appearance (isSystemInDarkTheme)
 * - LIGHT: Force clean monastic light theme
 * - DARK: Force calm high-contrast dark theme
 */
enum class ThemeMode(val title: String, val subtitle: String) {
  SYSTEM("System default", "Matches device appearance"),
  LIGHT("Light", "Clean monastic light mode"),
  DARK("Dark", "Calm high-contrast dark mode")
}

/**
 * Singleton theme state manager providing reactive theme selection
 * across the entire application with optional SharedPreferences persistence.
 */
object ThemeManager {
  private const val PREFS_NAME = "haven_theme_prefs"
  private const val KEY_THEME_MODE = "theme_mode"

  var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    private set

  fun init(context: Context) {
    try {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
      themeMode = ThemeMode.valueOf(saved)
    } catch (_: Exception) {
      themeMode = ThemeMode.SYSTEM
    }
  }

  fun setMode(mode: ThemeMode, context: Context? = null) {
    themeMode = mode
    context?.let { ctx ->
      try {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
      } catch (_: Exception) {
        // Fallback to in-memory state
      }
    }
  }
}
