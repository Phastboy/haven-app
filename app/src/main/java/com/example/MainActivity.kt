package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.credentials.exceptions.GetCredentialCancellationException
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
import com.example.auth.NativeGoogleSignIn
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
    auth = HavenAuthClient.shared(this)
    setContent {
      HavenTheme {
        HavenApp(signInViewModel = signInViewModel,
          onStartGoogleSignIn = ::startSignIn,
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

  private fun startSignIn() {
    HavenAuthClient.logDiagnostic(
      flow = "AUTH_ROUTER",
      step = "START_SIGN_IN",
      message = "startSignIn() triggered. USE_NATIVE_GOOGLE_SIGN_IN=${HavenAuthClient.USE_NATIVE_GOOGLE_SIGN_IN} -> Routing to ${if (HavenAuthClient.USE_NATIVE_GOOGLE_SIGN_IN) "NATIVE CREDENTIAL MANAGER (startNativeGoogleSignIn)" else "BROWSER AUTH FALLBACK (startBrowserSignIn)"}"
    )
    if (HavenAuthClient.USE_NATIVE_GOOGLE_SIGN_IN) startNativeGoogleSignIn() else startBrowserSignIn()
  }

  private fun startNativeGoogleSignIn() {
    HavenAuthClient.logDiagnostic(
      flow = "NATIVE_GOOGLE",
      step = "START_NATIVE_FLOW",
      message = "Starting native Google Sign-in flow with Android Credential Manager."
    )
    lifecycleScope.launch {
      val nonce = auth.beginNativeGoogle()
      val errorMsg: String? = try {
        HavenAuthClient.logDiagnostic(
          flow = "NATIVE_GOOGLE",
          step = "INVOKING_NATIVE_GOOGLE_SIGN_IN",
          message = "Invoking NativeGoogleSignIn.idToken with GOOGLE_WEB_CLIENT_ID and single-use nonce."
        )
        val idToken = NativeGoogleSignIn.idToken(
          this@MainActivity,
          HavenAuthClient.GOOGLE_WEB_CLIENT_ID,
          nonce
        )
        HavenAuthClient.logDiagnostic(
          flow = "NATIVE_GOOGLE",
          step = "ID_TOKEN_ACQUIRED",
          message = "Google ID token acquired successfully (token length=${idToken.length}). Dispatching to Haven backend..."
        )
        withContext(Dispatchers.IO) { auth.completeNativeGoogle(idToken, nonce) }
      } catch (e: GetCredentialCancellationException) {
        HavenAuthClient.logDiagnostic(
          flow = "NATIVE_GOOGLE",
          step = "FLOW_CANCELLED",
          message = "Credential Manager was cancelled by the user. Not falling back to browser.",
          throwable = e
        )
        auth.cancelNativeGoogle()
        "Google sign-in was cancelled."
      } catch (e: Exception) {
        val isNoCredential = e is androidx.credentials.exceptions.NoCredentialException ||
          e.message?.contains("no credentials", ignoreCase = true) == true
        HavenAuthClient.logDiagnostic(
          flow = "NATIVE_GOOGLE",
          step = if (isNoCredential) "NO_CREDENTIALS_AVAILABLE" else "FLOW_EXCEPTION",
          message = "Native Google Sign-in encountered an exception: ${e.javaClass.name}: ${e.message}. (isNoCredential=$isNoCredential)",
          isError = true,
          throwable = e
        )
        auth.cancelNativeGoogle()
        if (isNoCredential) {
          "Google sign-in found no credentials on this device. Ensure an active Google account is signed in on the device, or sign in with your email."
        } else {
          "Google sign-in failed: ${e.message ?: "unexpected error"}"
        }
      }
      if (errorMsg != null) {
        HavenAuthClient.logDiagnostic(
          flow = "NATIVE_GOOGLE",
          step = "POSTING_UI_ERROR",
          message = "Posting auth error message to SignInViewModel: $errorMsg",
          isError = true
        )
        signInViewModel.onAuthError(errorMsg)
      } else {
        HavenAuthClient.logDiagnostic(
          flow = "NATIVE_GOOGLE",
          step = "FLOW_SUCCESS",
          message = "Native Google Sign-In completed successfully without opening browser."
        )
      }
    }
  }

  private fun startBrowserSignIn() {
    val caller = Thread.currentThread().stackTrace
      .filter { !it.className.contains("MainActivity") && !it.className.contains("dalvik") && !it.className.contains("java.lang") }
      .take(2)
      .joinToString("; ") { "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" }
    HavenAuthClient.logDiagnostic(
      flow = "BROWSER_AUTH",
      step = "START_BROWSER_SIGN_IN",
      message = "startBrowserSignIn() triggered! Caller: [$caller]"
    )
    try {
      val authUri = auth.beginAuthorization()
      val scheme = when (ThemeManager.themeMode) {
        ThemeMode.SYSTEM -> CustomTabsIntent.COLOR_SCHEME_SYSTEM
        ThemeMode.LIGHT -> CustomTabsIntent.COLOR_SCHEME_LIGHT
        ThemeMode.DARK -> CustomTabsIntent.COLOR_SCHEME_DARK
      }
      val customTabsIntent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setColorScheme(scheme)
        .build()
      HavenAuthClient.logDiagnostic(
        flow = "BROWSER_AUTH",
        step = "LAUNCH_CUSTOM_TABS",
        message = "Launching CustomTabs with URL: $authUri"
      )
      customTabsIntent.launchUrl(this, authUri)
    } catch (e: Exception) {
      HavenAuthClient.logDiagnostic(
        flow = "BROWSER_AUTH",
        step = "CUSTOM_TABS_EXCEPTION",
        message = "Custom Tabs launch failed, attempting ACTION_VIEW fallback: ${e.message}",
        isError = true,
        throwable = e
      )
      try {
        val authUri = auth.beginAuthorization()
        HavenAuthClient.logDiagnostic(
          flow = "BROWSER_AUTH",
          step = "LAUNCH_ACTION_VIEW",
          message = "Launching ACTION_VIEW with URL: $authUri"
        )
        startActivity(Intent(Intent.ACTION_VIEW, authUri))
      } catch (e2: Exception) {
        HavenAuthClient.logDiagnostic(
          flow = "BROWSER_AUTH",
          step = "ACTION_VIEW_EXCEPTION",
          message = "Could not open browser via ACTION_VIEW: ${e2.message}",
          isError = true,
          throwable = e2
        )
        signInViewModel.onAuthError("Could not open your browser. Please try again.")
      }
    }
  }

  private fun handleCallback(callback: Intent) {
    val uri = callback.data ?: return
    HavenAuthClient.logDiagnostic(
      flow = "BROWSER_AUTH",
      step = "HANDLE_CALLBACK",
      message = "Received callback intent with URI: $uri"
    )
    lifecycleScope.launch {
      val errorMsg = withContext(Dispatchers.IO) {
        try {
          auth.completeAuthorization(uri)
        } catch (e: Exception) {
          HavenAuthClient.logDiagnostic(
            flow = "BROWSER_AUTH",
            step = "COMPLETE_CALLBACK_EXCEPTION",
            message = "Exception in completeAuthorization: ${e.message}",
            isError = true,
            throwable = e
          )
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
  onStartGoogleSignIn: () -> Unit = {},
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
          onStartGoogleSignIn = onStartGoogleSignIn,
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
