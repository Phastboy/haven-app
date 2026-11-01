package com.example.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OffersTab {
  OFFERS,
  MESSAGES,
  PROFILE
}

data class FilterCriteria(
  val currency: String = "Any",
  val minPrice: Double? = null,
  val maxPrice: Double? = null
)

data class OffersUiState(
  val searchQuery: String = "",
  val activeFilters: FilterCriteria = FilterCriteria(),
  // Pending filter draft state in bottom sheet
  val draftCurrency: String = "USD",
  val draftMinPriceText: String = "",
  val draftMaxPriceText: String = "30",
  val isFilterSheetVisible: Boolean = false,
  val isLoading: Boolean = false,
  val allOffers: List<Offer> = SampleOffers.defaultOffers,
  val selectedOffer: Offer? = null,
  val activeTab: OffersTab = OffersTab.OFFERS,
  val hasUnreadMessages: Boolean = true
) {
  val filteredOffers: List<Offer>
    get() {
      return allOffers.filter { offer ->
        val matchesSearch = if (searchQuery.isBlank()) {
          true
        } else {
          offer.title.contains(searchQuery, ignoreCase = true) ||
              offer.creatorName.contains(searchQuery, ignoreCase = true) ||
              offer.category.contains(searchQuery, ignoreCase = true)
        }

        val matchesCurrency = when (activeFilters.currency) {
          "Any" -> true
          else -> offer.currency.equals(activeFilters.currency, ignoreCase = true) ||
              (offer.priceNumeric == 0.0) // Free items match any currency
        }

        val matchesMin = activeFilters.minPrice?.let { offer.priceNumeric >= it } ?: true
        val matchesMax = activeFilters.maxPrice?.let { offer.priceNumeric <= it } ?: true

        matchesSearch && matchesCurrency && matchesMin && matchesMax
      }
    }

  val activeFilterCount: Int
    get() {
      var count = 0
      if (activeFilters.currency != "Any") count++
      if (activeFilters.minPrice != null) count++
      if (activeFilters.maxPrice != null) count++
      return count
    }
}

class OffersViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(OffersUiState())
  val uiState: StateFlow<OffersUiState> = _uiState.asStateFlow()

  private var loadJob: Job? = null

  init {
    // Initial entry skeleton load
    simulateLoading(durationMs = 600)
  }

  fun onSearchQueryChange(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
  }

  fun onToggleFavorite(offerId: String) {
    _uiState.update { current ->
      val updatedList = current.allOffers.map { offer ->
        if (offer.id == offerId) {
          offer.copy(isFavorite = !offer.isFavorite)
        } else {
          offer
        }
      }
      val updatedSelection = current.selectedOffer?.let { selected ->
        if (selected.id == offerId) {
          selected.copy(isFavorite = !selected.isFavorite)
        } else {
          selected
        }
      }
      current.copy(allOffers = updatedList, selectedOffer = updatedSelection)
    }
  }

  fun openFilterSheet() {
    _uiState.update { current ->
      current.copy(
        isFilterSheetVisible = true,
        draftCurrency = if (current.activeFilters.currency == "Any") "USD" else current.activeFilters.currency,
        draftMinPriceText = current.activeFilters.minPrice?.toInt()?.toString() ?: "",
        draftMaxPriceText = current.activeFilters.maxPrice?.toInt()?.toString() ?: "30"
      )
    }
  }

  fun closeFilterSheet() {
    _uiState.update { it.copy(isFilterSheetVisible = false) }
  }

  fun onDraftCurrencyChange(currency: String) {
    _uiState.update { it.copy(draftCurrency = currency) }
  }

  fun onDraftMinPriceChange(text: String) {
    _uiState.update { it.copy(draftMinPriceText = text) }
  }

  fun onDraftMaxPriceChange(text: String) {
    _uiState.update { it.copy(draftMaxPriceText = text) }
  }

  fun applyFilters() {
    val current = _uiState.value
    val minVal = current.draftMinPriceText.toDoubleOrNull()
    val maxVal = current.draftMaxPriceText.toDoubleOrNull()
    val newCriteria = FilterCriteria(
      currency = current.draftCurrency,
      minPrice = minVal,
      maxPrice = maxVal
    )

    _uiState.update {
      it.copy(
        activeFilters = newCriteria,
        isFilterSheetVisible = false
      )
    }

    // Brief skeleton flash to visually confirm new filtered dataset
    simulateLoading(durationMs = 450)
  }

  fun clearFilters() {
    _uiState.update {
      it.copy(
        activeFilters = FilterCriteria(currency = "Any", minPrice = null, maxPrice = null),
        draftCurrency = "Any",
        draftMinPriceText = "",
        draftMaxPriceText = "",
        isFilterSheetVisible = false
      )
    }
    simulateLoading(durationMs = 350)
  }

  fun refresh() {
    simulateLoading(durationMs = 750)
  }

  fun simulateLoading(durationMs: Long = 700) {
    loadJob?.cancel()
    loadJob = viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      delay(durationMs)
      _uiState.update { it.copy(isLoading = false) }
    }
  }

  fun selectOffer(offer: Offer?) {
    _uiState.update { it.copy(selectedOffer = offer) }
  }

  fun selectTab(tab: OffersTab) {
    _uiState.update { it.copy(activeTab = tab) }
  }
}
