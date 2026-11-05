package com.example

import com.example.data.EditorImage
import com.example.data.OfferFormOptions
import com.example.ui.model.OfferEditorState
import com.example.ui.model.OfferEditorViewModel
import org.junit.Assert.*
import org.junit.Test

class OfferEditorValidationTest {
  private fun valid() = OfferEditorState(
    title = "House in Ibadan", description = "A house available for rent.", category = "housing-real-estate",
    price = "150000", currency = "NGN", images = listOf(EditorImage(url = "content://photo")),
    options = OfferFormOptions(listOf("housing-real-estate" to "Housing & real estate"), listOf("NGN"), emptyList(), ""), loading = false
  )
  @Test fun lostUploadAcknowledgementKeepsTheConfirmedPhoto() {
    val pending = EditorImage(url = "content://photo", uploadId = "stable-upload")
    val existing = EditorImage(id = "existing", url = "https://cdn/existing")
    val uploaded = EditorImage(id = "uploaded", url = "https://cdn/offers/offer/stable-upload")
    val recovered = OfferEditorViewModel.reconcileUploadedImages(listOf(existing, pending), listOf(existing, uploaded))
    assertEquals(listOf("existing", "uploaded"), recovered.map { it.id })
    assertEquals(existing, recovered.first())
    assertEquals(listOf(pending), OfferEditorViewModel.reconcileUploadedImages(listOf(pending), emptyList()))
  }
  @Test fun acceptsFreeAndPaidOffersWithoutRequiringLocation() {
    assertNull(OfferEditorViewModel.validate(valid()))
    assertNull(OfferEditorViewModel.validate(valid().copy(price = "0")))
  }
  @Test fun rejectsInvalidMoneyAndMissingPicturesBeforeSubmitting() {
    for (price in listOf("-1", "1.5", "", "2147483648")) assertNotNull(OfferEditorViewModel.validate(valid().copy(price = price)))
    assertNotNull(OfferEditorViewModel.validate(valid().copy(images = emptyList())))
    assertNotNull(OfferEditorViewModel.validate(valid().copy(images = List(6) { EditorImage(url = "content://photo/$it") })))
  }
  @Test fun usesBackendCharacterBoundsIncludingUnicode() {
    assertNull(OfferEditorViewModel.validate(valid().copy(title = "🏡".repeat(50))))
    assertNotNull(OfferEditorViewModel.validate(valid().copy(title = "a".repeat(51))))
    assertNotNull(OfferEditorViewModel.validate(valid().copy(description = "short")))
    assertNotNull(OfferEditorViewModel.validate(valid().copy(category = "invented")))
  }
}
