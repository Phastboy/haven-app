package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.example.data.FeedOffer
import com.example.data.Person
import com.example.data.ProviderProfile
import com.example.ui.screens.reconcileProviderSavedStates
import com.example.ui.screens.rememberProviderVisitScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.example.ui.screens.resolvedSavedState
import com.example.ui.screens.requestedSavedState
import com.example.ui.screens.ProviderProfileScreen
import com.example.ui.theme.HavenTheme
import com.example.ui.theme.ThemeMode
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w390dp-h844dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProviderProfileRobolectricTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
  private val person = Person("provider", "maker", "Test maker", "A real catalogue biography", "March 2026", null)
  private val offers = (1..3).map { FeedOffer("$it", "offer-$it", "Offer $it", "Test maker", "100 NGN", null) }

  /** Provider catalogue uses the shared cards, equal columns, action order and actual save callback. */
  @Test fun `shared cards align in two columns and save independently from opening`() {
    var saved = ""
    var opened = ""
    val theme = mutableStateOf(ThemeMode.LIGHT)
    compose.setContent {
      HavenTheme(themeMode = theme.value) {
        ProviderProfileScreen(ProviderProfile(person, offers, null), false, null, {}, {},
          { opened = it }, {}, {}, modifier = Modifier.width(390.dp), onSaveOffer = { saved = it })
      }
    }
    val first = compose.onNodeWithTag("offer_card_1").fetchSemanticsNode().boundsInRoot
    val second = compose.onNodeWithTag("offer_card_2").fetchSemanticsNode().boundsInRoot
    assertEquals(first.top, second.top, 1f)
    assertEquals(first.width, second.width, 1f)
    assertTrue(first.right < second.left)
    val follow = compose.onNodeWithTag("provider_follow_button").fetchSemanticsNode().boundsInRoot
    val message = compose.onNodeWithTag("provider_message_button").fetchSemanticsNode().boundsInRoot
    assertTrue(follow.left < message.left)
    val avatar = compose.onNodeWithTag("provider_avatar").fetchSemanticsNode().boundsInRoot
    assertEquals((first.left + second.right) / 2, avatar.center.x, 1f)
    compose.onAllNodesWithContentDescription("Save offer").onFirst().performClick()
    assertEquals("offer-1", saved)
    assertEquals("", opened)
    compose.onNodeWithTag("offer_title_1", useUnmergedTree = true).performClick()
    assertEquals("offer-1", opened)
    for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
      compose.runOnIdle { theme.value = mode }
      compose.waitForIdle()
      val view = compose.activity.window.decorView
      val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
      compose.runOnUiThread { view.draw(Canvas(bitmap)) }
      val file = File("build/reports/screenshots/provider-${mode.name.lowercase()}.png")
      file.parentFile.mkdirs()
      file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    compose.onNodeWithTag("offer_card_3").performScrollTo()
    val third = compose.onNodeWithTag("offer_card_3").fetchSemanticsNode().boundsInRoot
    assertEquals(first.width, third.width, 1f)
  }

  /** Missing provider metadata must not label an unsave mutation as a save action. */
  @Test fun `saved feed fallback matches the displayed action and mutation target`() {
    val saved = resolvedSavedState(null, null, true, null)
    var requested: Boolean? = null
    compose.setContent {
      HavenTheme {
        ProviderProfileScreen(ProviderProfile(person, offers.take(1), null), false, null,
          {}, {}, {}, {}, {}, savedStates = mapOf("offer-1" to saved),
          onSaveOffer = { requested = requestedSavedState(saved) })
      }
    }
    compose.onNodeWithContentDescription("Unsave offer").performClick()
    assertEquals(false, requested)
    assertFalse(resolvedSavedState(false, null, true, true))
    assertFalse(resolvedSavedState(null, false, true, true))
    assertFalse(resolvedSavedState(null, null, null, null))
  }

  /** Owner actions cannot accidentally save an owned offer; loaded failures stay visible. */
  @Test fun `owner hides save and loaded failure keeps retry available`() {
    var retries = 0
    compose.setContent {
      HavenTheme {
        ProviderProfileScreen(ProviderProfile(person, offers.take(2), "next"), false, "Could not load more",
          {}, {}, {}, {}, {}, onLoadMore = { retries++ }, isOwnProfile = true, savedStates = mapOf("offer-2" to true))
      }
    }
    compose.onAllNodesWithContentDescription("Save offer").assertCountEquals(0)
    compose.onAllNodesWithContentDescription("Unsave offer").assertCountEquals(0)
    compose.onNodeWithTag("provider_action_error").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Load more offers").performScrollTo().performClick()
    assertEquals(1, retries)
  }

  /** Fresh data replaces rollback values without clobbering a write still in flight. */
  @Test fun `profile refresh reconciles settled saves and preserves pending saves`() {
    val refreshed = offers.take(2).map { it.copy(isSaved = true) }
    val actual = reconcileProviderSavedStates(mapOf("offer-1" to false, "offer-2" to false), setOf("offer-2"), refreshed)
    assertEquals(true, actual["offer-1"])
    assertEquals(false, actual["offer-2"])
    val absent = reconcileProviderSavedStates(actual, emptySet(), offers.take(1))
    assertEquals(true, absent["offer-1"])
  }

  /** Returning to the same handle cannot revive paging work from the previous visit. */
  @Test fun `leaving and reopening provider cancels old paging response`() {
    val handle = mutableStateOf<String?>("maker")
    lateinit var visitScope: CoroutineScope
    val response = CompletableDeferred<Unit>()
    var appended = false
    compose.setContent { visitScope = rememberProviderVisitScope(handle.value, "actor", 0) }
    lateinit var request: kotlinx.coroutines.Job
    compose.runOnIdle { request = visitScope.launch { response.await(); appended = true } }
    compose.runOnIdle { handle.value = null }
    compose.waitForIdle()
    compose.runOnIdle { handle.value = "maker" }
    compose.waitForIdle()
    compose.runOnIdle { response.complete(Unit) }
    compose.waitForIdle()
    assertTrue(request.isCancelled)
    assertFalse(appended)
  }

  /** Loading renders the shared card skeleton rather than a separate provider-card approximation. */
  @Test fun `loading uses shared card skeletons`() {
    compose.setContent {
      HavenTheme { ProviderProfileScreen(null, true, null, {}, {}, {}, {}, {}) }
    }
    compose.onAllNodesWithTag("offer_card_skeleton").assertCountEquals(4)
  }
}
