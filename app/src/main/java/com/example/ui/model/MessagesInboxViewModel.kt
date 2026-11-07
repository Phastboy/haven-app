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

data class MessagesInboxUiState(
  val items: List<InboxConversation> = emptyList(),
  val nextCursor: String? = null,
  val loading: Boolean = false,
  val loadingMore: Boolean = false,
  val error: String? = null,
  val globalUnread: Boolean = false,
  val unreadConfirmed: Boolean = false
)

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
    if (!force && actor == personId && (_state.value.loading || _state.value.items.isNotEmpty() || _state.value.nextCursor == null && _state.value.error != null)) {
      refreshUnread()
      return
    }
    if (actor != personId) clear()
    job?.cancel()
    actor = personId

    val hasExistingItems = _state.value.items.isNotEmpty()
    if (!hasExistingItems) {
      _state.value = _state.value.copy(loading = true, loadingMore = false, error = null)
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
              loading = if (inboxItems.isNotEmpty()) false else _state.value.loading
            )
          }
        }
      }

      // 2. Refresh from transport in background
      job = viewModelScope.launch {
        val result = localMessagingRepository.refreshConversations(personId)
        if (actor == personId) {
          if (result.isSuccess) {
            _state.value = _state.value.copy(loading = false, error = null)
          } else {
            val isCacheAvailable = _state.value.items.isNotEmpty()
            _state.value = _state.value.copy(
              loading = false,
              error = if (isCacheAvailable) null else "Couldn’t load messages. Please try again."
            )
          }
        }
      }
    } else {
      // Fallback for tests or when local repository is not provided
      job = viewModelScope.launch {
        try {
          val page = repository.load(null)
          if (actor == personId) _state.value = page.toState().copy(globalUnread = _state.value.globalUnread)
        } catch (cancelled: CancellationException) {
          throw cancelled
        } catch (_: Exception) {
          if (actor == personId) _state.value = MessagesInboxUiState(error = "Couldn’t load messages. Please try again.")
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
