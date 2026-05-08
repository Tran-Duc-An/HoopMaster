package com.example.hoopmaster.data.demo

import com.example.hoopmaster.data.model.PlanningChatResponseDto
import com.example.hoopmaster.data.model.PlanningHistoryResponseDto
import com.example.hoopmaster.data.repository.PlanningDataSource

class DemoPlanningRepository : PlanningDataSource {
    override suspend fun sendMessage(userId: String, text: String): Result<PlanningChatResponseDto> {
        val response = DemoFixtures.planningReply(text).let {
            it.copy(plan = it.plan?.copy(userId = userId), planDraft = it.planDraft?.copy(userId = userId))
        }
        return Result.success(response)
    }

    override suspend fun confirmPlan(userId: String, planId: String): Result<PlanningChatResponseDto> {
        return Result.success(DemoFixtures.confirmedPlan(planId).copy(plan = DemoFixtures.activePlan().copy(id = planId, userId = userId, status = "active")))
    }

    override suspend fun getHistory(userId: String): Result<PlanningHistoryResponseDto> {
        return Result.success(PlanningHistoryResponseDto(history = DemoFixtures.planningHistory()))
    }
}
