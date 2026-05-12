package com.example.hoopmaster.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hoopmaster.core.di.AppContainer
import com.example.hoopmaster.data.model.CoachTone
import com.example.hoopmaster.data.model.WorkoutHistoryDayDto
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

data class ProfileTrainingChartDay(
    val label: String,
    val hours: Float
)

data class ProfileTrainingChartData(
    val days: List<ProfileTrainingChartDay>,
    val totalLabel: String,
    val maxHours: Float
)

fun buildWeeklyTrainingChartData(days: List<WorkoutHistoryDayDto>): ProfileTrainingChartData {
    val chartDays = days.map { day ->
        ProfileTrainingChartDay(
            label = day.dayLabel,
            hours = day.totalMinutes / 60f
        )
    }
    val totalHours = chartDays.sumOf { it.hours.toDouble() }.toFloat()
    val maxHours = chartDays.maxOfOrNull { it.hours }?.takeIf { it > 0f } ?: 1f
    return ProfileTrainingChartData(
        days = chartDays,
        totalLabel = "${formatHours(totalHours)} total",
        maxHours = maxHours
    )
}

private fun formatHours(hours: Float): String {
    return if (hours % 1f == 0f) {
        "${hours.toInt()}h"
    } else {
        String.format(Locale.US, "%.1fh", hours)
    }
}

data class ProfileUiState(
    val userId: String? = null,
    val displayName: String = "",
    val email: String = "",
    val tone: String = "neutral",
    val toneSaving: Boolean = false,
    val weeklyTrainingDays: List<WorkoutHistoryDayDto> = emptyList(),
    val weeklyTrainingLoading: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ProfileAction {
    data class LoadProfile(val userId: String?) : ProfileAction
    data class ToneChanged(val value: String) : ProfileAction
    data object Logout : ProfileAction
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val container = AppContainer(application)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.LoadProfile -> loadProfile(action.userId)

            is ProfileAction.ToneChanged -> updateTone(action.value)

            ProfileAction.Logout -> logout()
        }
    }

    fun loadProfile(userId: String?) {
        val sessionTone = container.sessionStore.getTone().backendValue()
        val resolvedUserId = userId ?: container.sessionStore.getUserId()
        _uiState.update {
            it.copy(
                userId = resolvedUserId,
                tone = sessionTone,
                isLoading = false,
                weeklyTrainingLoading = !resolvedUserId.isNullOrBlank(),
                errorMessage = null
            )
        }
        if (resolvedUserId.isNullOrBlank()) {
            return
        }
        viewModelScope.launch {
            container.sessionRepository.getWeeklyWorkoutHistory(resolvedUserId).fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(
                            weeklyTrainingDays = response.days,
                            weeklyTrainingLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            weeklyTrainingLoading = false,
                            errorMessage = error.message ?: "Failed to load workout history"
                        )
                    }
                }
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
