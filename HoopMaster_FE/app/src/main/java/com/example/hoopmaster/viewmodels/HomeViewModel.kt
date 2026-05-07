package com.example.hoopmaster.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hoopmaster.core.di.AppContainer
import com.example.hoopmaster.data.model.PlanExerciseDto
import com.example.hoopmaster.data.model.TrainingPlanDto
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val plan: TrainingPlanDto? = null,
    val exercises: List<PlanExerciseDto> = emptyList(),
    val userName: String? = null,
    val activePlanTitle: String? = null,
    val activePlanDescription: String? = null,
    val streakDays: Int = 0,
    val currentSessionState: String = "idle",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface HomeAction {
    data object LoadHome : HomeAction
    data object Refresh : HomeAction
    data class SetGreeting(val userName: String?) : HomeAction
    data class SetActivePlan(val title: String?) : HomeAction
    data class SetPlan(val plan: TrainingPlanDto?) : HomeAction
    data class SetStreak(val days: Int) : HomeAction
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val container = AppContainer(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.LoadHome, HomeAction.Refresh -> loadHome()

            is HomeAction.SetGreeting ->
                _uiState.update { it.copy(userName = action.userName, isLoading = false) }

            is HomeAction.SetActivePlan ->
                _uiState.update { it.copy(activePlanTitle = action.title, isLoading = false) }

            is HomeAction.SetPlan ->
                _uiState.update {
                    it.copy(
                        plan = action.plan,
                        activePlanTitle = action.plan?.title,
                        activePlanDescription = action.plan?.description,
                        exercises = action.plan?.exercises.orEmpty(),
                        isLoading = false,
                        errorMessage = null
                    )
                }

            is HomeAction.SetStreak ->
                _uiState.update { it.copy(streakDays = action.days, isLoading = false) }
        }
    }

    fun loadHome() {
        viewModelScope.launch {
            val userId = container.sessionStore.getUserId()
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val planResult = when {
                userId.isNullOrBlank() -> container.planRepository.getDefaultPlan()
                else -> container.planRepository.getActivePlan(userId).fold(
                    onSuccess = { plan ->
                        if (plan != null) Result.success(plan) else container.planRepository.getDefaultPlan()
                    },
                    onFailure = { container.planRepository.getDefaultPlan() }
                )
            }

            planResult.fold(
                onSuccess = { plan ->
                    _uiState.update {
                        it.copy(
                            plan = plan,
                            activePlanTitle = plan.title,
                            activePlanDescription = plan.description,
                            exercises = plan.exercises.orEmpty(),
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load home"
                        )
                    }
                }
            )
        }
    }
}
