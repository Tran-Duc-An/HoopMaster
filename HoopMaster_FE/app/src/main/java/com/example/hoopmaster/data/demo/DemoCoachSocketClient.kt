package com.example.hoopmaster.data.demo

import com.example.hoopmaster.data.model.ConnectedEvent
import com.example.hoopmaster.data.model.CoachSocketEvent
import com.example.hoopmaster.data.model.ExerciseProgressEvent
import com.example.hoopmaster.data.model.PostShotFeedbackEvent
import com.example.hoopmaster.data.realtime.CoachSocket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

class DemoCoachSocketClient : CoachSocket {
    private val _events = MutableSharedFlow<CoachSocketEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<CoachSocketEvent> = _events.asSharedFlow()

    private var connected = false
    private var currentExerciseId: Int? = null

    override fun connect(userId: String?) {
        connected = true
        _events.tryEmit(
            ConnectedEvent(
                message = "Demo connected",
                socketId = "demo-socket-${userId ?: "guest"}"
            )
        )
    }

    override fun disconnect() {
        connected = false
        currentExerciseId = null
    }

    override fun sendPoseData(payload: JSONObject) {
        if (!connected) return
        val progress = JSONObject().apply {
            put("isActive", currentExerciseId != null)
            put("message", "Tracking demo form...")
            put("exerciseId", currentExerciseId ?: -1)
            put("timestamp", System.currentTimeMillis())
            put("rawPose", payload)
        }
        _events.tryEmit(ExerciseProgressEvent(raw = progress))
    }

    override fun startExercise(exerciseId: Int, sets: Int?, reps: Int?, restSeconds: Int?) {
        if (!connected) return
        currentExerciseId = exerciseId
        val progress = JSONObject().apply {
            put("isActive", true)
            put("message", "Exercise started")
            put("exerciseId", exerciseId)
            sets?.let { put("sets", it) }
            reps?.let { put("reps", it) }
            restSeconds?.let { put("restSeconds", it) }
        }
        _events.tryEmit(ExerciseProgressEvent(raw = progress))
    }

    override fun stopExercise() {
        if (!connected) return
        currentExerciseId = null
        val progress = JSONObject().apply {
            put("isActive", false)
            put("message", "Exercise stopped")
        }
        _events.tryEmit(ExerciseProgressEvent(raw = progress))
    }

    override fun sendShotReleased() {
        if (!connected) return
        _events.tryEmit(
            PostShotFeedbackEvent(
                text = "Nice release. Keep elbow aligned.",
                audioBase64 = null,
                stats = JSONObject().apply {
                    put("releaseAngle", 49.5)
                    put("arcQuality", "good")
                    put("timingMs", 620)
                }
            )
        )
    }

    override fun requestSessionInfo() {
        if (!connected) return
        val progress = JSONObject().apply {
            put("isActive", currentExerciseId != null)
            put("message", "Demo session ready")
            put("exerciseId", currentExerciseId ?: -1)
        }
        _events.tryEmit(ExerciseProgressEvent(raw = progress))
    }
}
