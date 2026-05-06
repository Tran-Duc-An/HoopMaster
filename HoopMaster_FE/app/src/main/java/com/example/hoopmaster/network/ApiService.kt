package com.example.hoopmaster.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.Response
import retrofit2.http.Path

// --- Data Models ---
// Tùy thuộc vào backend của bạn yêu cầu field gì, ở đây mình giả định dùng email & password
data class AuthRequest(val email: String, val password: String)

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
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/users/signup")
    suspend fun signup(@Body request: AuthRequest): Response<AuthResponse>

    @PUT("api/users/profile/{id}/tone")
    suspend fun updateTone(@Path("id") id: String, @Body body: Map<String, String>): Response<Any>

    @POST("api/feedback/shot")
    suspend fun getShotFeedback(@Body shotData: Map<String, Any>): Response<Map<String, String>>
}