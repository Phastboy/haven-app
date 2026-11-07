package com.example.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A transient network failure must not route a saved session to welcome. */
enum class StartupState { RESTORING, AUTHENTICATED, SIGNED_OUT, RETRY }

class StartupViewModel(
  private val restore: suspend () -> Boolean,
  private val hasSavedSession: suspend () -> Boolean
) : ViewModel() {
  private val _state = MutableStateFlow(StartupState.RESTORING)
  val state = _state.asStateFlow()
  private var restoring = false

  init { retry() }

  fun retry() {
    if (restoring) return
    restoring = true
    _state.value = StartupState.RESTORING
    viewModelScope.launch {
      try {
        val authenticated = try { restore() } catch (cancelled: CancellationException) {
          throw cancelled
        } catch (_: Exception) { false }
        _state.value = when {
          authenticated -> StartupState.AUTHENTICATED
          hasSavedSession() -> StartupState.RETRY
          else -> StartupState.SIGNED_OUT
        }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        // Storage being unavailable is not evidence that the person signed out.
        _state.value = StartupState.RETRY
      } finally {
        restoring = false
      }
    }
  }
}
