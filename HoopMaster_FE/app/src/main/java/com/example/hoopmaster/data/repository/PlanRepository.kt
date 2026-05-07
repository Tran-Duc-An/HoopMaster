package com.example.hoopmaster.data.repository

import com.example.hoopmaster.data.api.HoopMasterApi
import com.example.hoopmaster.data.model.TrainingPlanDto

class PlanRepository(private val api: HoopMasterApi) : PlanDataSource {
    override
    suspend fun getActivePlan(userId: String): Result<TrainingPlanDto?> = runCatching {
        val response = api.getActivePlan(userId)
        val body = response.bodyOrThrow("load active plan")
        body.plan
    }.mapError("load active plan")

    override
    suspend fun getPlans(
        userId: String,
        source: String?,
        status: String?
    ): Result<List<TrainingPlanDto>> = runCatching {
        val response = api.getPlans(userId, source = source, status = status)
        val body = response.bodyOrThrow("load plans")
        body.plans ?: throw Exception("Empty response from load plans")
    }.mapError("load plans")

    override
    suspend fun getDefaultPlan(): Result<TrainingPlanDto> = runCatching {
        val response = api.getDefaultTrainingPlan()
        response.bodyOrThrow("load default plan")
    }.mapError("load default plan")

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
