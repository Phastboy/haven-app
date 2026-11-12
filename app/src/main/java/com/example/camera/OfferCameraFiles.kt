package com.example.camera

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * Private, short-lived files backing one camera capture.
 *
 * Captures live under the app cache directory and are shared with the system
 * camera only through a dedicated [FileProvider] authority. The editor keeps
 * only the file name across recreation, so a pending capture can be recovered
 * without persisting an unsafe or stale URI grant.
 */
object OfferCameraFiles {
  /** Suffix appended to the application id by the manifest provider entry. */
  const val AUTHORITY_SUFFIX = ".captures"

  /** Abandoned captures older than this are deleted when the editor opens. */
  const val MAX_AGE_MS = 24L * 60L * 60L * 1000L

  const val MAX_IMAGE_BYTES = 5L * 1024L * 1024L

  private const val DIRECTORY = "offer-captures"

  fun authority(context: Context): String = context.packageName + AUTHORITY_SUFFIX

  /** Creates an empty capture file in the private cache directory. */
  fun create(context: Context): File {
    val directory = directory(context)
    return File(directory, "${UUID.randomUUID()}.jpg").also { it.createNewFile() }
  }

  /** Content URI the system camera may write to for [file]. */
  fun uri(context: Context, file: File): Uri = FileProvider.getUriForFile(context, authority(context), file)

  /**
   * Returns the provider URI for a confirmed capture, or null when the camera
   * produced no usable file. Empty output is deleted rather than added.
   */
  fun consume(context: Context, name: String): String? {
    val file = resolve(context, name) ?: return null
    if (!file.isFile || file.length() <= 0L || file.length() > MAX_IMAGE_BYTES) {
      file.delete()
      return null
    }
    return uri(context, file).toString()
  }

  /** Deletes a cancelled or failed capture. */
  fun cancel(context: Context, name: String) {
    resolve(context, name)?.delete()
  }

  /**
   * Deletes abandoned captures older than [MAX_AGE_MS]. Referenced draft
   * images and the pending capture are preserved regardless of age.
   */
  fun cleanUp(context: Context, keep: Set<String>, now: Long = System.currentTimeMillis()) {
    val cutoff = now - MAX_AGE_MS
    directory(context).listFiles()?.forEach { file ->
      if (file.isFile && file.name !in keep && file.lastModified() < cutoff) file.delete()
    }
  }

  /**
   * Deletes the capture behind [uri] when it belongs to this editor. Gallery
   * and remote images are ignored because their names never resolve here.
   */
  fun deleteCapture(context: Context, uri: String) {
    fileName(context, uri)?.let { cancel(context, it) }
  }

  /** File name of one of this editor's capture URIs, or null for another source. */
  fun fileName(context: Context, uri: String): String? {
    val parsed = Uri.parse(uri)
    if (parsed.scheme != ContentResolver.SCHEME_CONTENT) return null
    if (parsed.authority != authority(context)) return null
    return parsed.lastPathSegment?.takeIf(::isSafeName)
  }

  private fun directory(context: Context): File =
    File(context.cacheDir, DIRECTORY).apply { mkdirs() }

  private fun resolve(context: Context, name: String): File? {
    if (!isSafeName(name)) return null
    val directory = directory(context)
    return try {
      File(directory, name).takeIf { it.parentFile?.canonicalFile == directory.canonicalFile }
    } catch (_: Exception) {
      null
    }
  }

  private fun isSafeName(name: String): Boolean =
    name.isNotBlank() && name == File(name).name && name != "." && name != ".."
}
