package com.example

import com.example.data.ProfileOverview
import com.example.data.ProfileRepository
import com.example.ui.model.ProfileViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  @Before fun setup() { Dispatchers.setMain(dispatcher) }
  @After fun cleanup() { Dispatchers.resetMain() }
  private fun profile(id: String) = ProfileOverview(id, id, "Real person", null, "Joined November 2026", null, 0, 0, 0, emptyList())

  private fun fakeProfileRepo(fetcher: suspend () -> ProfileOverview) = object : ProfileRepository {
    override suspend fun load(): ProfileOverview = fetcher()
    override suspend fun updateProfile(displayName: String, handle: String, bio: String?): Result<ProfileOverview> = Result.success(fetcher())
    override suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): Result<ProfileOverview> = Result.success(fetcher())
    override suspend fun removeAvatar(): Result<ProfileOverview> = Result.success(fetcher())
  }

  @Test fun `identity mismatch cannot display another persons profile`() = runTest(dispatcher) {
    val model = ProfileViewModel(fakeProfileRepo { profile("other") })
    model.load("owner")
    advanceUntilIdle()
    assertNull(model.state.value.profile)
    assertNotNull(model.state.value.error)
  }
  @Test fun `network error has no dummy identity and successful retry uses server profile`() = runTest(dispatcher) {
    var fail = true
    val model = ProfileViewModel(fakeProfileRepo { if (fail) error("offline") else profile("owner") })
    model.load("owner")
    advanceUntilIdle()
    assertNull(model.state.value.profile)
    assertNotNull(model.state.value.error)
    fail = false
    model.load("owner", force = true)
    advanceUntilIdle()
    assertEquals("owner", model.state.value.profile?.id)
    model.clear()
    assertNull(model.state.value.profile)
  }
}
