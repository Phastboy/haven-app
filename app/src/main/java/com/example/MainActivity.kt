package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.credentials.exceptions.GetCredentialCancellationException
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ui.model.StartupViewModel
import com.example.ui.screens.StartupGate
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
  override fun attachBaseContext(newBase: android.content.Context) {
    com.example.ui.theme.ThemeManager.init(newBase)
    val config = android.content.res.Configuration(newBase.resources.configuration)
    val mode = com.example.ui.theme.ThemeManager.themeMode
    if (mode != com.example.ui.theme.ThemeMode.SYSTEM) {
      config.uiMode = (config.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or
        if (mode == com.example.ui.theme.ThemeMode.DARK) android.content.res.Configuration.UI_MODE_NIGHT_YES
        else android.content.res.Configuration.UI_MODE_NIGHT_NO
    }
    super.attachBaseContext(newBase.createConfigurationContext(config))
  }

  private val signInViewModel: SignInViewModel by viewModels()
  private lateinit var auth: HavenAuthClient
  private val startupViewModel: StartupViewModel by viewModels {
    val client = auth
    object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T = StartupViewModel(
        restore = { withContext(Dispatchers.IO) { client.restore() } },
        hasSavedSession = { withContext(Dispatchers.IO) { client.hasSavedSession() } }
      ) as T
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    com.example.ui.theme.ThemeManager.init(this)
    com.example.ui.theme.ThemeManager.applyLaunchMode(this)
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    com.example.ui.theme.ThemeManager.init(this)
    auth = HavenAuthClient.shared(this)
    setContent {
      HavenTheme {
        com.example.updates.UpdatePrompt()
        val startup by startupViewModel.state.collectAsState()
        StartupGate(startup, startupViewModel::retry) { authenticated ->
          HavenApp(signInViewModel = signInViewModel,
            initialScreen = if (authenticated) Screen.OFFERS else Screen.WELCOME,
            onStartGoogleSignIn = ::startSignIn,
            onSignOut = ::signOut,
            onExit = ::finish)
        }
      }
    }
  }

  private fun startSignIn() {
    HavenAuthClient.logDiagnostic(
      flow = "AUTH_ROUTER",
      step = "START_SIGN_IN",
      message = "startSignIn() triggered; routing to native Google Credential Manager."
    )
    startNativeGoogleSignIn()
  }

  private fun startNativeGoogleSignIn() {
    HavenAuthClient.logDiagnostic(
      flow = "NATIVE_GOOGLE",
      step = "START_NATIVE_FLOW",
      message = "Starting native Google Sign-in flow with Android Credential Manager."
    )
    lifecycleScope.launch {
      val errorMsg: String? = try {
        val nonce = withContext(Dispatchers.IO) { auth.beginNativeGoogle() }
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
          "Google sign-in found no credentials on this device. Add a Google account in system settings and try again."
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

  private fun signOut() {
    lifecycleScope.launch(Dispatchers.IO) { auth.signOut() }
  }
}

@Composable
fun HavenApp(
  signInViewModel: SignInViewModel = viewModel(),
  offersViewModel: OffersViewModel = viewModel(factory = OffersViewModel.factory(androidx.compose.ui.platform.LocalContext.current)),
  initialScreen: Screen = Screen.WELCOME,
  onStartGoogleSignIn: () -> Unit = {},
  onSignOut: () -> Unit = {},
  onExit: () -> Unit = {}
) {
  var currentScreen by remember { mutableStateOf(initialScreen) }
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
          onNavigateToOffers = { currentScreen = Screen.OFFERS }
        )
      }
      Screen.OFFERS -> {
        OffersScreen(
          viewModel = offersViewModel,
          onNavigateBack = {
            if (session.isAuthenticated) onExit() else currentScreen = Screen.WELCOME
          },
          onNavigateToSignIn = { currentScreen = Screen.SIGN_IN },
          onSignOut = onSignOut
        )
      }
    }
  }
}
