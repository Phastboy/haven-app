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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MessagesInboxUiState(
  val items: List<InboxConversation> = emptyList(),
  val nextCursor: String? = null,
  val loading: Boolean = false,
  val loadingMore: Boolean = false,
  val error: String? = null
)

class MessagesInboxViewModel(private val repository: MessagesInboxRepository) : ViewModel() {
  private val _state = MutableStateFlow(MessagesInboxUiState())
  val state = _state.asStateFlow()
  private var actor: String? = null
  private var job: Job? = null

  fun clear() {
    job?.cancel()
    actor = null
    _state.value = MessagesInboxUiState()
  }

  fun load(personId: String, force: Boolean = false) {
    if (!force && actor == personId && (_state.value.loading || _state.value.items.isNotEmpty() || _state.value.nextCursor == null && _state.value.error != null)) return
    job?.cancel()
    actor = personId
    _state.value = _state.value.copy(loading = true, loadingMore = false, error = null)
    job = viewModelScope.launch {
      try {
        val page = repository.load(null)
        if (actor == personId) _state.value = page.toState()
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        if (actor == personId) _state.value = MessagesInboxUiState(error = "Couldn’t load messages. Please try again.")
      }
    }
  }

  fun loadMore() {
    val personId = actor ?: return
    val cursor = _state.value.nextCursor ?: return
    if (_state.value.loading || _state.value.loadingMore) return
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

  private fun ConversationInboxPage.toState() = MessagesInboxUiState(items, nextCursor)

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MessagesInboxViewModel::class.java))
        return MessagesInboxViewModel(HavenMessagesRepository(HavenAuthClient.shared(context))) as T
      }
    }
  }
}
