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
  val loadingMore: Boolean = false
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
    _state.update { it.copy(loading = it.offers == null, error = null) }
    viewModelScope.launch {
      repository.getMyOffers()
        .onSuccess { page ->
          _state.update { it.copy(offers = page, loading = false, error = null) }
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
    _state.update { it.copy(loadingMore = true) }
    viewModelScope.launch {
      repository.getMyOffers(after = cursor)
        .onSuccess { morePage ->
          _state.update { current ->
            val combined = (current.offers?.items.orEmpty() + morePage.items).distinctBy { it.id }
            current.copy(
              offers = PaginatedOffers(items = combined, nextCursor = morePage.nextCursor),
              loadingMore = false
            )
          }
        }
        .onFailure { error ->
          if (error is CancellationException) throw error
          _state.update { it.copy(loadingMore = false) }
        }
    }
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
