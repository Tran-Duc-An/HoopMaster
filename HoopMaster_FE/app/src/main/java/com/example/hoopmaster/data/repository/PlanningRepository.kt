package com.example.hoopmaster.data.repository

import com.example.hoopmaster.data.api.HoopMasterApi
import com.example.hoopmaster.data.model.ConfirmPlanRequest
import com.example.hoopmaster.data.model.PlanningChatResponseDto
import com.example.hoopmaster.data.model.PlanningHistoryResponseDto
import com.example.hoopmaster.data.model.PlanningMessageRequest
import com.example.hoopmaster.data.model.PlanningSessionRequest
import com.example.hoopmaster.data.model.PlanningSessionResponseDto
import com.example.hoopmaster.data.model.PlanningSessionsResponseDto

class PlanningRepository(private val api: HoopMasterApi) : PlanningDataSource {
    override
    suspend fun sendMessage(userId: String, text: String, sessionId: String): Result<PlanningChatResponseDto> = runCatching {
        val response = api.sendPlanningMessage(userId, PlanningMessageRequest(text = text, sessionId = sessionId))
        response.bodyOrThrow("send planning message")
    }.mapError("send planning message")

    override
    suspend fun confirmPlan(userId: String, planId: String): Result<PlanningChatResponseDto> = runCatching {
        val response = api.confirmPlan(userId, ConfirmPlanRequest(planId = planId))
        response.bodyOrThrow("confirm plan")
    }.mapError("confirm plan")

    override
    suspend fun getHistory(userId: String, sessionId: String): Result<PlanningHistoryResponseDto> = runCatching {
        val response = api.getPlanningHistory(userId, sessionId)
        response.bodyOrThrow("load planning history")
    }.mapError("load planning history")

    override
    suspend fun createSession(userId: String): Result<PlanningSessionResponseDto> = runCatching {
        val response = api.createPlanningSession(userId, PlanningSessionRequest())
        response.bodyOrThrow("create planning session")
    }.mapError("create planning session")

    override
    suspend fun getSessions(userId: String): Result<PlanningSessionsResponseDto> = runCatching {
        val response = api.getPlanningSessions(userId)
        response.bodyOrThrow("load planning sessions")
    }.mapError("load planning sessions")

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
