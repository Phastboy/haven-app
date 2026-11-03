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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import com.example.data.HavenNativePagesRepository
import com.example.data.OfferDetail
import com.example.data.ProviderProfile
import com.example.data.PaginatedOffers
import com.example.data.PaginatedFollowing
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import com.example.ui.components.HavenEmptyState
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

sealed interface OffersDestination {
  data class OfferDetail(val slug: String) : OffersDestination
  data class ProviderProfile(val handle: String) : OffersDestination
  data object MyOffers : OffersDestination
  data object SavedOffers : OffersDestination
  data object Following : OffersDestination
}

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
  val nativePagesRepo = remember(context.applicationContext) { HavenNativePagesRepository(HavenAuthClient.shared(context.applicationContext)) }
  val scope = rememberCoroutineScope()
  var navStack by remember { mutableStateOf<List<OffersDestination>>(emptyList()) }
  val currentDestination = navStack.lastOrNull()

  val activeSlug = (currentDestination as? OffersDestination.OfferDetail)?.slug
  var detail by remember(session.personId) { mutableStateOf<OfferDetail?>(null) }
  var detailLoading by remember { mutableStateOf(false) }
  var detailError by remember { mutableStateOf<String?>(null) }
  var detailRetry by remember { mutableStateOf(0) }
  var savedStates by remember(session.personId) { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
  var savingSlugs by remember(session.personId) { mutableStateOf<Set<String>>(emptySet()) }
  var saveError by remember { mutableStateOf<String?>(null) }

  // Provider profile state
  val activeProviderHandle = (currentDestination as? OffersDestination.ProviderProfile)?.handle
  var providerProfile by remember { mutableStateOf<ProviderProfile?>(null) }
  var providerLoading by remember { mutableStateOf(false) }
  var providerError by remember { mutableStateOf<String?>(null) }
  var providerRetry by remember { mutableStateOf(0) }

  // My offers state
  val isMyOffersActive = currentDestination is OffersDestination.MyOffers
  var myOffers by remember(session.personId) { mutableStateOf<PaginatedOffers?>(null) }
  var myOffersLoading by remember { mutableStateOf(false) }
  var myOffersError by remember { mutableStateOf<String?>(null) }
  var myOffersRetry by remember { mutableStateOf(0) }

  // Saved offers state
  val isSavedOffersActive = currentDestination is OffersDestination.SavedOffers
  var savedOffers by remember(session.personId) { mutableStateOf<PaginatedOffers?>(null) }
  var savedOffersLoading by remember { mutableStateOf(false) }
  var savedOffersError by remember { mutableStateOf<String?>(null) }
  var savedOffersRetry by remember { mutableStateOf(0) }

  // Following state
  val isFollowingActive = currentDestination is OffersDestination.Following
  var following by remember(session.personId) { mutableStateOf<PaginatedFollowing?>(null) }
  var followingLoading by remember { mutableStateOf(false) }
  var followingError by remember { mutableStateOf<String?>(null) }
  var followingRetry by remember { mutableStateOf(0) }

  var browserError by remember { mutableStateOf<String?>(null) }
  fun openWeb(path: String) {
    try {
      val scheme = when (ThemeManager.themeMode) {
        ThemeMode.SYSTEM -> CustomTabsIntent.COLOR_SCHEME_SYSTEM
        ThemeMode.LIGHT -> CustomTabsIntent.COLOR_SCHEME_LIGHT
        ThemeMode.DARK -> CustomTabsIntent.COLOR_SCHEME_DARK
      }
      CustomTabsIntent.Builder().setShowTitle(true).setColorScheme(scheme).build()
        .launchUrl(context, Uri.parse(base + path))
    } catch (_: Exception) {
      try { uriHandler.openUri(base + path) }
      catch (_: Exception) { browserError = "Couldn’t open your browser. Please try again." }
    }
  }

  LaunchedEffect(activeSlug, session.personId, detailRetry) {
    detail = null
    detailError = null
    val slug = activeSlug ?: return@LaunchedEffect
    detailLoading = true
    try {
      val response = detailRepository.load(slug, session.isAuthenticated)
      detail = response
      savedStates = savedStates + (slug to response.isSaved)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { detailError = "Couldn’t load this offer. Please try again." }
    finally { detailLoading = false }
  }

  LaunchedEffect(activeProviderHandle, session.isAuthenticated, providerRetry) {
    providerProfile = null
    providerError = null
    val handle = activeProviderHandle ?: return@LaunchedEffect
    providerLoading = true
    try {
      providerProfile = nativePagesRepo.getProviderProfile(handle, authenticated = session.isAuthenticated)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { providerError = "Couldn’t load provider profile. Please try again." }
    finally { providerLoading = false }
  }

  LaunchedEffect(isMyOffersActive, session.personId, myOffersRetry) {
    if (!isMyOffersActive) return@LaunchedEffect
    myOffersLoading = true
    myOffersError = null
    try {
      myOffers = nativePagesRepo.getMyOffers()
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { myOffersError = "Couldn’t load your offers. Please try again." }
    finally { myOffersLoading = false }
  }

  LaunchedEffect(isSavedOffersActive, session.personId, savedOffersRetry) {
    if (!isSavedOffersActive) return@LaunchedEffect
    savedOffersLoading = true
    savedOffersError = null
    try {
      val response = nativePagesRepo.getSavedOffers()
      savedOffers = response
      savedStates = savedStates + response.items.associate { it.slug to true }
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { savedOffersError = "Couldn’t load saved offers. Please try again." }
    finally { savedOffersLoading = false }
  }

  LaunchedEffect(isFollowingActive, session.personId, followingRetry) {
    if (!isFollowingActive) return@LaunchedEffect
    followingLoading = true
    followingError = null
    try {
      following = nativePagesRepo.getFollowing()
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { followingError = "Couldn’t load followed providers. Please try again." }
    finally { followingLoading = false }
  }

  fun saveOffer(slug: String) {
    if (!session.isAuthenticated) { onNavigateToSignIn(); return }
    if (slug in savingSlugs) return
    val actor = session.personId
    savingSlugs = savingSlugs + slug
    scope.launch {
      try {
        val confirmed = detailRepository.setSaved(slug, requestedSavedState(savedStates[slug]))
        if (SessionManager.session.value.personId == actor) {
          savedStates = savedStates + (slug to confirmed)
          if (detail?.slug == slug) detail = detail?.copy(isSaved = confirmed)
          if (!confirmed) {
            savedOffers = savedOffers?.copy(items = (savedOffers?.items ?: emptyList()).filter { it.slug != slug })
          }
        }
      } catch (cancelled: CancellationException) { throw cancelled }
      catch (_: Exception) { saveError = "Couldn’t update saved offer. Please try again." }
      finally { if (SessionManager.session.value.personId == actor) savingSlugs = savingSlugs - slug }
    }
  }

  if (saveError != null) AlertDialog(onDismissRequest = { saveError = null },
    title = { Text("Couldn’t save offer") }, text = { Text(saveError.orEmpty()) },
    confirmButton = { TextButton(onClick = { saveError = null }) { Text("Close") } })

  if (currentDestination != null) {
    when (currentDestination) {
      is OffersDestination.OfferDetail -> {
        NativeOfferDetailScreen(
          offer = detail,
          loading = detailLoading,
          error = detailError,
          isAuthenticated = session.isAuthenticated,
          onBack = { navStack = navStack.dropLast(1) },
          onRetry = { detailRetry++ },
          onToggleSave = { saveOffer(currentDestination.slug) },
          onMessageProvider = { openWeb("/offers/${currentDestination.slug}") },
          onOpenProvider = { handle -> navStack = navStack + OffersDestination.ProviderProfile(handle) },
          onSignInRequested = onNavigateToSignIn,
          modifier = modifier
        )
        return
      }
      is OffersDestination.ProviderProfile -> {
        ProviderProfileScreen(
          profile = providerProfile,
          loading = providerLoading,
          error = providerError,
          onBack = { navStack = navStack.dropLast(1) },
          onRetry = { providerRetry++ },
          onOpenOffer = { slug -> navStack = navStack + OffersDestination.OfferDetail(slug) },
          onMessageProvider = { openWeb("/profiles/${Uri.encode(currentDestination.handle)}") },
          onFollowProvider = { openWeb("/profiles/${Uri.encode(currentDestination.handle)}") },
          onLoadMore = {
            providerProfile?.nextCursor?.let { cursor ->
              scope.launch {
                try {
                  val more = nativePagesRepo.getProviderProfile(currentDestination.handle, after = cursor, authenticated = session.isAuthenticated)
                  providerProfile = providerProfile?.copy(
                    offers = (providerProfile?.offers ?: emptyList()) + more.offers,
                    nextCursor = more.nextCursor
                  )
                } catch (_: Exception) {}
              }
            }
          },
          isOwnProfile = (session.personId != null && providerProfile?.person?.id == session.personId),
          modifier = modifier
        )
        return
      }
      is OffersDestination.MyOffers -> {
        if (session.isAuthenticated) {
          MyOffersScreen(
            offers = myOffers,
            loading = myOffersLoading,
            error = myOffersError,
            onBack = { navStack = navStack.dropLast(1) },
            onRetry = { myOffersRetry++ },
            onOpenOffer = { slug -> navStack = navStack + OffersDestination.OfferDetail(slug) },
            onCreateOffer = { openWeb("/offers/new") },
            onEditOffer = { slug -> openWeb("/offers/$slug/edit") },
            onLoadMore = {
              myOffers?.nextCursor?.let { cursor ->
                scope.launch {
                  try {
                    val more = nativePagesRepo.getMyOffers(after = cursor)
                    myOffers = myOffers?.copy(
                      items = (myOffers?.items ?: emptyList()) + more.items,
                      nextCursor = more.nextCursor
                    )
                  } catch (_: Exception) {}
                }
              }
            },
            modifier = modifier
          )
          return
        }
      }
      is OffersDestination.SavedOffers -> {
        if (session.isAuthenticated) {
          SavedOffersScreen(
            offers = savedOffers,
            loading = savedOffersLoading,
            error = savedOffersError,
            savingSlugs = savingSlugs,
            onBack = { navStack = navStack.dropLast(1) },
            onRetry = { savedOffersRetry++ },
            onOpenOffer = { slug -> navStack = navStack + OffersDestination.OfferDetail(slug) },
            onUnsaveOffer = { slug -> saveOffer(slug) },
            onBrowseOffers = {
              navStack = emptyList()
              showProfile = false
            },
            onOpenProvider = { handle -> navStack = navStack + OffersDestination.ProviderProfile(handle) },
            onLoadMore = {
              savedOffers?.nextCursor?.let { cursor ->
                scope.launch {
                  try {
                    val more = nativePagesRepo.getSavedOffers(after = cursor)
                    savedOffers = savedOffers?.copy(
                      items = (savedOffers?.items ?: emptyList()) + more.items,
                      nextCursor = more.nextCursor
                    )
                  } catch (_: Exception) {}
                }
              }
            },
            modifier = modifier
          )
          return
        }
      }
      is OffersDestination.Following -> {
        if (session.isAuthenticated) {
          FollowingScreen(
            following = following,
            loading = followingLoading,
            error = followingError,
            onBack = { navStack = navStack.dropLast(1) },
            onRetry = { followingRetry++ },
            onOpenProvider = { handle -> navStack = navStack + OffersDestination.ProviderProfile(handle) },
            onOpenWebFollow = { handle -> openWeb("/profiles/${Uri.encode(handle)}") },
            onDiscoverOffers = {
              navStack = emptyList()
              showProfile = false
            },
            onLoadMore = {
              following?.nextCursor?.let { cursor ->
                scope.launch {
                  try {
                    val more = nativePagesRepo.getFollowing(after = cursor)
                    following = following?.copy(
                      items = (following?.items ?: emptyList()) + more.items,
                      nextCursor = more.nextCursor
                    )
                  } catch (_: Exception) {}
                }
              }
            },
            modifier = modifier
          )
          return
        }
      }
    }
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
      messagesModel.load(person)
      if (showProfile) profileModel.load(person)
      if (showMessages) messagesModel.load(person)
    }
  }
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    session.personId?.let { personId ->
      messagesModel.load(personId, force = false)
      if (showProfile) profileModel.load(personId, force = true)
      if (showMessages) messagesModel.load(personId, force = true)
    }
  }

  BackHandler {
    when {
      navStack.isNotEmpty() -> navStack = navStack.dropLast(1)
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
        onOpenWeb = ::openWeb,
        onSignOut = onSignOut,
        onOpenMyOffers = { navStack = navStack + OffersDestination.MyOffers },
        onOpenSavedOffers = { navStack = navStack + OffersDestination.SavedOffers },
        onOpenFollowing = { navStack = navStack + OffersDestination.Following },
        onOpenOffer = { slug -> navStack = navStack + OffersDestination.OfferDetail(slug) },
        onOpenPublicProfile = { handle -> navStack = navStack + OffersDestination.ProviderProfile(handle) },
        modifier = Modifier.weight(1f))
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
      val isFeedDisabled = (browserError ?: state.error) != null && state.items.isEmpty()
      Column(modifier = if (isFeedDisabled) Modifier.alpha(0.45f) else Modifier) {
        Row(
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            Modifier
              .weight(1f)
              .height(40.dp)
              .clip(RoundedCornerShape(20.dp))
              .background(HavenColors.SurfaceContainer)
              .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Outlined.Search, null, tint = HavenColors.TextSecondary, modifier = Modifier.size(18.dp))
            Box(Modifier.weight(1f)) {
              if (state.query.search.isEmpty()) {
                Text(
                  "Search offers…",
                  fontSize = 14.sp,
                  lineHeight = 18.sp,
                  fontFamily = InterFontFamily,
                  color = HavenColors.TextSecondary,
                  maxLines = 1
                )
              }
              BasicTextField(
                value = state.query.search,
                onValueChange = viewModel::onSearchQueryChange,
                enabled = !isFeedDisabled,
                singleLine = true,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().testTag("offer_search"),
                textStyle = TextStyle(fontFamily = InterFontFamily, fontSize = 14.sp, color = HavenColors.TextPrimary),
                cursorBrush = SolidColor(HavenColors.Primary)
              )
            }
            if (state.query.search.isNotEmpty()) {
              IconButton(
                onClick = { viewModel.onSearchQueryChange("") },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(Icons.Outlined.Close, "Clear search", tint = HavenColors.TextSecondary, modifier = Modifier.size(16.dp))
              }
            }
          }
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(20.dp))
              .background(HavenColors.SurfaceContainer)
              .clickable(enabled = !isFeedDisabled && state.currencies.isNotEmpty(), onClick = viewModel::openFilterSheet)
              .testTag("open_filters_button"),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Outlined.Tune, "Filters", tint = HavenColors.TextPrimary, modifier = Modifier.size(18.dp))
            if (state.activeFilterCount > 0) {
              Box(
                Modifier
                  .align(Alignment.TopEnd)
                  .padding(top = 8.dp, end = 8.dp)
                  .size(7.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(HavenColors.Primary)
              )
            }
          }
          IconButton(
            onClick = { openWeb("/offers") },
            enabled = !isFeedDisabled,
            modifier = Modifier
              .size(40.dp)
              .background(HavenColors.SurfaceContainer, RoundedCornerShape(20.dp))
          ) {
            Icon(Icons.Outlined.LocationOn, "Choose a place on Haven", tint = HavenColors.TextPrimary, modifier = Modifier.size(18.dp))
          }
          IconButton(
            onClick = {
              if (session.isAuthenticated) openWeb("/offers/new") else onNavigateToSignIn()
            },
            enabled = !isFeedDisabled,
            modifier = Modifier
              .size(40.dp)
              .background(HavenColors.Primary, RoundedCornerShape(20.dp))
              .testTag("post_offer_button")
          ) {
            Icon(Icons.Outlined.Add, "Post an offer", tint = HavenColors.TextOnDark, modifier = Modifier.size(20.dp))
          }
        }
        if (!isFeedDisabled) {
          Row(
            Modifier
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (state.isLoading && state.categories.isEmpty()) {
              repeat(5) { index ->
                Box(
                  Modifier
                    .size(width = listOf(42, 82, 104, 90, 76)[index].dp, height = 34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .shimmerBackground(RoundedCornerShape(17.dp))
                    .testTag(if (index == 0) "category_chip_skeleton" else "category_chip_skeleton_$index")
                )
              }
            } else {
              CategoryChip("All", state.query.category.isEmpty()) { viewModel.selectCategory("") }
              state.categories.forEach { category ->
                CategoryChip(category.label, state.query.category == category.slug) {
                  viewModel.selectCategory(category.slug)
                }
              }
            }
          }
        }
      }
    }

    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      modifier = Modifier.weight(1f).testTag("offers_grid"),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      val error = browserError ?: state.error
      if (error == null) {
        if (state.isLoading && state.items.isEmpty()) {
          item(span = { GridItemSpan(2) }) {
            Text(
              "Loading offers…",
              fontSize = 12.sp,
              fontFamily = InterFontFamily,
              color = HavenColors.TextSecondary
            )
          }
        } else if (state.items.isNotEmpty()) {
          item(span = { GridItemSpan(2) }) {
            val count = state.items.size
            val countLabel = if (count == 1) "1 offer" else "$count offers"
            val text = if (state.nextCursor != null) "$countLabel loaded" else countLabel
            Text(
              text,
              fontSize = 12.sp,
              fontFamily = InterFontFamily,
              color = HavenColors.TextSecondary
            )
          }
        }
      }

      items(state.items, key = { it.id }) { offer ->
        FeedOfferCard(
          offer = offer,
          saved = savedStates[offer.slug] ?: false,
          saving = offer.slug in savingSlugs,
          onSave = { saveOffer(offer.slug) },
          onOpenProvider = { handle -> navStack = navStack + OffersDestination.ProviderProfile(handle) },
          onOpen = { navStack = navStack + OffersDestination.OfferDetail(offer.slug) }
        )
      }

      if (state.isLoading && state.items.isEmpty()) {
        items(4) { index -> OfferCardSkeleton(Modifier.testTag("offers_loading_skeleton_$index")) }
      } else if (state.isLoading) {
        item(span = { GridItemSpan(2) }) {
          Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(24.dp).testTag("offers_loading"), color = HavenColors.Primary)
          }
        }
      }

      if (error != null) {
        item(span = { GridItemSpan(2) }) {
          val isOffline = error.contains("network", ignoreCase = true) ||
            error.contains("connect", ignoreCase = true) ||
            error.contains("timeout", ignoreCase = true) ||
            error.contains("offline", ignoreCase = true) ||
            error.contains("resolve host", ignoreCase = true)
          val errorTitle = if (isOffline) "You're offline" else "Something went wrong on our side"
          val errorBody = if (isOffline) "Check your connection and try again." else error
          val errorIcon = if (isOffline) Icons.Outlined.CloudOff else Icons.Outlined.ErrorOutline

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
          ) {
            HavenEmptyState(
              icon = errorIcon,
              title = errorTitle,
              body = errorBody,
              primaryActionLabel = "Try again",
              onPrimaryAction = {
                browserError = null
                if (state.items.isNotEmpty() && state.nextCursor != null) viewModel.loadMore() else viewModel.refresh()
              },
              testTag = "offers_error"
            )
          }
        }
      }

      if (!state.isLoading && error == null && state.items.isEmpty()) {
        item(span = { GridItemSpan(2) }) {
          val isSearch = state.query.search.isNotBlank()
          val isCategory = state.query.category.isNotBlank()
          val activeCategoryLabel = state.categories.find { it.slug == state.query.category }?.label

          val icon = when {
            isSearch -> Icons.Outlined.SearchOff
            isCategory -> Icons.Outlined.Category
            else -> Icons.Outlined.Inventory2
          }
          val title = when {
            isSearch -> "No results for “${state.query.search}”"
            isCategory -> "No ${activeCategoryLabel ?: "offers"} yet"
            else -> "No offers found"
          }
          val body = when {
            isSearch -> "Try checking your spelling or adjusting your keywords."
            isCategory -> "Be the first to post something here."
            else -> "Try widening your area or clearing your filters."
          }
          val primaryLabel = when {
            isSearch -> "Clear search"
            isCategory -> "Post an offer"
            else -> "Clear filters"
          }
          val onPrimary: () -> Unit = when {
            isSearch -> { { viewModel.onSearchQueryChange("") } }
            isCategory -> { { if (session.isAuthenticated) openWeb("/offers/new") else onNavigateToSignIn() } }
            else -> { { viewModel.clearFilters() } }
          }
          val secondaryLabel = when {
            isSearch -> "Post an offer"
            isCategory -> "Clear filters"
            else -> "Post an offer"
          }
          val onSecondary: () -> Unit = when {
            isSearch -> { { if (session.isAuthenticated) openWeb("/offers/new") else onNavigateToSignIn() } }
            isCategory -> { { viewModel.clearFilters() } }
            else -> { { if (session.isAuthenticated) openWeb("/offers/new") else onNavigateToSignIn() } }
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 36.dp),
            contentAlignment = Alignment.Center
          ) {
            HavenEmptyState(
              icon = icon,
              title = title,
              body = body,
              primaryActionLabel = primaryLabel,
              onPrimaryAction = onPrimary,
              secondaryActionLabel = secondaryLabel,
              onSecondaryAction = onSecondary,
              testTag = "offers_empty_state"
            )
          }
        }
      }

      if (!state.isLoading && state.nextCursor != null && error == null) {
        item(span = { GridItemSpan(2) }) {
          Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            TextButton(onClick = viewModel::loadMore, modifier = Modifier.testTag("load_more_offers")) {
              Text("More offers", fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, color = HavenColors.Primary)
            }
          }
        }
      }
    }

    }
    HavenBottomBarContainer(
      backgroundColor = HavenColors.Canvas,
      showTopBorder = true,
      borderColor = HavenColors.BorderSubtle
    ) {
      Row(
        Modifier
          .fillMaxWidth()
          .height(58.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        FeedNavItem(
          label = "Offers",
          activeIcon = Icons.Filled.GridView,
          inactiveIcon = Icons.Outlined.GridView,
          selected = !showProfile && !showMessages
        ) {
          showProfile = false
          showMessages = false
          viewModel.refresh()
        }

        val unreadCount = messagesState.items.count { it.isUnread }
        FeedNavItem(
          label = "Messages",
          activeIcon = Icons.Filled.ChatBubble,
          inactiveIcon = Icons.Outlined.ChatBubbleOutline,
          selected = showMessages,
          badgeCount = unreadCount
        ) {
          if (session.isAuthenticated) {
            showProfile = false
            showMessages = true
            session.personId?.let { messagesModel.load(it) }
          } else {
            HavenAuthClient.logDiagnostic("UI", "NAVIGATE_TO_SIGN_IN", "Messages tab clicked while unauthenticated; navigating to Sign In screen.")
            onNavigateToSignIn()
          }
        }

        FeedNavItem(
          label = if (session.isAuthenticated) "Profile" else "Log in",
          activeIcon = Icons.Filled.Person,
          inactiveIcon = Icons.Outlined.PersonOutline,
          selected = showProfile && session.isAuthenticated,
          isLogInAction = !session.isAuthenticated
        ) {
          if (session.isAuthenticated) {
            showMessages = false
            showProfile = true
          } else {
            HavenAuthClient.logDiagnostic("UI", "NAVIGATE_TO_SIGN_IN", "Log in tab clicked while unauthenticated; navigating to Sign In screen.")
            onNavigateToSignIn()
          }
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

/** An outlined Save action always saves, including when status has not been fetched. */
internal fun requestedSavedState(confirmed: Boolean?): Boolean = confirmed != true

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(18.dp))
      .background(if (selected) HavenColors.Primary else HavenColors.SurfaceContainer)
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 8.dp)
  ) {
    Text(
      text = label,
      fontFamily = InterFontFamily,
      fontSize = 13.sp,
      lineHeight = 18.sp,
      fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
      color = if (selected) HavenColors.TextOnDark else HavenColors.TextSecondary
    )
  }
}

@Composable
private fun RowScope.FeedNavItem(
  label: String,
  activeIcon: ImageVector,
  inactiveIcon: ImageVector,
  selected: Boolean,
  badgeCount: Int = 0,
  isLogInAction: Boolean = false,
  onClick: () -> Unit
) {
  val tag = "nav_tab_${label.lowercase().replace(" ", "_")}"
  val activeColor = HavenColors.Primary
  val inactiveColor = HavenColors.TextSecondary

  Box(
    modifier = Modifier
      .weight(1f)
      .height(58.dp)
      .clickable(onClick = onClick)
      .testTag(tag),
    contentAlignment = Alignment.Center
  ) {
    if (selected) {
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .fillMaxWidth(0.5f)
          .height(2.5.dp)
          .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
          .background(activeColor)
      )
    }

    if (isLogInAction) {
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(16.dp))
          .background(HavenColors.SurfaceContainer)
          .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = inactiveIcon,
          contentDescription = null,
          tint = HavenColors.Primary,
          modifier = Modifier.size(18.dp)
        )
        Text(
          text = label,
          fontFamily = InterFontFamily,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = HavenColors.Primary
        )
      }
    } else {
      Column(
        modifier = Modifier.padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        BadgedBox(
          badge = {
            if (badgeCount > 0) {
              Badge(
                containerColor = HavenColors.Primary,
                contentColor = HavenColors.TextOnDark
              ) {
                Text(
                  text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = InterFontFamily
                )
              }
            }
          }
        ) {
          Icon(
            imageVector = if (selected) activeIcon else inactiveIcon,
            contentDescription = null,
            tint = if (selected) activeColor else inactiveColor,
            modifier = Modifier.size(22.dp)
          )
        }
        Text(
          text = label,
          fontFamily = InterFontFamily,
          fontSize = 11.sp,
          fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
          color = if (selected) activeColor else inactiveColor
        )
      }
    }
  }
}
