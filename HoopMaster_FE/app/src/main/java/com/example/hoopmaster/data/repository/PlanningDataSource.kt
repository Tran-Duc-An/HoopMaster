package com.example.hoopmaster.data.repository

import com.example.hoopmaster.data.model.PlanningChatResponseDto
import com.example.hoopmaster.data.model.PlanningHistoryResponseDto
import com.example.hoopmaster.data.model.PlanningSessionResponseDto
import com.example.hoopmaster.data.model.PlanningSessionsResponseDto

interface PlanningDataSource {
    suspend fun sendMessage(userId: String, text: String, sessionId: String): Result<PlanningChatResponseDto>
    suspend fun confirmPlan(userId: String, planId: String): Result<PlanningChatResponseDto>
    suspend fun getHistory(userId: String, sessionId: String): Result<PlanningHistoryResponseDto>
    suspend fun createSession(userId: String): Result<PlanningSessionResponseDto>
    suspend fun getSessions(userId: String): Result<PlanningSessionsResponseDto>
}
