package com.example.ui.screens

import android.net.Uri
import androidx.core.net.toUri
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.key
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.ui.components.CategoryScrollBoundary
import com.example.ui.components.HavenRootPager
import com.example.ui.components.ObserveRootTabSelection
import com.example.ui.components.RootTab
import com.example.ui.components.rememberRootTabsState
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
import coil3.compose.SubcomposeAsyncImage
import com.example.auth.HavenAuthClient
import com.example.data.FeedOffer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.ui.model.ProfileViewModel
import com.example.ui.model.MessagesInboxViewModel
import com.example.ui.model.ConversationThreadViewModel
import com.example.ui.model.InquiryComposeViewModel
import com.example.ui.components.FeedHeaderLayout
import com.example.ui.components.FeedActionButton
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
  data class OfferEditor(val id: String? = null, val entryKey: String = java.util.UUID.randomUUID().toString()) : OffersDestination
  data object MyOffers : OffersDestination
  data object SavedOffers : OffersDestination
  data object Inquired : OffersDestination
  data object Following : OffersDestination
  data class ConversationThread(val conversationId: String) : OffersDestination
  data object EditProfile : OffersDestination
  data object ReferenceLocation : OffersDestination
}

/** Public discovery from Haven. Adjacent journeys open their real web pages. */
@Composable
fun OffersScreen(
  viewModel: OffersViewModel,
  onNavigateBack: () -> Unit,
  onNavigateToSignIn: () -> Unit,
  modifier: Modifier = Modifier,
  onSignOut: () -> Unit = {}
) {
  val state by viewModel.uiState.collectAsState()
  val session by SessionManager.session.collectAsState()
  val uriHandler = LocalUriHandler.current
  val base = "https://${HavenAuthClient.HOST}"
  val rootTabs = rememberRootTabsState(session.personId)
  val inboxScroll = key(session.personId) { rememberLazyListState() }
  val profileScroll = key(session.personId) { rememberScrollState() }
  val categoryScroll = key(session.personId) { rememberScrollState() }
  var profileModalOpen by remember(session.personId) { mutableStateOf(false) }
  val context = LocalContext.current
  val detailRepository = remember(context.applicationContext) { HavenOfferDetailRepository(HavenAuthClient.shared(context.applicationContext)) }
  val nativePagesRepo = remember(context.applicationContext) { HavenNativePagesRepository(HavenAuthClient.shared(context.applicationContext)) }
  val scope = rememberCoroutineScope()
  val inquiryModel: InquiryComposeViewModel = viewModel(key = "InquiryComposeViewModel_${session.personId}", factory = remember(context.applicationContext) {
    InquiryComposeViewModel.factory(context.applicationContext)
  })
  val inquiryState by inquiryModel.state.collectAsState()
  DisposableEffect(inquiryModel, session.personId) {
    val actor = session.personId
    onDispose {
      if (SessionManager.session.value.personId != actor) inquiryModel.clear()
    }
  }
  val navigationSaver = remember {
    androidx.compose.runtime.saveable.listSaver<List<OffersDestination>, String>(
      save = { stack -> stack.map { destination -> when (destination) {
        is OffersDestination.OfferEditor -> "editor|${destination.id.orEmpty()}|${destination.entryKey}"
        is OffersDestination.OfferDetail -> "offer|${destination.slug}"
        is OffersDestination.ProviderProfile -> "profile|${destination.handle}"
        is OffersDestination.ConversationThread -> "thread|${destination.conversationId}"
        OffersDestination.MyOffers -> "mine"
        OffersDestination.SavedOffers -> "saved"
        OffersDestination.Inquired -> "inquired"
        OffersDestination.Following -> "following"
        OffersDestination.EditProfile -> "edit_profile"
        OffersDestination.ReferenceLocation -> "reference_location"
      } } },
      restore = { entries -> entries.mapNotNull { entry ->
        val parts = entry.split('|')
        when (parts.firstOrNull()) {
          "editor" -> parts.getOrNull(2)?.let { OffersDestination.OfferEditor(parts.getOrNull(1)?.takeIf { id -> id.isNotEmpty() }, it) }
          "offer" -> parts.getOrNull(1)?.let { OffersDestination.OfferDetail(it) }
          "profile" -> parts.getOrNull(1)?.let { OffersDestination.ProviderProfile(it) }
          "thread" -> parts.getOrNull(1)?.let { OffersDestination.ConversationThread(it) }
          "mine" -> OffersDestination.MyOffers
          "saved" -> OffersDestination.SavedOffers
          "inquired" -> OffersDestination.Inquired
          "following" -> OffersDestination.Following
          "edit_profile" -> OffersDestination.EditProfile
          "reference_location" -> OffersDestination.ReferenceLocation
          else -> null
        }
      } }
    )
  }
  var navStack by androidx.compose.runtime.saveable.rememberSaveable(stateSaver = navigationSaver) { mutableStateOf<List<OffersDestination>>(emptyList()) }
  val currentDestination = navStack.lastOrNull()
  val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState(
    initialFirstVisibleItemIndex = viewModel.scrollIndex,
    initialFirstVisibleItemScrollOffset = viewModel.scrollOffset
  )
  LaunchedEffect(gridState, viewModel) {
    androidx.compose.runtime.snapshotFlow {
      gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
    }.collect { (index, offset) -> viewModel.updateScrollPosition(index, offset) }
  }

  val activeSlug = (currentDestination as? OffersDestination.OfferDetail)?.slug
  var detail by remember(session.personId) { mutableStateOf<OfferDetail?>(null) }
  var detailLoading by remember { mutableStateOf(false) }
  var detailError by remember { mutableStateOf<String?>(null) }
  var detailRetry by remember { androidx.compose.runtime.mutableIntStateOf(0) }
  var savedStates by remember(session.personId) { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
  var savingSlugs by remember(session.personId) { mutableStateOf<Set<String>>(emptySet()) }
  var saveError by remember(session.personId) { mutableStateOf<String?>(null) }

  // Provider profile state
  val activeProviderHandle = (currentDestination as? OffersDestination.ProviderProfile)?.handle
  val currentProviderHandle by rememberUpdatedState(activeProviderHandle)
  var providerProfile by remember(session.personId) { mutableStateOf<ProviderProfile?>(null) }
  var providerLoading by remember(session.personId, activeProviderHandle) { mutableStateOf(false) }
  var providerError by remember(session.personId, activeProviderHandle) { mutableStateOf<String?>(null) }
  var providerRetry by remember { androidx.compose.runtime.mutableIntStateOf(0) }
  var followPending by remember(session.personId, activeProviderHandle) { mutableStateOf(false) }
  val providerScope = rememberProviderVisitScope(activeProviderHandle, session.personId, providerRetry)

  // My offers state
  val isMyOffersActive = currentDestination is OffersDestination.MyOffers
  var myOffers by remember(session.personId) { mutableStateOf<PaginatedOffers?>(null) }
  var myOffersLoading by remember { mutableStateOf(false) }
  var myOffersError by remember { mutableStateOf<String?>(null) }
  var myOffersRetry by remember { androidx.compose.runtime.mutableIntStateOf(0) }

  // Saved offers state
  val isSavedOffersActive = currentDestination is OffersDestination.SavedOffers
  var savedOffers by remember(session.personId) { mutableStateOf<PaginatedOffers?>(null) }
  var savedOffersLoading by remember { mutableStateOf(false) }
  var savedOffersError by remember { mutableStateOf<String?>(null) }
  var savedOffersRetry by remember { androidx.compose.runtime.mutableIntStateOf(0) }

  // Following state
  val isFollowingActive = currentDestination is OffersDestination.Following
  var following by remember(session.personId) { mutableStateOf<PaginatedFollowing?>(null) }
  var followingLoading by remember { mutableStateOf(false) }
  var followingError by remember { mutableStateOf<String?>(null) }
  var followingRetry by remember { androidx.compose.runtime.mutableIntStateOf(0) }

  var browserError by remember { mutableStateOf<String?>(null) }
  fun openWeb(path: String) {
    try {
      val scheme = when (ThemeManager.themeMode) {
        ThemeMode.SYSTEM -> CustomTabsIntent.COLOR_SCHEME_SYSTEM
        ThemeMode.LIGHT -> CustomTabsIntent.COLOR_SCHEME_LIGHT
        ThemeMode.DARK -> CustomTabsIntent.COLOR_SCHEME_DARK
      }
      CustomTabsIntent.Builder().setShowTitle(true).setColorScheme(scheme).build()
        .launchUrl(context, (base + path).toUri())
    } catch (_: Exception) {
      try { uriHandler.openUri(base + path) }
      catch (_: Exception) { browserError = "Couldn’t open your browser. Please try again." }
    }
  }

  val profileModel: ProfileViewModel = viewModel(key = "ProfileViewModel_${session.personId}", factory = remember(context.applicationContext) {
    ProfileViewModel.factory(context.applicationContext)
  })
  val profileState by profileModel.state.collectAsState()
  val messagesModel: MessagesInboxViewModel = viewModel(key = "MessagesInboxViewModel_${session.personId}", factory = remember(context.applicationContext) {
    MessagesInboxViewModel.factory(context.applicationContext)
  })
  val messagesState by messagesModel.state.collectAsState()
  LaunchedEffect(session.personId) {
    val person = session.personId
    if (person == null) {
      profileModel.clear()
      messagesModel.clear()
    } else messagesModel.loadUnread(person)
  }
  var rootResumeGeneration by remember(session.personId) { androidx.compose.runtime.mutableIntStateOf(0) }
  ObserveRootTabSelection(
    state = rootTabs,
    authenticated = session.isAuthenticated,
    rootVisible = currentDestination == null,
    onSignIn = onNavigateToSignIn,
    onSelected = { tab -> session.personId?.let { person ->
      when (tab) {
        RootTab.OFFERS -> Unit
        RootTab.MESSAGES -> messagesModel.load(person, force = true)
        RootTab.PROFILE -> profileModel.load(person, force = true)
      }
    } },
    activationKey = rootResumeGeneration
  )
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    rootResumeGeneration++
    session.personId?.let { person ->
      messagesModel.loadUnread(person)
    }
  }

  LaunchedEffect(activeSlug, session.personId, detailRetry) {
    val slug = activeSlug ?: run {
      detail = null
      detailError = null
      detailLoading = false
      return@LaunchedEffect
    }
    // Instant cache pre-population from feed items for seamless navigation
    val cachedSummary = state.items.firstOrNull { it.slug == slug }
    if (detail?.slug != slug) {
      if (cachedSummary != null) {
        detail = OfferDetail(
          id = cachedSummary.id,
          slug = cachedSummary.slug,
          title = cachedSummary.title,
          description = "",
          category = cachedSummary.category,
          priceDisplay = cachedSummary.priceDisplay,
          makerName = cachedSummary.makerName,
          makerHandle = cachedSummary.makerHandle,
          images = listOfNotNull(cachedSummary.coverUrl),
          location = null,
          isSaved = savedStates[slug] ?: false
        )
      } else {
        detail = null
      }
    }
    detailError = null
    detailLoading = (detail == null)
    try {
      val response = detailRepository.load(slug, session.isAuthenticated)
      detail = response
      savedStates = savedStates + (slug to response.isSaved)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) {
      if (detail == null) detailError = "Couldn’t load this offer. Please try again."
    }
    finally { detailLoading = false }
  }

  LaunchedEffect(activeProviderHandle, session.personId, providerRetry) {
    providerProfile = null
    providerError = null
    val handle = activeProviderHandle ?: return@LaunchedEffect
    providerLoading = true
    try {
      val refreshed = nativePagesRepo.getProviderProfile(handle, authenticated = session.isAuthenticated)
      savedStates = reconcileProviderSavedStates(savedStates, savingSlugs, refreshed.offers)
      providerProfile = refreshed
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

  fun currentSavedState(slug: String): Boolean = resolvedSavedState(
    savedStates[slug],
    providerProfile?.offers?.firstOrNull { it.slug == slug }?.isSaved,
    state.items.firstOrNull { it.slug == slug }?.isSaved,
    detail?.takeIf { it.slug == slug }?.isSaved
  )

  fun saveOffer(slug: String) {
    if (!session.isAuthenticated) { onNavigateToSignIn(); return }
    if (slug in savingSlugs) return
    val actor = session.personId
    savingSlugs = savingSlugs + slug

    val previousState = currentSavedState(slug)
    val optimisticTarget = requestedSavedState(previousState)
    // Instant optimistic update
    savedStates = savedStates + (slug to optimisticTarget)
    if (detail?.slug == slug) detail = detail?.copy(isSaved = optimisticTarget)

    scope.launch {
      try {
        val confirmed = detailRepository.setSaved(slug, optimisticTarget)
        if (SessionManager.session.value.personId == actor) {
          savedStates = savedStates + (slug to confirmed)
          if (detail?.slug == slug) detail = detail?.copy(isSaved = confirmed)
          if (!confirmed) {
            savedOffers = savedOffers?.copy(items = (savedOffers?.items ?: emptyList()).filter { it.slug != slug })
          }
        }
      } catch (cancelled: CancellationException) { throw cancelled }
      catch (_: Exception) {
        // Rollback on failure
        if (SessionManager.session.value.personId == actor) {
          savedStates = savedStates + (slug to previousState)
          if (detail?.slug == slug) detail = detail?.copy(isSaved = previousState)
        }
        if (SessionManager.session.value.personId == actor)
          saveError = "Couldn’t update saved offer. Please try again."
      }
      finally { if (SessionManager.session.value.personId == actor) savingSlugs = savingSlugs - slug }
    }
  }

  if (saveError != null) AlertDialog(onDismissRequest = { saveError = null },
    title = { Text("Couldn’t save offer") }, text = { Text(saveError.orEmpty()) },
    confirmButton = { TextButton(onClick = { saveError = null }) { Text("Close") } })

  inquiryState.target?.let { target ->
    NativeMessageComposerSheet(
      target = target,
      text = inquiryState.text,
      isSending = inquiryState.isSending,
      error = inquiryState.error,
      onTextChanged = inquiryModel::onTextChanged,
      onSend = inquiryModel::send,
      onDismiss = inquiryModel::dismiss
    )
  }
  LaunchedEffect(inquiryState.startedConversationId) {
    val conversationId = inquiryState.startedConversationId ?: return@LaunchedEffect
    inquiryModel.consumeStartedConversation()
    navStack = navStack + OffersDestination.ConversationThread(conversationId)
  }

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
          onMessageProvider = {
            inquiryModel.openOffer(
              slug = currentDestination.slug,
              offerTitle = detail?.title.orEmpty(),
              makerName = detail?.makerName.orEmpty()
            )
          },
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
          onMessageProvider = { handle ->
            if (session.isAuthenticated) {
              inquiryModel.openPerson(
                handle = handle,
                displayName = providerProfile?.person?.displayName.orEmpty()
              )
            } else {
              onNavigateToSignIn()
            }
          },
          onFollowProvider = { handle ->
            if (!session.isAuthenticated) {
              onNavigateToSignIn()
            } else {
              val actor = session.personId
              val person = providerProfile?.person
              if (person?.isFollowing != null && !followPending) {
                val target = !person.isFollowing
                followPending = true
                providerError = null
                scope.launch {
                  try {
                    val confirmed = if (target) nativePagesRepo.follow(handle) else nativePagesRepo.unfollow(handle)
                    if (SessionManager.session.value.personId == actor && currentProviderHandle == handle) {
                      providerProfile = providerProfile?.let {
                        if (it.person.id == person.id) it.copy(person = it.person.copy(isFollowing = confirmed)) else it
                      }
                    }
                  } catch (cancelled: CancellationException) { throw cancelled }
                  catch (_: Exception) {
                    if (SessionManager.session.value.personId == actor && currentProviderHandle == handle)
                      providerError = "Couldn’t update following. Please try again."
                  } finally {
                    if (SessionManager.session.value.personId == actor && currentProviderHandle == handle) followPending = false
                  }
                }
              }
            }
          },
          onLoadMore = {
            val cursor = providerProfile?.nextCursor
            if (cursor != null && !providerLoading) {
              val actor = session.personId
              val handle = currentDestination.handle
              providerLoading = true
              providerError = null
              providerScope.launch {
                try {
                  val more = nativePagesRepo.getProviderProfile(handle, after = cursor, authenticated = actor != null)
                  kotlinx.coroutines.currentCoroutineContext().ensureActive()
                  if (SessionManager.session.value.personId == actor && currentProviderHandle == handle) {
                    savedStates = reconcileProviderSavedStates(savedStates, savingSlugs, more.offers)
                    providerProfile = providerProfile?.copy(
                      offers = (providerProfile?.offers.orEmpty() + more.offers).distinctBy { it.id },
                      nextCursor = more.nextCursor
                    )
                  }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                  if (SessionManager.session.value.personId == actor && currentProviderHandle == handle)
                    providerError = "Couldn’t load more offers. Please try again."
                } finally {
                  if (SessionManager.session.value.personId == actor && currentProviderHandle == handle)
                    providerLoading = false
                }
              }
            }
          },
          followPending = followPending,
          savedStates = providerProfile?.offers.orEmpty().associate { it.slug to currentSavedState(it.slug) },
          savingSlugs = savingSlugs,
          onSaveOffer = ::saveOffer,
          isOwnProfile = (session.personId != null && providerProfile?.person?.id == session.personId),
          isFollowing = providerProfile?.person?.isFollowing ?: false,
          modifier = modifier
        )
        return
      }
      is OffersDestination.EditProfile -> {
        if (!session.isAuthenticated) {
          onNavigateToSignIn()
          navStack = navStack.dropLast(1)
          return
        }
        val editProfileModel: com.example.ui.model.EditProfileViewModel = viewModel(
          key = "edit_profile_${session.personId}",
          factory = remember(context.applicationContext) {
            com.example.ui.model.EditProfileViewModel.factory(context.applicationContext)
          }
        )
        EditProfileScreen(
          viewModel = editProfileModel,
          initialProfile = profileState.profile,
          onBack = { navStack = navStack.dropLast(1) },
          onSaveSuccess = {
            session.personId?.let { profileModel.load(it, force = true) }
            navStack = navStack.dropLast(1)
          },
          modifier = modifier
        )
        return
      }
      is OffersDestination.ReferenceLocation -> {
        ReferenceLocationScreen(
          currentReference = state.activeReference,
          onApplyReference = { candidate ->
            viewModel.applyReference(candidate)
            navStack = navStack.dropLast(1)
          },
          onTurnOffProximity = {
            viewModel.turnOffProximity()
            navStack = navStack.dropLast(1)
          },
          onBack = { navStack = navStack.dropLast(1) },
          modifier = modifier
        )
        return
      }
      is OffersDestination.OfferEditor -> {
        if (session.isAuthenticated) {
          val editor: com.example.ui.model.OfferEditorViewModel = viewModel(
            key = "offer_editor_${session.personId}_${currentDestination.entryKey}",
            factory = remember(currentDestination.entryKey, session.personId) {
              com.example.ui.model.OfferEditorViewModel.factory(context.applicationContext, currentDestination.id, session.personId)
            }
          )
          NativeOfferEditorScreen(editor, editing = currentDestination.id != null,
            onBack = { navStack = navStack.dropLast(1) },
            onSaved = {
              myOffersRetry++
              detailRetry++
              viewModel.refresh()
              navStack = listOf(OffersDestination.MyOffers)
            })
          return
        }
      }
      is OffersDestination.MyOffers -> {
        if (!session.isAuthenticated) {
          onNavigateToSignIn()
          navStack = navStack.dropLast(1)
          return
        }
        val myOffersModel: com.example.ui.model.MyOffersViewModel = viewModel(
          key = "my_offers_${session.personId}_$myOffersRetry",
          factory = remember(context.applicationContext, session.personId) {
            com.example.ui.model.MyOffersViewModel.factory(context.applicationContext, session.personId)
          }
        )
        MyOffersScreen(
          viewModel = myOffersModel,
          onBack = { navStack = navStack.dropLast(1) },
          onOpenOffer = { slug -> navStack = navStack + OffersDestination.OfferDetail(slug) },
          onCreateOffer = { navStack = navStack + OffersDestination.OfferEditor() },
          onEditOffer = { id -> navStack = navStack + OffersDestination.OfferEditor(id) },
          modifier = modifier
        )
        return
      }
      is OffersDestination.SavedOffers -> {
        if (!session.isAuthenticated) {
          onNavigateToSignIn()
          navStack = navStack.dropLast(1)
          return
        }
        val savedOffersModel: com.example.ui.model.SavedOffersViewModel = viewModel(
          key = "saved_offers_${session.personId}_$savedOffersRetry",
          factory = remember(context.applicationContext) {
            com.example.ui.model.SavedOffersViewModel.factory(context.applicationContext)
          }
        )
        SavedOffersScreen(
          viewModel = savedOffersModel,
          onBack = { navStack = navStack.dropLast(1) },
          onOpenOffer = { slug -> navStack = navStack + OffersDestination.OfferDetail(slug) },
          onBrowseOffers = {
            navStack = emptyList()
            rootTabs.select(RootTab.OFFERS)
          },
          onOpenProvider = { handle -> navStack = navStack + OffersDestination.ProviderProfile(handle) },
          modifier = modifier
        )
        return
      }
      is OffersDestination.Inquired -> {
        if (!session.isAuthenticated) {
          onNavigateToSignIn()
          navStack = navStack.dropLast(1)
          return
        }
        val inquiredModel: com.example.ui.model.InquiredViewModel = viewModel(
          key = "inquired_${session.personId}",
          factory = remember(context.applicationContext, session.personId) {
            com.example.ui.model.InquiredViewModel.factory(context.applicationContext, session.personId)
          }
        )
        InquiredScreen(
          viewModel = inquiredModel,
          onBack = { navStack = navStack.dropLast(1) },
          onOpenOffer = { slug -> navStack = navStack + OffersDestination.OfferDetail(slug) },
          onBrowseOffers = {
            navStack = emptyList()
            rootTabs.select(RootTab.OFFERS)
          },
          onOpenProvider = { handle -> navStack = navStack + OffersDestination.ProviderProfile(handle) },
          modifier = modifier
        )
        return
      }
      is OffersDestination.Following -> {
        if (!session.isAuthenticated) {
          onNavigateToSignIn()
          navStack = navStack.dropLast(1)
          return
        }
        val followingModel: com.example.ui.model.FollowingViewModel = viewModel(
          key = "following_${session.personId}_$followingRetry",
          factory = remember(context.applicationContext) {
            com.example.ui.model.FollowingViewModel.factory(context.applicationContext)
          }
        )
        FollowingScreen(
          viewModel = followingModel,
          onBack = { navStack = navStack.dropLast(1) },
          onOpenProvider = { handle -> navStack = navStack + OffersDestination.ProviderProfile(handle) },
          onOpenWebFollow = { handle -> openWeb("/profiles/${Uri.encode(handle)}") },
          onDiscoverOffers = {
            navStack = emptyList()
            rootTabs.select(RootTab.OFFERS)
          },
          modifier = modifier
        )
        return
      }
      is OffersDestination.ConversationThread -> {
        val threadModel: ConversationThreadViewModel = viewModel(
          key = "thread_${session.personId}_${currentDestination.conversationId}",
          factory = remember(context.applicationContext) {
            ConversationThreadViewModel.factory(context.applicationContext)
          }
        )
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(threadModel, lifecycle, session.personId) {
          threadModel.openConversation(currentDestination.conversationId, session.personId)
          val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) threadModel.startLiveUpdates()
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) threadModel.stopLiveUpdates()
          }
          lifecycle.addObserver(observer)
          if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) threadModel.startLiveUpdates()
          onDispose { lifecycle.removeObserver(observer); threadModel.stopLiveUpdates() }
        }
        val threadState by threadModel.uiState.collectAsState()
        NativeConversationThreadScreen(
          state = threadState,
          onBack = { navStack = navStack.dropLast(1) },
          onInputChanged = threadModel::onInputTextChanged,
          onSendMessage = threadModel::sendMessage,
          onRetryMessage = threadModel::retryMessage,
          onLoadOlder = threadModel::loadOlderMessages,
          onOpenProfile = { handle -> navStack = navStack + OffersDestination.ProviderProfile(handle) },
          modifier = modifier
        )
        return
      }
    }
  }

  BackHandler {
    when {
      navStack.isNotEmpty() -> navStack = navStack.dropLast(1)
      rootTabs.currentTab != RootTab.OFFERS -> rootTabs.select(RootTab.OFFERS)
      else -> onNavigateBack()
    }
  }

  Column(modifier.fillMaxSize().background(HavenColors.Canvas).testTag("offers_screen")) {
    HavenRootPager(
      state = rootTabs,
      authenticated = session.isAuthenticated,
      modifier = Modifier.weight(1f).fillMaxWidth(),
      swipeEnabled = navStack.isEmpty() && !state.isFilterSheetVisible && browserError == null && !profileModalOpen
    ) { tab ->
      when (tab) {
        RootTab.OFFERS -> Column(Modifier.fillMaxSize()) {
          HavenTopBarContainer(backgroundColor = HavenColors.Canvas) {
      val isFeedDisabled = (browserError ?: state.error) != null && state.items.isEmpty()
      Column(modifier = if (isFeedDisabled) Modifier.alpha(0.45f) else Modifier) {
        FeedHeaderLayout(search = { searchModifier ->
          Row(
            searchModifier
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
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
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
        }, actions = {
          FeedActionButton(
            icon = Icons.Outlined.Tune, description = "Filters", tag = "open_filters_button",
            enabled = !isFeedDisabled && state.currencies.isNotEmpty(),
            badge = state.activeFilterCount > 0, onClick = viewModel::openFilterSheet
          )
          FeedActionButton(
            icon = Icons.Outlined.LocationOn, description = "Choose a place on Haven", tag = "choose_location_button",
            enabled = !isFeedDisabled, emphasized = state.activeReference != null,
            onClick = { navStack = navStack + OffersDestination.ReferenceLocation }
          )
          FeedActionButton(
            icon = Icons.Outlined.Add, description = "Post an offer", tag = "post_offer_button",
            enabled = !isFeedDisabled, emphasized = true,
            onClick = {
              if (session.isAuthenticated) navStack = navStack + OffersDestination.OfferEditor() else onNavigateToSignIn()
            }
          )
        })
        if (!isFeedDisabled) {
          if (state.activeReference != null) {
            Row(
              Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                Modifier
                  .clip(RoundedCornerShape(14.dp))
                  .background(HavenColors.SurfaceContainer)
                  .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  Icons.Outlined.LocationOn,
                  contentDescription = null,
                  tint = HavenColors.TextPrimary,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = "Near ${state.activeReference?.label}",
                  fontFamily = InterFontFamily,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium,
                  color = HavenColors.TextPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.clickable { navStack = navStack + OffersDestination.ReferenceLocation }
                )
                IconButton(
                  onClick = { viewModel.turnOffProximity() },
                  modifier = Modifier.size(18.dp)
                ) {
                  Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Turn off proximity",
                    tint = HavenColors.TextSecondary,
                    modifier = Modifier.size(12.dp)
                  )
                }
              }
            }
          }
          Row(
            Modifier
              .nestedScroll(CategoryScrollBoundary)
              .horizontalScroll(categoryScroll)
              .testTag("offer_categories")
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
              CategoryChip("All", state.query.category.isEmpty() && state.query.scope != "following") {
                if (state.query.scope == "following") viewModel.setScope("public")
                viewModel.selectCategory("")
              }
              if (session.isAuthenticated) {
                CategoryChip("Following", state.query.scope == "following") {
                  if (state.query.scope == "following") {
                    viewModel.setScope("public")
                  } else {
                    viewModel.setScope("following")
                  }
                }
              }
              state.categories.forEach { category ->
                CategoryChip(category.label, state.query.category == category.slug && state.query.scope != "following") {
                  if (state.query.scope == "following") viewModel.setScope("public")
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
      state = gridState,
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
          showDistance = state.activeReference != null,
          saved = currentSavedState(offer.slug),
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
          val errorTitle = if (state.requiresSignIn) "Sign in to continue"
            else if (isOffline) "You're offline" else "Something went wrong on our side"
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
              primaryActionLabel = if (state.requiresSignIn) "Sign in" else "Try again",
              onPrimaryAction = {
                browserError = null
                if (state.requiresSignIn) onNavigateToSignIn()
                else if (state.items.isNotEmpty() && state.nextCursor != null) viewModel.loadMore() else viewModel.refresh()
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
            isCategory -> { { if (session.isAuthenticated) run { navStack = navStack + OffersDestination.OfferEditor() } else onNavigateToSignIn() } }
            else -> { { viewModel.clearFilters() } }
          }
          val secondaryLabel = when {
            isSearch -> "Post an offer"
            isCategory -> "Clear filters"
            else -> "Post an offer"
          }
          val onSecondary: () -> Unit = when {
            isSearch -> { { if (session.isAuthenticated) run { navStack = navStack + OffersDestination.OfferEditor() } else onNavigateToSignIn() } }
            isCategory -> { { viewModel.clearFilters() } }
            else -> { { if (session.isAuthenticated) run { navStack = navStack + OffersDestination.OfferEditor() } else onNavigateToSignIn() } }
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
        RootTab.MESSAGES -> {
          if (session.isAuthenticated) {
            MessagesInboxScreen(
              state = messagesState,
              onRetry = { session.personId?.let { messagesModel.load(it, force = true) } },
              onLoadMore = messagesModel::loadMore,
              onSelectConversation = { id ->
                navStack = navStack + OffersDestination.ConversationThread(id)
              },
              modifier = Modifier.fillMaxSize(),
              listState = inboxScroll
            )
          } else {
            Box(Modifier.fillMaxSize())
          }
        }
        RootTab.PROFILE -> {
          if (session.isAuthenticated) {
            val visibleProfile = if (profileState.profile?.let { it.id != session.personId } == true)
              com.example.ui.model.ProfileUiState(loading = true) else profileState
            ProfileScreen(
              visibleProfile,
              onRetry = { session.personId?.let { profileModel.load(it, force = true) } },
              onOpenWeb = ::openWeb,
              onSignOut = onSignOut,
              onOpenMyOffers = { navStack = navStack + OffersDestination.MyOffers },
              onOpenSavedOffers = { navStack = navStack + OffersDestination.SavedOffers },
              onOpenInquired = { navStack = navStack + OffersDestination.Inquired },
              onOpenFollowing = { navStack = navStack + OffersDestination.Following },
              onOpenOffer = { slug -> navStack = navStack + OffersDestination.OfferDetail(slug) },
              onOpenPublicProfile = { handle -> navStack = navStack + OffersDestination.ProviderProfile(handle) },
              onOpenEditProfile = { navStack = navStack + OffersDestination.EditProfile },
              modifier = Modifier.fillMaxSize(),
              scrollState = profileScroll,
              onModalVisibilityChanged = { profileModalOpen = it }
            )
          } else {
            Box(Modifier.fillMaxSize())
          }
        }
      }
    }
    HavenBottomBarContainer(
      backgroundColor = HavenColors.Canvas,
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
          selected = rootTabs.currentTab == RootTab.OFFERS
        ) {
          val wasAlreadyOnOffers = rootTabs.currentTab == RootTab.OFFERS && navStack.isEmpty()
          if (navStack.isNotEmpty()) {
            navStack = emptyList()
          }
          rootTabs.select(RootTab.OFFERS)
          if (wasAlreadyOnOffers) {
            scope.launch { gridState.animateScrollToItem(0) }
            viewModel.refresh()
          }
        }

        val hasUnread = if (messagesState.unreadConfirmed) messagesState.globalUnread
          else messagesState.globalUnread || messagesState.items.any { it.isUnread }
        FeedNavItem(
          label = "Messages",
          activeIcon = Icons.Filled.ChatBubble,
          inactiveIcon = Icons.Outlined.ChatBubbleOutline,
          selected = rootTabs.currentTab == RootTab.MESSAGES && session.isAuthenticated,
          badgeCount = 0,
          hasUnreadDot = hasUnread
        ) {
          if (session.isAuthenticated) {
            if (navStack.isNotEmpty()) navStack = emptyList()
            rootTabs.select(RootTab.MESSAGES)
          } else {
            HavenAuthClient.logDiagnostic("UI", "NAVIGATE_TO_SIGN_IN", "Messages tab clicked while unauthenticated; navigating to Sign In screen.")
            onNavigateToSignIn()
          }
        }

        FeedNavItem(
          label = if (session.isAuthenticated) "Profile" else "Log in",
          activeIcon = Icons.Filled.Person,
          inactiveIcon = Icons.Outlined.PersonOutline,
          selected = rootTabs.currentTab == RootTab.PROFILE && session.isAuthenticated,
          isLogInAction = !session.isAuthenticated
        ) {
          if (session.isAuthenticated) {
            if (navStack.isNotEmpty()) navStack = emptyList()
            rootTabs.select(RootTab.PROFILE)
          } else {
            HavenAuthClient.logDiagnostic("UI", "NAVIGATE_TO_SIGN_IN", "Log in tab clicked while unauthenticated; navigating to Sign In screen.")
            onNavigateToSignIn()
          }
        }
      }
    }
  }
  if (rootTabs.currentTab != RootTab.OFFERS && browserError != null) AlertDialog(
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
  hasUnreadDot: Boolean = false,
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
      .selectable(selected = selected, role = Role.Tab, onClick = onClick)
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
            } else if (hasUnreadDot) {
              Badge(
                containerColor = HavenColors.Primary,
                modifier = Modifier.size(6.dp)
              )
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

/** Rendering and mutation share authoritative local state and the same optional-response fallbacks. */
internal fun resolvedSavedState(local: Boolean?, provider: Boolean?, feed: Boolean?, detail: Boolean?): Boolean =
  local ?: provider ?: feed ?: detail ?: false

/** A profile visit owns paging work; leaving or reloading cancels its previous requests. */
@Composable
internal fun rememberProviderVisitScope(handle: String?, actor: String?, retry: Int): kotlinx.coroutines.CoroutineScope =
  key(handle, actor, retry) { rememberCoroutineScope() }

/** Fresh known server values supersede settled overrides, while pending optimistic saves remain intact. */
internal fun reconcileProviderSavedStates(
  overrides: Map<String, Boolean>,
  pending: Set<String>,
  offers: List<com.example.data.FeedOffer>
): Map<String, Boolean> = overrides + offers.mapNotNull { offer ->
  offer.isSaved?.takeIf { offer.slug !in pending }?.let { offer.slug to it }
}.toMap()
