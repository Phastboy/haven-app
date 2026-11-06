package com.example.ui.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FeedOffer
import com.example.data.HavenSavedOffersRepository
import com.example.data.PaginatedOffers
import com.example.data.SavedOffersRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavedOffersUiState(
  val offers: PaginatedOffers? = null,
  val loading: Boolean = false,
  val error: String? = null,
  val savingSlugs: Set<String> = emptySet(),
  val loadingMore: Boolean = false,
  val pagingError: String? = null
) {
  val items: List<FeedOffer> get() = offers?.items.orEmpty()
}

class SavedOffersViewModel(
  private val repository: SavedOffersRepository
) : ViewModel() {

  private val _state = MutableStateFlow(SavedOffersUiState())
  val state: StateFlow<SavedOffersUiState> = _state.asStateFlow()

  init {
    load()
  }

  fun load() {
    _state.update { it.copy(loading = it.offers == null, error = null, pagingError = null) }
    viewModelScope.launch {
      repository.getSavedOffers()
        .onSuccess { page ->
          _state.update { it.copy(offers = page, loading = false, error = null, pagingError = null) }
        }
        .onFailure { error ->
          if (error is CancellationException) throw error
          _state.update {
            it.copy(
              loading = false,
              error = if (it.offers == null) "Couldn’t load saved offers. Please try again." else null
            )
          }
        }
    }
  }

  fun loadMore() {
    val cursor = _state.value.offers?.nextCursor ?: return
    if (_state.value.loadingMore) return
    _state.update { it.copy(loadingMore = true, pagingError = null) }
    viewModelScope.launch {
      repository.getSavedOffers(after = cursor)
        .onSuccess { morePage ->
          _state.update { current ->
            val combined = (current.offers?.items.orEmpty() + morePage.items).distinctBy { it.id }
            current.copy(
              offers = PaginatedOffers(items = combined, nextCursor = morePage.nextCursor),
              loadingMore = false,
              pagingError = null
            )
          }
        }
        .onFailure { error ->
          if (error is CancellationException) throw error
          _state.update { it.copy(loadingMore = false, pagingError = "Couldn’t load more saved offers. Tap to retry.") }
        }
    }
  }

  fun toggleSave(slug: String) {
    if (slug in _state.value.savingSlugs) return
    _state.update { it.copy(savingSlugs = it.savingSlugs + slug) }
    viewModelScope.launch {
      // Optimistic removal from saved list
      val currentItems = _state.value.offers?.items.orEmpty()
      val removed = currentItems.filterNot { it.slug == slug }
      _state.update { it.copy(offers = it.offers?.copy(items = removed)) }

      repository.unsaveOffer(slug)
        .onSuccess {
          _state.update { it.copy(savingSlugs = it.savingSlugs - slug) }
        }
        .onFailure { error ->
          if (error is CancellationException) throw error
          // Rollback on failure
          _state.update {
            it.copy(
              offers = it.offers?.copy(items = currentItems),
              savingSlugs = it.savingSlugs - slug,
              error = "Couldn’t update saved status. Please try again."
            )
          }
        }
    }
  }

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = HavenSavedOffersRepository.create(context)
        return SavedOffersViewModel(repo) as T
      }
    }
  }
}
