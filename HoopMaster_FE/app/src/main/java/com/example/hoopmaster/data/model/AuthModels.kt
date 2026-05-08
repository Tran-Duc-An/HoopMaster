package com.example.hoopmaster.data.model

import com.google.gson.annotations.SerializedName

enum class CoachTone {
    @SerializedName("strict")
    STRICT,

    @SerializedName("cheerful")
    CHEERFUL,

    @SerializedName("neutral")
    NEUTRAL;

    fun backendValue(): String = when (this) {
        STRICT -> "strict"
        CHEERFUL -> "cheerful"
        NEUTRAL -> "neutral"
    }
}

data class LoginRequest(
    val usernameOrEmail: String? = null,
    val password: String? = null
)

data class SignupRequest(
    val username: String? = null,
    val email: String? = null,
    val password: String? = null,
    val name: String? = null
)

data class UpdateToneRequest(
    val tone: String? = null
)

data class UserDto(
    @SerializedName("_id")
    val id: String? = null,
    val username: String? = null,
    val email: String? = null,
    val name: String? = null,
    val tone: CoachTone? = null,
    val avatarUrl: String? = null,
    val token: String? = null
)

data class UserResponseDto(
    @SerializedName("_id")
    val id: String? = null,
    val username: String? = null,
    val email: String? = null,
    val name: String? = null,
    val tone: CoachTone? = null,
    val avatarUrl: String? = null,
    val token: String? = null,
    val user: UserDto? = null,
    val data: UserDto? = null
)

data class AuthResponseDto(
    val token: String? = null,
    val user: UserDto? = null,
    val data: UserDto? = null,
    val message: String? = null
)
