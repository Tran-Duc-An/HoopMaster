package com.example.hoopmaster.data.model

import org.json.JSONObject

sealed interface CoachSocketEvent {
    val type: String?
}

data class ConnectedEvent(
    override val type: String? = "connected",
    val message: String? = null,
    val socketId: String? = null
) : CoachSocketEvent

data class AudioFeedbackEvent(
    override val type: String? = "audio_feedback",
    val text: String? = null,
    val audioBase64: String? = null
) : CoachSocketEvent

data class AnglesUpdateEvent(
    override val type: String? = "angles_update",
    val raw: JSONObject? = null
) : CoachSocketEvent

data class ExerciseProgressEvent(
    override val type: String? = "exercise_progress",
    val raw: JSONObject? = null
) : CoachSocketEvent

data class PostShotFeedbackEvent(
    override val type: String? = "post_shot_feedback",
    val text: String? = null,
    val audioBase64: String? = null,
    val stats: JSONObject? = null
) : CoachSocketEvent

data class ShotCountUpdateEvent(
    override val type: String? = "shot_count_update",
    val shotCount: Int = 0,
    val stats: JSONObject? = null
) : CoachSocketEvent

data class SocketErrorEvent(
    override val type: String? = "error",
    val message: String? = null,
    val code: String? = null,
    val raw: JSONObject? = null
) : CoachSocketEvent
