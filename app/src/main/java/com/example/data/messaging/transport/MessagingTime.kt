package com.example.data.messaging.transport

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Shared ISO-8601 formatting for server timestamps. */
object MessagingTime {

  private const val ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX"

  fun parseIsoToMillis(isoString: String): Long {
    val normalized = isoString.replace(Regex("\\.\\d+(?=(?:Z|[+-]\\d{2}:?\\d{2})$)"), "")
    return runCatching {
      SimpleDateFormat(ISO_PATTERN, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
      }.parse(normalized)?.time
    }.getOrNull() ?: System.currentTimeMillis()
  }

  fun formatIso(epochMillis: Long): String {
    return SimpleDateFormat(ISO_PATTERN, Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(epochMillis))
  }

  fun formatTimeLabel(epochMillis: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMillis))
  }
}
