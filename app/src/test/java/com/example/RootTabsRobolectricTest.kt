package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.theme.HavenColors
import com.example.ui.theme.HavenTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35, 37], qualifiers = "w390dp-h844dp")
class RootTabsRobolectricTest {
  @get:Rule val compose = createComposeRule()
  private lateinit var tabs: RootTabsState
  private lateinit var dragScope: kotlinx.coroutines.CoroutineScope
  private val actor = mutableStateOf<String?>("owner")
  private val rootVisible = mutableStateOf(true)
  private val blocked = mutableStateOf(false)
  private val selected = mutableListOf<RootTab>()
  private val activation = mutableIntStateOf(0)
  private var signIns = 0
  private lateinit var inboxScroll: androidx.compose.foundation.lazy.LazyListState

  @Composable private fun Harness() {
    HavenTheme {
      tabs = rememberRootTabsState(actor.value)
      dragScope = rememberCoroutineScope()
      inboxScroll = key(actor.value) { rememberLazyListState() }
      ObserveRootTabSelection(tabs, actor.value != null, rootVisible.value, { signIns++ }, { selected += it }, activation.intValue)
      Column(Modifier.fillMaxSize()) {
        if (rootVisible.value) HavenRootPager(
          state = tabs,
          authenticated = actor.value != null,
          modifier = Modifier.weight(1f).fillMaxWidth(),
          swipeEnabled = !blocked.value
        ) { tab ->
          when (tab) {
            RootTab.OFFERS -> Column(Modifier.fillMaxSize().testTag("offers_page")) {
              Row(Modifier.fillMaxWidth().height(48.dp).nestedScroll(CategoryScrollBoundary)
                .horizontalScroll(rememberScrollState()).testTag("categories")) {
                repeat(20) { Text("Category $it", Modifier.width(120.dp)) }
              }
              LazyColumn(Modifier.fillMaxSize().testTag("offers_list")) {
                items(40) { Text("Offer $it", Modifier.fillMaxWidth().height(80.dp)) }
              }
            }
            RootTab.MESSAGES -> Box(Modifier.fillMaxSize().testTag("inbox_page")) {
              com.example.ui.screens.MessagesInboxScreen(
                com.example.ui.model.MessagesInboxUiState(items = List(40) { index ->
                  com.example.data.InboxConversation("conversation-$index", "peer-$index", "Person $index",
                    "person-$index", null, "A conversation", "2026-11-07T10:00:00Z", false)
                }), {}, {}, {}, listState = inboxScroll)
            }
            RootTab.PROFILE -> Box(Modifier.fillMaxSize().testTag("profile_page")) { Text("Profile") }
          }
        } else Box(Modifier.weight(1f).fillMaxWidth().testTag("child_route")) { Text("Child route") }
        Row(Modifier.fillMaxWidth().height(58.dp)) {
          RootTab.entries.forEach { tab ->
            Text(tab.name, Modifier.weight(1f).fillMaxHeight()
              .selectable(tabs.currentTab == tab, role = Role.Tab) {
                if (actor.value == null && tab != RootTab.OFFERS) signIns++ else tabs.select(tab)
              }.testTag("tab_${tab.name}"))
          }
        }
      }
    }
  }

  private fun show() { compose.setContent { Harness() }; compose.waitForIdle() }
  private fun select(tab: RootTab) { compose.onNodeWithTag("tab_${tab.name}").performClick(); compose.waitForIdle() }

  @Test fun `swipe and tap use the same selected state without wraparound`() {
    show()
    compose.onNodeWithTag("root_tab_pager").performTouchInput { swipe(Offset(width * 0.75f, height * 0.5f), Offset(width * 0.25f, height * 0.5f)) }
    compose.waitForIdle()
    compose.onNodeWithTag("tab_MESSAGES").assertIsSelected()
    assertEquals(RootTab.MESSAGES, selected.last())
    select(RootTab.PROFILE)
    compose.onNodeWithTag("root_tab_pager").performTouchInput { swipe(Offset(width * 0.75f, height * 0.5f), Offset(width * 0.25f, height * 0.5f)) }
    compose.waitForIdle()
    compose.onNodeWithTag("tab_PROFILE").assertIsSelected()
    compose.onNodeWithTag("root_tab_pager").performTouchInput { swipe(Offset(width * 0.25f, height * 0.5f), Offset(width * 0.75f, height * 0.5f)) }
    compose.waitForIdle()
    compose.onNodeWithTag("tab_MESSAGES").assertIsSelected()
  }

  @Test fun `cancelled intermediate drag has no protected-page activation`() {
    show()
    val release = CompletableDeferred<Unit>()
    compose.runOnIdle {
      dragScope.launch {
        tabs.pager.scroll {
          scrollBy(tabs.pager.layoutInfo.pageSize * 0.7f)
          release.await()
          scrollBy(-tabs.pager.layoutInfo.pageSize * 0.7f)
        }
      }
    }
    compose.waitForIdle()
    assertEquals(listOf(RootTab.OFFERS), selected)
    compose.runOnIdle { release.complete(Unit) }
    compose.waitForIdle()
    assertEquals(listOf(RootTab.OFFERS), selected.distinct())
  }

  @Test fun `guest cancelled drag does not sign in and committed swipe gates once`() {
    actor.value = null
    show()
    val release = CompletableDeferred<Unit>()
    compose.runOnIdle {
      dragScope.launch {
        tabs.pager.scroll {
          scrollBy(tabs.pager.layoutInfo.pageSize * 0.7f)
          release.await()
          scrollBy(-tabs.pager.layoutInfo.pageSize * 0.7f)
        }
      }
    }
    compose.waitForIdle()
    assertEquals(0, signIns)
    compose.onNodeWithTag("inbox_page").assertDoesNotExist()
    compose.runOnIdle { release.complete(Unit) }
    compose.waitForIdle()
    assertEquals(0, signIns)
    compose.onNodeWithTag("root_tab_pager").performTouchInput { swipe(Offset(width * 0.75f, height * 0.5f), Offset(width * 0.25f, height * 0.5f)) }
    compose.waitForIdle()
    assertEquals(1, signIns)
    compose.onNodeWithTag("tab_OFFERS").assertIsSelected()
    compose.onNodeWithTag("inbox_page").assertDoesNotExist()
  }

  @Test fun `rapid tab requests leave the last request selected without transient loads`() {
    show()
    compose.runOnIdle { tabs.select(RootTab.PROFILE); tabs.select(RootTab.MESSAGES); tabs.select(RootTab.OFFERS) }
    compose.waitForIdle()
    compose.onNodeWithTag("tab_OFFERS").assertIsSelected()
    assertEquals(listOf(RootTab.OFFERS), selected.distinct())
  }

  @Test fun `resume during a cancelled drag reactivates the previously settled inbox`() {
    show()
    select(RootTab.MESSAGES)
    val before = selected.size
    val release = CompletableDeferred<Unit>()
    compose.runOnIdle {
      dragScope.launch {
        tabs.pager.scroll {
          scrollBy(tabs.pager.layoutInfo.pageSize * 0.3f)
          release.await()
          scrollBy(-tabs.pager.layoutInfo.pageSize * 0.3f)
        }
      }
    }
    compose.waitForIdle()
    compose.runOnIdle { activation.intValue++ }
    compose.waitForIdle()
    assertEquals(before, selected.size)
    compose.runOnIdle { release.complete(Unit) }
    compose.waitForIdle()
    assertEquals(before + 1, selected.size)
    assertEquals(RootTab.MESSAGES, selected.last())
  }

  @Test fun `inbox scroll survives tab changes and visiting a child route`() {
    show()
    select(RootTab.MESSAGES)
    compose.onNodeWithTag("messages_list").performScrollToIndex(20)
    compose.waitForIdle()
    val index = inboxScroll.firstVisibleItemIndex
    assertTrue(index > 0)
    select(RootTab.PROFILE)
    select(RootTab.MESSAGES)
    assertEquals(index, inboxScroll.firstVisibleItemIndex)
    compose.runOnIdle { rootVisible.value = false }
    compose.onNodeWithTag("child_route").performTouchInput { swipe(Offset(width * 0.75f, height * 0.5f), Offset(width * 0.25f, height * 0.5f)) }
    compose.runOnIdle { rootVisible.value = true }
    compose.waitForIdle()
    compose.onNodeWithTag("tab_MESSAGES").assertIsSelected()
    assertEquals(index, inboxScroll.firstVisibleItemIndex)
  }

  @Test fun `blocked modal and category strip cannot switch root tabs`() {
    show()
    compose.runOnIdle { blocked.value = true }
    compose.onNodeWithTag("root_tab_pager").performTouchInput { swipe(Offset(width * 0.75f, height * 0.5f), Offset(width * 0.25f, height * 0.5f)) }
    compose.waitForIdle()
    compose.onNodeWithTag("tab_OFFERS").assertIsSelected()
    compose.runOnIdle { blocked.value = false }
    compose.onNodeWithText("Category 19").performScrollTo()
    repeat(2) { compose.onNodeWithTag("categories").performTouchInput { swipe(Offset(width * 0.75f, height * 0.5f), Offset(width * 0.25f, height * 0.5f)) } }
    compose.waitForIdle()
    compose.onNodeWithTag("tab_OFFERS").assertIsSelected()
    compose.onNodeWithTag("offers_list").performTouchInput { swipeUp() }
    compose.waitForIdle()
    compose.onNodeWithTag("tab_OFFERS").assertIsSelected()
  }

  /** Cancelling a running multi-page animation must not activate its intermediate page. */
  @Test fun `return to Offers during navigation does not activate Messages`() {
    show()
    compose.mainClock.autoAdvance = false
    compose.runOnIdle { tabs.select(RootTab.PROFILE) }
    compose.mainClock.advanceTimeUntil(timeoutMillis = 2000) {
      tabs.pager.currentPage == RootTab.MESSAGES.ordinal
    }
    compose.runOnIdle { tabs.select(RootTab.OFFERS) }
    compose.mainClock.autoAdvance = true
    compose.waitForIdle()
    compose.onNodeWithTag("tab_OFFERS").assertIsSelected()
    assertEquals(listOf(RootTab.OFFERS), selected.distinct())
  }

  /** Signing out discards private tab state without opening authentication. */
  @Test fun `sign out resets the selected tab without prompting sign in`() {
    show()
    select(RootTab.PROFILE)
    compose.runOnIdle { actor.value = null }
    compose.waitForIdle()
    compose.onNodeWithTag("tab_OFFERS").assertIsSelected()
    compose.onNodeWithTag("profile_page").assertDoesNotExist()
    assertEquals(0, signIns)
  }

  /** A guest transition cannot carry the previous actor's selected tab forward. */
  @Test fun `sign in after a guest session starts at Offers`() {
    show()
    select(RootTab.MESSAGES)
    compose.runOnIdle { actor.value = null }
    compose.waitForIdle()
    compose.runOnIdle { actor.value = "another-owner" }
    compose.waitForIdle()
    compose.onNodeWithTag("tab_OFFERS").assertIsSelected()
    assertEquals(RootTab.OFFERS, selected.last())
    assertEquals(0, signIns)
  }

  @Test fun `restoration keeps selected tab while a different account starts at Offers`() {
    val restoration = StateRestorationTester(compose)
    restoration.setContent { Harness() }
    select(RootTab.PROFILE)
    restoration.emulateSavedInstanceStateRestore()
    compose.onNodeWithTag("tab_PROFILE").assertIsSelected()
    compose.runOnIdle { actor.value = "another-owner" }
    compose.waitForIdle()
    compose.onNodeWithTag("tab_OFFERS").assertIsSelected()
  }
}
