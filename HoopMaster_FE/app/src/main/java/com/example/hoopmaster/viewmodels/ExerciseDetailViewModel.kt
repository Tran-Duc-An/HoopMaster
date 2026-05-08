package com.example.hoopmaster.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hoopmaster.core.di.AppContainer
import com.example.hoopmaster.data.model.ExerciseDto
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ExerciseDetailUiState(
    val exerciseId: Int? = null,
    val exercise: ExerciseDto? = null,
    val title: String = "",
    val category: String = "",
    val description: String = "",
    val steps: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val sets: Int? = null,
    val reps: Int? = null,
    val restSeconds: Int? = null,
    val voiceScriptReady: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ExerciseDetailAction {
    data class LoadExercise(val exerciseId: Int) : ExerciseDetailAction
    data class SetTargets(val sets: Int?, val reps: Int?, val restSeconds: Int?) : ExerciseDetailAction
    data object VoiceScriptLoaded : ExerciseDetailAction
    data object StartExercise : ExerciseDetailAction
    data object Close : ExerciseDetailAction
}

class ExerciseDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val container = AppContainer(application)
    private val exerciseRepository = container.exerciseRepository

    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    fun onAction(action: ExerciseDetailAction) {
        when (action) {
            is ExerciseDetailAction.LoadExercise -> loadExercise(action.exerciseId)

            is ExerciseDetailAction.SetTargets ->
                _uiState.update {
                    it.copy(
                        sets = action.sets,
                        reps = action.reps,
                        restSeconds = action.restSeconds,
                        isLoading = false
                    )
                }

            ExerciseDetailAction.VoiceScriptLoaded ->
                _uiState.update { it.copy(voiceScriptReady = true, isLoading = false) }

            ExerciseDetailAction.StartExercise ->
                _uiState.update { it.copy(isLoading = false) }

            ExerciseDetailAction.Close ->
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
        }
    }

    fun loadExercise(exerciseId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(exerciseId = exerciseId, isLoading = true, errorMessage = null) }

            exerciseRepository.getExercise(exerciseId).fold(
                onSuccess = { exercise ->
                    _uiState.update {
                        it.copy(
                            exercise = exercise,
                            title = exercise.name.orEmpty(),
                            category = exercise.category.orEmpty(),
                            description = exercise.description.orEmpty(),
                            steps = buildSteps(exercise),
                            warnings = buildWarnings(exercise),
                            sets = exercise.target?.sets ?: exercise.sets,
                            reps = exercise.target?.reps ?: exercise.reps,
                            restSeconds = exercise.target?.restSeconds,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load exercise"
                        )
                    }
                }
            )
        }
    }

    private fun buildSteps(exercise: ExerciseDto): List<String> {
        val items = mutableListOf<String>()
        exercise.voiceScript?.script?.forEach { item ->
            item.text?.takeIf { it.isNotBlank() }?.let(items::add)
        }
        if (items.isEmpty()) {
            items += listOfNotNull(
                exercise.voiceCues?.setup,
                exercise.voiceCues?.intro,
                exercise.description
            )
        }
        return items
    }

    private fun buildWarnings(exercise: ExerciseDto): List<String> {
        val warnings = mutableListOf<String>()
        exercise.voiceCues?.warnings?.let(warnings::addAll)
        exercise.safetyNotes?.takeIf { it.isNotBlank() }?.let(warnings::add)
        return warnings.distinct()
    }
}
