package com.example.hoopmaster.data.model

data class PlanningMessageRequest(
    val text: String? = null,
    val audioBase64: String? = null
)

data class ConfirmPlanRequest(
    val planId: String? = null
)

data class PlanningProfileDto(
    val goal: String? = null,
    val experienceLevel: String? = null,
    val preferredTone: String? = null,
    val sport: String? = null,
    val age: Int? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val handedness: String? = null,
    val notes: String? = null,
    val raw: Map<String, Any?>? = null
)

data class PlanDraftDto(
    val title: String? = null,
    val summary: String? = null,
    val goal: String? = null,
    val durationDays: Int? = null,
    val durationWeeks: Int? = null,
    val source: String? = null,
    val status: String? = null,
    val metadata: PlanMetadataDto? = null,
    val raw: Map<String, Any?>? = null
)

data class PlanningChatMessageDto(
    val id: String? = null,
    val type: String? = null,
    val role: String? = null,
    val content: String? = null,
    val text: String? = null,
    val reply: String? = null,
    val audioBase64: String? = null,
    val collectedProfile: PlanningProfileDto? = null,
    val missingFields: List<String>? = null,
    val planDraft: TrainingPlanDto? = null,
    val plan: TrainingPlanDto? = null,
    val createdAt: String? = null,
    val timestamp: String? = null,
    val raw: Map<String, Any?>? = null
)

data class PlanningChatResponseDto(
    val type: String? = null,
    val reply: String? = null,
    val audioBase64: String? = null,
    val collectedProfile: PlanningProfileDto? = null,
    val missingFields: List<String>? = null,
    val planDraft: TrainingPlanDto? = null,
    val plan: TrainingPlanDto? = null,
    val history: List<PlanningChatMessageDto>? = null,
    val message: String? = null,
    val status: String? = null,
    val raw: Map<String, Any?>? = null
)

data class PlanningHistoryResponseDto(
    val history: List<PlanningChatMessageDto>? = null,
    val messages: List<PlanningChatMessageDto>? = null,
    val chat: List<PlanningChatMessageDto>? = null,
    val raw: Map<String, Any?>? = null
)
