package com.example.updates

import android.content.Intent
import androidx.core.net.toUri
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

/** Checks once per activity composition; unavailable GitHub never blocks opening Haven. */
@Composable
fun UpdatePrompt() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var update by remember { mutableStateOf<AvailableUpdate?>(null) }
  var dismissed by rememberSaveable { mutableStateOf(false) }
  var downloading by remember { mutableStateOf(false) }
  var downloaded by remember { mutableStateOf<File?>(null) }
  var error by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(Unit) {
    try { update = GitHubUpdates.check() }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { /* Retry on the next launch; the feed stays usable. */ }
  }
  val available = update ?: return
  if (dismissed) return
  AlertDialog(
    onDismissRequest = { if (!downloading) dismissed = true },
    title = { Text("Haven ${available.versionName} is available") },
    text = { Text(error ?: if (downloading) "Downloading update…" else available.notes.ifBlank { "A new version of Haven is ready." }) },
    dismissButton = { TextButton(enabled = !downloading, onClick = { dismissed = true }) { Text("Later") } },
    confirmButton = {
      TextButton(enabled = !downloading, onClick = {
        scope.launch {
          error = null
          downloading = true
          try {
            val apk = downloaded ?: GitHubUpdates.download(context, available).also { downloaded = it }
            if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
              error = "Allow Haven to install updates, then return and tap Install."
              context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:${context.packageName}".toUri()))
            } else {
              GitHubUpdates.install(context, apk)
            }
          } catch (cancelled: CancellationException) { throw cancelled }
          catch (_: Exception) { error = "Couldn’t prepare the update. Check your connection and try again."; downloaded = null }
          finally { downloading = false }
        }
      }) { Text(if (downloaded == null) "Download update" else "Install") }
    }
  )
}
