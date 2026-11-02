package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.example.data.HavenOfferDetailRepository
import com.example.data.OfferDetail
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.browser.customtabs.CustomTabsIntent
import coil.compose.SubcomposeAsyncImage
import com.example.auth.HavenAuthClient
import com.example.data.FeedOffer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.ui.model.ProfileViewModel
import com.example.ui.model.MessagesInboxViewModel
import com.example.ui.components.FeedOfferCard
import com.example.ui.components.OfferCardSkeleton
import com.example.ui.components.shimmerBackground
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.HavenBottomBarContainer
import com.example.ui.components.HavenTopBarContainer
import com.example.ui.model.OffersViewModel
import com.example.ui.model.SessionManager
import com.example.ui.theme.HavenColors
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.ThemeMode

/** Public discovery from Haven. Adjacent journeys open their real web pages. */
@Composable
fun OffersScreen(
  viewModel: OffersViewModel,
  onNavigateBack: () -> Unit,
  onNavigateToSignIn: () -> Unit,
  onSignOut: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val state by viewModel.uiState.collectAsState()
  val session by SessionManager.session.collectAsState()
  val uriHandler = LocalUriHandler.current
  val base = "https://${HavenAuthClient.HOST}"
  var showProfile by remember { mutableStateOf(false) }
  var showMessages by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val detailRepository = remember(context.applicationContext) { HavenOfferDetailRepository(HavenAuthClient.shared(context.applicationContext)) }
  val scope = rememberCoroutineScope()
  var selectedSlug by remember { mutableStateOf<String?>(null) }
  var detail by remember(session.personId) { mutableStateOf<OfferDetail?>(null) }
  var detailLoading by remember { mutableStateOf(false) }
  var detailError by remember { mutableStateOf<String?>(null) }
  var detailRetry by remember { mutableStateOf(0) }
  var savedStates by remember(session.personId) { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
  var savingSlugs by remember(session.personId) { mutableStateOf<Set<String>>(emptySet()) }
  var saveError by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(selectedSlug, session.personId, detailRetry) {
    detail = null
    detailError = null
    val slug = selectedSlug ?: return@LaunchedEffect
    detailLoading = true
    try {
      val response = detailRepository.load(slug, session.isAuthenticated)
      detail = response
      savedStates = savedStates + (slug to response.isSaved)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { detailError = "Couldn’t load this offer. Please try again." }
    finally { detailLoading = false }
  }
  fun saveOffer(slug: String) {
    if (!session.isAuthenticated) { onNavigateToSignIn(); return }
    if (slug in savingSlugs) return
    val actor = session.personId
    savingSlugs = savingSlugs + slug
    scope.launch {
      try {
        val current = savedStates[slug] ?: detailRepository.load(slug).isSaved
        val confirmed = detailRepository.setSaved(slug, !current)
        if (SessionManager.session.value.personId == actor) {
          savedStates = savedStates + (slug to confirmed)
          if (detail?.slug == slug) detail = detail?.copy(isSaved = confirmed)
        }
      } catch (cancelled: CancellationException) { throw cancelled }
      catch (_: Exception) { saveError = "Couldn’t update saved offer. Please try again." }
      finally { if (SessionManager.session.value.personId == actor) savingSlugs = savingSlugs - slug }
    }
  }
  if (saveError != null) AlertDialog(onDismissRequest = { saveError = null },
    title = { Text("Couldn’t save offer") }, text = { Text(saveError.orEmpty()) },
    confirmButton = { TextButton(onClick = { saveError = null }) { Text("Close") } })
  selectedSlug?.let { slug ->
    NativeOfferDetailScreen(detail, detailLoading, detailError, session.isAuthenticated,
      onBack = { selectedSlug = null }, onRetry = { detailRetry++ },
      onToggleSave = { saveOffer(slug) },
      onMessageProvider = { uriHandler.openUri("$base/offers/$slug") },
      onSignInRequested = onNavigateToSignIn, modifier = modifier)
    return
  }
  val profileModel: ProfileViewModel = viewModel(factory = remember(context.applicationContext) {
    ProfileViewModel.factory(context.applicationContext)
  })
  val profileState by profileModel.state.collectAsState()
  val messagesModel: MessagesInboxViewModel = viewModel(factory = remember(context.applicationContext) {
    MessagesInboxViewModel.factory(context.applicationContext)
  })
  val messagesState by messagesModel.state.collectAsState()
  LaunchedEffect(showProfile, showMessages, session.personId) {
    val person = session.personId
    if (person == null) {
      profileModel.clear()
      messagesModel.clear()
    } else {
      if (showProfile) profileModel.load(person)
      if (showMessages) messagesModel.load(person)
    }
  }
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    if (showProfile) session.personId?.let { profileModel.load(it, force = true) }
    if (showMessages) session.personId?.let { messagesModel.load(it, force = true) }
  }
  var browserError by remember { mutableStateOf<String?>(null) }
  fun openWeb(path: String) {
    try { uriHandler.openUri(base + path) }
    catch (_: Exception) { browserError = "Couldn’t open your browser. Please try again." }
  }
  BackHandler {
    when {
      showProfile -> showProfile = false
      showMessages -> showMessages = false
      else -> onNavigateBack()
    }
  }

  Column(modifier.fillMaxSize().background(HavenColors.Canvas).testTag("offers_screen")) {
    if (showProfile && session.isAuthenticated) {
      val visibleProfile = if (profileState.profile?.let { it.id != session.personId } == true)
        com.example.ui.model.ProfileUiState(loading = true) else profileState
      ProfileScreen(visibleProfile,
        onRetry = { session.personId?.let { profileModel.load(it, force = true) } },
        onOpenWeb = ::openWeb, onSignOut = onSignOut, modifier = Modifier.weight(1f))
    } else if (showMessages && session.isAuthenticated) {
      MessagesInboxScreen(messagesState,
        onRetry = { session.personId?.let { messagesModel.load(it, force = true) } },
        onLoadMore = messagesModel::loadMore,
        onSelectConversation = { id ->
          try {
            val scheme = when (ThemeManager.themeMode) {
              ThemeMode.SYSTEM -> CustomTabsIntent.COLOR_SCHEME_SYSTEM
              ThemeMode.LIGHT -> CustomTabsIntent.COLOR_SCHEME_LIGHT
              ThemeMode.DARK -> CustomTabsIntent.COLOR_SCHEME_DARK
            }
            CustomTabsIntent.Builder().setShowTitle(true).setColorScheme(scheme).build()
              .launchUrl(context, Uri.parse("$base/conversations/$id"))
          } catch (_: Exception) {
            browserError = "Couldn’t open this conversation. Please try again."
          }
        }, modifier = Modifier.weight(1f))
    } else {
    HavenTopBarContainer(backgroundColor = HavenColors.Canvas, showBottomBorder = false) {
      Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Row(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp))
            .background(HavenColors.SurfaceContainer).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Search, null, tint = HavenColors.TextSecondary, modifier = Modifier.size(20.dp))
            Box(Modifier.weight(1f)) {
              if (state.query.search.isEmpty()) Text("Search offer titles", fontSize = 16.sp,
                fontFamily = InterFontFamily, color = HavenColors.TextSecondary)
              BasicTextField(state.query.search, viewModel::onSearchQueryChange,
                singleLine = true, modifier = Modifier.fillMaxWidth().testTag("offer_search"),
                textStyle = TextStyle(fontFamily = InterFontFamily, fontSize = 16.sp, color = HavenColors.TextPrimary),
                cursorBrush = SolidColor(HavenColors.Primary))
            }
          }
          Row(Modifier.height(48.dp).clip(RoundedCornerShape(24.dp))
            .background(HavenColors.SurfaceContainer)
            .clickable(enabled = state.currencies.isNotEmpty(), onClick = viewModel::openFilterSheet)
            .padding(horizontal = 12.dp).testTag("open_filters_button"),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.Tune, null, modifier = Modifier.size(18.dp))
            Text(if (state.activeFilterCount > 0) "Filters (1)" else "Filters", fontSize = 13.sp, fontFamily = InterFontFamily)
          }
          IconButton(onClick = { openWeb("/offers") }, modifier = Modifier
            .background(HavenColors.SurfaceContainer, RoundedCornerShape(24.dp))) {
            Icon(Icons.Outlined.LocationOn, "Choose a place on Haven")
          }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (state.isLoading && state.categories.isEmpty()) {
            repeat(5) { index ->
              Box(Modifier.padding(vertical = 14.dp).size(width = listOf(42, 82, 104, 90, 76)[index].dp, height = 34.dp)
                .clip(RoundedCornerShape(24.dp)).shimmerBackground(RoundedCornerShape(24.dp))
                .testTag(if (index == 0) "category_chip_skeleton" else "category_chip_skeleton_$index"))
            }
          } else {
            CategoryChip("All", state.query.category.isEmpty()) { viewModel.selectCategory("") }
            state.categories.forEach { category ->
              CategoryChip(category.label, state.query.category == category.slug) { viewModel.selectCategory(category.slug) }
            }
          }
        }
      }
    }

    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f).testTag("offers_grid"),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
      item(span = { GridItemSpan(2) }) {
        Text(if (state.isLoading && state.items.isEmpty()) "Loading offers…" else "${state.items.size} offers${if (state.nextCursor != null) " loaded" else ""}",
          fontSize = 12.sp, fontFamily = InterFontFamily, color = HavenColors.TextSecondary)
      }
      items(state.items, key = { it.id }) { offer ->
        FeedOfferCard(offer, saved = savedStates[offer.slug] ?: false,
          saving = offer.slug in savingSlugs, onSave = { saveOffer(offer.slug) }) { selectedSlug = offer.slug }
      }
      if (state.isLoading && state.items.isEmpty()) {
        items(4) { index -> OfferCardSkeleton(Modifier.testTag("offers_loading_skeleton_$index")) }
      } else if (state.isLoading) item(span = { GridItemSpan(2) }) {
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(Modifier.size(24.dp).testTag("offers_loading"), color = HavenColors.Primary)
        }
      }
      val error = browserError ?: state.error
      if (error != null) item(span = { GridItemSpan(2) }) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
          Text(error, fontFamily = InterFontFamily, fontSize = 14.sp, modifier = Modifier.testTag("offers_error"))
          TextButton(onClick = {
            browserError = null
            if (state.items.isNotEmpty() && state.nextCursor != null) viewModel.loadMore() else viewModel.refresh()
          }) { Text("Try again") }
        }
      }
      if (!state.isLoading && state.error == null && state.items.isEmpty()) item(span = { GridItemSpan(2) }) {
        Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          Text("No offers found", fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold)
          TextButton(onClick = viewModel::clearFilters) { Text("Clear search and filters") }
        }
      }
      if (!state.isLoading && state.nextCursor != null && state.error == null) item(span = { GridItemSpan(2) }) {
        TextButton(onClick = viewModel::loadMore, modifier = Modifier.testTag("load_more_offers")) { Text("More offers") }
      }
    }

    }
    HavenBottomBarContainer(backgroundColor = HavenColors.Canvas, showTopBorder = true,
      borderColor = HavenColors.BorderSubtle) {
      Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceAround) {
        FeedNavItem("Offers", Icons.Outlined.GridView, !showProfile && !showMessages) {
          showProfile = false
          showMessages = false
          viewModel.refresh()
        }
        FeedNavItem("Messages", Icons.Outlined.ChatBubbleOutline, showMessages) {
          if (session.isAuthenticated) {
            showProfile = false
            showMessages = true
            session.personId?.let { messagesModel.load(it) }
          } else onNavigateToSignIn()
        }
        FeedNavItem(if (session.isAuthenticated) "Profile" else "Log in", Icons.Outlined.PersonOutline, showProfile && session.isAuthenticated) {
          if (session.isAuthenticated) { showMessages = false; showProfile = true } else onNavigateToSignIn()
        }
      }
    }
  }
  if ((showProfile || showMessages) && browserError != null) AlertDialog(
    onDismissRequest = { browserError = null }, title = { Text("Couldn’t open page") },
    text = { Text(browserError ?: "") },
    confirmButton = { TextButton(onClick = { browserError = null }) { Text("Close") } })
  if (state.isFilterSheetVisible) FilterBottomSheet(
    currency = state.draftCurrency, minPriceText = state.draftMinPriceText,
    maxPriceText = state.draftMaxPriceText, currencies = listOf("Any") + state.currencies,
    onCurrencyChange = viewModel::onDraftCurrencyChange,
    onMinPriceChange = viewModel::onDraftMinPriceChange, onMaxPriceChange = viewModel::onDraftMaxPriceChange,
    onApply = viewModel::applyFilters, onClear = viewModel::clearFilters, onDismiss = viewModel::closeFilterSheet)

}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
  Box(Modifier.clip(RoundedCornerShape(24.dp))
    .background(if (selected) HavenColors.Primary else HavenColors.SurfaceContainer)
    .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 14.dp)) {
    Text(label, fontFamily = InterFontFamily, fontSize = 13.sp,
      color = if (selected) HavenColors.TextOnDark else HavenColors.TextSecondary)
  }
}

@Composable
private fun FeedNavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
  val tag = "nav_tab_${label.lowercase().replace(" ", "_")}"
  Column(Modifier.clickable(onClick = onClick).testTag(tag).padding(horizontal = 20.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
    val color = if (selected) HavenColors.Primary else HavenColors.TextSecondary
    Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
    Text(label, fontFamily = InterFontFamily, fontSize = 11.sp, color = color)
  }
}
