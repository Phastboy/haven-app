package com.example.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import java.io.File
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

/**
 * Verifies the camera capture file contract: app-private storage, a narrowly
 * scoped provider URI, confirmation/cancel/empty handling, recreation recovery
 * by file name, and safe cleanup of abandoned captures.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OfferCameraFilesTest {
  private val context: Context get() = ApplicationProvider.getApplicationContext()
  private val captureDirectory: File get() = File(context.cacheDir, "offer-captures")

  @Before fun clearCaptures() {
    captureDirectory.deleteRecursively()
  }

  @After fun removeCaptures() {
    captureDirectory.deleteRecursively()
  }

  private fun capturedFile(): File =
    OfferCameraFiles.create(context).also { it.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())) }

  @Test fun capturesStayInsideTheApplicationCache() {
    val file = OfferCameraFiles.create(context)
    assertTrue(file.isFile)
    assertEquals(captureDirectory.canonicalFile, file.parentFile?.canonicalFile)
    assertTrue(file.canonicalPath.startsWith(context.cacheDir.canonicalPath + File.separator))
  }

  @Test fun providerUriUsesOnlyTheCaptureAuthorityAndDirectory() {
    val file = capturedFile()
    val uri = OfferCameraFiles.uri(context, file)
    assertEquals("content", uri.scheme)
    assertEquals("${context.packageName}.captures", uri.authority)
    assertEquals(file.name, uri.lastPathSegment)
    assertEquals("/offer_captures/${file.name}", uri.path)
  }

  @Test fun confirmedCaptureBecomesAProviderContentUri() {
    val file = capturedFile()
    val uri = OfferCameraFiles.consume(context, file.name)
    assertNotNull(uri)
    assertEquals(OfferCameraFiles.uri(context, file).toString(), uri)
    assertTrue("confirmed file stays available for upload", file.exists())
  }

  @Test fun cancellationDeletesTheCaptureAndAddsNothing() {
    val file = capturedFile()
    OfferCameraFiles.cancel(context, file.name)
    assertFalse(file.exists())
    assertNull(OfferCameraFiles.consume(context, file.name))
  }

  @Test fun emptyCaptureIsRejectedAndDeleted() {
    val file = OfferCameraFiles.create(context)
    assertEquals(0L, file.length())
    assertNull(OfferCameraFiles.consume(context, file.name))
    assertFalse(file.exists())
  }

  @Test fun pendingCaptureSurvivesRecreationThroughItsFileName() {
    val file = capturedFile()
    val restoredName = file.name
    val restoredUri = OfferCameraFiles.consume(context, restoredName)
    assertNotNull("a recreated editor can rebuild the capture URI", restoredUri)
    assertTrue(restoredUri!!.endsWith("/${file.name}"))
  }

  @Test fun cleanupRemovesOnlyStaleUnreferencedCaptures() {
    val now = System.currentTimeMillis()
    val stale = capturedFile().apply { setLastModified(now - OfferCameraFiles.MAX_AGE_MS - 1_000) }
    val referenced = capturedFile().apply { setLastModified(now - OfferCameraFiles.MAX_AGE_MS - 1_000) }
    val pending = capturedFile().apply { setLastModified(now - OfferCameraFiles.MAX_AGE_MS - 1_000) }
    val recent = capturedFile()

    OfferCameraFiles.cleanUp(context, keep = setOf(referenced.name, pending.name), now = now)

    assertFalse("abandoned capture is deleted", stale.exists())
    assertTrue("draft image is kept", referenced.exists())
    assertTrue("pending capture is kept", pending.exists())
    assertTrue("recent capture is kept", recent.exists())
  }

  @Test fun unsafeNamesCannotEscapeTheCaptureDirectory() {
    val outside = File(context.cacheDir, "not-a-capture.jpg").apply { writeText("private") }
    for (name in listOf("../not-a-capture.jpg", "nested/not-a-capture.jpg", "", ".", "..")) {
      assertNull(OfferCameraFiles.consume(context, name))
      OfferCameraFiles.cancel(context, name)
    }
    assertTrue("path traversal cannot delete unrelated cache files", outside.exists())
  }

  @Test fun onlyThisEditorsProviderUrisAreTreatedAsCaptures() {
    val file = capturedFile()
    assertNull(OfferCameraFiles.fileName(context, "content://media/external/images/${file.name}"))
    assertNull(OfferCameraFiles.fileName(context, "https://cdn.example/offer.jpg"))
    assertEquals(file.name, OfferCameraFiles.fileName(context, OfferCameraFiles.uri(context, file).toString()))
  }

  @Test fun oversizedCaptureIsRejectedBeforeEnteringDraft() {
    val file = capturedFile()
    java.io.RandomAccessFile(file, "rw").use { it.setLength(OfferCameraFiles.MAX_IMAGE_BYTES + 1) }
    assertNull(OfferCameraFiles.consume(context, file.name))
    assertFalse(file.exists())
  }

  @Test fun cameraAndUpdateProvidersHaveIndependentComponentIdentities() {
    val camera = context.packageManager.resolveContentProvider(OfferCameraFiles.authority(context), 0)!!
    val updates = context.packageManager.resolveContentProvider(context.packageName + ".updates", 0)!!
    assertFalse(camera.name == updates.name)
    val file = File(context.cacheDir, "updates/test.apk").apply { parentFile?.mkdirs(); writeText("apk") }
    val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".updates", file)
    assertEquals("/updates/test.apk", uri.path)
    assertNotNull(OfferCameraFiles.consume(context, capturedFile().name))
  }

  @Test fun sharedPhotoPreflightRejectsOversizedAndInvalidBytes() {
    val file = capturedFile()
    OfferImageInputs.validate(context.contentResolver, OfferCameraFiles.uri(context, file).toString())
    java.io.RandomAccessFile(file, "rw").use { it.setLength(OfferCameraFiles.MAX_IMAGE_BYTES + 1) }
    try {
      OfferImageInputs.validate(context.contentResolver, OfferCameraFiles.uri(context, file).toString())
      throw AssertionError("oversized photo accepted")
    } catch (_: java.io.IOException) { }
    file.writeText("not a photo")
    try {
      OfferImageInputs.validate(context.contentResolver, OfferCameraFiles.uri(context, file).toString())
      throw AssertionError("invalid photo accepted")
    } catch (_: java.io.IOException) { }
  }

  @Test fun cameraCaptureNeedsNoAppPermission() {
    @Suppress("DEPRECATION")
    val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
    val requested = info.requestedPermissions.orEmpty().toSet()
    assertFalse("system camera owns its permission", requested.contains(Manifest.permission.CAMERA))
    assertFalse(requested.contains(Manifest.permission.READ_EXTERNAL_STORAGE))
    assertFalse(requested.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE))
  }
}
