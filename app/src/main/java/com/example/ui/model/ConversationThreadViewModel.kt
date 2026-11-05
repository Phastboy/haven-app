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
import kotlinx.coroutines.Job
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
  val inputText: String = "",
  val isSyncing: Boolean = false,
  val syncError: String? = null,
  val pendingAttachedOfferSlug: String? = null,
  val pendingAttachedOfferTitle: String? = null,
  val pendingAttachedOfferPrice: String? = null
)

class ConversationThreadViewModel(
  private val repository: MessagingRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow(ConversationThreadUiState())
  val uiState: StateFlow<ConversationThreadUiState> = _uiState.asStateFlow()

  private var observeMessagesJob: Job? = null
  private var observeConversationJob: Job? = null
  private var currentConversationId: String? = null
  private var ownerPersonId: String? = null

  fun clear() {
    observeMessagesJob?.cancel()
    observeConversationJob?.cancel()
    currentConversationId = null
    ownerPersonId = null
    _uiState.value = ConversationThreadUiState()
  }

  override fun onCleared() {
    super.onCleared()
    clear()
  }

  fun openConversation(conversationId: String, ownerPersonId: String? = null) {
    this.ownerPersonId = ownerPersonId
    if (currentConversationId == conversationId) {
      refreshThread()
      return
    }
    currentConversationId = conversationId

    observeMessagesJob?.cancel()
    observeConversationJob?.cancel()

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

  /** Re-reads server state, for example when the app returns to the foreground. */
  fun refreshThread() {
    val convId = currentConversationId ?: return
    _uiState.update { it.copy(isSyncing = true, syncError = null) }
    viewModelScope.launch {
      repository.markConversationRead(convId)
      val syncResult = repository.syncThreadHistory(convId, ownerPersonId)
      _uiState.update {
        it.copy(
          isSyncing = false,
          syncError = if (syncResult.isFailure) "Offline - viewing cached messages" else null
        )
      }
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

    if (text.isEmpty() && offerSlug == null) return

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

      repository.sendMessage(
        conversationId = convId,
        text = text,
        attachedOfferSlug = offerSlug,
        attachedOfferTitle = state.pendingAttachedOfferTitle,
        attachedOfferPrice = state.pendingAttachedOfferPrice
      )
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
        return ConversationThreadViewModel(repository) as T
      }
    }
  }
}
