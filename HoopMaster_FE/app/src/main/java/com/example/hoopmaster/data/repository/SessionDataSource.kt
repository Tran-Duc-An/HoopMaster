package com.example.hoopmaster.data.repository

import com.example.hoopmaster.data.model.SessionInfoDto

interface SessionDataSource {
    suspend fun getSessionInfo(socketId: String): Result<SessionInfoDto>
}
