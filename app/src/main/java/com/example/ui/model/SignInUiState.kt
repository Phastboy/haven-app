package com.example.ui.model

data class SignInUiState(
  val email: String = "",
  val emailError: String? = null,
  val isSending: Boolean = false,
  val googleErrorBanner: String? = null,
  val isSuccess: Boolean = false,
  val successEmail: String = ""
) {
  val isError: Boolean
    get() = emailError != null

  val isInputsDisabled: Boolean
    get() = isSending
}
