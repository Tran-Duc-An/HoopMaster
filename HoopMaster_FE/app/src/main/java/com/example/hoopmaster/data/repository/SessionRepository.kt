package com.example.hoopmaster.data.repository

import com.example.hoopmaster.data.api.HoopMasterApi
import com.example.hoopmaster.data.model.SessionInfoDto

class SessionRepository(private val api: HoopMasterApi) : SessionDataSource {
    override suspend fun getSessionInfo(socketId: String): Result<SessionInfoDto> = runCatching {
        val response = api.getSessionInfo(socketId)
        response.bodyOrThrow("load session info")
    }.mapError("load session info")

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
