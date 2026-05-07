package com.example.hoopmaster.data.repository

import com.example.hoopmaster.data.api.HoopMasterApi
import com.example.hoopmaster.data.model.ExerciseDto
import com.example.hoopmaster.data.model.ExerciseVoiceScriptDto

class ExerciseRepository(private val api: HoopMasterApi) : ExerciseDataSource {
    override
    suspend fun getExercises(): Result<List<ExerciseDto>> = runCatching {
        val response = api.getExercises()
        response.bodyOrThrow("load exercises")
    }.mapError("load exercises")

    override
    suspend fun getExercise(id: Int): Result<ExerciseDto> = runCatching {
        val response = api.getExercise(id)
        response.bodyOrThrow("load exercise")
    }.mapError("load exercise")

    override
    suspend fun getVoiceScript(
        id: Int,
        sets: Int?,
        reps: Int?,
        restSeconds: Int?
    ): Result<ExerciseVoiceScriptDto> = runCatching {
        val response = api.getExerciseVoiceScript(
            id = id,
            sets = sets,
            reps = reps,
            restSeconds = restSeconds
        )
        response.bodyOrThrow("load voice script")
    }.mapError("load voice script")

    private fun <T> retrofit2.Response<T>.bodyOrThrow(action: String): T {
        if (!isSuccessful) {
            throw Exception("HTTP ${code()} during $action")
        }
        return body() ?: throw Exception("Empty response from $action")
    }

    private fun <T> Result<T>.mapError(action: String): Result<T> = fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(Exception("${action} failed: ${it.message ?: "unknown error"}")) }
    )
}
