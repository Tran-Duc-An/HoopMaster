package com.example.hoopmaster.data.model

import com.google.gson.annotations.SerializedName

data class PlanMetadataDto(
    val source: String? = null,
    val status: String? = null,
    val tone: CoachTone? = null,
    val tags: List<String>? = null,
    val notes: String? = null,
    val version: String? = null,
    val raw: Map<String, Any?>? = null
)

data class TrainingPlanScheduleDto(
    val daysPerWeek: Int? = null,
    val sessionDurationMinutes: Int? = null,
    val raw: Map<String, Any?>? = null
)

data class PlanExerciseDto(
    val exerciseId: Int? = null,
    val exercise: ExerciseDto? = null,
    val name: String? = null,
    val category: String? = null,
    val pose: String? = null,
    val description: String? = null,
    val order: Int? = null,
    val sets: Int? = null,
    val reps: Int? = null,
    val duration: String? = null,
    val reason: String? = null,
    val safetyNotes: String? = null,
    val target: ExerciseTargetDto? = null,
    val counting: ExerciseCountingDto? = null,
    val tracking: ExerciseTrackingDto? = null,
    val voiceCues: ExerciseVoiceCuesDto? = null,
    val raw: Map<String, Any?>? = null
)

data class TrainingPlanDto(
    @SerializedName("_id")
    val id: String? = null,
    val userId: String? = null,
    val createdBy: String? = null,
    val title: String? = null,
    val description: String? = null,
    val goal: String? = null,
    val injuryConstraints: List<String>? = null,
    val source: String? = null,
    val status: String? = null,
    val schedule: TrainingPlanScheduleDto? = null,
    val exercises: List<PlanExerciseDto>? = null,
    val metadata: PlanMetadataDto? = null,
    val activatedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val raw: Map<String, Any?>? = null
)

data class PlanResponseDto(
    val plan: TrainingPlanDto? = null
)

data class PlansResponseDto(
    val plans: List<TrainingPlanDto>? = null
)
