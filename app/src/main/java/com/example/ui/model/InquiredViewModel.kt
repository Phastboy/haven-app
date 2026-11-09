package com.example.ui.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FeedOffer
import com.example.data.HavenInquiredRepository
import com.example.data.InquiredRepository
import com.example.data.PaginatedOffers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Loaded collection, initial/refresh progress and independently retryable paging. */
data class InquiredUiState(
  val offers: PaginatedOffers? = null,
  val loading: Boolean = false,
  val error: String? = null,
  val loadingMore: Boolean = false,
  val pagingError: String? = null
) {
  /** Available offers in server activity order, including accumulated pages. */
  val items: List<FeedOffer> get() = offers?.items.orEmpty()
}

/** Account-bound Inquired state; visit cancellation never discards loaded pages. */
class InquiredViewModel(
  private val repository: InquiredRepository
) : ViewModel() {

  private val _state = MutableStateFlow(InquiredUiState())
  val state: StateFlow<InquiredUiState> = _state.asStateFlow()

  private val actor = SessionManager.session.value
  private var request = 0L
  private var loadJob: kotlinx.coroutines.Job? = null

  /** Grid coordinates retained while a child destination is open. */
  var scrollIndex: Int = 0
    private set
  var scrollOffset: Int = 0
    private set

  /** Save the visible grid position without fetching or changing collection data. */
  fun updateScrollPosition(index: Int, offset: Int) {
    scrollIndex = index
    scrollOffset = offset
  }

  /** Load only a never-loaded collection; a confirmed empty page is retained too. */
  fun loadIfNeeded() {
    if (_state.value.offers == null && !_state.value.loading) load()
  }

  /** End this visit's requests while keeping usable data and paging position. */
  fun cancelLoading() {
    request++
    loadJob?.cancel()
    _state.update { it.copy(loading = false, loadingMore = false) }
  }

  /** Explicitly refresh the first page, retaining content until a confirmed response. */
  fun load() {
    cancelLoading()
    val generation = request
    _state.update { it.copy(loading = true, error = null, pagingError = null) }
    loadJob = viewModelScope.launch {
      try {
        val page = repository.getInquired().getOrThrow()
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        if (generation == request && SessionManager.session.value.sameAccount(actor)) {
          _state.update { it.copy(offers = page, loading = false, error = null, pagingError = null) }
        }
      } catch (cancelled: CancellationException) { throw cancelled }
      catch (_: Exception) {
        if (generation == request && SessionManager.session.value.sameAccount(actor)) {
          _state.update { it.copy(loading = false, error = "Couldn’t load inquired offers. Please try again.") }
        }
      }
    }
  }

  /** Append one page, rejecting duplicate, cancelled and superseded results. */
  fun loadMore() {
    val before = _state.value
    val cursor = before.offers?.nextCursor ?: return
    if (before.loading || before.loadingMore) return
    val generation = ++request
    _state.update { it.copy(loadingMore = true, pagingError = null) }
    loadJob = viewModelScope.launch {
      try {
        val page = repository.getInquired(after = cursor).getOrThrow()
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        if (generation == request && SessionManager.session.value.sameAccount(actor)) {
          _state.update { it.copy(
            offers = PaginatedOffers((before.items + page.items).distinctBy { offer -> offer.id }, page.nextCursor),
            loadingMore = false, pagingError = null
          ) }
        }
      } catch (cancelled: CancellationException) { throw cancelled }
      catch (_: Exception) {
        if (generation == request && SessionManager.session.value.sameAccount(actor)) {
          _state.update { it.copy(loadingMore = false, pagingError = "Couldn’t load more inquired offers. Tap to retry.") }
        }
      }
    }
  }

  companion object {
    /** Create a model only while the requested account is still current. */
    fun factory(context: Context, personId: String?): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      /** Reject a different model/account instead of constructing cross-account state. */
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(InquiredViewModel::class.java))
        require(SessionManager.session.value.personId == personId)
        val repo = HavenInquiredRepository.create(context)
        return InquiredViewModel(repo) as T
      }
    }
  }
}
