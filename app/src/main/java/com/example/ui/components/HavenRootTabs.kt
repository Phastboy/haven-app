package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class RootTab {
  OFFERS,
  MESSAGES,
  PROFILE
}

class RootTabsState(
  val pager: PagerState,
  private val coroutineScope: CoroutineScope
) {
  var requestedTab by mutableStateOf<RootTab?>(null)
    private set

  val currentTab: RootTab
    get() = requestedTab ?: RootTab.entries.getOrElse(pager.currentPage) { RootTab.OFFERS }

  private var scrollJob: Job? = null

  fun select(tab: RootTab) {
    requestedTab = tab
    scrollJob?.cancel()
    scrollJob = coroutineScope.launch {
      try {
        if (pager.currentPage != tab.ordinal) {
          pager.animateScrollToPage(tab.ordinal)
        }
      } finally {
        if (requestedTab == tab) {
          requestedTab = null
        }
      }
    }
  }
}

@Composable
fun rememberRootTabsState(personId: String? = null): RootTabsState {
  val coroutineScope = rememberCoroutineScope()
  val pager = rememberPagerState(initialPage = 0, pageCount = { RootTab.entries.size })
  val state = remember(pager, coroutineScope) { RootTabsState(pager, coroutineScope) }
  var lastPersonId by rememberSaveable { mutableStateOf(personId) }

  LaunchedEffect(personId) {
    if (lastPersonId != null && personId != null && lastPersonId != personId) {
      pager.scrollToPage(RootTab.OFFERS.ordinal)
    }
    lastPersonId = personId
  }

  return state
}

@Composable
fun ObserveRootTabSelection(
  state: RootTabsState,
  authenticated: Boolean,
  rootVisible: Boolean,
  onSignIn: () -> Unit,
  onSelected: (RootTab) -> Unit,
  activationKey: Any? = null
) {
  val currentTab = state.currentTab
  val isScrolling = state.pager.isScrollInProgress

  LaunchedEffect(currentTab, isScrolling, authenticated, rootVisible, activationKey) {
    if (!rootVisible || isScrolling) return@LaunchedEffect

    if (!authenticated && currentTab != RootTab.OFFERS) {
      onSignIn()
      state.pager.scrollToPage(RootTab.OFFERS.ordinal)
      return@LaunchedEffect
    }

    onSelected(currentTab)
  }
}

@Composable
fun HavenRootPager(
  state: RootTabsState,
  authenticated: Boolean,
  modifier: Modifier = Modifier,
  swipeEnabled: Boolean = true,
  content: @Composable (RootTab) -> Unit
) {
  HorizontalPager(
    state = state.pager,
    modifier = modifier.testTag("root_tab_pager"),
    userScrollEnabled = swipeEnabled,
    beyondViewportPageCount = 0
  ) { page ->
    val tab = RootTab.entries.getOrElse(page) { RootTab.OFFERS }
    if (authenticated || tab == RootTab.OFFERS) {
      content(tab)
    } else {
      Box(Modifier.fillMaxSize())
    }
  }
}
