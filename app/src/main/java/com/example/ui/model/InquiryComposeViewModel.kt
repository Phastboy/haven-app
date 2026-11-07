package com.example.ui.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auth.HavenAuthClient
import com.example.data.messaging.HavenLocalFirstMessagingRepository
import com.example.data.messaging.MessagingRepository
import com.example.data.messaging.db.HavenDatabase
import com.example.data.messaging.transport.HavenProductionMessagingTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The person or offer a new inquiry is about. */
sealed interface InquiryTarget {
  data class Offer(
    val slug: String,
    val offerTitle: String,
    val makerName: String
  ) : InquiryTarget

  data class Person(
    val handle: String,
    val displayName: String
  ) : InquiryTarget
}

data class InquiryComposeUiState(
  val target: InquiryTarget? = null,
  val text: String = "",
  val isSending: Boolean = false,
  val error: String? = null,
  val startedConversationId: String? = null
) {
  val isOpen: Boolean get() = target != null
}

/**
 * Drives the native "Message provider" composer.
 *
 * The first message is sent directly so the server can create or reuse the
 * conversation and return its id. Nothing is stored as if it had been sent when
 * the request fails.
 */
class InquiryComposeViewModel(
  private val repository: MessagingRepository
) : ViewModel() {

  private val _state = MutableStateFlow(InquiryComposeUiState())
  val state = _state.asStateFlow()

  private var generation = 0L
  private var sendJob: kotlinx.coroutines.Job? = null

  fun clear() {
    generation++
    sendJob?.cancel()
    _state.value = InquiryComposeUiState()
  }

  fun openOffer(slug: String, offerTitle: String, makerName: String) {
    _state.value = InquiryComposeUiState(
      target = InquiryTarget.Offer(slug, offerTitle, makerName)
    )
  }

  fun openPerson(handle: String, displayName: String) {
    _state.value = InquiryComposeUiState(
      target = InquiryTarget.Person(handle, displayName)
    )
  }

  fun onTextChanged(text: String) {
    _state.update {
      it.copy(
        text = text.take(MAX_BODY_CHARS),
        error = null
      )
    }
  }

  fun dismiss() {
    if (_state.value.isSending) return
    _state.value = InquiryComposeUiState()
  }

  fun consumeStartedConversation() {
    _state.update { it.copy(startedConversationId = null) }
  }

  fun send() {
    val current = _state.value
    val target = current.target ?: return
    if (current.isSending) return
    val body = current.text.trim()
    if (body.isEmpty()) {
      _state.update { it.copy(error = "Write a message first.") }
      return
    }

    _state.update { it.copy(isSending = true, error = null) }
    val request = generation
    sendJob = viewModelScope.launch {
      val result = when (target) {
        is InquiryTarget.Offer -> repository.startOfferInquiry(target.slug, body)
        is InquiryTarget.Person -> repository.startProfileInquiry(target.handle, body)
      }
      if (request != generation) return@launch
      result.fold(
        onSuccess = { conversationId ->
          _state.value = InquiryComposeUiState(startedConversationId = conversationId)
        },
        onFailure = {
          _state.update {
            it.copy(
              isSending = false,
              error = "Couldn’t send your message. Check your connection and try again."
            )
          }
        }
      )
    }
  }

  companion object {
    const val MAX_BODY_CHARS = 2000

    fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val database = HavenDatabase.getInstance(context)
        val authClient = HavenAuthClient.shared(context)
        val transport = HavenProductionMessagingTransport(authClient)
        val repository = HavenLocalFirstMessagingRepository(database, transport)
        return InquiryComposeViewModel(repository) as T
      }
    }
  }
}
