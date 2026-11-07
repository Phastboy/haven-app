package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.FeedOffer
import com.example.data.InboxConversation
import com.example.data.ProfileOverview
import com.example.ui.components.FeedOfferCard
import com.example.ui.components.offerDistanceLabel
import com.example.ui.model.MessagesInboxUiState
import com.example.ui.model.ProfileUiState
import com.example.ui.screens.MessagesInboxScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.*
import java.io.File
import org.junit.After
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
class ProfileAndThemeRobolectricTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
  @After fun resetTheme() { ThemeManager.setMode(ThemeMode.SYSTEM, ApplicationProvider.getApplicationContext()) }
  private val offer = FeedOffer("test", "real-test", "Visible offer title", "Real person", "100 NGN", null, 1250.0)

  private fun captureRootBitmap(): Bitmap {
    compose.waitForIdle()
    val view = compose.activity.window.decorView
    val width = view.width.takeIf { it > 0 } ?: 1080
    val height = view.height.takeIf { it > 0 } ?: 2340
    if (view.width <= 0 || view.height <= 0) {
      view.measure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
      )
      view.layout(0, 0, width, height)
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    view.draw(canvas)
    return bitmap
  }

  private fun captureNodeBitmap(tag: String): Bitmap {
    val root = captureRootBitmap()
    val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
    val left = bounds.left.toInt().coerceIn(0, root.width - 1)
    val top = bounds.top.toInt().coerceIn(0, root.height - 1)
    val width = bounds.width.toInt().coerceIn(1, root.width - left)
    val height = bounds.height.toInt().coerceIn(1, root.height - top)
    return Bitmap.createBitmap(root, left, top, width, height)
  }

  private fun verifyCard(mode: ThemeMode) {
    var foreground = Color.Unspecified
    compose.setContent {
      HavenTheme(mode) {
        val contentColor = LocalContentColor.current
        SideEffect { foreground = contentColor }
        Column(Modifier.width(180.dp).background(HavenColors.Canvas)) {
          FeedOfferCard(offer, showDistance = true, onOpen = {})
        }
      }
    }
    compose.onNodeWithTag("offer_title_test", useUnmergedTree = true).assertIsDisplayed()
    compose.onNodeWithText("Real person", useUnmergedTree = true).assertIsDisplayed()
    compose.onNodeWithText("1.3 km", useUnmergedTree = true).assertIsDisplayed()
    assertEquals(if (mode == ThemeMode.DARK) HavenDarkPalette.textPrimary else HavenLightPalette.textPrimary, foreground)
    for (tag in listOf("offer_title_test", "offer_price_test")) {
      val pixels = captureNodeBitmap(tag).asImageBitmap().toPixelMap()
      var darkest = 1f
      var brightest = 0f
      for (x in 0 until pixels.width) for (y in 0 until pixels.height) {
        val luminance = pixels[x, y].luminance()
        darkest = minOf(darkest, luminance)
        brightest = maxOf(brightest, luminance)
      }
      if (mode == ThemeMode.DARK) assertTrue("Dark title must contain light foreground pixels", brightest > 0.5f)
      else assertTrue("Light title must contain dark foreground pixels", darkest < 0.15f)
    }
    saveScreenshot("offer-card-${mode.name.lowercase()}.png")
  }

  private fun saveScreenshot(name: String) {
    val file = File("build/reports/screenshots/$name")
    file.parentFile?.mkdirs()
    file.outputStream().use { captureRootBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
  }

  @Test fun `dark offer title and price are readable`() = verifyCard(ThemeMode.DARK)
  @Test fun `light offer title and price are readable`() = verifyCard(ThemeMode.LIGHT)
  @Test fun `distance stays absent without valid server data`() {
    assertNull(offerDistanceLabel(null))
    assertNull(offerDistanceLabel(Double.NaN))
    assertNull(offerDistanceLabel(-1.0))
    assertEquals("250 m", offerDistanceLabel(250.0))
  }
  @Test fun `profile has real identity and persisted appearance without spec controls`() {
    ThemeManager.setMode(ThemeMode.LIGHT, ApplicationProvider.getApplicationContext())
    compose.setContent {
      HavenTheme {
        ProfileScreen(ProfileUiState(profile = ProfileOverview("person", "real-person", "Real person",
          null, "Joined November 2026", null, 0, 2, 1, emptyList())), {}, {}, {})
      }
    }
    compose.onNodeWithTag("profile_display_name").assertIsDisplayed()
    compose.onNodeWithText("2 saved").assertIsDisplayed()
    compose.onNodeWithTag("profile_bio").assertDoesNotExist()
    compose.onNodeWithText("State A: Updated").assertDoesNotExist()
    saveScreenshot("profile-light.png")
    compose.onNodeWithTag("profile_link_appearance").performScrollTo().performClick()
    compose.onNodeWithTag("appearance_option_dark").performClick()
    assertEquals(ThemeMode.DARK, ThemeManager.themeMode)
    ThemeManager.init(ApplicationProvider.getApplicationContext())
    assertEquals(ThemeMode.DARK, ThemeManager.themeMode)
    compose.onNodeWithTag("profile_display_name").performScrollTo()
    saveScreenshot("profile-dark.png")
  }

  @Test fun `messages inbox renders actual rows and unread state in both themes`() {
    val rows = listOf(
      InboxConversation("conv-1", "person-1", "Olumide Adeleke", "olumide-adeleke", null,
        "Here are three offers to take a look at. Let's discuss.", "2026-11-02T09:44:00Z", true),
      InboxConversation("conv-2", "person-2", "Sarah Nwachukwu", "sarah-nwachukwu", null,
        "I sent over the revised logo concepts.", "2026-11-01T09:15:00Z", false)
    )
    val mode = mutableStateOf(ThemeMode.LIGHT)
    var selected: String? = null
    compose.setContent {
      HavenTheme(mode.value) {
        MessagesInboxScreen(MessagesInboxUiState(items = rows), {}, {}, { selected = it })
      }
    }
    for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
      mode.value = theme
      compose.waitForIdle()
      compose.onNodeWithText("Messages").assertIsDisplayed()
      compose.onNodeWithText("Here are three offers to take a look at. Let's discuss.").assertIsDisplayed()
      compose.onNodeWithTag("unread_conv-1", useUnmergedTree = true).assertIsDisplayed()
      compose.onNodeWithTag("conversation_chevron_conv-1", useUnmergedTree = true).assertIsDisplayed()
      compose.onNodeWithTag("conversation_conv-1").performClick()
      assertEquals("conv-1", selected)
      saveScreenshot("messages-${theme.name.lowercase()}.png")
    }
  }
}
