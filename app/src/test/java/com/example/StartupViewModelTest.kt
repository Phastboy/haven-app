package com.example

import com.example.ui.model.StartupState
import com.example.ui.model.StartupViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class StartupViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  @Before fun setup() { Dispatchers.setMain(dispatcher) }
  @After fun cleanup() { Dispatchers.resetMain() }

  @Test fun `cold launch waits for restoration then opens authenticated feed`() = runTest(dispatcher) {
    val pending = CompletableDeferred<Boolean>()
    val model = StartupViewModel({ pending.await() }, { true })
    advanceUntilIdle()
    assertEquals(StartupState.RESTORING, model.state.value)
    pending.complete(true)
    advanceUntilIdle()
    assertEquals(StartupState.AUTHENTICATED, model.state.value)
  }

  @Test fun `no credentials or rejected refresh opens welcome`() = runTest(dispatcher) {
    val model = StartupViewModel({ false }, { false })
    advanceUntilIdle()
    assertEquals(StartupState.SIGNED_OUT, model.state.value)
  }

  @Test fun `offline startup keeps session retryable and successful refresh opens feed`() = runTest(dispatcher) {
    var offline = true
    val model = StartupViewModel({ if (offline) throw IOException("offline") else true }, { true })
    advanceUntilIdle()
    assertEquals(StartupState.RETRY, model.state.value)
    offline = false
    model.retry()
    advanceUntilIdle()
    assertEquals(StartupState.AUTHENTICATED, model.state.value)
  }

  @Test fun `server failure with saved refresh does not masquerade as signed out`() = runTest(dispatcher) {
    val model = StartupViewModel({ false }, { true })
    advanceUntilIdle()
    assertEquals(StartupState.RETRY, model.state.value)
  }

  @Test fun `credential storage failure remains retryable instead of crashing or signing out`() = runTest(dispatcher) {
    val model = StartupViewModel({ throw IOException("storage unavailable") }, { throw IOException("storage unavailable") })
    advanceUntilIdle()
    assertEquals(StartupState.RETRY, model.state.value)
  }

  @Test fun `repeated retry cannot rotate refresh credentials concurrently`() = runTest(dispatcher) {
    var calls = 0
    val pending = CompletableDeferred<Boolean>()
    val model = StartupViewModel({ calls++; pending.await() }, { true })
    model.retry()
    model.retry()
    advanceUntilIdle()
    assertEquals(1, calls)
    pending.complete(true)
    advanceUntilIdle()
    assertEquals(StartupState.AUTHENTICATED, model.state.value)
  }
}
