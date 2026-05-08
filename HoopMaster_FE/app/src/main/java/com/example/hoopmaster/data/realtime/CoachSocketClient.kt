package com.example.hoopmaster.data.realtime

import android.util.Log
import com.example.hoopmaster.data.model.AnglesUpdateEvent
import com.example.hoopmaster.data.model.AudioFeedbackEvent
import com.example.hoopmaster.data.model.CoachSocketEvent
import com.example.hoopmaster.data.model.ConnectedEvent
import com.example.hoopmaster.data.model.ExerciseProgressEvent
import com.example.hoopmaster.data.model.PostShotFeedbackEvent
import com.example.hoopmaster.data.model.ActiveExerciseSessionDto
import com.example.hoopmaster.data.model.SessionInfoDto
import com.example.hoopmaster.data.model.SessionInfoEvent
import com.example.hoopmaster.data.model.SessionStatsDto
import com.example.hoopmaster.data.model.ShotCountUpdateEvent
import com.example.hoopmaster.data.model.SocketErrorEvent
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

class CoachSocketClient(
    private val socketUrl: String
) : CoachSocket {
    private val _events = MutableSharedFlow<CoachSocketEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<CoachSocketEvent> = _events.asSharedFlow()

    private var socket: Socket? = null
    private var currentUserId: String? = null

    override fun connect(userId: String?) {
        if (socket?.connected() == true && currentUserId == userId) return

        if (socket != null && currentUserId != userId) {
            disconnect()
        }

        if (socket == null) {
            socket = createSocket(userId)
            currentUserId = userId
            registerListeners(socket!!)
        }

        socket?.connect()
    }

    override fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
        currentUserId = null
    }

    override fun sendPoseData(payload: JSONObject) {
        socket?.emit("pose_data", payload)
    }

    override fun startExercise(exerciseId: Int, sets: Int?, reps: Int?, restSeconds: Int?) {
        val payload = JSONObject().apply {
            put("exerciseId", exerciseId)
            sets?.let { put("sets", it) }
            reps?.let { put("reps", it) }
            restSeconds?.let { put("restSeconds", it) }
        }
        socket?.emit("start_exercise", payload)
    }

    override fun stopExercise() {
        socket?.emit("stop_exercise")
    }

    override fun sendShotReleased() {
        socket?.emit("shot_released")
    }

    override fun requestSessionInfo() {
        socket?.emit("request_session_info")
    }

    private fun createSocket(userId: String?): Socket {
        val options = IO.Options().apply {
            reconnection = true
            reconnectionAttempts = 5
            reconnectionDelay = 2000
            if (userId != null) {
                query = "userId=$userId"
            }
        }
        return IO.socket(socketUrl, options)
    }

    private fun registerListeners(socket: Socket) {
        socket.on("connected") { args ->
            val payload = firstJsonObject(args)
            val message = payload?.optString("message").orEmpty().ifBlank {
                payload?.optString("text").orEmpty().ifBlank { "Đã kết nối!" }
            }
            emitEvent(
                ConnectedEvent(
                    message = message,
                    socketId = payload?.optString("socketId").orEmpty().ifBlank { socket.id() }
                )
            )
        }

        socket.on("audio_feedback") { args ->
            val payload = firstJsonObject(args)
            emitEvent(
                AudioFeedbackEvent(
                    text = payload?.optString("text"),
                    audioBase64 = payload?.optString("audioBase64"),
                )
            )
        }

        socket.on("angles_update") { args ->
            emitEvent(AnglesUpdateEvent(raw = firstJsonObject(args)))
        }

        socket.on("exercise_progress") { args ->
            emitEvent(ExerciseProgressEvent(raw = firstJsonObject(args)))
        }

        socket.on("llm_post_shot_feedback") { args ->
            val payload = firstJsonObject(args)
            emitEvent(
                PostShotFeedbackEvent(
                    text = payload?.optString("text"),
                    audioBase64 = payload?.optString("audioBase64"),
                    stats = payload?.optJSONObject("stats") ?: payload
                )
            )
        }

        socket.on("shot_count_update") { args ->
            val payload = firstJsonObject(args)
            emitEvent(
                ShotCountUpdateEvent(
                    shotCount = payload?.optInt("shotCount", 0) ?: 0,
                    stats = payload?.optJSONObject("stats") ?: payload
                )
            )
        }

        socket.on("session_info") { args ->
            val payload = firstJsonObject(args)
            emitEvent(
                SessionInfoEvent(
                    info = payload?.toSessionInfoDto(),
                    raw = payload
                )
            )
        }

        socket.on("error") { args ->
            val payload = firstJsonObject(args)
            emitEvent(
                SocketErrorEvent(
                    message = payload?.optString("message").orEmpty().ifBlank {
                        payload?.optString("error").orEmpty().ifBlank { "Socket error" }
                    },
                    code = payload?.optString("code"),
                    raw = payload
                )
            )
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val payload = firstJsonObject(args)
            emitEvent(
                SocketErrorEvent(
                    message = payload?.optString("message").orEmpty().ifBlank {
                        payload?.optString("error").orEmpty().ifBlank {
                            args.firstOrNull()?.toString().orEmpty().ifBlank { "Connect error" }
                        }
                    },
                    code = payload?.optString("code"),
                    raw = payload
                )
            )
        }

        socket.on(Socket.EVENT_DISCONNECT) { args ->
            Log.d("CoachSocketClient", "disconnect: ${args.firstOrNull()}")
        }
    }

    private fun emitEvent(event: CoachSocketEvent) {
        _events.tryEmit(event)
    }

    private fun firstJsonObject(args: Array<out Any>): JSONObject? {
        val first = args.firstOrNull() ?: return null
        return when (first) {
            is JSONObject -> first
            is String -> first.toJsonObjectOrNull()
            else -> JSONObject().apply {
                put("message", first.toString())
            }
        }
    }

    private fun String.toJsonObjectOrNull(): JSONObject? = runCatching {
        JSONObject(this)
    }.getOrNull()

    private fun JSONObject.toSessionInfoDto(): SessionInfoDto {
        val statsJson = optJSONObject("stats")
        val exerciseJson = optJSONObject("exercise")
        return SessionInfoDto(
            socketId = optStringOrNull("socketId"),
            uptime = optLongOrNull("uptime"),
            lastActivity = optLongOrNull("lastActivity"),
            stats = statsJson?.toSessionStatsDto(),
            bufferSize = optIntOrNull("bufferSize"),
            exercise = exerciseJson?.toActiveExerciseSessionDto()
        )
    }

    private fun JSONObject.toSessionStatsDto(): SessionStatsDto = SessionStatsDto(
        totalFrames = optIntOrNull("totalFrames"),
        feedbackCount = optIntOrNull("feedbackCount"),
        shotsCompleted = optIntOrNull("shotsCompleted"),
        exercisesCompleted = optIntOrNull("exercisesCompleted")
    )

    private fun JSONObject.toActiveExerciseSessionDto(): ActiveExerciseSessionDto = ActiveExerciseSessionDto(
        exerciseId = optIntOrNull("exerciseId"),
        name = optStringOrNull("name"),
        set = optIntOrNull("set"),
        reps = optIntOrNull("reps"),
        targetSets = optIntOrNull("targetSets"),
        targetReps = optIntOrNull("targetReps"),
        phase = optStringOrNull("phase"),
        active = optBooleanOrNull("active"),
        completed = optBooleanOrNull("completed")
    )

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).ifBlank { null }
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key)
    }

    private fun JSONObject.optLongOrNull(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return optLong(key)
    }

    private fun JSONObject.optBooleanOrNull(key: String): Boolean? {
        if (!has(key) || isNull(key)) return null
        return optBoolean(key)
    }
}
