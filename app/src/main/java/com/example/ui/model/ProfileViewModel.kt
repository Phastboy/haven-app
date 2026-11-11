package com.example.ui.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auth.HavenAuthClient
import com.example.data.HavenProfileRepository
import com.example.data.ProfileOverview
import com.example.data.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * [loading] is true only when there is no retained [profile] to show yet (a true
 * cache miss). [refreshing] is true while a background refresh runs on top of
 * retained content. [error] is nonblocking whenever [profile] is present.
 */
data class ProfileUiState(
  val profile: ProfileOverview? = null,
  val loading: Boolean = false,
  val refreshing: Boolean = false,
  val error: String? = null
)

class ProfileViewModel(
  private val repository: ProfileRepository,
  private val now: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {
  private val _state = MutableStateFlow(ProfileUiState())
  val state = _state.asStateFlow()
  private var job: Job? = null
  private var actor: String? = null
  private var lastLoadedAt: Long? = null

  fun clear() {
    job?.cancel()
    actor = null
    lastLoadedAt = null
    _state.value = ProfileUiState()
  }

  /**
   * Marks any retained profile stale so the next [load] refreshes it, without
   * starting a request now. Used after profile edits and confirmed collection
   * changes (for example saving or unsaving an offer) so counts stay truthful
   * without a redundant background fetch while the profile is off-screen.
   */
  fun invalidate() {
    if (actor != null) lastLoadedAt = null
  }

  /**
   * Loads the signed-in person's profile.
   *
   * Retained content is never blanked: a refresh keeps the current profile
   * visible (flagged through [ProfileUiState.refreshing]) and a failure keeps it
   * too, with a nonblocking error. A different [personId] always starts from a
   * clean state, so another account's cached profile is never reused.
   *
   * [force] bypasses the freshness window, but no second request is started
   * while one is already in flight, so rapid re-entry cannot stack requests.
   */
  fun load(personId: String, force: Boolean = false) {
    if (actor != personId) {
      job?.cancel()
      actor = personId
      lastLoadedAt = null
      _state.value = ProfileUiState(loading = true)
    } else {
      if (job?.isActive == true) return
      val current = _state.value
      val fresh = current.profile != null &&
        lastLoadedAt?.let { now() - it < FRESHNESS_WINDOW_MS } == true
      if (!force && fresh) return
      _state.value = if (current.profile != null) {
        current.copy(loading = false, refreshing = true, error = null)
      } else {
        ProfileUiState(loading = true)
      }
    }
    val requestActor = actor
    job = viewModelScope.launch {
      try {
        val profile = repository.load()
        check(profile.id == personId) { "Profile identity changed" }
        if (actor != requestActor) return@launch
        lastLoadedAt = now()
        _state.value = ProfileUiState(profile = profile)
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        if (actor != requestActor) return@launch
        val retained = _state.value.profile
        _state.value = if (retained != null) {
          ProfileUiState(profile = retained, error = LOAD_ERROR)
        } else {
          ProfileUiState(error = LOAD_ERROR)
        }
      }
    }
  }

  companion object {
    /** A retained same-account profile is reused on re-entry within this window. */
    const val FRESHNESS_WINDOW_MS = 30_000L
    const val LOAD_ERROR = "Couldn’t load your profile. Please try again."

    fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProfileViewModel::class.java))
        return ProfileViewModel(HavenProfileRepository(HavenAuthClient.shared(context))) as T
      }
    }
  }
}
