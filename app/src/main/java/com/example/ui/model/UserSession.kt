package com.example.ui.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UserSession(
  val isAuthenticated: Boolean = false,
  val email: String? = null
) {
  val displayName: String
    get() {
      if (email.isNullOrBlank()) return "Guest Member"
      val namePart = email.substringBefore("@")
      return namePart.replace(".", " ")
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

  val initial: String
    get() = displayName.firstOrNull()?.uppercase() ?: "H"
}

object SessionManager {
  private val _session = MutableStateFlow(UserSession())
  val session: StateFlow<UserSession> = _session.asStateFlow()

  fun signIn(email: String) {
    _session.update {
      UserSession(
        isAuthenticated = true,
        email = email.trim()
      )
    }
  }

  fun signOut() {
    _session.update {
      UserSession(
        isAuthenticated = false,
        email = null
      )
    }
  }
}
