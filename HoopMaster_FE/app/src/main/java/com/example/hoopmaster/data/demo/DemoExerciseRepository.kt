package com.example.hoopmaster.data.demo

import com.example.hoopmaster.data.model.ExerciseDto
import com.example.hoopmaster.data.model.ExerciseVoiceScriptDto
import com.example.hoopmaster.data.repository.ExerciseDataSource

class DemoExerciseRepository : ExerciseDataSource {
    override suspend fun getExercises(): Result<List<ExerciseDto>> {
        return Result.success(DemoFixtures.exercises())
    }

    override suspend fun getExercise(id: Int): Result<ExerciseDto> {
        val exercise = DemoFixtures.exercise(id)
            ?: return Result.failure(Exception("Exercise not found"))
        return Result.success(exercise)
    }

    override suspend fun getVoiceScript(
        id: Int,
        sets: Int?,
        reps: Int?,
        restSeconds: Int?
    ): Result<ExerciseVoiceScriptDto> {
        DemoFixtures.exercise(id)
            ?: return Result.failure(Exception("Exercise not found"))
        return Result.success(DemoFixtures.voiceScript(id, sets, reps, restSeconds))
    }
}
