package com.example.hoopmaster.data.repository

import com.example.hoopmaster.core.session.SessionStore
import com.example.hoopmaster.data.api.HoopMasterApi
import com.example.hoopmaster.data.model.AuthResponseDto
import com.example.hoopmaster.data.model.CoachTone
import com.example.hoopmaster.data.model.LoginRequest
import com.example.hoopmaster.data.model.SignupRequest
import com.example.hoopmaster.data.model.UpdateToneRequest
import com.example.hoopmaster.data.model.UserDto
import com.example.hoopmaster.data.model.UserResponseDto

class AuthRepository(
    private val api: HoopMasterApi,
    private val sessionStore: SessionStore
) : AuthDataSource {
    override
    suspend fun login(usernameOrEmail: String, password: String): Result<UserDto> =
        runCatching {
            val response = api.login(LoginRequest(usernameOrEmail = usernameOrEmail, password = password))
            val body = response.bodyOrThrow("login")
            val user = body.requireUser("login")
            sessionStore.saveUser(user)
            user
        }.mapError("login")

    override
    suspend fun signup(
        username: String,
        email: String,
        password: String,
        name: String?
    ): Result<UserDto> = runCatching {
        val response = api.signup(
            SignupRequest(
                username = username,
                email = email,
                password = password,
                name = name
            )
        )
        val body = response.bodyOrThrow("signup")
        val user = body.requireUser("signup")
        sessionStore.saveUser(user)
        user
    }.mapError("signup")

    override
    suspend fun updateTone(userId: String, tone: CoachTone): Result<UserDto> = runCatching {
        val response = api.updateTone(userId, UpdateToneRequest(tone = tone.backendValue()))
        val body = response.bodyOrThrow("update tone")
        val user = body.requireUser("update tone")
        sessionStore.saveUser(user)
        sessionStore.saveTone(tone)
        user
    }.mapError("update tone")

    override
    fun logout() {
        sessionStore.clear()
    }

    private fun AuthResponseDto.requireUser(action: String): UserDto {
        return (user ?: data ?: throw Exception("Empty response from $action")).requireId(action)
    }

    private fun UserResponseDto.requireUser(action: String): UserDto {
        return (user ?: data ?: UserDto(
            id = id,
            username = username,
            email = email,
            name = name,
            tone = tone,
            avatarUrl = avatarUrl,
            token = token
        )).requireId(action)
    }

    private fun UserDto.requireId(action: String): UserDto {
        if (id.isNullOrBlank()) {
            throw Exception("Missing user id from $action")
        }
        return this
    }

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
