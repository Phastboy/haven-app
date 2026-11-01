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

data class ProfileUiState(val profile: ProfileOverview? = null, val loading: Boolean = false, val error: String? = null)

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {
  private val _state = MutableStateFlow(ProfileUiState())
  val state = _state.asStateFlow()
  private var job: Job? = null
  private var actor: String? = null

  fun clear() {
    job?.cancel()
    actor = null
    _state.value = ProfileUiState()
  }
  fun load(personId: String, force: Boolean = false) {
    if (!force && actor == personId && (_state.value.profile != null || _state.value.loading)) return
    job?.cancel()
    actor = personId
    _state.value = ProfileUiState(loading = true)
    job = viewModelScope.launch {
      try {
        val profile = repository.load()
        check(profile.id == personId) { "Profile identity changed" }
        _state.value = ProfileUiState(profile = profile)
      } catch (cancelled: CancellationException) { throw cancelled }
      catch (_: Exception) {
        _state.value = ProfileUiState(error = "Couldn’t load your profile. Please try again.")
      }
    }
  }

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProfileViewModel::class.java))
        return ProfileViewModel(HavenProfileRepository(HavenAuthClient.shared(context))) as T
      }
    }
  }
}
