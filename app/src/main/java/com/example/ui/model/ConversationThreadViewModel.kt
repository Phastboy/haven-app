package com.example.ui.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auth.HavenAuthClient
import com.example.data.messaging.HavenLocalFirstMessagingRepository
import com.example.data.messaging.MessagingRepository
import com.example.data.messaging.db.ConversationEntity
import com.example.data.messaging.db.HavenDatabase
import com.example.data.messaging.db.MessageEntity
import com.example.data.messaging.transport.HavenProductionMessagingTransport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the thread's message area should render for the initial history.
 *
 * Cached content always wins, so ordinary refreshes never blank the screen:
 * [RESOLVING_CACHE] and [LOADING_HISTORY] are only reachable while no cached
 * message exists to display.
 */
enum class ThreadContentState {
  /** Cached or synced messages are available to render. */
  CONTENT,

  /** The thread was opened and the first local Room emission has not arrived yet. */
  RESOLVING_CACHE,

  /** The local cache holds no message yet and the remote history load has not settled. */
  LOADING_HISTORY,

  /** A successful sync confirmed this thread has no messages. */
  EMPTY,

  /** The history could not be loaded and no cached message exists to fall back to. */
  UNAVAILABLE
}

data class ConversationThreadUiState(
  val conversation: ConversationEntity? = null,
  val messages: List<MessageEntity> = emptyList(),
  val isLoadingOlder: Boolean = false,
  val olderError: String? = null,
  val hasOlder: Boolean = true,
  val inputText: String = "",
  val isSyncing: Boolean = false,
  val syncError: String? = null,
  val sendError: String? = null,
  /** True once the local Room query has emitted at least once for this thread. */
  val localHistoryResolved: Boolean = false,
  /** True once a successful sync confirmed the persisted thread is empty. */
  val emptyThreadConfirmed: Boolean = false,
  val pendingAttachedOfferSlug: String? = null,
  val pendingAttachedOfferTitle: String? = null,
  val pendingAttachedOfferPrice: String? = null
) {
  /**
   * Derived, never stored: the renderer decides skeletons, empty state,
   * unavailable state or message content from this value alone.
   */
  val contentState: ThreadContentState
    get() = when {
      messages.isNotEmpty() -> ThreadContentState.CONTENT
      !localHistoryResolved -> ThreadContentState.RESOLVING_CACHE
      syncError != null && !isSyncing -> ThreadContentState.UNAVAILABLE
      emptyThreadConfirmed -> ThreadContentState.EMPTY
      else -> ThreadContentState.LOADING_HISTORY
    }
}

class ConversationThreadViewModel(
  private val repository: MessagingRepository,
  private val eventsListener: com.example.data.messaging.ConversationEventsListener? = null
) : ViewModel() {

  private val _uiState = MutableStateFlow(ConversationThreadUiState())
  val uiState: StateFlow<ConversationThreadUiState> = _uiState.asStateFlow()

  private var observeMessagesJob: Job? = null
  private var observeConversationJob: Job? = null
  private var sseJob: Job? = null
  private var pollJob: Job? = null
  private var refreshJob: Job? = null
  private var olderJob: Job? = null
  private var generation = 0L
  private var currentConversationId: String? = null
  private var ownerPersonId: String? = null

  fun clear() {
    generation++
    refreshJob?.cancel()
    olderJob?.cancel()
    observeMessagesJob?.cancel()
    observeConversationJob?.cancel()
    sseJob?.cancel()
    pollJob?.cancel()
    currentConversationId = null
    ownerPersonId = null
    _uiState.value = ConversationThreadUiState()
  }

  override fun onCleared() {
    clear()
  }

  fun openConversation(conversationId: String, ownerPersonId: String? = null) {
    if (currentConversationId == conversationId && this.ownerPersonId == ownerPersonId) {
      refreshThread()
      return
    }
    clear()
    this.ownerPersonId = ownerPersonId
    currentConversationId = conversationId

    observeMessagesJob?.cancel()
    observeConversationJob?.cancel()
    sseJob?.cancel()
    pollJob?.cancel()

    _uiState.update {
      it.copy(
        conversation = null,
        messages = emptyList(),
        inputText = "",
        isSyncing = true,
        syncError = null,
        sendError = null,
        localHistoryResolved = false,
        emptyThreadConfirmed = false
      )
    }

    // Observe local conversation summary
    observeConversationJob = viewModelScope.launch {
      repository.observeConversation(conversationId).collect { conv ->
        _uiState.update { it.copy(conversation = conv) }
      }
    }

    // Observe local messages from Room
    observeMessagesJob = viewModelScope.launch {
      repository.observeMessages(conversationId).collect { messageList ->
        _uiState.update {
          it.copy(
            messages = messageList,
            syncError = if (it.syncError != null) {
              if (messageList.isEmpty()) "Couldn't load messages" else "Offline - viewing cached messages"
            } else null,
            localHistoryResolved = true,
            emptyThreadConfirmed = it.emptyThreadConfirmed && messageList.isEmpty()
          )
        }
      }
    }

    refreshThread()
  }

  /** Called only while this thread screen is visible and resumed. */
  fun startLiveUpdates() {
    val conversationId = currentConversationId ?: return
    if (pollJob?.isActive == true) return
    if (eventsListener != null) {
      sseJob = viewModelScope.launch {
        eventsListener.listenToConversationEvents(conversationId) { refreshThread() }
      }
    }
    pollJob = viewModelScope.launch {
      while (isActive) {
        delay(30_000L)
        refreshThread()
      }
    }
    refreshThread()
  }

  fun stopLiveUpdates() {
    sseJob?.cancel()
    pollJob?.cancel()
    refreshJob?.cancel()
    olderJob?.cancel()
    _uiState.update { it.copy(isSyncing = false, isLoadingOlder = false) }
  }

  /** Re-reads server state, for example when the app returns to the foreground. */
  fun refreshThread() {
    val convId = currentConversationId ?: return
    if (refreshJob?.isActive == true) return
    val request = generation
    val owner = ownerPersonId
    _uiState.update { it.copy(isSyncing = true, syncError = null) }
    refreshJob = viewModelScope.launch {
      val syncResult = repository.syncThreadHistory(convId, owner)
      if (generation != request) return@launch
      if (syncResult.isSuccess) {
        repository.markConversationRead(convId)
        val hasOlder = repository.hasOlderMessages(convId)
        // Read the persisted rows directly: the reactive Room emission can lag
        // behind this update, and only a confirmed empty store may settle the
        // empty state without flashing it over content that is about to land.
        val snapshotEmpty = repository.snapshotMessages(convId).isEmpty()
        _uiState.update {
          it.copy(
            isSyncing = false,
            hasOlder = hasOlder,
            syncError = null,
            sendError = null,
            emptyThreadConfirmed = snapshotEmpty
          )
        }
      } else {
        _uiState.update {
          it.copy(
            isSyncing = false,
            syncError = if (it.messages.isEmpty()) {
              "Couldn't load messages"
            } else {
              "Offline - viewing cached messages"
            }
          )
        }
      }
    }
  }

  fun loadOlderMessages() {
    val id = currentConversationId ?: return
    // Paging back only makes sense once content exists; without it the request
    // would run against a thread whose history has not been loaded yet.
    if (_uiState.value.messages.isEmpty() || _uiState.value.isLoadingOlder || !_uiState.value.hasOlder) return
    _uiState.update { it.copy(isLoadingOlder = true, olderError = null) }
    val request = generation
    olderJob = viewModelScope.launch {
      val result = repository.loadOlderMessages(id)
      if (generation != request) return@launch
      _uiState.update { it.copy(
        isLoadingOlder = false,
        hasOlder = result.getOrNull() ?: it.hasOlder,
        olderError = if (result.isFailure) "Could not load earlier messages. Tap to retry." else null
      ) }
    }
  }

  fun onInputTextChanged(text: String) {
    _uiState.update { it.copy(inputText = text) }
  }

  fun attachOffer(slug: String, title: String, price: String) {
    _uiState.update {
      it.copy(
        pendingAttachedOfferSlug = slug,
        pendingAttachedOfferTitle = title,
        pendingAttachedOfferPrice = price
      )
    }
  }

  fun removeAttachedOffer() {
    _uiState.update {
      it.copy(
        pendingAttachedOfferSlug = null,
        pendingAttachedOfferTitle = null,
        pendingAttachedOfferPrice = null
      )
    }
  }

  fun sendMessage() {
    val convId = currentConversationId ?: return
    val state = _uiState.value
    val text = state.inputText.trim()
    val offerSlug = state.pendingAttachedOfferSlug

    if (text.isEmpty()) return

    viewModelScope.launch {
      // Clear input optimistically; a fresh attempt supersedes any earlier
      // queue failure feedback.
      _uiState.update {
        it.copy(
          inputText = "",
          pendingAttachedOfferSlug = null,
          pendingAttachedOfferTitle = null,
          pendingAttachedOfferPrice = null,
          sendError = null
        )
      }

      try {
      repository.sendMessage(
        conversationId = convId,
        text = text,
        attachedOfferSlug = offerSlug,
        attachedOfferTitle = state.pendingAttachedOfferTitle,
        attachedOfferPrice = state.pendingAttachedOfferPrice
      )
      } catch (cancelled: CancellationException) { throw cancelled }
      catch (_: Exception) {
        if (currentConversationId == convId) _uiState.update { it.copy(
          inputText = if (it.inputText.isBlank()) text else it.inputText,
          sendError = "Couldn’t queue your message. Please try again.") }
      }
    }
  }

  fun retryMessage(clientMessageId: String) {
    viewModelScope.launch {
      repository.retryMessage(clientMessageId)
    }
  }

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val database = HavenDatabase.getInstance(context)
        val authClient = HavenAuthClient.shared(context)
        val transport = HavenProductionMessagingTransport(authClient)
        val repository = HavenLocalFirstMessagingRepository(database, transport)
        val eventsListener = com.example.data.messaging.ConversationEventsListener(authClient)
        return ConversationThreadViewModel(repository, eventsListener) as T
      }
    }
  }
}
