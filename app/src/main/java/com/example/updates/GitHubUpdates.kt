package com.example.updates

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.example.BuildConfig
import com.example.data.network.HavenCioClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.utils.io.readAvailable
import io.ktor.client.statement.bodyAsChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Public GitHub traffic never carries Haven credentials. Installation remains an Android action. */
data class AvailableUpdate(
  val versionCode: Long, val versionName: String, val notes: String,
  val apkUrl: String, val sha256: String, val size: Long
)

/** Stable GitHub release discovery and verified, opt-in APK preparation. */
object GitHubUpdates {
  private const val REPOSITORY = "https://github.com/Phastboy/haven-app/releases/download/"
  private const val MAX_APK = 150L * 1024 * 1024

  /** Reject foreign download origins, drafts, previews and ambiguous artifacts. */
  fun parseRelease(release: JSONObject, metadata: JSONObject, installedCode: Long): AvailableUpdate? {
    if (release.getBoolean("draft") || release.getBoolean("prerelease")) return null
    val code = metadata.getLong("versionCode")
    if (code <= installedCode) return null
    val name = metadata.getString("apkName")
    require(name.matches(Regex("haven-v[0-9]+\\.[0-9]+\\.[0-9]+\\.apk")))
    val sha = metadata.getString("sha256")
    require(sha.matches(Regex("[a-f0-9]{64}")))
    val size = metadata.getLong("size")
    require(size in 1..MAX_APK)
    val assets = release.getJSONArray("assets")
    val matching = (0 until assets.length()).map { assets.getJSONObject(it) }.filter { it.getString("name") == name }
    require(matching.size == 1)
    val url = matching.single().getString("browser_download_url")
    require(url == "$REPOSITORY${release.getString("tag_name")}/$name")
    require(matching.single().getLong("size") == size)
    return AvailableUpdate(code, metadata.getString("versionName"), release.optString("body").take(6000), url, sha, size)
  }

  /** Return a newer stable release with supported metadata, or null for older release formats. */
  suspend fun check(): AvailableUpdate? = withContext(Dispatchers.IO) {
    val release = JSONObject(fetchBytes("https://api.github.com/repos/Phastboy/haven-app/releases/latest", 512 * 1024).decodeToString())
    if (release.getBoolean("draft") || release.getBoolean("prerelease")) return@withContext null
    val assets = release.getJSONArray("assets")
    val metadata = (0 until assets.length()).map { assets.getJSONObject(it) }.singleOrNull { it.getString("name") == "update.json" }
      ?: return@withContext null // Older releases did not publish update metadata.
    val url = metadata.getString("browser_download_url")
    require(url == "$REPOSITORY${release.getString("tag_name")}/update.json")
    parseRelease(release, JSONObject(fetchBytes(url, 16 * 1024).decodeToString()), BuildConfig.VERSION_CODE.toLong())
  }

  /** Read bounded public metadata; reject unsuccessful or oversized responses. */
  private suspend fun fetchBytes(url: String, limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    HavenCioClient.imageClient.prepareGet(url) { header("User-Agent", "Haven-Android/${BuildConfig.VERSION_NAME}") }.execute { response ->
      check(response.status.value == 200)
      val channel = response.bodyAsChannel()
      val buffer = ByteArray(8192)
      while (true) {
        val count = channel.readAvailable(buffer, 0, buffer.size)
        if (count == -1) break
        check(output.size() + count <= limit)
        output.write(buffer, 0, count)
      }
    }
    return output.toByteArray()
  }

  /** Stream into private storage, bound size, then verify digest and Android identity. */
  suspend fun download(context: Context, update: AvailableUpdate): File = withContext(Dispatchers.IO) {
    val directory = File(context.cacheDir, "updates").apply { mkdirs() }
    val file = File(directory, "haven-${update.versionCode}.apk")
    val partial = File.createTempFile("download-", ".part", directory)
    try {
      val digest = MessageDigest.getInstance("SHA-256")
      var size = 0L
      partial.outputStream().use { output ->
        HavenCioClient.imageClient.prepareGet(update.apkUrl) {
          timeout { requestTimeoutMillis = 15 * 60 * 1000L }
        }.execute { response ->
          check(response.status.value == 200)
          val channel = response.bodyAsChannel()
          val buffer = ByteArray(8192)
          while (true) {
            val count = channel.readAvailable(buffer, 0, buffer.size)
            if (count == -1) break
            size += count
            check(size <= update.size && size <= MAX_APK)
            digest.update(buffer, 0, count)
            output.write(buffer, 0, count)
          }
        }
      }
      check(size == update.size)
      check(digest.digest().joinToString("") { "%02x".format(it) } == update.sha256)
      verifyIdentity(context, partial, update)
      check(partial.renameTo(file))
      file
    } finally {
      partial.delete()
    }
  }

  /** Require a newer APK for this package with the installed certificate set. */
  // Legacy signatures are required only on API 24–27; modern devices use SigningInfo.
  @Suppress("DEPRECATION")
  private fun verifyIdentity(context: Context, file: File, update: AvailableUpdate) {
    val pm = context.packageManager
    val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
    val archive = checkNotNull(pm.getPackageArchiveInfo(file.path, flags))
    check(archive.packageName == context.packageName)
    check(PackageInfoCompat.getLongVersionCode(archive) == update.versionCode)
    check(archive.versionName == update.versionName)
    val installed = pm.getPackageInfo(context.packageName, flags)
    val currentCode = PackageInfoCompat.getLongVersionCode(installed)
    check(update.versionCode > currentCode)
    val candidates = if (Build.VERSION.SDK_INT >= 28) archive.signingInfo?.apkContentsSigners else archive.signatures
    val current = if (Build.VERSION.SDK_INT >= 28) installed.signingInfo?.apkContentsSigners else installed.signatures
    check(!candidates.isNullOrEmpty() && !current.isNullOrEmpty())
    // Require identical signer sets; Android's installer performs its own signature verification.
    check(candidates.map { it.toCharsString() }.toSet() == current.map { it.toCharsString() }.toSet())
  }

  /** Hand a private APK URI to Android; installation still requires confirmation. */
  fun install(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
    context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
      .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
  }
}
