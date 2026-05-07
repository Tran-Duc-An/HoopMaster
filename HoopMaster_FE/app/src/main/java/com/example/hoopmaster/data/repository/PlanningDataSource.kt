package com.example.hoopmaster.data.repository

import com.example.hoopmaster.data.model.PlanningChatResponseDto
import com.example.hoopmaster.data.model.PlanningHistoryResponseDto

interface PlanningDataSource {
    suspend fun sendMessage(userId: String, text: String): Result<PlanningChatResponseDto>
    suspend fun confirmPlan(userId: String, planId: String): Result<PlanningChatResponseDto>
    suspend fun getHistory(userId: String): Result<PlanningHistoryResponseDto>
}
