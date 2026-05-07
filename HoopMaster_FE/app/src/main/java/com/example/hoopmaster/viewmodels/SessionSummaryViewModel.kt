package com.example.hoopmaster.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SessionSummaryUiState(
    val sessionId: String? = null,
    val lastFeedback: String = "",
    val totalShots: Int = 0,
    val madeShots: Int = 0,
    val durationSeconds: Int = 0,
    val highlight: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

sealed interface SessionSummaryAction {
    data class LoadSummary(
        val sessionId: String?,
        val totalShots: Int,
        val madeShots: Int,
        val durationSeconds: Int,
        val lastFeedback: String = ""
    ) : SessionSummaryAction

    data class UpdateHighlight(val value: String) : SessionSummaryAction
    data object SaveSummary : SessionSummaryAction
    data object Dismiss : SessionSummaryAction
}

class SessionSummaryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SessionSummaryUiState())
    val uiState: StateFlow<SessionSummaryUiState> = _uiState.asStateFlow()

    fun onAction(action: SessionSummaryAction) {
        when (action) {
            is SessionSummaryAction.LoadSummary ->
                _uiState.update {
                    it.copy(
                        sessionId = action.sessionId,
                        lastFeedback = action.lastFeedback,
                        totalShots = action.totalShots,
                        madeShots = action.madeShots,
                        durationSeconds = action.durationSeconds,
                        isSaving = false,
                        errorMessage = null
                    )
                }

            is SessionSummaryAction.UpdateHighlight ->
                _uiState.update { it.copy(highlight = action.value) }

            SessionSummaryAction.SaveSummary ->
                _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            SessionSummaryAction.Dismiss ->
                _uiState.update { it.copy(isSaving = false, errorMessage = null) }
        }
    }
}
