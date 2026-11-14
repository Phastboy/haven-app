package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.OfferPlace
import com.example.location.AndroidDeviceLocationSource
import com.example.location.CapturedLocation
import com.example.location.OfferLocationCapture
import com.example.ui.model.OfferEditorState
import com.example.ui.model.SessionManager

/** Connects foreground permission/lifecycle to a reusable location editor and existing draft. */
@Composable
fun OfferLocationSection(
  editorKey: Any,
  state: OfferEditorState,
  onSelect: (OfferPlace?) -> Unit,
  onSearch: (String) -> Unit,
  sign: suspend (CapturedLocation) -> OfferPlace,
  onPendingChanged: (Boolean) -> Unit,
  cancelRequest: Int = 0,
) {
  val context = LocalContext.current
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val session by SessionManager.session.collectAsState()
  val scope = rememberCoroutineScope()
  val latestSelect by rememberUpdatedState(onSelect)
  val latestSign by rememberUpdatedState(sign)
  val actor = session.personId
  val capture = remember(editorKey, actor) {
    OfferLocationCapture(scope, AndroidDeviceLocationSource(context), { latestSign(it) }, {
      if (SessionManager.session.value.personId == actor) latestSelect(it)
    })
  }
  val captureState by capture.state.collectAsState()
  var permissionRequest by remember(capture) { mutableStateOf<Long?>(null) }
  var query by rememberSaveable { mutableStateOf("") }
  val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
    permissionRequest?.let { request ->
      permissionRequest = null
      capture.permissionResult(request, grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
        grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
    }
  }
  DisposableEffect(capture, lifecycle) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_STOP) { permissionRequest = null; capture.cancel() }
    }
    lifecycle.addObserver(observer)
    onDispose { lifecycle.removeObserver(observer); capture.cancel() }
  }
  LaunchedEffect(state.busy) { if (state.busy) capture.cancel() }
  LaunchedEffect(cancelRequest) { permissionRequest = null; capture.cancel() }
  SideEffect { onPendingChanged(captureState.pending) }
  OfferLocationEditor(
    state = state, query = query, onQueryChange = { query = it },
    captureState = captureState,
    onCapture = {
      val request = capture.begin()
      val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
      if (granted) capture.permissionResult(request, true) else {
        permissionRequest = request
        permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
      }
    },
    onCancelCapture = { permissionRequest = null; capture.cancel() },
    onSearch = { permissionRequest = null; capture.cancel(); onSearch(query) },
    onSelect = { place -> permissionRequest = null; capture.cancel(); onSelect(place) },
  )
}

