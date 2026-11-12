package com.example.images

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import androidx.core.net.toUri
import java.io.IOException

/** Shared preflight for camera/gallery bytes before they enter the offer draft. */
object OfferImageInputs {
  const val MAX_IMAGE_BYTES = 5L * 1024L * 1024L

  /** Reads at most limit+1 bytes; unknown or misleading provider sizes cannot bypass the limit. */
  fun validate(resolver: ContentResolver, uri: String) {
    resolver.openInputStream(uri.toUri())?.use { stream ->
      val bytes = ByteArrayOutputStream()
      val header = ByteArray(12)
      var headerSize = 0
      var total = 0L
      val chunk = ByteArray(8192)
      while (true) {
        val count = stream.read(chunk)
        if (count < 0) break
        total += count
        if (total > MAX_IMAGE_BYTES) throw IOException("Choose photos up to 5 MB.")
        bytes.write(chunk, 0, count)
        val copy = minOf(count, header.size - headerSize)
        if (copy > 0) { chunk.copyInto(header, headerSize, 0, copy); headerSize += copy }
      }
      val jpeg = headerSize >= 3 && header[0] == 0xff.toByte() && header[1] == 0xd8.toByte() && header[2] == 0xff.toByte()
      val png = headerSize >= 8 && header.take(8) == listOf(137, 80, 78, 71, 13, 10, 26, 10).map { it.toByte() }
      val webp = headerSize >= 12 && String(header, 0, 4, Charsets.US_ASCII) == "RIFF" && String(header, 8, 4, Charsets.US_ASCII) == "WEBP"
      if (!jpeg && !png && !webp) throw IOException("Choose a JPEG, PNG or WebP photo.")
      val data = bytes.toByteArray()
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("This photo is incomplete or cannot be decoded.")
      var sample = 1
      while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
      val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
      }
      val decoded = BitmapFactory.decodeByteArray(data, 0, data.size, options)
        ?: throw IOException("This photo is incomplete or cannot be decoded.")
      decoded.recycle()
    } ?: throw IOException("Could not open this photo. Choose it again.")
  }
}
