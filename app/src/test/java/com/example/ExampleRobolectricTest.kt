package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.model.SignInViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
@Config(sdk = [36])
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
  fun `email validation correctly identifies valid and invalid emails`() {
    assertTrue(SignInViewModel.isValidEmail("adaeze@crafts.ng"))
    assertTrue(SignInViewModel.isValidEmail("name@example.com"))
    assertTrue(SignInViewModel.isValidEmail("user.name+tag@sub.domain.co"))

    assertFalse(SignInViewModel.isValidEmail(""))
    assertFalse(SignInViewModel.isValidEmail("   "))
    assertFalse(SignInViewModel.isValidEmail("adaeze@not-an-email"))
    assertFalse(SignInViewModel.isValidEmail("plainaddress"))
    assertFalse(SignInViewModel.isValidEmail("@missingusername.com"))
    assertFalse(SignInViewModel.isValidEmail("user@.com"))
  }

  @Test
  fun `submitting invalid email displays validation error`() {
    val viewModel = SignInViewModel(testDispatcher)
    viewModel.onEmailChange("adaeze@not-an-email")
    viewModel.onSendMagicLink()

    val state = viewModel.uiState.value
    assertTrue(state.isError)
    assertEquals("Enter a valid email address.", state.emailError)
    assertFalse(state.isSending)
    assertFalse(state.isSuccess)
  }

  @Test
  fun `typing in email clears previous validation error`() {
    val viewModel = SignInViewModel(testDispatcher)
    viewModel.onEmailChange("invalid")
    viewModel.onSendMagicLink()
    assertTrue(viewModel.uiState.value.isError)

    viewModel.onEmailChange("adaeze@crafts.ng")
    assertFalse(viewModel.uiState.value.isError)
    assertNull(viewModel.uiState.value.emailError)
  }

  @Test
  fun `tapping Google sign-in triggers cancelled error banner`() {
    val viewModel = SignInViewModel(testDispatcher)
    assertNull(viewModel.uiState.value.googleErrorBanner)

    viewModel.onGoogleSignInClicked()
    assertNotNull(viewModel.uiState.value.googleErrorBanner)
    assertEquals("Google sign-in was cancelled.", viewModel.uiState.value.googleErrorBanner)

    viewModel.onDismissGoogleError()
    assertNull(viewModel.uiState.value.googleErrorBanner)
  }

  @Test
  fun `submitting valid email simulates 1_5s sending then transitions to success`() = runTest(testDispatcher) {
    val viewModel = SignInViewModel(testDispatcher)
    viewModel.onEmailChange("adaeze@crafts.ng")
    viewModel.onSendMagicLink()

    // Dispatch initial coroutine execution
    testScheduler.advanceTimeBy(100)
    assertTrue(viewModel.uiState.value.isSending)
    assertFalse(viewModel.uiState.value.isSuccess)

    // Advance past the 1.5s simulated network request
    testScheduler.advanceTimeBy(1600)

    assertFalse(viewModel.uiState.value.isSending)
    assertTrue(viewModel.uiState.value.isSuccess)
    assertEquals("adaeze@crafts.ng", viewModel.uiState.value.successEmail)
  }
}
