package com.example.ui.model

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.auth.HavenAuthClient
import com.example.data.HavenOfferEditorRepository
import com.example.data.OfferEditorRepository
import com.example.data.OfferPlace
import com.example.data.OfferFormOptions
import com.example.data.EditorImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class OfferEditorState(
  val title: String = "", val description: String = "", val category: String = "",
  val price: String = "", val currency: String = "NGN", val place: OfferPlace? = null,
  val images: List<EditorImage> = emptyList(), val options: OfferFormOptions? = null,
  val loading: Boolean = true, val busy: Boolean = false, val dirty: Boolean = false,
  val searching: Boolean = false, val places: List<OfferPlace> = emptyList(),
  val error: String? = null, val placeError: String? = null, val progress: String? = null,
  val allowPrivateSave: Boolean = false,
  val saved: Boolean = false,
  val savedSlug: String? = null,
  val savedMessage: String? = null
)

class OfferEditorViewModel(
  private val repository: OfferEditorRepository,
  private val handle: SavedStateHandle,
  private val initialId: String?
) : ViewModel() {
  private val mutable = MutableStateFlow(OfferEditorState())
  val state = mutable.asStateFlow()
  private var offerId: String? = handle["offer_id"] ?: initialId
  private val creationKey: String = handle.get<String>("creation_key") ?: UUID.randomUUID().toString().also { handle["creation_key"] = it }
  private var saveJob: Job? = null
  private var saveGeneration = 0L
  val captureKey: String get() = creationKey
  init { load() }

  fun load() {
    if (mutable.value.busy) return
    mutable.update { it.copy(loading = true, error = null) }
    viewModelScope.launch {
      try {
        val options = withTimeout(30_000L) { repository.options() }
        val restored = handle.get<String>("draft")
        if (restored != null) {
          val draft = JSONObject(restored)
          val location = draft.optJSONObject("location")
          val images = draft.getJSONArray("images")
          mutable.value = OfferEditorState(
            title = draft.getString("title"), description = draft.getString("description"),
            category = draft.getString("category"), price = draft.getString("price"), currency = draft.getString("currency"),
            place = location?.let { OfferPlace(it.getString("token"), it.getString("label"), it.optString("attribution").takeIf { text -> text.isNotBlank() }, it.optString("source_url").takeIf { url -> url == "https://www.geoapify.com/" }) },
            allowPrivateSave = draft.optBoolean("allow_private_save", false),
            images = List(images.length()) { images.getJSONObject(it).let { image -> EditorImage(
              id = image.optString("id").takeIf { value -> value.isNotBlank() }, url = image.getString("url"), uploadId = image.getString("upload_id")
            ) } }, options = options, loading = false, dirty = true
          )
        } else if (initialId != null) {
          val offer = withTimeout(30_000L) { repository.load(initialId) }
          mutable.value = OfferEditorState(title = offer.title, description = offer.description, category = offer.category,
            price = offer.price, currency = offer.currency, place = offer.place, images = offer.images, options = options, loading = false, allowPrivateSave = !offer.isPublic)
        } else {
          mutable.value = OfferEditorState(options = options, loading = false,
            place = options.recentPlaces.firstOrNull { it.token == options.defaultPlaceToken })
        }
      } catch (_: TimeoutCancellationException) { mutable.update { it.copy(loading = false, error = "Loading the offer form took too long. Please retry.") } }
      catch (cancelled: CancellationException) { throw cancelled }
      catch (_: Exception) { mutable.update { it.copy(loading = false, error = "Could not load the offer form. Please retry.") } }
    }
  }

  private fun retain() {
    val state = mutable.value
    handle["draft"] = JSONObject().put("title", state.title).put("description", state.description)
      .put("category", state.category).put("price", state.price).put("currency", state.currency).put("allow_private_save", state.allowPrivateSave)
      .put("location", state.place?.let { JSONObject().put("token", it.token).put("label", it.label).put("attribution", it.attribution).put("source_url", it.sourceUrl) })
      .put("images", JSONArray(state.images.map { image -> JSONObject().put("id", image.id ?: "").put("url", image.url).put("upload_id", image.uploadId) })).toString()
  }
  fun change(transform: (OfferEditorState) -> OfferEditorState) {
    if (mutable.value.busy || mutable.value.loading) return
    mutable.update { transform(it).copy(dirty = true, error = null) }
    retain()
  }
  suspend fun capturedPlace(point: com.example.location.CapturedLocation): OfferPlace = repository.capturedPlace(point)

  fun showError(message: String) { mutable.update { it.copy(error = message) } }

  fun addImages(uris: List<String>) {
    if (mutable.value.images.size + uris.size > 5) {
      mutable.update { it.copy(error = "Choose at most five photos in total.") }; return
    }
    change { it.copy(images = it.images + uris.map { uri -> EditorImage(url = uri) }) }
  }
  fun moveImage(index: Int, delta: Int) = change { state ->
    val images = state.images.toMutableList()
    val next = index + delta
    if (index in images.indices && next in images.indices) java.util.Collections.swap(images, index, next)
    state.copy(images = images)
  }
  fun search(query: String) {
    if (mutable.value.searching || mutable.value.busy) return
    mutable.update { it.copy(searching = true, placeError = null) }
    viewModelScope.launch {
      try { val places = withTimeout(15_000L) { repository.search(query) }; mutable.update { it.copy(places = places, searching = false) } }
      catch (_: TimeoutCancellationException) { mutable.update { it.copy(searching = false, placeError = "Place search took too long. Retry or continue without location.") } }
      catch (cancelled: CancellationException) { throw cancelled }
      catch (_: Exception) { mutable.update { it.copy(searching = false, placeError = "Place search is unavailable. Retry, reuse a recent place, or save without a location.") } }
    }
  }
  fun save() {
    val draft = mutable.value
    if (draft.busy || draft.loading || draft.options == null) return
    val error = validate(draft)
    if (error != null) { mutable.update { it.copy(error = error) }; return }
    retain()
    mutable.update { it.copy(busy = true, error = null, progress = "Saving offer…") }
    val generation = ++saveGeneration
    saveJob = viewModelScope.launch {
      try {
        withTimeout(180_000L) {
        suspend fun <T> step(action: suspend () -> T): T {
          currentCoroutineContext().ensureActive()
          val value = action()
          currentCoroutineContext().ensureActive()
          if (generation != saveGeneration) throw CancellationException("Save replaced")
          return value
        }
        val input = JSONObject().put("title", draft.title.trim()).put("description", draft.description.trim())
          .put("category", draft.category).put("price", draft.price.trim()).put("currency", draft.currency)
          .put("place_token", draft.place?.token ?: "none").put("idempotency_key", creationKey)
        val id = offerId ?: step { repository.save(null, input) }.id.also { offerId = it; handle["offer_id"] = it }
        var remote = step { repository.load(id) }
        var desired = reconcileUploadedImages(draft.images, remote.images)
        mutable.update { it.copy(images = desired, allowPrivateSave = !remote.isPublic) }; retain()
        // Free slots without removing the last published photo; replacement uploads then make the final removal safe.
        for (image in remote.images.filter { current -> desired.none { it.id == current.id } }) {
          if (remote.images.size <= 1) break
          remote = step { repository.remove(id, image.id!!) }
        }
        for (image in desired.filter { it.id == null }) {
          mutable.update { it.copy(progress = "Uploading photo…") }
          val before = remote.images.map { it.id }.toSet()
          remote = step { repository.upload(id, image) }
          val uploaded = remote.images.firstOrNull { it.url.endsWith("/${image.uploadId}") }
            ?: remote.images.filter { it.id !in before }.singleOrNull()
            ?: throw java.io.IOException("Could not confirm the uploaded photo")
          desired = desired.map { if (it.uploadId == image.uploadId) uploaded else it }
          mutable.update { it.copy(images = desired, allowPrivateSave = !remote.isPublic) }; retain()
          for (old in remote.images.filter { current -> desired.none { it.id == current.id } }) {
            if (remote.images.size > 1) remote = step { repository.remove(id, old.id!!) }
          }
        }
        for (image in remote.images.filter { current -> desired.none { it.id == current.id } }) {
          remote = step { repository.remove(id, image.id!!) }
        }
        if (desired.isNotEmpty()) step { repository.reorder(id, desired) }
        mutable.update { it.copy(progress = "Confirming your offer…") }
        val confirmed = step { repository.save(id, input) }
        if (desired.isNotEmpty() && !confirmed.isPublic) throw java.io.IOException("Publication was not confirmed")
        if (generation != saveGeneration) throw CancellationException("Save replaced")
        handle.remove<String>("draft")
        mutable.update { it.copy(busy = false, dirty = false, progress = null, saved = true,
          savedSlug = confirmed.slug.takeIf { confirmed.isPublic },
          savedMessage = if (!confirmed.isPublic) "Private offer saved" else if (initialId != null) "Changes saved" else "Offer published") }
        }
      } catch (_: TimeoutCancellationException) {
        if (generation == saveGeneration) saveInterrupted("Saving took too long. The offer may already be saved. Your entries are retained; retry safely to finish or check My offers.")
      } catch (cancelled: CancellationException) { throw cancelled }
      catch (_: Exception) {
        if (generation == saveGeneration) saveInterrupted("Could not finish saving. Some changes may already be saved. Your entries are retained; retry to finish. If a place selection expired, choose it again.")
      }
    }
  }

  /** A cancelled response is not proof the server rejected a write. Keep retry identities. */
  fun cancelSave() {
    if (!mutable.value.busy) return
    saveGeneration++
    saveJob?.cancel()
    saveInterrupted("Saving stopped. The offer may already be saved. Your entries are retained; retry safely or check My offers.")
  }

  private fun saveInterrupted(message: String) {
    mutable.update { it.copy(busy = false, progress = null, error = message) }
    retain()
  }

  companion object {
    fun submissionBlocker(state: OfferEditorState, locationPending: Boolean, photoPending: Boolean): String? = when {
      state.loading -> "Loading the offer form…"
      state.options == null -> "The offer form could not load. Retry to continue."
      state.busy -> null
      locationPending -> "Finding your location. Cancel the lookup to continue without waiting."
      photoPending -> "Preparing your photo. Cancel preparation to use the photos already in your draft."
      else -> null
    }

    fun reconcileUploadedImages(desired: List<EditorImage>, remote: List<EditorImage>): List<EditorImage> =
      desired.map { image ->
        if (image.id == null) remote.firstOrNull { it.url.endsWith("/${image.uploadId}") } ?: image else image
      }

    fun validate(state: OfferEditorState): String? = when {
      state.title.trim().codePointCount(0, state.title.trim().length) !in 3..50 -> "Use a title between 3 and 50 characters."
      state.description.trim().codePointCount(0, state.description.trim().length) !in 10..2000 -> "Use a description between 10 and 2,000 characters."
      state.options?.categories?.none { it.first == state.category } != false -> "Choose a category."
      state.price.trim().toIntOrNull()?.let { it >= 0 } != true -> "Enter a whole price in minor units, zero for free."
      state.currency !in state.options.currencies -> "Choose a currency."
      state.images.size > 5 || (state.images.isEmpty() && !state.allowPrivateSave) -> "Add between one and five photos to publish."
      else -> null
    }
    fun factory(context: Context, id: String?, ownerPersonId: String? = com.example.ui.model.SessionManager.session.value.personId): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        OfferEditorViewModel(HavenOfferEditorRepository(HavenAuthClient.shared(context), context.applicationContext, ownerPersonId), extras.createSavedStateHandle(), id) as T
    }
  }
}
