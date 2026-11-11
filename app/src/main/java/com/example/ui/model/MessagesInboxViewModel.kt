package com.example.ui.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auth.HavenAuthClient
import com.example.data.ConversationInboxPage
import com.example.data.HavenMessagesRepository
import com.example.data.InboxConversation
import com.example.data.MessagesInboxRepository
import com.example.data.messaging.HavenLocalFirstMessagingRepository
import com.example.data.messaging.MessagingRepository
import com.example.data.messaging.db.ConversationEntity
import com.example.data.messaging.db.HavenDatabase
import com.example.data.messaging.transport.HavenProductionMessagingTransport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the inbox message area should render.
 *
 * Cached rows always win, so ordinary revalidations never blank the screen:
 * [RESOLVING_CACHE] and [LOADING] are only reachable while no cached row
 * exists to display, and a successfully loaded empty inbox keeps its truthful
 * empty shell across later refreshes instead of flashing a spinner again.
 */
enum class InboxContentState {
  /** Cached or synced conversation rows are available to render. */
  CONTENT,

  /** The inbox was opened and the first local Room emission has not arrived yet. */
  RESOLVING_CACHE,

  /** The local cache holds no row yet and the remote inbox load has not settled. */
  LOADING,

  /** A successful load confirmed this inbox has no conversations. */
  EMPTY,

  /** The inbox could not be loaded and no cached row exists to fall back to. */
  UNAVAILABLE
}

data class MessagesInboxUiState(
  val items: List<InboxConversation> = emptyList(),
  val nextCursor: String? = null,
  val loading: Boolean = false,
  val loadingMore: Boolean = false,
  val error: String? = null,
  val globalUnread: Boolean = false,
  val unreadConfirmed: Boolean = false,
  /** Offline-style refresh failure while cached rows stay visible. */
  val offlineBanner: String? = null,
  /** True once the local Room query has emitted at least once for this owner. */
  val localHistoryResolved: Boolean = false,
  /** True once a successful load confirmed the persisted inbox is empty. */
  val emptyInboxConfirmed: Boolean = false
) {
  /**
   * Derived, never stored: the renderer decides row skeletons, the empty
   * shell, the unavailable state or conversation rows from this value alone.
   */
  val contentState: InboxContentState
    get() = when {
      items.isNotEmpty() -> InboxContentState.CONTENT
      !localHistoryResolved -> InboxContentState.RESOLVING_CACHE
      error != null && !loading -> InboxContentState.UNAVAILABLE
      emptyInboxConfirmed -> InboxContentState.EMPTY
      else -> InboxContentState.LOADING
    }
}

class MessagesInboxViewModel(
  private val repository: MessagesInboxRepository,
  private val localMessagingRepository: MessagingRepository? = null
) : ViewModel() {
  private val _state = MutableStateFlow(MessagesInboxUiState())
  val state = _state.asStateFlow()
  private var actor: String? = null
  private var job: Job? = null
  private var observeJob: Job? = null
  private var unreadJob: Job? = null

  fun clear() {
    job?.cancel()
    unreadJob?.cancel()
    observeJob?.cancel()
    actor = null
    _state.value = MessagesInboxUiState()
  }

  /** Global badge loading must not fetch/precompose the private inbox off-tab. */
  fun loadUnread(personId: String) {
    if (actor != personId) {
      clear()
      actor = personId
    }
    refreshUnread()
  }

  fun refreshUnread() {
    val requestedActor = actor ?: return
    if (unreadJob?.isActive == true) return
    unreadJob = viewModelScope.launch {
      try {
        val hasUnread = repository.getGlobalUnread()
        if (actor == requestedActor) _state.update { it.copy(globalUnread = hasUnread, unreadConfirmed = true) }
      } catch (cancelled: CancellationException) { throw cancelled }
      catch (_: Exception) {
        if (actor == requestedActor) _state.update { it.copy(unreadConfirmed = false) }
      }
    }
  }

  fun load(personId: String, force: Boolean = false) {
    // A loaded-empty inbox counts as loaded: re-entering the tab revalidates
    // quietly instead of flashing the cold loading state again.
    if (!force && actor == personId && (_state.value.loading || _state.value.items.isNotEmpty() || _state.value.emptyInboxConfirmed || _state.value.nextCursor == null && _state.value.error != null)) {
      refreshUnread()
      return
    }
    if (actor != personId) clear()
    job?.cancel()
    actor = personId

    val hasExistingItems = _state.value.items.isNotEmpty()
    if (!hasExistingItems) {
      _state.value = _state.value.copy(loading = true, loadingMore = false, error = null, offlineBanner = null)
    }

    refreshUnread()

    if (localMessagingRepository != null) {
      // 1. Observe local Room database reactively
      observeJob?.cancel()
      observeJob = viewModelScope.launch {
        localMessagingRepository.observeConversations(personId).collect { entities ->
          if (actor == personId) {
            val inboxItems = entities.map { it.toInboxConversation() }
            _state.value = _state.value.copy(
              items = inboxItems,
              loading = if (inboxItems.isNotEmpty()) false else _state.value.loading,
              localHistoryResolved = true,
              emptyInboxConfirmed = _state.value.emptyInboxConfirmed && inboxItems.isEmpty()
            )
          }
        }
      }

      // 2. Refresh from transport in background
      job = viewModelScope.launch {
        val result = localMessagingRepository.refreshConversations(personId)
        if (actor == personId) {
          if (result.isSuccess) {
            // Read the persisted rows directly: the reactive Room emission can
            // lag behind this update, and only a confirmed empty store may
            // settle the empty state without flashing it over rows that are
            // about to land.
            val snapshotEmpty = localMessagingRepository.snapshotConversations(personId).isEmpty()
            _state.value = _state.value.copy(
              loading = false,
              error = null,
              offlineBanner = null,
              emptyInboxConfirmed = snapshotEmpty
            )
          } else {
            val isCacheAvailable = _state.value.items.isNotEmpty()
            _state.value = _state.value.copy(
              loading = false,
              error = if (isCacheAvailable) null else "Couldn’t load messages. Please try again.",
              offlineBanner = if (isCacheAvailable) "Offline - viewing cached messages" else null
            )
          }
        }
      }
    } else {
      // Fallback for tests or when local repository is not provided
      job = viewModelScope.launch {
        try {
          val page = repository.load(null)
          if (actor == personId) _state.value = page.toState().copy(
            globalUnread = _state.value.globalUnread,
            unreadConfirmed = _state.value.unreadConfirmed,
            localHistoryResolved = true,
            emptyInboxConfirmed = page.items.isEmpty()
          )
        } catch (cancelled: CancellationException) {
          throw cancelled
        } catch (_: Exception) {
          if (actor == personId) _state.value = MessagesInboxUiState(
            error = "Couldn’t load messages. Please try again.",
            localHistoryResolved = true
          )
        }
      }
    }
  }

  fun loadMore() {
    val personId = actor ?: return
    if (_state.value.loading || _state.value.loadingMore) return

    if (localMessagingRepository != null) {
      _state.value = _state.value.copy(loadingMore = true, error = null)
      job = viewModelScope.launch {
        val result = localMessagingRepository.loadMoreConversations(personId)
        if (actor == personId) {
          _state.value = _state.value.copy(
            loadingMore = false,
            error = if (result.isFailure) "Couldn’t load more messages. Please try again." else null
          )
        }
      }
    } else {
      val cursor = _state.value.nextCursor ?: return
      _state.value = _state.value.copy(loadingMore = true, error = null)
      job = viewModelScope.launch {
        try {
          val page = repository.load(cursor)
          if (actor == personId) {
            val current = _state.value
            _state.value = current.copy(
              items = (current.items + page.items).distinctBy(InboxConversation::id),
              nextCursor = page.nextCursor,
              loadingMore = false,
              error = null
            )
          }
        } catch (cancelled: CancellationException) {
          throw cancelled
        } catch (_: Exception) {
          if (actor == personId) _state.value = _state.value.copy(loadingMore = false, error = "Couldn’t load more messages. Please try again.")
        }
      }
    }
  }

  private fun ConversationInboxPage.toState() = MessagesInboxUiState(items, nextCursor)

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MessagesInboxViewModel::class.java))
        val authClient = HavenAuthClient.shared(context)
        val database = HavenDatabase.getInstance(context)
        val transport = HavenProductionMessagingTransport(authClient)
        val localRepo = HavenLocalFirstMessagingRepository(database, transport)
        val httpRepo = HavenMessagesRepository(authClient)
        return MessagesInboxViewModel(
          repository = httpRepo,
          localMessagingRepository = localRepo
        ) as T
      }
    }
  }
}

private fun ConversationEntity.toInboxConversation(): InboxConversation {
  return InboxConversation(
    id = id,
    otherUserId = otherUserId,
    displayName = displayName,
    handle = handle,
    avatarUrl = avatarUrl,
    lastMessageBody = lastMessageBody,
    lastMessageAt = lastMessageAt,
    isUnread = isUnread
  )
}
