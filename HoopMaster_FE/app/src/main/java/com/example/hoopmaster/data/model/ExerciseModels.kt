package com.example.hoopmaster.data.model

import com.google.gson.annotations.SerializedName

data class ExerciseTargetDto(
    val sets: Int? = null,
    val reps: Int? = null,
    val restSeconds: Int? = null,
    val raw: Map<String, Any?>? = null
)

data class ExerciseCountingDto(
    val mode: String? = null,
    val countOnPhase: String? = null,
    val secondsPerRep: Int? = null,
    val phases: List<ExercisePhaseDto>? = null,
    val raw: Map<String, Any?>? = null
)

data class ExercisePhaseDto(
    val key: String? = null,
    val cue: String? = null,
    val durationMs: Int? = null,
    val countRep: Boolean? = null,
    val raw: Map<String, Any?>? = null
)

data class ExerciseVoiceCuesDto(
    val intro: String? = null,
    val setup: String? = null,
    val repTemplate: String? = null,
    val setComplete: String? = null,
    val complete: String? = null,
    val warnings: List<String>? = null,
    val raw: Map<String, Any?>? = null
)

data class ExerciseTrackingDto(
    val type: String? = null,
    val primaryJoints: List<String>? = null,
    val counter: ExerciseCounterDto? = null,
    val raw: Map<String, Any?>? = null
)

data class ExerciseCounterDto(
    val joint: String? = null,
    val downThreshold: Double? = null,
    val upThreshold: Double? = null,
    val raw: Map<String, Any?>? = null
)

data class ExerciseVoiceScriptDto(
    val exerciseId: Int? = null,
    val name: String? = null,
    val category: String? = null,
    val pose: String? = null,
    val target: ExerciseTargetDto? = null,
    val warnings: List<String>? = null,
    val script: List<ExerciseScriptItemDto>? = null,
    val raw: Map<String, Any?>? = null
)

data class ExerciseScriptItemDto(
    val type: String? = null,
    val text: String? = null,
    val set: Int? = null,
    val rep: Int? = null,
    val restSeconds: Int? = null,
    val raw: Map<String, Any?>? = null
)

data class ExerciseDto(
    @SerializedName("id")
    val id: Int? = null,
    @SerializedName("_id")
    val mongoId: String? = null,
    val name: String? = null,
    val category: String? = null,
    val pose: String? = null,
    val description: String? = null,
    val sets: Int? = null,
    val reps: Int? = null,
    val duration: String? = null,
    val reason: String? = null,
    val safetyNotes: String? = null,
    val order: Int? = null,
    val target: ExerciseTargetDto? = null,
    val counting: ExerciseCountingDto? = null,
    val voiceCues: ExerciseVoiceCuesDto? = null,
    val tracking: ExerciseTrackingDto? = null,
    val voiceScript: ExerciseVoiceScriptDto? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val raw: Map<String, Any?>? = null
)
