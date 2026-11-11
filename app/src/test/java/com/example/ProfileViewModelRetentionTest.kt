package com.example

import com.example.data.ProfileOverview
import com.example.data.ProfileRepository
import com.example.ui.model.ProfileViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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

/**
 * Issue #30: returning to the profile tab must not blank a usable same-account
 * profile. These cover repeated entry, slow/offline refresh, explicit freshness,
 * invalidation after edits/collection changes and account transitions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelRetentionTest {
  private val dispatcher = StandardTestDispatcher()
  private var clock = 0L

  @Before fun setup() { Dispatchers.setMain(dispatcher) }
  @After fun cleanup() { Dispatchers.resetMain() }

  private fun profile(id: String) = ProfileOverview(
    id = id, handle = id, displayName = "Real person", bio = null,
    joined = "Joined November 2026", avatarUrl = null,
    offerCount = 3, savedCount = 2, followingCount = 1, offers = emptyList())

  private class FakeRepo(var fetcher: suspend () -> ProfileOverview) : ProfileRepository {
    var loads = 0
    override suspend fun load(): ProfileOverview { loads++; return fetcher() }
    override suspend fun updateProfile(displayName: String, handle: String, bio: String?): Result<ProfileOverview> = Result.success(fetcher())
    override suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): Result<ProfileOverview> = Result.success(fetcher())
    override suspend fun removeAvatar(): Result<ProfileOverview> = Result.success(fetcher())
  }

  @Test fun `repeated tab entry reuses fresh profile without blanking`() = runTest(dispatcher) {
    val repo = FakeRepo { profile("owner") }
    val model = ProfileViewModel(repo) { clock }

    model.load("owner")
    advanceUntilIdle()
    assertEquals("owner", model.state.value.profile?.id)

    clock += 1_000
    model.load("owner") // re-entry inside the freshness window
    advanceUntilIdle()

    assertEquals("owner", model.state.value.profile?.id)
    assertFalse(model.state.value.loading)
    assertFalse(model.state.value.refreshing)
    assertEquals(1, repo.loads)
  }

  @Test fun `stale re-entry keeps the profile visible while refreshing`() = runTest(dispatcher) {
    val gate = CompletableDeferred<Unit>()
    var first = true
    val repo = FakeRepo { if (first) profile("owner") else { gate.await(); profile("owner") } }
    val model = ProfileViewModel(repo) { clock }

    model.load("owner")
    advanceUntilIdle()
    first = false
    clock += ProfileViewModel.FRESHNESS_WINDOW_MS + 1

    model.load("owner") // stale, non-forced refresh
    runCurrent()

    assertEquals("owner", model.state.value.profile?.id) // retained, not blanked
    assertTrue(model.state.value.refreshing)
    assertFalse(model.state.value.loading)

    gate.complete(Unit)
    advanceUntilIdle()
    assertEquals("owner", model.state.value.profile?.id)
    assertFalse(model.state.value.refreshing)
  }

  @Test fun `rapid re-entry does not stack overlapping requests`() = runTest(dispatcher) {
    val gate = CompletableDeferred<Unit>()
    val repo = FakeRepo { gate.await(); profile("owner") }
    val model = ProfileViewModel(repo) { clock }

    model.load("owner")
    runCurrent()
    model.load("owner") // ordinary re-entry coalesces
    runCurrent()
    model.load("owner")
    runCurrent()

    gate.complete(Unit)
    advanceUntilIdle()
    assertEquals(1, repo.loads)
    assertEquals("owner", model.state.value.profile?.id)
  }

  @Test fun `failed refresh retains content and offers a nonblocking retry`() = runTest(dispatcher) {
    var fail = false
    val repo = FakeRepo { if (fail) error("offline") else profile("owner") }
    val model = ProfileViewModel(repo) { clock }

    model.load("owner")
    advanceUntilIdle()
    assertEquals("owner", model.state.value.profile?.id)

    fail = true
    clock += ProfileViewModel.FRESHNESS_WINDOW_MS + 1
    model.load("owner")
    advanceUntilIdle()

    assertEquals("owner", model.state.value.profile?.id) // retained on failure
    assertNotNull(model.state.value.error)
    assertFalse(model.state.value.loading)

    fail = false
    model.load("owner", force = true)
    advanceUntilIdle()
    assertEquals("owner", model.state.value.profile?.id)
    assertNull(model.state.value.error)
  }

  @Test fun `cold miss failure exposes an error without a dummy profile`() = runTest(dispatcher) {
    val repo = FakeRepo { error("offline") }
    val model = ProfileViewModel(repo) { clock }

    model.load("owner")
    assertTrue(model.state.value.loading) // genuine cache miss
    assertNull(model.state.value.profile)

    advanceUntilIdle()
    assertNull(model.state.value.profile)
    assertNotNull(model.state.value.error)
    assertFalse(model.state.value.loading)
  }

  @Test fun `switching accounts never reuses the previous profile`() = runTest(dispatcher) {
    val repo = FakeRepo { profile("A") }
    val model = ProfileViewModel(repo) { clock }

    model.load("A")
    advanceUntilIdle()
    assertEquals("A", model.state.value.profile?.id)

    repo.fetcher = { profile("B") }
    model.load("B")
    assertNull(model.state.value.profile) // A cleared immediately
    assertTrue(model.state.value.loading)

    advanceUntilIdle()
    assertEquals("B", model.state.value.profile?.id)
  }

  @Test fun `invalidate refreshes the retained profile on the next entry`() = runTest(dispatcher) {
    val repo = FakeRepo { profile("owner") }
    val model = ProfileViewModel(repo) { clock }

    model.load("owner")
    advanceUntilIdle()
    model.load("owner") // still fresh: reused
    advanceUntilIdle()
    assertEquals(1, repo.loads)

    model.invalidate() // e.g. after an edit or a confirmed save
    model.load("owner")
    advanceUntilIdle()
    assertEquals(2, repo.loads)
    assertEquals("owner", model.state.value.profile?.id)
  }

  @Test fun `invalidation during fetch survives its stale response`() = runTest(dispatcher) {
    val gate = CompletableDeferred<Unit>()
    val repo = FakeRepo { gate.await(); profile("owner") }
    val model = ProfileViewModel(repo) { clock }
    model.load("owner")
    runCurrent()
    model.invalidate()
    gate.complete(Unit)
    advanceUntilIdle()
    model.load("owner")
    advanceUntilIdle()
    assertEquals(2, repo.loads)
  }

  @Test fun `forced load during fetch queues one post mutation refresh`() = runTest(dispatcher) {
    val gate = CompletableDeferred<Unit>()
    var calls = 0
    val repo = FakeRepo {
      if (++calls == 1) { gate.await(); profile("owner") }
      else profile("owner").copy(displayName = "Updated person")
    }
    val model = ProfileViewModel(repo) { clock }
    model.load("owner")
    runCurrent()
    model.invalidate()
    model.load("owner", force = true)
    model.load("owner", force = true)
    gate.complete(Unit)
    advanceUntilIdle()
    assertEquals(2, repo.loads)
    assertEquals("Updated person", model.state.value.profile?.displayName)
  }

  @Test fun `account switch drops queued refresh for previous account`() = runTest(dispatcher) {
    val gate = CompletableDeferred<Unit>()
    val repo = FakeRepo { gate.await(); profile("A") }
    val model = ProfileViewModel(repo) { clock }
    model.load("A")
    runCurrent()
    model.load("A", force = true)
    repo.fetcher = { profile("B") }
    model.load("B")
    gate.complete(Unit)
    advanceUntilIdle()
    assertEquals(2, repo.loads)
    assertEquals("B", model.state.value.profile?.id)
  }

  @Test fun `clear resets so the next load is a fresh cache miss`() = runTest(dispatcher) {
    val repo = FakeRepo { profile("owner") }
    val model = ProfileViewModel(repo) { clock }

    model.load("owner")
    advanceUntilIdle()
    model.clear()
    assertNull(model.state.value.profile)

    model.load("owner")
    assertTrue(model.state.value.loading)
    advanceUntilIdle()
    assertEquals("owner", model.state.value.profile?.id)
  }
}
