package com.example.hoopmaster.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hoopmaster.core.di.AppContainer
import com.example.hoopmaster.data.model.CoachTone
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ProfileUiState(
    val userId: String? = null,
    val displayName: String = "",
    val email: String = "",
    val tone: String = "neutral",
    val toneSaving: Boolean = false,
    val editing: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ProfileAction {
    data class LoadProfile(val userId: String?) : ProfileAction
    data class NameChanged(val value: String) : ProfileAction
    data class EmailChanged(val value: String) : ProfileAction
    data class ToneChanged(val value: String) : ProfileAction
    data object ToggleEditing : ProfileAction
    data object SaveProfile : ProfileAction
    data object Logout : ProfileAction
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val container = AppContainer(application)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.LoadProfile -> loadProfile(action.userId)

            is ProfileAction.NameChanged ->
                _uiState.update { it.copy(displayName = action.value) }

            is ProfileAction.EmailChanged ->
                _uiState.update { it.copy(email = action.value) }

            is ProfileAction.ToneChanged -> updateTone(action.value)

            ProfileAction.ToggleEditing ->
                _uiState.update { it.copy(editing = !it.editing) }

            ProfileAction.SaveProfile -> _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            ProfileAction.Logout -> logout()
        }
    }

    fun loadProfile(userId: String?) {
        val sessionTone = container.sessionStore.getTone().backendValue()
        _uiState.update {
            it.copy(
                userId = userId ?: container.sessionStore.getUserId(),
                tone = sessionTone,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    fun updateTone(value: String) {
        viewModelScope.launch {
            val userId = container.sessionStore.getUserId()
            if (userId.isNullOrBlank()) {
                _uiState.update { it.copy(errorMessage = "Missing user session") }
                return@launch
            }

            val tone = value.toCoachTone()
            _uiState.update { it.copy(toneSaving = true, errorMessage = null, tone = tone.backendValue()) }

            container.authRepository.updateTone(userId, tone).fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            userId = user.id ?: userId,
                            toneSaving = false,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            toneSaving = false,
                            errorMessage = error.message ?: "Failed to update tone"
                        )
                    }
                }
            )
        }
    }

    fun logout() {
        container.sessionStore.clear()
        _uiState.update { ProfileUiState() }
    }

    private fun String.toCoachTone(): CoachTone = when (lowercase()) {
        "strict" -> CoachTone.STRICT
        "cheerful" -> CoachTone.CHEERFUL
        else -> CoachTone.NEUTRAL
    }
}
