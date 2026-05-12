package com.example.hoopmaster.data.repository

import com.example.hoopmaster.data.model.SessionInfoDto
import com.example.hoopmaster.data.model.WorkoutHistoryWeeklyResponseDto

interface SessionDataSource {
    suspend fun getSessionInfo(socketId: String): Result<SessionInfoDto>
    suspend fun getWeeklyWorkoutHistory(userId: String): Result<WorkoutHistoryWeeklyResponseDto>
}
