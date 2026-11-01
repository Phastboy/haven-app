package com.example.ui.model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SignInViewModel(
  @Suppress("UNUSED_PARAMETER") ioDispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {
  private val _uiState = MutableStateFlow(SignInUiState())
  val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

  fun onEmailChange(newEmail: String) {
    _uiState.update { it.copy(email = newEmail, emailError = null) }
  }

  /** Browser sign-in handles the actual email delivery and Google authentication. */
  fun onSendMagicLink(): Boolean {
    if (!isValidEmail(_uiState.value.email.trim())) {
      _uiState.update { it.copy(emailError = "Enter a valid email address.") }
      return false
    }
    _uiState.update { it.copy(emailError = null, googleErrorBanner = null) }
    return true
  }

  fun onGoogleSignInClicked() {
    _uiState.update { it.copy(googleErrorBanner = null) }
  }

  fun onAuthError(message: String) {
    _uiState.update { it.copy(googleErrorBanner = message) }
  }

  fun onDismissGoogleError() {
    _uiState.update { it.copy(googleErrorBanner = null) }
  }

  companion object {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    fun isValidEmail(email: String): Boolean = email.isNotBlank() && EMAIL_REGEX.matches(email)
  }
}
