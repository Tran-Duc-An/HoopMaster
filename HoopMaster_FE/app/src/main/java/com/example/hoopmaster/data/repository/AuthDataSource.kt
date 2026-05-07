package com.example.hoopmaster.data.repository

import com.example.hoopmaster.data.model.CoachTone
import com.example.hoopmaster.data.model.UserDto

interface AuthDataSource {
    suspend fun login(usernameOrEmail: String, password: String): Result<UserDto>
    suspend fun signup(
        username: String,
        email: String,
        password: String,
        name: String?
    ): Result<UserDto>
    suspend fun updateTone(userId: String, tone: CoachTone): Result<UserDto>
    fun logout()
}
