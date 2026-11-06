package com.example.ui.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FeedOffer
import com.example.data.HavenMyOffersRepository
import com.example.data.MyOffersRepository
import com.example.data.PaginatedOffers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyOffersUiState(
  val offers: PaginatedOffers? = null,
  val loading: Boolean = false,
  val error: String? = null,
  val loadingMore: Boolean = false,
  val pagingError: String? = null,
  val pendingDeleteSlugs: Set<String> = emptySet(),
  val actionMessage: String? = null
) {
  val items: List<FeedOffer> get() = offers?.items.orEmpty()
}

class MyOffersViewModel(
  private val repository: MyOffersRepository
) : ViewModel() {

  private val _state = MutableStateFlow(MyOffersUiState())
  val state: StateFlow<MyOffersUiState> = _state.asStateFlow()

  init {
    load()
  }

  fun load() {
    _state.update { it.copy(loading = it.offers == null, error = null, pagingError = null) }
    viewModelScope.launch {
      repository.getMyOffers()
        .onSuccess { page ->
          _state.update { it.copy(offers = page, loading = false, error = null, pagingError = null) }
        }
        .onFailure { error ->
          if (error is CancellationException) throw error
          _state.update {
            it.copy(
              loading = false,
              error = if (it.offers == null) "Couldn’t load your offers. Please try again." else null
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
      repository.getMyOffers(after = cursor)
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
          _state.update { it.copy(loadingMore = false, pagingError = "Couldn’t load more offers. Tap to retry.") }
        }
    }
  }

  fun deleteOffer(slug: String, onSuccess: () -> Unit = {}) {
    if (slug in _state.value.pendingDeleteSlugs) return
    val currentOffers = _state.value.offers ?: return
    val offer = currentOffers.items.firstOrNull { it.slug == slug } ?: return

    _state.update {
      it.copy(
        pendingDeleteSlugs = it.pendingDeleteSlugs + slug,
        actionMessage = null
      )
    }

    viewModelScope.launch {
      repository.deleteOffer(slug)
        .onSuccess { deleted ->
          if (deleted) {
            _state.update { current -> current.copy(
              pendingDeleteSlugs = current.pendingDeleteSlugs - slug,
              offers = current.offers?.copy(items = current.items.filter { it.slug != slug }),
              actionMessage = "Deleted “${offer.title}”.") }
            onSuccess()
          } else {
            _state.update { it.copy(pendingDeleteSlugs = it.pendingDeleteSlugs - slug,
              actionMessage = "Deletion wasn’t confirmed. Refresh your offers before retrying.") }
          }
        }
        .onFailure { error ->
          if (error is CancellationException) throw error
          _state.update { current ->
            current.copy(
              pendingDeleteSlugs = current.pendingDeleteSlugs - slug,
              actionMessage = "Couldn’t delete “${offer.title}”. Please try again."
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
        val repo = HavenMyOffersRepository.create(context)
        return MyOffersViewModel(repo) as T
      }
    }
  }
}
