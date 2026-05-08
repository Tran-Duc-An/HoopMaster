package com.example.hoopmaster.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.Response
import retrofit2.http.Path

// --- Data Models ---
data class LoginAuthRequest(val usernameOrEmail: String, val password: String)
data class SignupAuthRequest(
    val username: String,
    val email: String,
    val password: String,
    val name: String? = null
)

data class User(
    val _id: String,
    val email: String,
    val tone: String?
)

data class AuthResponse(
    val token: String?, // Dành cho JWT Token (nếu backend có trả về)
    val user: User
)

// --- API Interface ---
interface ApiService {
    @POST("api/users/login")
    suspend fun login(@Body request: LoginAuthRequest): Response<AuthResponse>

    @POST("api/users/signup")
    suspend fun signup(@Body request: SignupAuthRequest): Response<AuthResponse>

    @PUT("api/users/profile/{id}/tone")
    suspend fun updateTone(@Path("id") id: String, @Body body: Map<String, String>): Response<Any>

    @POST("api/feedback/shot")
    suspend fun getShotFeedback(@Body shotData: Map<String, Any>): Response<Map<String, String>>
}
