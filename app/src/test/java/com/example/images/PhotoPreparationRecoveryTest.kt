package com.example.images

import java.io.IOException
import org.junit.Assert.*
import org.junit.Test

class PhotoPreparationRecoveryTest {
  @Test fun `saturation preserves captured inputs and reports honest manual retry`() {
    val inputs = mutableListOf("content://captures/new-photo")
    var message = ""
    val keptForRetry = recoverPhotoPreparation(PhotoReadSaturatedException(),
      discardInvalidInputs = { inputs.clear() }, report = { message = it })
    assertTrue(keptForRetry)
    assertEquals(listOf("content://captures/new-photo"), inputs)
    assertTrue(message.contains("kept for retry"))
    assertFalse(message.contains("JPEG"))
  }

  @Test fun `invalid bytes still discard pending inputs and report supported formats`() {
    val inputs = mutableListOf("content://captures/invalid-photo")
    var message = ""
    val keptForRetry = recoverPhotoPreparation(IOException("invalid image bytes"),
      discardInvalidInputs = { inputs.clear() }, report = { message = it })
    assertFalse(keptForRetry)
    assertTrue(inputs.isEmpty())
    assertTrue(message.contains("JPEG, PNG or WebP"))
  }
}
