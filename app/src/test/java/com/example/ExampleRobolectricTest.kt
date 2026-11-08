package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.model.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalCoroutinesApi::class)
class ExampleRobolectricTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Haven", appName)
  }

  @Test
  fun `session manager handles sign in and sign out correctly`() {
    SessionManager.signOut()
    assertFalse(SessionManager.session.value.isAuthenticated)
    assertNull(SessionManager.session.value.email)
    assertEquals("Guest Member", SessionManager.session.value.displayName)

    SessionManager.signInPerson("person-1", "Adaeze Obi", "adaeze")
    assertTrue(SessionManager.session.value.isAuthenticated)
    assertEquals("person-1", SessionManager.session.value.personId)
    assertEquals("Adaeze Obi", SessionManager.session.value.displayName)
    assertEquals("A", SessionManager.session.value.initial)

    SessionManager.signOut()
    assertFalse(SessionManager.session.value.isAuthenticated)
  }

}
