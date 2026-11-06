package com.example.ui.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auth.HavenAuthClient
import com.example.data.HavenProfileRepository
import com.example.data.ProfileOverview
import com.example.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

data class EditProfileUiState(
  val displayName: String = "",
  val handle: String = "",
  val bio: String = "",
  val avatarUrl: String? = null,
  val initialLoaded: Boolean = false,
  val loading: Boolean = false,
  val saving: Boolean = false,
  val uploadingAvatar: Boolean = false,
  val removingAvatar: Boolean = false,
  val nameError: String? = null,
  val handleError: String? = null,
  val bioError: String? = null,
  val generalError: String? = null,
  val saveSuccessMessage: String? = null
)

class EditProfileViewModel(
  private val repository: ProfileRepository
) : ViewModel() {

  private val _state = MutableStateFlow(EditProfileUiState())
  val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

  fun initialize(profile: ProfileOverview? = null) {
    if (profile != null) {
      _state.update {
        it.copy(
          displayName = profile.displayName,
          handle = profile.handle,
          bio = profile.bio.orEmpty(),
          avatarUrl = profile.avatarUrl,
          initialLoaded = true
        )
      }
    } else {
      load()
    }
  }

  fun load() {
    _state.update { it.copy(loading = true, generalError = null) }
    viewModelScope.launch {
      try {
        val overview = repository.load()
        _state.update {
          it.copy(
            displayName = overview.displayName,
            handle = overview.handle,
            bio = overview.bio.orEmpty(),
            avatarUrl = overview.avatarUrl,
            initialLoaded = true,
            loading = false
          )
        }
      } catch (e: Exception) {
        _state.update { it.copy(loading = false, generalError = "Couldn’t load profile. Please try again.") }
      }
    }
  }

  fun onDisplayNameChange(value: String) {
    _state.update {
      it.copy(
        displayName = value,
        nameError = if (it.nameError != null && value.trim().length >= 2) null else it.nameError
      )
    }
  }

  fun onHandleChange(value: String) {
    val clean = value.trim().lowercase()
    _state.update {
      it.copy(
        handle = clean,
        handleError = null
      )
    }
  }

  fun onBioChange(value: String) {
    _state.update {
      it.copy(
        bio = value,
        bioError = if (it.bioError != null && value.length <= 500) null else it.bioError
      )
    }
  }

  fun validate(): Boolean {
    val current = _state.value
    var valid = true
    var nameErr: String? = null
    var handleErr: String? = null
    var bioErr: String? = null

    if (current.displayName.trim().length < 2) {
      nameErr = "Display name must be at least 2 characters."
      valid = false
    } else if (current.displayName.trim().length > 50) {
      nameErr = "Display name cannot exceed 50 characters."
      valid = false
    }

    val handlePattern = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")
    val cleanHandle = current.handle.trim().lowercase()
    if (cleanHandle.length < 3 || cleanHandle.length > 30 || !handlePattern.matches(cleanHandle)) {
      handleErr = "Handle must be 3–30 characters and contain only lowercase letters, digits, and single hyphens."
      valid = false
    }

    if (current.bio.length > 500) {
      bioErr = "Bio cannot exceed 500 characters."
      valid = false
    }

    _state.update { it.copy(nameError = nameErr, handleError = handleErr, bioError = bioErr) }
    return valid
  }

  fun save(onSuccess: (ProfileOverview) -> Unit) {
    if (!validate()) return
    val current = _state.value
    _state.update { it.copy(saving = true, generalError = null) }

    viewModelScope.launch {
      val result = repository.updateProfile(
        displayName = current.displayName.trim(),
        handle = current.handle.trim().lowercase(),
        bio = current.bio.trim()
      )
      result
        .onSuccess { updated ->
          _state.update {
            it.copy(
              saving = false,
              saveSuccessMessage = "Your profile has been updated.",
              displayName = updated.displayName,
              handle = updated.handle,
              bio = updated.bio.orEmpty(),
              avatarUrl = updated.avatarUrl
            )
          }
          onSuccess(updated)
        }
        .onFailure { error ->
          var fieldHandleErr: String? = null
          var genericErr = "Couldn’t update your profile. Please try again."

          if (error is HavenAuthClient.HttpException) {
            if (error.statusCode == 409) {
              try {
                val json = JSONObject(error.responseBody)
                if (json.optString("error") == "handle_taken") {
                  fieldHandleErr = json.optString("message", "This handle is already taken.")
                }
              } catch (_: Exception) {
                fieldHandleErr = "This handle is already taken."
              }
            } else if (error.statusCode == 400) {
              genericErr = "Please check your inputs and try again."
            }
          }

          _state.update {
            it.copy(
              saving = false,
              handleError = fieldHandleErr ?: it.handleError,
              generalError = if (fieldHandleErr != null) null else genericErr
            )
          }
        }
    }
  }

  fun uploadAvatar(bytes: ByteArray, mimeType: String = "image/jpeg") {
    _state.update { it.copy(uploadingAvatar = true, generalError = null) }
    viewModelScope.launch {
      val result = repository.uploadAvatar(bytes, mimeType)
      result
        .onSuccess { updated ->
          _state.update {
            it.copy(
              uploadingAvatar = false,
              avatarUrl = updated.avatarUrl,
              saveSuccessMessage = "Avatar updated successfully."
            )
          }
        }
        .onFailure { error ->
          val msg = if (error is HavenAuthClient.HttpException && error.statusCode == 413) {
            "Avatar file is too large (maximum 5 MB)."
          } else {
            "Couldn’t upload avatar. Please try again."
          }
          _state.update { it.copy(uploadingAvatar = false, generalError = msg) }
        }
    }
  }

  fun removeAvatar() {
    _state.update { it.copy(removingAvatar = true, generalError = null) }
    viewModelScope.launch {
      val result = repository.removeAvatar()
      result
        .onSuccess { updated ->
          _state.update {
            it.copy(
              removingAvatar = false,
              avatarUrl = updated.avatarUrl,
              saveSuccessMessage = "Avatar removed."
            )
          }
        }
        .onFailure {
          _state.update {
            it.copy(removingAvatar = false, generalError = "Couldn’t remove avatar. Please try again.")
          }
        }
    }
  }

  fun dismissSuccessMessage() {
    _state.update { it.copy(saveSuccessMessage = null) }
  }

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val auth = HavenAuthClient.shared(context)
        val repo = HavenProfileRepository(auth)
        return EditProfileViewModel(repo) as T
      }
    }
  }
}
