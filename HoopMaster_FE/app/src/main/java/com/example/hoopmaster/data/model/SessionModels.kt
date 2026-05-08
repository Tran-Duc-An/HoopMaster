package com.example.hoopmaster.data.model

data class SessionStatsDto(
    val totalFrames: Int? = null,
    val feedbackCount: Int? = null,
    val shotsCompleted: Int? = null,
    val exercisesCompleted: Int? = null
)

data class ActiveExerciseSessionDto(
    val exerciseId: Int? = null,
    val name: String? = null,
    val set: Int? = null,
    val reps: Int? = null,
    val targetSets: Int? = null,
    val targetReps: Int? = null,
    val phase: String? = null,
    val active: Boolean? = null,
    val completed: Boolean? = null
)

data class SessionInfoDto(
    val socketId: String? = null,
    val uptime: Long? = null,
    val lastActivity: Long? = null,
    val stats: SessionStatsDto? = null,
    val bufferSize: Int? = null,
    val exercise: ActiveExerciseSessionDto? = null
)
