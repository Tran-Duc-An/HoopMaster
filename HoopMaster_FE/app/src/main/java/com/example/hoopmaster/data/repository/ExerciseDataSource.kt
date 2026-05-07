package com.example.hoopmaster.data.repository

import com.example.hoopmaster.data.model.ExerciseDto
import com.example.hoopmaster.data.model.ExerciseVoiceScriptDto

interface ExerciseDataSource {
    suspend fun getExercises(): Result<List<ExerciseDto>>
    suspend fun getExercise(id: Int): Result<ExerciseDto>
    suspend fun getVoiceScript(
        id: Int,
        sets: Int?,
        reps: Int?,
        restSeconds: Int?
    ): Result<ExerciseVoiceScriptDto>
}
