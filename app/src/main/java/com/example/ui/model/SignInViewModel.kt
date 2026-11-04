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

  fun onGoogleSignInClicked() {
    _uiState.update { it.copy(authError = null) }
  }

  fun onAuthError(message: String) {
    _uiState.update { it.copy(authError = message) }
  }
}
