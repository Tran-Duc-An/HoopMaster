package com.example.hoopmaster.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hoopmaster.core.di.AppContainer
import com.example.hoopmaster.data.model.SessionInfoDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionSummaryUiState(
    val socketId: String? = null,
    val summary: SessionInfoDto? = null,
    val isLoading: Boolean = false,
    val isMissing: Boolean = false,
    val errorMessage: String? = null
)

sealed interface SessionSummaryAction {
    data class LoadSummary(val socketId: String?) : SessionSummaryAction
    data object Dismiss : SessionSummaryAction
}

class SessionSummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val container = AppContainer(application)

    private val _uiState = MutableStateFlow(SessionSummaryUiState())
    val uiState: StateFlow<SessionSummaryUiState> = _uiState.asStateFlow()

    fun onAction(action: SessionSummaryAction) {
        when (action) {
            is SessionSummaryAction.LoadSummary -> loadSummary(action.socketId)
            SessionSummaryAction.Dismiss -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadSummary(socketId: String?) {
        if (socketId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    socketId = socketId,
                    summary = null,
                    isLoading = false,
                    isMissing = true,
                    errorMessage = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    socketId = socketId,
                    summary = null,
                    isLoading = true,
                    isMissing = false,
                    errorMessage = null
                )
            }

            container.sessionRepository.getSessionInfo(socketId).fold(
                onSuccess = { info ->
                    _uiState.update {
                        it.copy(
                            summary = info,
                            isLoading = false,
                            isMissing = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    val isNotFound = error.message?.contains("404") == true
                    _uiState.update {
                        it.copy(
                            summary = null,
                            isLoading = false,
                            isMissing = isNotFound,
                            errorMessage = if (isNotFound) null else (error.message ?: "Failed to load summary")
                        )
                    }
                }
            )
        }
    }
}
