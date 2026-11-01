package com.example.ui.model

data class SignInUiState(
  val email: String = "",
  val emailError: String? = null,
  val authError: String? = null
) {
  val isError: Boolean
    get() = emailError != null
}
