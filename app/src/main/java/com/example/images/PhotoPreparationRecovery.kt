package com.example.images

import java.io.IOException

/** Reports an input failure, returning whether the pending inputs must be kept for manual retry. */
internal fun recoverPhotoPreparation(
  failure: IOException,
  discardInvalidInputs: () -> Unit,
  report: (String) -> Unit,
): Boolean {
  if (failure is PhotoReadSaturatedException) {
    report(failure.message ?: "Photo preparation is busy. Your photos are kept for retry.")
    return true
  }
  discardInvalidInputs()
  report("Choose JPEG, PNG or WebP photos up to 5 MB. Your existing photos are unchanged.")
  return false
}
