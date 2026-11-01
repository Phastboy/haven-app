package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.ThemeMode
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.HavenAuthClient
import com.example.auth.SecureAuthStore
import com.example.ui.model.OffersViewModel
import com.example.ui.model.SessionManager
import com.example.ui.model.SignInViewModel
import com.example.ui.screens.OffersScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.HavenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen {
  WELCOME,
  SIGN_IN,
  OFFERS
}

class MainActivity : ComponentActivity() {
  private val signInViewModel: SignInViewModel by viewModels()
  private lateinit var auth: HavenAuthClient

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT
    enableEdgeToEdge()
    com.example.ui.theme.ThemeManager.init(this)
    auth = HavenAuthClient(SecureAuthStore(this))
    setContent {
      HavenTheme {
        HavenApp(signInViewModel = signInViewModel,
          onStartBrowserSignIn = ::startBrowserSignIn,
          onSignOut = ::signOut)
      }
    }
    if (auth.acceptsCallback(intent?.data)) handleCallback(intent)
    else lifecycleScope.launch(Dispatchers.IO) {
      try { auth.restore() } catch (_: Exception) { /* Network failure keeps the device signed out. */ }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (auth.acceptsCallback(intent.data)) handleCallback(intent)
  }

  private fun startBrowserSignIn() {
    try {
      val scheme = when (ThemeManager.themeMode) {
        ThemeMode.SYSTEM -> CustomTabsIntent.COLOR_SCHEME_SYSTEM
        ThemeMode.LIGHT -> CustomTabsIntent.COLOR_SCHEME_LIGHT
        ThemeMode.DARK -> CustomTabsIntent.COLOR_SCHEME_DARK
      }
      CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setColorScheme(scheme)
        .build()
        .launchUrl(this, auth.beginAuthorization())
    } catch (_: Exception) {
      signInViewModel.onAuthError("Could not open your browser. Please try again.")
    }
  }

  private fun handleCallback(callback: Intent) {
    val uri = callback.data ?: return
    lifecycleScope.launch {
      val errorMsg = withContext(Dispatchers.IO) {
        try {
          auth.completeAuthorization(uri)
        } catch (e: Exception) {
          "Sign-in failed: ${e.message ?: "unexpected error"}"
        }
      }
      if (errorMsg != null) {
        signInViewModel.onAuthError(errorMsg)
      }
    }
  }

  private fun signOut() {
    lifecycleScope.launch(Dispatchers.IO) { auth.signOut() }
  }
}

@Composable
fun HavenApp(
  signInViewModel: SignInViewModel = viewModel(),
  offersViewModel: OffersViewModel = viewModel(),
  onStartBrowserSignIn: () -> Unit = {},
  onSignOut: () -> Unit = {}
) {
  var currentScreen by remember { mutableStateOf(Screen.WELCOME) }
  val session by SessionManager.session.collectAsState()
  LaunchedEffect(session.isAuthenticated) {
    if (session.isAuthenticated) currentScreen = Screen.OFFERS
  }

  AnimatedContent(
    targetState = currentScreen,
    transitionSpec = {
      if (targetState == Screen.SIGN_IN || targetState == Screen.OFFERS) {
        // Forward navigation
        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
          slideOutHorizontally { width -> -width / 3 } + fadeOut()
        )
      } else {
        // Return / Back navigation
        (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
          slideOutHorizontally { width -> width } + fadeOut()
        )
      }
    },
    label = "screen_navigation",
    modifier = Modifier.fillMaxSize()
  ) { screen ->
    when (screen) {
      Screen.WELCOME -> {
        WelcomeScreen(
          onNavigateToSignIn = { currentScreen = Screen.SIGN_IN },
          onNavigateToOffers = { currentScreen = Screen.OFFERS }
        )
      }
      Screen.SIGN_IN -> {
        SignInScreen(
          viewModel = signInViewModel,
          onNavigateBack = { currentScreen = Screen.WELCOME },
          onStartBrowserSignIn = onStartBrowserSignIn,
          onNavigateToOffers = { currentScreen = Screen.OFFERS }
        )
      }
      Screen.OFFERS -> {
        OffersScreen(
          viewModel = offersViewModel,
          onNavigateBack = { currentScreen = Screen.WELCOME },
          onNavigateToSignIn = { currentScreen = Screen.SIGN_IN },
          onSignOut = onSignOut
        )
      }
    }
  }
}
