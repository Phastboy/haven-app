package com.example.ui.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Public person fields confirmed by GET /api/v1/me. */
data class UserSession(
  val isAuthenticated: Boolean = false,
  val personId: String? = null,
  val handle: String? = null,
  val displayName: String = "Guest Member",
  val email: String? = null
) {
  val initial: String get() = displayName.firstOrNull()?.uppercase() ?: "H"
}

object SessionManager {
  private val _session = MutableStateFlow(UserSession())
  val session: StateFlow<UserSession> = _session.asStateFlow()

  fun signInPerson(id: String, displayName: String, handle: String) {
    _session.value = UserSession(true, id, handle, displayName)
  }

  fun updateProfileIfCurrent(id: String, displayName: String, handle: String) {
    _session.update { current ->
      if (current.personId == id) current.copy(displayName = displayName, handle = handle) else current
    }
  }

  fun signOut() {
    _session.value = UserSession()
  }
}
