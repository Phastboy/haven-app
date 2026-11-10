package com.example

import com.example.updates.GitHubUpdates
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@org.robolectric.annotation.Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class GitHubUpdatesTest {
  private fun release(url: String = "https://github.com/Phastboy/haven-app/releases/download/v0.0.5/haven-v0.0.5.apk") = JSONObject()
    .put("draft", false).put("prerelease", false).put("tag_name", "v0.0.5")
    .put("assets", org.json.JSONArray().put(JSONObject().put("name", "haven-v0.0.5.apk").put("size", 123).put("browser_download_url", url)))
  private fun metadata() = JSONObject().put("versionCode", 15).put("versionName", "0.0.5")
    .put("apkName", "haven-v0.0.5.apk").put("size", 123).put("sha256", "a".repeat(64))

  @Test fun newerStableReleaseIsOffered() {
    assertEquals(15L, GitHubUpdates.parseRelease(release(), metadata(), 14)?.versionCode)
  }
  @Test fun installedOrOlderReleaseIsIgnored() {
    assertNull(GitHubUpdates.parseRelease(release(), metadata(), 15))
    assertNull(GitHubUpdates.parseRelease(release(), metadata(), 16))
  }
  @Test fun draftsAndPreviewsAreIgnored() {
    assertNull(GitHubUpdates.parseRelease(release().put("draft", true), metadata(), 14))
    assertNull(GitHubUpdates.parseRelease(release().put("prerelease", true), metadata(), 14))
  }
  @Test(expected = IllegalArgumentException::class) fun foreignApkIsRejected() {
    GitHubUpdates.parseRelease(release("https://example.com/app.apk"), metadata(), 14)
  }
  @Test(expected = IllegalArgumentException::class) fun oversizedApkIsRejected() {
    GitHubUpdates.parseRelease(release(), metadata().put("size", 200L * 1024 * 1024), 14)
  }
  @Test(expected = IllegalArgumentException::class) fun malformedDigestIsRejected() {
    GitHubUpdates.parseRelease(release(), metadata().put("sha256", "broken"), 14)
  }
}
