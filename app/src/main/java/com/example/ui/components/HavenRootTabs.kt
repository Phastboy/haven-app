package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.HavenColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

enum class RootTab { OFFERS, MESSAGES, PROFILE }

/** Pager and tab presses share one state; the last press cancels previous navigation. */
@Stable
class RootTabsState internal constructor(val pager: PagerState, private val scope: CoroutineScope) {
  private var navigation: Job? = null
  val currentTab: RootTab get() = RootTab.entries[pager.currentPage]
  val settledTab: RootTab get() = RootTab.entries[pager.settledPage]

  fun select(tab: RootTab) {
    navigation?.cancel()
    navigation = scope.launch { pager.animateScrollToPage(tab.ordinal) }
  }

  suspend fun reset() {
    navigation?.cancel()
    pager.scrollToPage(RootTab.OFFERS.ordinal)
  }
}

/** A different person starts at Offers, with no inherited private tab selection. */
@Composable
fun rememberRootTabsState(personId: String?): RootTabsState = key(personId) {
  val pager = rememberPagerState(initialPage = 0, pageCount = { RootTab.entries.size })
  val scope = rememberCoroutineScope()
  remember(pager, scope) { RootTabsState(pager, scope) }
}

/** Side effects follow completed navigation, never an intermediate/cancelled drag. */
@Composable
fun ObserveRootTabSelection(
  state: RootTabsState,
  authenticated: Boolean,
  rootVisible: Boolean,
  onSignIn: () -> Unit,
  onSelected: (RootTab) -> Unit,
  activationKey: Int = 0
) {
  val signIn = rememberUpdatedState(onSignIn)
  val selected = rememberUpdatedState(onSelected)
  LaunchedEffect(state, authenticated, rootVisible, activationKey) {
    if (!rootVisible) return@LaunchedEffect
    snapshotFlow { if (state.pager.isScrollInProgress) null else state.settledTab }
      .filterNotNull().distinctUntilChanged().collect { tab ->
        if (!authenticated && tab != RootTab.OFFERS) {
          state.reset()
          signIn.value()
        } else selected.value(tab)
      }
  }
}

@Composable
fun HavenRootPager(
  state: RootTabsState,
  authenticated: Boolean,
  swipeEnabled: Boolean,
  modifier: Modifier = Modifier,
  content: @Composable (RootTab) -> Unit
) {
  HorizontalPager(
    state = state.pager,
    key = { RootTab.entries[it].name },
    modifier = modifier.testTag("root_tab_pager"),
    userScrollEnabled = swipeEnabled
  ) { page ->
    val tab = RootTab.entries[page]
    if (tab == RootTab.OFFERS || authenticated) content(tab)
    else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("Sign in to open ${tab.name.lowercase()}", color = HavenColors.TextSecondary)
    }
  }
}
