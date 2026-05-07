package com.example.hoopmaster.data.repository

import com.example.hoopmaster.data.api.HoopMasterApi
import com.example.hoopmaster.data.model.ConfirmPlanRequest
import com.example.hoopmaster.data.model.PlanningChatResponseDto
import com.example.hoopmaster.data.model.PlanningHistoryResponseDto
import com.example.hoopmaster.data.model.PlanningMessageRequest

class PlanningRepository(private val api: HoopMasterApi) {
    suspend fun sendMessage(userId: String, text: String): Result<PlanningChatResponseDto> = runCatching {
        val response = api.sendPlanningMessage(userId, PlanningMessageRequest(text = text))
        response.bodyOrThrow("send planning message")
    }.mapError("send planning message")

    suspend fun confirmPlan(userId: String, planId: String): Result<PlanningChatResponseDto> = runCatching {
        val response = api.confirmPlan(userId, ConfirmPlanRequest(planId = planId))
        response.bodyOrThrow("confirm plan")
    }.mapError("confirm plan")

    suspend fun getHistory(userId: String): Result<PlanningHistoryResponseDto> = runCatching {
        val response = api.getPlanningHistory(userId)
        response.bodyOrThrow("load planning history")
    }.mapError("load planning history")

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
