package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.model.OffersViewModel
import com.example.ui.model.SignInViewModel
import com.example.ui.screens.OffersScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.HavenTheme

enum class Screen {
  WELCOME,
  SIGN_IN,
  OFFERS
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT
    enableEdgeToEdge()
    com.example.ui.theme.ThemeManager.init(this)
    setContent {
      HavenTheme {
        HavenApp()
      }
    }
  }
}

@Composable
fun HavenApp(
  signInViewModel: SignInViewModel = viewModel(),
  offersViewModel: OffersViewModel = viewModel()
) {
  var currentScreen by remember { mutableStateOf(Screen.WELCOME) }

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
          onNavigateToOffers = { currentScreen = Screen.OFFERS }
        )
      }
      Screen.OFFERS -> {
        OffersScreen(
          viewModel = offersViewModel,
          onNavigateBack = { currentScreen = Screen.WELCOME },
          onNavigateToSignIn = { currentScreen = Screen.SIGN_IN }
        )
      }
    }
  }
}
