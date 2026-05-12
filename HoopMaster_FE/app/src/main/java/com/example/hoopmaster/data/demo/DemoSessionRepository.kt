package com.example.hoopmaster.data.demo

import com.example.hoopmaster.data.model.SessionInfoDto
import com.example.hoopmaster.data.model.SessionStatsDto
import com.example.hoopmaster.data.model.WorkoutHistoryDayDto
import com.example.hoopmaster.data.model.WorkoutHistoryWeeklyResponseDto
import com.example.hoopmaster.data.repository.SessionDataSource

class DemoSessionRepository : SessionDataSource {
    override suspend fun getSessionInfo(socketId: String): Result<SessionInfoDto> {
        return Result.success(
            SessionInfoDto(
                socketId = socketId,
                uptime = 0L,
                lastActivity = System.currentTimeMillis(),
                stats = SessionStatsDto(
                    totalFrames = 0,
                    feedbackCount = 0,
                    shotsCompleted = 0,
                    exercisesCompleted = 0
                ),
                bufferSize = 0,
                exercise = null
            )
        )
    }

    override suspend fun getWeeklyWorkoutHistory(userId: String): Result<WorkoutHistoryWeeklyResponseDto> {
        return Result.success(
            WorkoutHistoryWeeklyResponseDto(
                days = listOf(
                    WorkoutHistoryDayDto("2026-05-06", "Wed", totalExercises = 1, totalMinutes = 35, totalSets = 3),
                    WorkoutHistoryDayDto("2026-05-07", "Thu", totalExercises = 2, totalMinutes = 75, totalSets = 5),
                    WorkoutHistoryDayDto("2026-05-08", "Fri", totalExercises = 0, totalMinutes = 0, totalSets = 0),
                    WorkoutHistoryDayDto("2026-05-09", "Sat", totalExercises = 1, totalMinutes = 45, totalSets = 4),
                    WorkoutHistoryDayDto("2026-05-10", "Sun", totalExercises = 1, totalMinutes = 25, totalSets = 2),
                    WorkoutHistoryDayDto("2026-05-11", "Mon", totalExercises = 2, totalMinutes = 90, totalSets = 6),
                    WorkoutHistoryDayDto("2026-05-12", "Tue", totalExercises = 1, totalMinutes = 50, totalSets = 4)
                )
            )
        )
    }
}
