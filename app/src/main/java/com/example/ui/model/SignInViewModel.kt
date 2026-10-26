package com.example.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel(
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

  private val _uiState = MutableStateFlow(SignInUiState())
  val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

  private var sendJob: Job? = null

  fun onEmailChange(newEmail: String) {
    _uiState.update { current ->
      current.copy(
        email = newEmail,
        // Clear validation error when user begins correcting the text
        emailError = null
      )
    }
  }

  fun onSendMagicLink() {
    val currentEmail = _uiState.value.email.trim()

    // Validate email
    if (!isValidEmail(currentEmail)) {
      _uiState.update { current ->
        current.copy(
          emailError = "Enter a valid email address.",
          googleErrorBanner = null
        )
      }
      return
    }

    // Email is valid: start sending simulation
    sendJob?.cancel()
    sendJob = viewModelScope.launch(ioDispatcher) {
      _uiState.update { current ->
        current.copy(
          isSending = true,
          emailError = null,
          googleErrorBanner = null
        )
      }

      // Simulate 1.5 seconds network request
      delay(1500)

      _uiState.update { current ->
        current.copy(
          isSending = false,
          isSuccess = true,
          successEmail = currentEmail
        )
      }
    }
  }

  fun onGoogleSignInClicked() {
    if (_uiState.value.isSending) return
    _uiState.update { current ->
      current.copy(
        googleErrorBanner = "Google sign-in was cancelled."
      )
    }
  }

  fun onDismissGoogleError() {
    _uiState.update { current ->
      current.copy(googleErrorBanner = null)
    }
  }

  fun onResetSuccess() {
    _uiState.update { current ->
      current.copy(
        isSuccess = false,
        email = "",
        emailError = null,
        googleErrorBanner = null
      )
    }
  }

  fun setInvalidEmailPreset() {
    _uiState.update {
      SignInUiState(
        email = "adaeze@not-an-email",
        emailError = "Enter a valid email address."
      )
    }
  }

  fun setGoogleErrorPreset() {
    _uiState.update {
      SignInUiState(
        email = "",
        googleErrorBanner = "Google sign-in was cancelled."
      )
    }
  }

  fun setLoadingPreset() {
    _uiState.update {
      SignInUiState(
        email = "adaeze.obi@haven.ng",
        isSending = true
      )
    }
  }

  fun setIdlePreset() {
    _uiState.update {
      SignInUiState()
    }
  }

  companion object {
    private val EMAIL_REGEX = Regex(
      "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )

    fun isValidEmail(email: String): Boolean {
      if (email.isBlank()) return false
      return EMAIL_REGEX.matches(email)
    }
  }
}
