package com.example

import androidx.lifecycle.SavedStateHandle
import com.example.data.*
import com.example.location.CapturedLocation
import com.example.ui.model.OfferEditorState
import com.example.ui.model.OfferEditorViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.*
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OfferEditorSaveStateTest {
  private val dispatcher = StandardTestDispatcher()
  @Before fun setup() { Dispatchers.setMain(dispatcher) }
  @After fun cleanup() { Dispatchers.resetMain() }

  private class Repo : OfferEditorRepository {
    var remote = EditableOffer("offer-id", "genuine-offer-slug", "PC for sale", "Working PC available from my shop.", "other", "150000", "NGN", null, emptyList(), false)
    var createCalls = 0
    val creationKeys = mutableListOf<String>()
    var blocker: CompletableDeferred<Unit>? = null
    var failUpload = false
    var ignoreCancellation = false
    override suspend fun options() = OfferFormOptions(listOf("other" to "Other"), listOf("NGN"), emptyList(), "none")
    override suspend fun load(id: String) = remote
    override suspend fun save(id: String?, input: JSONObject): EditableOffer {
      if (ignoreCancellation) withContext(NonCancellable) { blocker?.await() } else blocker?.await()
      if (id == null) { createCalls++; creationKeys += input.getString("idempotency_key") }
      return remote
    }
    override suspend fun search(query: String) = emptyList<OfferPlace>()
    override suspend fun capturedPlace(point: CapturedLocation) = OfferPlace("signed", "Readable place")
    override suspend fun upload(id: String, image: EditorImage): EditableOffer {
      if (failUpload) error("connection lost")
      remote = remote.copy(images = listOf(EditorImage("confirmed-photo", "https://images.test/offers/${image.uploadId}")), isPublic = true)
      return remote
    }
    override suspend fun remove(id: String, imageId: String) = remote
    override suspend fun reorder(id: String, images: List<EditorImage>) = remote
  }

  private fun fill(model: OfferEditorViewModel) {
    model.change { it.copy(title = "PC for sale", description = "Working PC available from my shop.", category = "other", price = "150000", images = listOf(EditorImage(url = "content://photo", uploadId = "stable-photo"))) }
  }

  @Test fun `valid publish responds immediately and success is authoritative and opens public offer`() = runTest(dispatcher) {
    val repo = Repo()
    val model = OfferEditorViewModel(repo, SavedStateHandle(), null)
    runCurrent(); fill(model)
    model.save()
    assertTrue(model.state.value.busy)
    assertEquals("Saving offer…", model.state.value.progress)
    model.save() // Repeated taps cannot start another write sequence.
    runCurrent()
    assertFalse(model.state.value.busy)
    assertTrue(model.state.value.saved)
    assertEquals("Offer published", model.state.value.savedMessage)
    assertEquals(repo.remote.slug, model.state.value.savedSlug)
    assertEquals(1, repo.createCalls)
  }

  @Test fun `partial upload failure keeps draft and known id for a safe retry`() = runTest(dispatcher) {
    val repo = Repo().apply { failUpload = true }
    val handle = SavedStateHandle()
    val model = OfferEditorViewModel(repo, handle, null)
    runCurrent(); fill(model); model.save(); runCurrent()
    assertFalse(model.state.value.saved)
    assertNotNull(model.state.value.error)
    assertEquals("offer-id", handle.get<String>("offer_id"))
    assertNotNull(handle.get<String>("draft"))
    repo.failUpload = false
    model.save(); runCurrent()
    assertTrue(model.state.value.saved)
    assertEquals(1, repo.createCalls)
    assertNull(handle.get<String>("draft"))
  }

  @Test fun `save deadline releases controls and preserves uncertain outcome and retry key`() = runTest(dispatcher) {
    val repo = Repo().apply { blocker = CompletableDeferred() }
    val handle = SavedStateHandle()
    val model = OfferEditorViewModel(repo, handle, null)
    runCurrent(); fill(model)
    val key = handle.get<String>("creation_key")
    model.save(); runCurrent()
    advanceTimeBy(180_001L); runCurrent()
    assertFalse(model.state.value.busy)
    assertFalse(model.state.value.saved)
    assertTrue(model.state.value.error!!.contains("may already be saved"))
    assertEquals(key, handle.get<String>("creation_key"))
    assertNotNull(handle.get<String>("draft"))
    repo.blocker = null
    model.save(); runCurrent()
    assertTrue(model.state.value.saved)
    assertEquals(key, repo.creationKeys.single())
  }

  @Test fun `cancel saving keeps entries and does not claim failure or publication`() = runTest(dispatcher) {
    val repo = Repo().apply { blocker = CompletableDeferred() }
    val model = OfferEditorViewModel(repo, SavedStateHandle(), null)
    runCurrent(); fill(model); model.save(); runCurrent()
    model.cancelSave(); runCurrent()
    assertFalse(model.state.value.busy)
    assertFalse(model.state.value.saved)
    assertEquals("PC for sale", model.state.value.title)
    assertTrue(model.state.value.error!!.contains("may already be saved"))
  }

  @Test fun `cancelled late response cannot overwrite a newer draft or claim success`() = runTest(dispatcher) {
    val gate = CompletableDeferred<Unit>()
    val repo = Repo().apply { blocker = gate; ignoreCancellation = true }
    val model = OfferEditorViewModel(repo, SavedStateHandle(), null)
    runCurrent(); fill(model); model.save(); runCurrent()
    model.cancelSave()
    model.change { it.copy(title = "A corrected title") }
    gate.complete(Unit); runCurrent()
    assertEquals("A corrected title", model.state.value.title)
    assertFalse(model.state.value.busy)
    assertFalse(model.state.value.saved)
  }

  @Test fun `private save and public edit have truthful distinct success messages`() = runTest(dispatcher) {
    val privateRepo = Repo()
    val privateModel = OfferEditorViewModel(privateRepo, SavedStateHandle(), null)
    runCurrent(); fill(privateModel)
    privateModel.change { it.copy(images = emptyList(), allowPrivateSave = true) }
    privateModel.save(); runCurrent()
    assertEquals("Private offer saved", privateModel.state.value.savedMessage)
    assertNull(privateModel.state.value.savedSlug)
    val editRepo = Repo().apply { remote = remote.copy(images = listOf(EditorImage("photo", "https://images.test/photo")), isPublic = true) }
    val editModel = OfferEditorViewModel(editRepo, SavedStateHandle(), "offer-id")
    runCurrent(); editModel.save(); runCurrent()
    assertEquals("Changes saved", editModel.state.value.savedMessage)
    assertEquals("genuine-offer-slug", editModel.state.value.savedSlug)
    assertEquals(0, editRepo.createCalls)
  }

  @Test fun `every non-saving submit blocker has a readable reason`() {
    val state = OfferEditorState(options = OfferFormOptions(emptyList(), emptyList(), emptyList(), "none"), loading = false)
    assertNotNull(OfferEditorViewModel.submissionBlocker(state, true, false))
    assertNotNull(OfferEditorViewModel.submissionBlocker(state, false, true))
    assertNotNull(OfferEditorViewModel.submissionBlocker(state.copy(loading = true), false, false))
    assertNotNull(OfferEditorViewModel.submissionBlocker(state.copy(options = null), false, false))
    assertNull(OfferEditorViewModel.submissionBlocker(state, false, false))
  }

  @Test fun `validation error stays explicit without starting a save`() = runTest(dispatcher) {
    val repo = Repo()
    val model = OfferEditorViewModel(repo, SavedStateHandle(), null)
    runCurrent(); model.save(); runCurrent()
    assertFalse(model.state.value.busy)
    assertNotNull(model.state.value.error)
    assertEquals(0, repo.createCalls)
  }
}
