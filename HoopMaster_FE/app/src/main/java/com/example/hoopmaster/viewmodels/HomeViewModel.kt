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
    val defaultExercises: List<PlanExerciseDto> = emptyList(),
    val personalExercises: List<PlanExerciseDto> = emptyList(),
    val selectedExerciseTag: String = HomeViewModel.EXERCISE_TAG_PERSONAL,
    val userName: String? = null,
    val activePlanTitle: String? = null,
    val activePlanDescription: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface HomeAction {
    data object LoadHome : HomeAction
    data object Refresh : HomeAction
    data class SetGreeting(val userName: String?) : HomeAction
    data class SetActivePlan(val title: String?) : HomeAction
    data class SetPlan(val plan: TrainingPlanDto?) : HomeAction
    data class SelectExerciseTag(val tag: String) : HomeAction
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

            is HomeAction.SelectExerciseTag ->
                _uiState.update { current ->
                    val nextTag = if (action.tag == EXERCISE_TAG_DEFAULT) EXERCISE_TAG_DEFAULT else EXERCISE_TAG_PERSONAL
                    current.copy(
                        selectedExerciseTag = nextTag,
                        exercises = if (nextTag == EXERCISE_TAG_DEFAULT) current.defaultExercises else current.personalExercises
                    )
                }
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
                        if (plan != null) {
                            Result.success(plan)
                        } else {
                            getLatestPersonalizedPlan(userId)
                        }
                    },
                    onFailure = { getLatestPersonalizedPlan(userId) }
                )
            }

            val defaultPlanResult = container.planRepository.getDefaultPlan()
            val personalPlanResult = if (userId.isNullOrBlank()) {
                Result.success(null)
            } else {
                container.planRepository.getPlans(userId, source = "personalized", status = null)
                    .map { plans -> plans.firstOrNull() }
            }

            planResult.fold(
                onSuccess = { plan ->
                    val defaultExercises = defaultPlanResult.getOrNull()?.exercises.orEmpty()
                    val personalExercises = personalPlanResult.getOrNull()?.exercises.orEmpty()
                    val preferredTag = when {
                        personalExercises.isNotEmpty() -> EXERCISE_TAG_PERSONAL
                        else -> EXERCISE_TAG_DEFAULT
                    }
                    _uiState.update {
                        it.copy(
                            plan = plan,
                            activePlanTitle = plan.title,
                            activePlanDescription = plan.description,
                            defaultExercises = defaultExercises,
                            personalExercises = personalExercises,
                            selectedExerciseTag = preferredTag,
                            exercises = if (preferredTag == EXERCISE_TAG_DEFAULT) defaultExercises else personalExercises,
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

    private suspend fun getLatestPersonalizedPlan(userId: String): Result<TrainingPlanDto> {
        return container.planRepository.getPlans(userId, source = "personalized", status = null).fold(
            onSuccess = { plans ->
                plans.firstOrNull()?.let { Result.success(it) } ?: container.planRepository.getDefaultPlan()
            },
            onFailure = { container.planRepository.getDefaultPlan() }
        )
    }

    companion object {
        const val EXERCISE_TAG_DEFAULT = "default"
        const val EXERCISE_TAG_PERSONAL = "personal"
    }
}
