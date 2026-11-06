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
import com.example.data.OffersRepository
import com.example.data.messaging.db.HavenDatabase
import com.example.data.offers.HavenLocalFirstOffersRepository
import com.example.data.offers.LocalFirstOffersRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OffersUiState(
  val query: OffersQuery = OffersQuery(),
  val items: List<FeedOffer> = emptyList(),
  val categories: List<FeedCategory> = emptyList(),
  val currencies: List<String> = emptyList(),
  val nextCursor: String? = null,
  val isLoading: Boolean = false,
  val isRefreshing: Boolean = false,
  val error: String? = null,
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

  // Retained scroll coordinates for seamless navigation restoration
  var scrollIndex: Int = 0
    private set
  var scrollOffset: Int = 0
    private set

  init {
    refresh()
  }

  fun updateScrollPosition(index: Int, offset: Int) {
    scrollIndex = index
    scrollOffset = offset
  }

  fun refresh() = load(reset = true)

  fun loadMore() {
    if (!_uiState.value.isLoading && _uiState.value.nextCursor != null) {
      load(reset = false)
    }
  }

  private fun load(reset: Boolean) {
    loadJob?.cancel()
    val before = _uiState.value
    val query = before.query.copy(after = if (reset) null else before.nextCursor)

    // Check local cache immediately on reset to deliver 0ms perceived latency
    if (reset && localOffersRepository != null) {
      viewModelScope.launch {
        val cached = localOffersRepository.getCachedPage(query)
        if (cached != null && cached.items.isNotEmpty()) {
          _uiState.update { current ->
            if (current.query == before.query) {
              current.copy(
                items = cached.items,
                categories = if (cached.categories.isNotEmpty()) cached.categories else current.categories,
                currencies = if (cached.currencies.isNotEmpty()) cached.currencies else current.currencies,
                nextCursor = cached.nextCursor,
                isLoading = false,
                error = null
              )
            } else current
          }
        }
      }
    }

    val hasExistingItems = _uiState.value.items.isNotEmpty()
    _uiState.update {
      it.copy(
        isLoading = if (hasExistingItems && reset && localOffersRepository != null) false else true,
        isRefreshing = if (hasExistingItems && reset) true else false,
        error = null,
        items = if (reset && localOffersRepository == null) emptyList() else it.items,
        nextCursor = if (reset && localOffersRepository == null) null else it.nextCursor
      )
    }

    loadJob = viewModelScope.launch {
      try {
        if (localOffersRepository != null) {
          val result = if (reset) {
            localOffersRepository.fetchAndCache(query)
          } else {
            val currentIds = _uiState.value.items.map { it.id }
            localOffersRepository.fetchMoreAndCache(query, currentIds)
          }

          if (result.isSuccess) {
            val page = result.getOrThrow()
            _uiState.update { current ->
              val mergedItems = if (reset) {
                page.items
              } else {
                (current.items + page.items).distinctBy { offer -> offer.id }
              }
              current.copy(
                isLoading = false,
                isRefreshing = false,
                items = mergedItems,
                categories = if (page.categories.isNotEmpty()) page.categories else current.categories,
                currencies = if (page.currencies.isNotEmpty()) page.currencies else current.currencies,
                nextCursor = page.nextCursor,
                error = null
              )
            }
          } else {
            val currentItems = _uiState.value.items
            _uiState.update { current ->
              current.copy(
                isLoading = false,
                isRefreshing = false,
                // Only show blocking error if there are no items in cache
                error = if (currentItems.isEmpty()) "Couldn’t load offers. Check your connection and try again." else null
              )
            }
          }
        } else {
          // Direct remote fallback for tests where local repository is not supplied
          val page = repository.fetch(query)
          _uiState.update {
            it.copy(
              isLoading = false,
              isRefreshing = false,
              items = if (reset) page.items else (it.items + page.items).distinctBy { offer -> offer.id },
              categories = page.categories,
              currencies = page.currencies,
              nextCursor = page.nextCursor
            )
          }
        }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        val currentItems = _uiState.value.items
        _uiState.update {
          it.copy(
            isLoading = false,
            isRefreshing = false,
            error = if (currentItems.isEmpty()) "Couldn’t load offers. Check your connection and try again." else null
          )
        }
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
  }

  fun applyReference(candidate: com.example.data.PlaceCandidate) {
    _uiState.update {
      it.copy(
        activeReference = candidate,
        query = it.query.copy(near = candidate.near)
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
    if (_uiState.value.query.scope == scope) return
    _uiState.update {
      it.copy(query = it.query.copy(scope = scope))
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
