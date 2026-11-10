package com.example.ui.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auth.HavenAuthClient
import com.example.data.FeedCategory
import com.example.data.FeedOffer
import com.example.data.HavenOffersRepository
import com.example.data.OffersPage
import com.example.data.OffersQuery
import com.example.data.OffersRequestException
import com.example.data.OffersRepository
import com.example.data.messaging.db.HavenDatabase
import com.example.data.offers.HavenLocalFirstOffersRepository
import com.example.data.offers.LocalFirstOffersRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.launch

data class OffersUiState(
  val query: OffersQuery = OffersQuery(),
  val items: List<FeedOffer> = emptyList(),
  val categories: List<FeedCategory> = emptyList(),
  val currencies: List<String> = emptyList(),
  val nextCursor: String? = null,
  val isLoading: Boolean = false,
  val isLoadingMore: Boolean = false,
  val isRefreshing: Boolean = false,
  val error: String? = null,
  val requiresSignIn: Boolean = false,
  val isFilterSheetVisible: Boolean = false,
  val draftCurrency: String = "Any",
  val draftMinPriceText: String = "",
  val draftMaxPriceText: String = "",
  val activeReference: com.example.data.PlaceCandidate? = null
) {
  val activeFilterCount: Int get() = (if (query.currency.isNotBlank()) 1 else 0) + (if (activeReference != null) 1 else 0)
}

/**
 * Local-First ViewModel for Haven Offers Feed.
 *
 * Implements instant cache rendering, background revalidation,
 * stable scroll state retention, and authoritative ranking preservation.
 */
class OffersViewModel(
  private val repository: OffersRepository = HavenOffersRepository(),
  private val localOffersRepository: LocalFirstOffersRepository? = null
) : ViewModel() {

  private val _uiState = MutableStateFlow(OffersUiState())
  val uiState = _uiState.asStateFlow()
  private var loadJob: Job? = null
  private var generation = 0L
  private var displayedQuery: OffersQuery? = null

  // Retained scroll coordinates for seamless navigation restoration
  var scrollIndex: Int = 0
    private set
  var scrollOffset: Int = 0
    private set

  init {
    viewModelScope.launch {
      SessionManager.session.map { it.personId }.distinctUntilChanged().drop(1).collect {
        loadJob?.cancel()
        generation++
        displayedQuery = null
        scrollIndex = 0
        scrollOffset = 0
        _uiState.value = OffersUiState()
        refresh()
      }
    }
    refresh()
  }

  fun updateScrollPosition(index: Int, offset: Int) {
    scrollIndex = index
    scrollOffset = offset
  }

  fun refresh() = load(reset = true)

  fun loadMore() {
    if (!_uiState.value.isLoadingMore && !_uiState.value.isLoading && !_uiState.value.isRefreshing && _uiState.value.nextCursor != null) {
      load(reset = false)
    }
  }

  private fun load(reset: Boolean) {
    loadJob?.cancel()
    val request = ++generation
    val actor = SessionManager.session.value
    val before = _uiState.value
    val query = before.query.copy(after = if (reset) null else before.nextCursor)
    val sameQuery = displayedQuery == before.query.copy(after = null)
    val retained = if (!reset || sameQuery) before.items else emptyList()
    _uiState.update { it.copy(
      items = retained,
      nextCursor = if (reset) null else before.nextCursor,
      isLoading = retained.isEmpty(),
      isLoadingMore = !reset,
      isRefreshing = reset && retained.isNotEmpty(),
      error = null, requiresSignIn = false
    ) }
    loadJob = viewModelScope.launch {
      fun isCurrent() = generation == request && SessionManager.session.value.sameAccount(actor)
      try {
        if (reset && localOffersRepository != null) {
          val cached = localOffersRepository.getCachedPage(query)
          coroutineContext.ensureActive()
          if (!isCurrent()) return@launch
          if (cached != null) {
            displayedQuery = before.query.copy(after = null)
            _uiState.update { it.copy(items = cached.items, categories = cached.categories,
              currencies = cached.currencies, nextCursor = cached.nextCursor,
              isLoading = cached.items.isEmpty(), isRefreshing = cached.items.isNotEmpty()) }
          }
        }
        val page = if (localOffersRepository != null) {
          if (reset) localOffersRepository.fetchAndCache(query).getOrThrow()
          else localOffersRepository.fetchMoreAndCache(query, before.items.map { it.id }).getOrThrow()
        } else repository.fetch(query)
        coroutineContext.ensureActive()
        if (!isCurrent()) return@launch
        check(reset || page.nextCursor == null || page.nextCursor != query.after) {
          "Offer pagination cursor did not advance"
        }
        displayedQuery = before.query.copy(after = null)
        _uiState.update { it.copy(
          items = if (reset) page.items else (before.items + page.items).distinctBy { offer -> offer.id },
          categories = page.categories, currencies = page.currencies, nextCursor = page.nextCursor,
          isLoading = false, isLoadingMore = false, isRefreshing = false, error = null, requiresSignIn = false
        ) }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (error: Exception) {
        if (isCurrent()) _uiState.update { it.copy(isLoading = false, isLoadingMore = false, isRefreshing = false,
          requiresSignIn = (error as? OffersRequestException)?.requiresSignIn == true,
          error = if (error is OffersRequestException) error.message
            else if (reset) "Couldn’t load offers. Check your connection and try again."
            else "Couldn’t load more offers. Please try again.") }
      }
    }
  }

  fun onSearchQueryChange(value: String) {
    _uiState.update { it.copy(query = it.query.copy(search = value)) }
    refresh()
  }

  fun selectCategory(slug: String) {
    _uiState.update { it.copy(query = it.query.copy(category = slug)) }
    refresh()
  }

  fun openFilterSheet() {
    _uiState.update {
      it.copy(
        isFilterSheetVisible = true,
        draftCurrency = it.query.currency.ifBlank { "Any" },
        draftMinPriceText = it.query.minPrice?.toString() ?: "",
        draftMaxPriceText = it.query.maxPrice?.toString() ?: ""
      )
    }
  }

  fun closeFilterSheet() {
    _uiState.update { it.copy(isFilterSheetVisible = false) }
  }

  fun onDraftCurrencyChange(value: String) {
    _uiState.update { it.copy(draftCurrency = value) }
  }

  fun onDraftMinPriceChange(value: String) {
    _uiState.update { it.copy(draftMinPriceText = value) }
  }

  fun onDraftMaxPriceChange(value: String) {
    _uiState.update { it.copy(draftMaxPriceText = value) }
  }

  fun applyFilters() {
    val state = _uiState.value
    val min = state.draftMinPriceText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
    val max = state.draftMaxPriceText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
    val invalid = (state.draftMinPriceText.isNotBlank() && min == null) ||
      (state.draftMaxPriceText.isNotBlank() && max == null) ||
      (min != null && min < 0) || (max != null && max < 0) ||
      (min != null && max != null && min > max) ||
      ((min != null || max != null) && state.draftCurrency == "Any") ||
      (state.draftCurrency != "Any" && min == null && max == null)
    if (invalid) {
      _uiState.update {
        it.copy(
          isFilterSheetVisible = false,
          error = "Choose a currency and a valid whole-number price range. Use 0 for free offers."
        )
      }
      return
    }
    _uiState.update {
      it.copy(
        isFilterSheetVisible = false,
        query = it.query.copy(
          currency = state.draftCurrency.takeUnless { value -> value == "Any" } ?: "",
          minPrice = min,
          maxPrice = max
        )
      )
    }
    refresh()
  }

  fun applyReference(candidate: com.example.data.PlaceCandidate) {
    _uiState.update {
      it.copy(
        activeReference = candidate,
        query = it.query.copy(scope = "public", near = candidate.near)
      )
    }
    refresh()
  }

  fun turnOffProximity() {
    _uiState.update {
      it.copy(
        activeReference = null,
        query = it.query.copy(near = "off")
      )
    }
    refresh()
  }

  fun setScope(scope: String) {
    if (scope != "public" && scope != "following") return
    if (_uiState.value.query.scope == scope) return
    _uiState.update {
      it.copy(
        query = it.query.copy(scope = scope, near = if (scope == "following") "off" else it.query.near),
        activeReference = if (scope == "following") null else it.activeReference
      )
    }
    refresh()
  }

  fun clearFilters() {
    val currentReference = _uiState.value.activeReference
    _uiState.update {
      it.copy(
        query = OffersQuery(near = currentReference?.near),
        isFilterSheetVisible = false,
        draftCurrency = "Any",
        draftMinPriceText = "",
        draftMaxPriceText = ""
      )
    }
    refresh()
  }

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(OffersViewModel::class.java))
        val db = HavenDatabase.getInstance(context)
        val authClient = HavenAuthClient.shared(context)
        val remoteRepo = HavenOffersRepository(authClient)
        val localRepo = HavenLocalFirstOffersRepository(db, remoteRepo)
        return OffersViewModel(remoteRepo, localRepo) as T
      }
    }
  }
}
