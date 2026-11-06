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

data class ConversationThreadUiState(
  val conversation: ConversationEntity? = null,
  val messages: List<MessageEntity> = emptyList(),
  val isLoadingOlder: Boolean = false,
  val olderError: String? = null,
  val hasOlder: Boolean = true,
  val inputText: String = "",
  val isSyncing: Boolean = false,
  val syncError: String? = null,
  val pendingAttachedOfferSlug: String? = null,
  val pendingAttachedOfferTitle: String? = null,
  val pendingAttachedOfferPrice: String? = null
)

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
    super.onCleared()
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
        syncError = null
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
        _uiState.update { it.copy(messages = messageList) }
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
      if (syncResult.isSuccess) repository.markConversationRead(convId)
      _uiState.update {
        it.copy(
          isSyncing = false,
          syncError = if (syncResult.isFailure) "Offline - viewing cached messages" else null
        )
      }
    }
  }

  fun loadOlderMessages() {
    val id = currentConversationId ?: return
    if (_uiState.value.isLoadingOlder || !_uiState.value.hasOlder) return
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
      // Clear input optimistically
      _uiState.update {
        it.copy(
          inputText = "",
          pendingAttachedOfferSlug = null,
          pendingAttachedOfferTitle = null,
          pendingAttachedOfferPrice = null
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
          syncError = "Couldn’t queue your message. Please try again.") }
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
