package com.example.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class MessagesUiState(
  val conversations: List<Conversation> = SampleConversations.defaultConversations,
  val selectedConversationId: String? = null,
  val messageInputText: String = "",
  val pendingAttachedOffer: Offer? = null,
  val isAttachSheetVisible: Boolean = false,
  val inspectedOfferDetail: Offer? = null
) {
  val selectedConversation: Conversation?
    get() = conversations.firstOrNull { it.id == selectedConversationId }

  val unreadCount: Int
    get() = conversations.count { it.isUnread }
}

class MessagesViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(MessagesUiState())
  val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

  fun selectConversation(conversationId: String) {
    _uiState.update { current ->
      val updatedConversations = current.conversations.map { conv ->
        if (conv.id == conversationId) conv.copy(isUnread = false) else conv
      }
      current.copy(
        conversations = updatedConversations,
        selectedConversationId = conversationId,
        messageInputText = "",
        pendingAttachedOffer = null
      )
    }
  }

  fun closeConversationThread() {
    _uiState.update {
      it.copy(
        selectedConversationId = null,
        messageInputText = "",
        pendingAttachedOffer = null
      )
    }
  }

  fun onMessageInputChange(text: String) {
    _uiState.update { it.copy(messageInputText = text) }
  }

  fun openAttachSheet() {
    _uiState.update { it.copy(isAttachSheetVisible = true) }
  }

  fun closeAttachSheet() {
    _uiState.update { it.copy(isAttachSheetVisible = false) }
  }

  fun selectOfferToAttach(offer: Offer) {
    _uiState.update {
      it.copy(
        pendingAttachedOffer = offer,
        isAttachSheetVisible = false
      )
    }
  }

  fun removeAttachedOffer() {
    _uiState.update { it.copy(pendingAttachedOffer = null) }
  }

  fun inspectOffer(offer: Offer?) {
    _uiState.update { it.copy(inspectedOfferDetail = offer) }
  }

  fun sendMessage() {
    val current = _uiState.value
    val text = current.messageInputText.trim()
    val attachment = current.pendingAttachedOffer

    if (text.isEmpty() && attachment == null) return
    val activeId = current.selectedConversationId ?: return

    val newMessage = Message(
      id = "msg-${UUID.randomUUID()}",
      isFromMe = true,
      text = text,
      timestamp = "Just now",
      attachedOffer = attachment
    )

    val lastPreview = when {
      attachment != null && text.isNotEmpty() -> "Attached: ${attachment.title} — $text"
      attachment != null -> "Attached offer: ${attachment.title}"
      else -> text
    }

    _uiState.update { state ->
      val updated = state.conversations.map { conv ->
        if (conv.id == activeId) {
          conv.copy(
            messages = conv.messages + newMessage,
            lastMessage = lastPreview,
            lastMessageTimestamp = "Just now"
          )
        } else {
          conv
        }
      }
      state.copy(
        conversations = updated,
        messageInputText = "",
        pendingAttachedOffer = null
      )
    }

    // Simulate an artisan reply after 1.5 seconds if sent to Adaeze or Tunde
    viewModelScope.launch {
      delay(1600)
      val conversation = _uiState.value.conversations.firstOrNull { it.id == activeId } ?: return@launch
      val replyText = when {
        attachment != null -> "Thank you for attaching the ${attachment.title}! The terms look great. When are you free to complete the swap?"
        else -> "Thanks for reaching out! Let's arrange a time to finalize the barter exchange."
      }
      val replyMessage = Message(
        id = "reply-${UUID.randomUUID()}",
        isFromMe = false,
        text = replyText,
        timestamp = "Just now"
      )

      _uiState.update { state ->
        val updated = state.conversations.map { conv ->
          if (conv.id == activeId) {
            conv.copy(
              messages = conv.messages + replyMessage,
              lastMessage = replyText,
              lastMessageTimestamp = "Just now",
              isUnread = state.selectedConversationId != activeId
            )
          } else {
            conv
          }
        }
        state.copy(conversations = updated)
      }
    }
  }

  fun startConversationForOffer(offer: Offer) {
    val currentConversations = _uiState.value.conversations
    val existing = currentConversations.firstOrNull { it.participantName == offer.creatorName }

    if (existing != null) {
      _uiState.update {
        it.copy(
          selectedConversationId = existing.id,
          pendingAttachedOffer = offer
        )
      }
    } else {
      val newConvId = "conv-${UUID.randomUUID()}"
      val newConv = Conversation(
        id = newConvId,
        participantName = offer.creatorName,
        participantInitial = offer.creatorInitial,
        participantRole = "Haven Artisan",
        lastMessage = "Started trade inquiry for ${offer.title}",
        lastMessageTimestamp = "Just now",
        isUnread = false,
        relatedOffer = offer,
        messages = listOf(
          Message(
            id = "msg-${UUID.randomUUID()}",
            isFromMe = false,
            text = "Welcome to Haven! I'm happy to discuss a barter trade for ${offer.title}.",
            timestamp = "Just now",
            attachedOffer = offer
          )
        )
      )

      _uiState.update {
        it.copy(
          conversations = listOf(newConv) + it.conversations,
          selectedConversationId = newConvId,
          pendingAttachedOffer = offer
        )
      }
    }
  }
}
