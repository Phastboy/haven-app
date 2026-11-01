package com.example.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FeedCategory
import com.example.data.FeedOffer
import com.example.data.HavenOffersRepository
import com.example.data.OffersQuery
import com.example.data.OffersRepository
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
  val error: String? = null,
  val isFilterSheetVisible: Boolean = false,
  val draftCurrency: String = "Any",
  val draftMinPriceText: String = "",
  val draftMaxPriceText: String = ""
) {
  val activeFilterCount: Int get() = if (query.currency.isNotBlank()) 1 else 0
}

/** Real request state only. No local filtering, fabricated rows, or timed loading. */
class OffersViewModel(
  private val repository: OffersRepository = HavenOffersRepository()
) : ViewModel() {
  private val _uiState = MutableStateFlow(OffersUiState())
  val uiState = _uiState.asStateFlow()
  private var loadJob: Job? = null

  init { refresh() }

  fun refresh() = load(reset = true)
  fun loadMore() {
    if (!_uiState.value.isLoading && _uiState.value.nextCursor != null) load(reset = false)
  }

  private fun load(reset: Boolean) {
    loadJob?.cancel()
    val before = _uiState.value
    val query = before.query.copy(after = if (reset) null else before.nextCursor)
    _uiState.update { it.copy(isLoading = true, error = null,
      items = if (reset) emptyList() else it.items, nextCursor = if (reset) null else it.nextCursor) }
    loadJob = viewModelScope.launch {
      try {
        val page = repository.fetch(query)
        _uiState.update { it.copy(isLoading = false,
          items = if (reset) page.items else (it.items + page.items).distinctBy { offer -> offer.id },
          categories = page.categories, currencies = page.currencies, nextCursor = page.nextCursor) }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        _uiState.update { it.copy(isLoading = false, error = "Couldn’t load offers. Check your connection and try again.") }
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
    _uiState.update { it.copy(isFilterSheetVisible = true,
      draftCurrency = it.query.currency.ifBlank { "Any" },
      draftMinPriceText = it.query.minPrice?.toString() ?: "",
      draftMaxPriceText = it.query.maxPrice?.toString() ?: "") }
  }
  fun closeFilterSheet() { _uiState.update { it.copy(isFilterSheetVisible = false) } }
  fun onDraftCurrencyChange(value: String) { _uiState.update { it.copy(draftCurrency = value) } }
  fun onDraftMinPriceChange(value: String) { _uiState.update { it.copy(draftMinPriceText = value) } }
  fun onDraftMaxPriceChange(value: String) { _uiState.update { it.copy(draftMaxPriceText = value) } }

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
      _uiState.update { it.copy(isFilterSheetVisible = false,
        error = "Choose a currency and a valid whole-number price range. Use 0 for free offers.") }
      return
    }
    _uiState.update { it.copy(isFilterSheetVisible = false,
      query = it.query.copy(currency = state.draftCurrency.takeUnless { value -> value == "Any" } ?: "",
        minPrice = min, maxPrice = max)) }
    refresh()
  }
  fun clearFilters() {
    _uiState.update { it.copy(query = OffersQuery(), isFilterSheetVisible = false,
      draftCurrency = "Any", draftMinPriceText = "", draftMaxPriceText = "") }
    refresh()
  }
}
