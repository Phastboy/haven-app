package com.example.ui.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FollowingRepository
import com.example.data.HavenFollowingRepository
import com.example.data.PaginatedFollowing
import com.example.data.Person
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FollowingUiState(
  val following: PaginatedFollowing? = null,
  val loading: Boolean = false,
  val error: String? = null,
  val loadingMore: Boolean = false,
  val pagingError: String? = null,
  val pendingUnfollowHandles: Set<String> = emptySet(),
  val actionMessage: String? = null
) {
  val items: List<Person> get() = following?.items.orEmpty()
}

class FollowingViewModel(
  private val repository: FollowingRepository
) : ViewModel() {

  private val _state = MutableStateFlow(FollowingUiState())
  val state: StateFlow<FollowingUiState> = _state.asStateFlow()

  init {
    load()
  }

  fun load() {
    _state.update { it.copy(loading = it.following == null, error = null, pagingError = null) }
    viewModelScope.launch {
      repository.getFollowing()
        .onSuccess { page ->
          _state.update { it.copy(following = page, loading = false, error = null, pagingError = null) }
        }
        .onFailure { error ->
          if (error is CancellationException) throw error
          _state.update {
            it.copy(
              loading = false,
              error = if (it.following == null) "Couldn’t load followed providers. Please try again." else null
            )
          }
        }
    }
  }

  fun loadMore() {
    val cursor = _state.value.following?.nextCursor ?: return
    if (_state.value.loadingMore) return
    _state.update { it.copy(loadingMore = true, pagingError = null) }
    viewModelScope.launch {
      repository.getFollowing(after = cursor)
        .onSuccess { morePage ->
          _state.update { current ->
            val combined = (current.following?.items.orEmpty() + morePage.items).distinctBy { it.id }
            current.copy(
              following = PaginatedFollowing(items = combined, nextCursor = morePage.nextCursor),
              loadingMore = false,
              pagingError = null
            )
          }
        }
        .onFailure { error ->
          if (error is CancellationException) throw error
          _state.update { it.copy(loadingMore = false, pagingError = "Couldn’t load more providers. Tap to retry.") }
        }
    }
  }

  fun unfollow(handle: String) {
    if (handle in _state.value.pendingUnfollowHandles) return
    val currentFollowing = _state.value.following ?: return
    val person = currentFollowing.items.firstOrNull { it.handle == handle } ?: return

    _state.update {
      it.copy(
        pendingUnfollowHandles = it.pendingUnfollowHandles + handle,
        following = currentFollowing.copy(
          items = currentFollowing.items.filter { it.handle != handle }
        ),
        actionMessage = "Unfollowed ${person.displayName}."
      )
    }

    viewModelScope.launch {
      repository.unfollow(handle)
        .onSuccess {
          _state.update {
            it.copy(pendingUnfollowHandles = it.pendingUnfollowHandles - handle)
          }
        }
        .onFailure { error ->
          if (error is CancellationException) throw error
          // Rollback on failure
          _state.update { current ->
            val restored = (current.following?.items.orEmpty() + person).distinctBy { it.id }
            current.copy(
              following = current.following?.copy(items = restored),
              pendingUnfollowHandles = current.pendingUnfollowHandles - handle,
              actionMessage = "Couldn’t unfollow ${person.displayName}. Please try again."
            )
          }
        }
    }
  }

  fun dismissActionMessage() {
    _state.update { it.copy(actionMessage = null) }
  }

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = HavenFollowingRepository.create(context)
        return FollowingViewModel(repo) as T
      }
    }
  }
}
