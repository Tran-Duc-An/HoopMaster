package com.example.hoopmaster.data.api

import com.example.hoopmaster.data.model.AuthResponseDto
import com.example.hoopmaster.data.model.ConfirmPlanRequest
import com.example.hoopmaster.data.model.ExerciseDto
import com.example.hoopmaster.data.model.ExerciseVoiceScriptDto
import com.example.hoopmaster.data.model.LoginRequest
import com.example.hoopmaster.data.model.PlansResponseDto
import com.example.hoopmaster.data.model.PlanningChatResponseDto
import com.example.hoopmaster.data.model.PlanningHistoryResponseDto
import com.example.hoopmaster.data.model.PlanningMessageRequest
import com.example.hoopmaster.data.model.PlanningSessionRequest
import com.example.hoopmaster.data.model.PlanningSessionResponseDto
import com.example.hoopmaster.data.model.PlanningSessionsResponseDto
import com.example.hoopmaster.data.model.PlanResponseDto
import com.example.hoopmaster.data.model.SignupRequest
import com.example.hoopmaster.data.model.SessionInfoDto
import com.example.hoopmaster.data.model.TrainingPlanDto
import com.example.hoopmaster.data.model.UpdateToneRequest
import com.example.hoopmaster.data.model.UserResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.PUT

interface HoopMasterApi {
    @POST("api/users/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponseDto>

    @POST("api/users/signup")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponseDto>

    @PUT("api/users/profile/{id}/tone")
    suspend fun updateTone(
        @Path("id") id: String,
        @Body request: UpdateToneRequest
    ): Response<UserResponseDto>

    @POST("api/users/{id}/planning-chat/message")
    suspend fun sendPlanningMessage(
        @Path("id") id: String,
        @Body request: PlanningMessageRequest
    ): Response<PlanningChatResponseDto>

    @POST("api/users/{id}/planning-chat/confirm-plan")
    suspend fun confirmPlan(
        @Path("id") id: String,
        @Body request: ConfirmPlanRequest
    ): Response<PlanningChatResponseDto>

    @GET("api/users/{id}/planning-chat/history")
    suspend fun getPlanningHistory(
        @Path("id") id: String,
        @Query("sessionId") sessionId: String? = null
    ): Response<PlanningHistoryResponseDto>

    @POST("api/users/{id}/planning-chat/sessions")
    suspend fun createPlanningSession(
        @Path("id") id: String,
        @Body request: PlanningSessionRequest = PlanningSessionRequest()
    ): Response<PlanningSessionResponseDto>

    @GET("api/users/{id}/planning-chat/sessions")
    suspend fun getPlanningSessions(
        @Path("id") id: String
    ): Response<PlanningSessionsResponseDto>

    @GET("api/users/{id}/plans/active")
    suspend fun getActivePlan(
        @Path("id") id: String
    ): Response<PlanResponseDto>

    @GET("api/users/{id}/plans")
    suspend fun getPlans(
        @Path("id") id: String,
        @Query("source") source: String? = null,
        @Query("status") status: String? = null
    ): Response<PlansResponseDto>

    @GET("api/exercises/default")
    suspend fun getDefaultTrainingPlan(): Response<TrainingPlanDto>

    @GET("api/exercises")
    suspend fun getExercises(): Response<List<ExerciseDto>>

    @GET("api/exercises/{id}")
    suspend fun getExercise(
        @Path("id") id: Int
    ): Response<ExerciseDto>

    @GET("api/exercises/{id}/voice-script")
    suspend fun getExerciseVoiceScript(
        @Path("id") id: Int,
        @Query("sets") sets: Int? = null,
        @Query("reps") reps: Int? = null,
        @Query("restSeconds") restSeconds: Int? = null
    ): Response<ExerciseVoiceScriptDto>

    @GET("api/sessions/{socketId}")
    suspend fun getSessionInfo(
        @Path("socketId") socketId: String
    ): Response<SessionInfoDto>
}
