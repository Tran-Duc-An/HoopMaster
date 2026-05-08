package com.example.hoopmaster.data.demo

import com.example.hoopmaster.data.model.SessionInfoDto
import com.example.hoopmaster.data.model.SessionStatsDto
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
}
