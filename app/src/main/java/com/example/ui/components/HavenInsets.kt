package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HavenColors

/**
 * App-wide centralized window insets and safe area infrastructure for Haven.
 * Robustly and dynamically handles:
 * 1. Top system insets: Status bar and display cutouts (camera notches / punch holes)
 * 2. Bottom system insets: Navigation bar (gesture navigation pill or 3-button navigation)
 * 3. Soft Keyboard / IME: Smooth resizing, keyboard elevation, and input visibility
 * 4. Edge-to-edge container continuity: Edge-to-edge backgrounds behind system bars with zero clipping
 */
object HavenInsets {
  /**
   * Safe top insets combining status bar and display cutouts.
   */
  val statusBarsAndCutouts: WindowInsets
    @Composable
    get() = WindowInsets.statusBars.union(WindowInsets.displayCutout)

  /**
   * Safe bottom insets for system navigation bars (gesture handle or 3-button bar).
   */
  val navigationBars: WindowInsets
    @Composable
    get() = WindowInsets.navigationBars

  /**
   * Soft keyboard / IME window insets.
   */
  val ime: WindowInsets
    @Composable
    get() = WindowInsets.ime

  /**
   * Combined safe bottom insets: Sits cleanly above system navigation when keyboard is closed,
   * and smoothly elevates above the keyboard when active without double-padding.
   */
  val bottomWithIme: WindowInsets
    @Composable
    get() = WindowInsets.navigationBars.union(WindowInsets.ime)

  /**
   * Full safe drawing bounds for the screen.
   */
  val safeDrawing: WindowInsets
    @Composable
    get() = WindowInsets.safeDrawing
}

/**
 * Modifier extension: Applies safe top padding (status bar + camera cutouts/notches).
 */
@Composable
fun Modifier.havenTopSafePadding(): Modifier = this.windowInsetsPadding(
  HavenInsets.statusBarsAndCutouts.only(WindowInsetsSides.Top)
)

/**
 * Modifier extension: Applies safe bottom padding for system navigation bars.
 */
@Composable
fun Modifier.havenBottomSafePadding(): Modifier = this.windowInsetsPadding(
  HavenInsets.navigationBars.only(WindowInsetsSides.Bottom)
)

/**
 * Modifier extension: Applies keyboard-aware bottom padding, lifting above IME keyboard
 * while resting above navigation bar when closed.
 */
@Composable
fun Modifier.havenKeyboardAwareBottomPadding(): Modifier = this.windowInsetsPadding(
  HavenInsets.bottomWithIme.only(WindowInsetsSides.Bottom)
)

/**
 * Centralized Top Bar Container:
 * Extends background continuously behind the status bar and camera punch-holes to the physical top edge,
 * while positioning the child top bar content below system cutouts.
 */
@Composable
fun HavenTopBarContainer(
  modifier: Modifier = Modifier,
  backgroundColor: Color = HavenColors.Canvas,
  content: @Composable () -> Unit
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(backgroundColor)
  ) {
    // Dynamically matches the runtime status bar and camera punch-hole height
    Spacer(modifier = Modifier.windowInsetsTopHeight(HavenInsets.statusBarsAndCutouts))
    content()
  }
}

/**
 * Centralized Bottom Bar Container:
 * Extends background continuously behind the navigation bar (gesture pill or 3 buttons) to the physical bottom edge,
 * while positioning interactive tabs, actions, or input composers safely above system controls.
 */
@Composable
fun HavenBottomBarContainer(
  modifier: Modifier = Modifier,
  backgroundColor: Color = HavenColors.Canvas,
  liftWithKeyboard: Boolean = false,
  content: @Composable () -> Unit
) {
  val bottomInsets = if (liftWithKeyboard) HavenInsets.bottomWithIme else HavenInsets.navigationBars

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(backgroundColor)
      .windowInsetsPadding(bottomInsets.only(WindowInsetsSides.Bottom))
  ) {
    content()
  }
}
