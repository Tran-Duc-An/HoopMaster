package com.example.hoopmaster.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hoopmaster.core.di.AppContainer
import com.example.hoopmaster.data.model.AnglesUpdateEvent
import com.example.hoopmaster.data.model.ExerciseProgressDto
import com.example.hoopmaster.data.model.AudioFeedbackEvent
import com.example.hoopmaster.data.model.ConnectedEvent
import com.example.hoopmaster.data.model.ExerciseProgressEvent
import com.example.hoopmaster.data.model.LiveAnglesDto
import com.example.hoopmaster.data.model.PostShotFeedbackEvent
import com.example.hoopmaster.data.model.SessionInfoDto
import com.example.hoopmaster.data.model.SessionInfoEvent
import com.example.hoopmaster.data.model.ShotStatsDto
import com.example.hoopmaster.data.model.ShotCountUpdateEvent
import com.example.hoopmaster.data.model.SocketErrorEvent
import com.example.hoopmaster.media.AudioPlayer
import com.example.hoopmaster.data.realtime.CoachSocket
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class TrackingUiState(
    val poseResult: PoseLandmarkerResult? = null,
    val feedbackText: String = "Đang kết nối Server...",
    val selectedTone: String = "neutral",
    val isConnected: Boolean = false,
    val socketId: String? = null,
    val isExerciseActive: Boolean = false,
    val shotCount: Int = 0,
    val liveAngles: LiveAnglesDto? = null,
    val lastShotStats: ShotStatsDto? = null,
    val sessionInfo: SessionInfoDto? = null,
    val lastShotReleasedAt: Long? = null,
    val errorMessage: String? = null,
    val llmFeedback: String? = null,
    // Exercise tracking progress
    val exerciseProgress: ExerciseProgressDto? = null
)

sealed interface TrackingAction {
    data object Connect : TrackingAction
    data object StartExercise : TrackingAction
    data class OnPoseDetected(val result: PoseLandmarkerResult) : TrackingAction
    data object OnShotReleased : TrackingAction
    data object StopExercise : TrackingAction
    data object Disconnect : TrackingAction
    data class ToneChanged(val tone: String) : TrackingAction
}

class TrackingViewModel(application: Application) : AndroidViewModel(application) {
    private val container = AppContainer(application)
    private val socketClient: CoachSocket = container.createSocketClient()

    val poseResult = mutableStateOf<PoseLandmarkerResult?>(null)
    val feedbackText = mutableStateOf("Đang kết nối Server...")
    val selectedTone = mutableStateOf("neutral")

    private val _uiState = MutableStateFlow(
        TrackingUiState(
            feedbackText = feedbackText.value,
            selectedTone = selectedTone.value,
            isConnected = false
        )
    )
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    private val audioPlayer = AudioPlayer(application.applicationContext)
    private var lastSendTime = 0L
    private val sendInterval = 100L

    init {
        observeSocketEvents()
        connect()
    }

    fun onAction(action: TrackingAction) {
        when (action) {
            TrackingAction.Connect -> connect()
            TrackingAction.StartExercise -> startExercise()
            is TrackingAction.OnPoseDetected -> onPoseDetected(action.result)
            TrackingAction.OnShotReleased -> onShotReleased()
            TrackingAction.StopExercise -> stopExercise()
            TrackingAction.Disconnect -> disconnect()
            is TrackingAction.ToneChanged -> updateTone(action.tone)
        }
    }

    fun connect() {
        socketClient.connect(container.sessionStore.getUserId())
    }

    fun startExercise(
        exerciseId: Int? = null,
        sets: Int? = null,
        reps: Int? = null,
        restSeconds: Int? = null
    ) {
        syncState { it.copy(isExerciseActive = true) }
        if (exerciseId != null) {
            socketClient.startExercise(exerciseId, sets, reps, restSeconds)
        }
    }

    fun onPoseDetected(result: PoseLandmarkerResult, mirrorX: Boolean = false) {
        poseResult.value = result
        syncState { it.copy(poseResult = result) }
        streamPoseToServer(result, mirrorX)
    }

    fun onShotReleased() {
        socketClient.sendShotReleased()
        syncState { it.copy(lastShotReleasedAt = System.currentTimeMillis()) }
    }

    fun stopExercise() {
        socketClient.stopExercise()
        syncState { it.copy(isExerciseActive = false) }
    }

    fun disconnect() {
        socketClient.disconnect()
        syncState {
            it.copy(
                isConnected = false,
                isExerciseActive = false,
                socketId = null
            )
        }
    }

    fun requestSessionInfo() {
        socketClient.requestSessionInfo()
    }

    private fun setFeedback(text: String) {
        feedbackText.value = text
        syncState { it.copy(feedbackText = text) }
    }

    fun streamPoseToServer(result: PoseLandmarkerResult, mirrorX: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSendTime < sendInterval) return
        lastSendTime = currentTime

        if (result.landmarks().isEmpty()) return

        val landmarks = result.landmarks()[0]
        val jsonArray = JSONArray()

        landmarks.forEachIndexed { index, landmark ->
            val transformedX = if (mirrorX) 1f - landmark.x() else landmark.x()
            val point = JSONObject().apply {
                put("id", index)
                put("x", transformedX)
                put("y", landmark.y())
                put("z", landmark.z())
                put("visibility", landmark.visibility().orElse(0f))
            }
            jsonArray.put(point)
        }

        val payload = JSONObject().apply {
            put("landmarks", jsonArray)
            put("exerciseType", "shooting")
            put("tone", selectedTone.value)
            put("timestamp", currentTime)
            put("action", "pose_data")
        }

        socketClient.sendPoseData(payload)
    }

    fun updateTone(tone: String) {
        selectedTone.value = tone
        syncState { it.copy(selectedTone = tone) }
    }

    private fun syncState(transform: (TrackingUiState) -> TrackingUiState) {
        val next = transform(_uiState.value)
        _uiState.value = next
        poseResult.value = next.poseResult
        feedbackText.value = next.feedbackText
        selectedTone.value = next.selectedTone
    }

    private fun observeSocketEvents() {
        viewModelScope.launch {
            socketClient.events.collect { event ->
                when (event) {
                    is ConnectedEvent -> {
                        val message = event.message?.takeIf { it.isNotBlank() } ?: "Đã kết nối!"
                        setFeedback(message)
                        syncState {
                            it.copy(
                                isConnected = true,
                                socketId = event.socketId,
                                errorMessage = null
                            )
                        }
                    }

                    is AudioFeedbackEvent -> {
                        event.text?.takeIf { it.isNotBlank() }?.let { setFeedback(it) }
                        event.audioBase64?.takeIf { it.isNotBlank() }?.let { audioPlayer.playBase64Audio(it) }
                    }

                    is AnglesUpdateEvent -> {
                        val parsed = event.raw?.toLiveAnglesDto()
                        if (parsed != null) {
                            syncState { it.copy(liveAngles = parsed) }
                        }
                        event.raw?.optString("feedback")?.takeIf { it.isNotBlank() }?.let { setFeedback(it) }
                    }

                    is ExerciseProgressEvent -> {
                        val active = event.raw?.optBoolean("isActive")
                        if (active != null) {
                            syncState { it.copy(isExerciseActive = active) }
                        }
                        event.raw?.optString("message")?.takeIf { it.isNotBlank() }?.let { setFeedback(it) }
                        val progressDto = event.raw?.toExerciseProgressDto()
                        if (progressDto != null) {
                            syncState { it.copy(exerciseProgress = progressDto) }
                        }
                    }

                    is PostShotFeedbackEvent -> {
                        val parsed = event.stats?.toShotStatsDto()
                        if (parsed != null) {
                            syncState { it.copy(lastShotStats = parsed) }
                        }
                        event.text?.takeIf { it.isNotBlank() }?.let { setFeedback(it) }
                        event.audioBase64?.takeIf { it.isNotBlank() }?.let { audioPlayer.playBase64Audio(it) }
                    }

                    is ShotCountUpdateEvent -> {
                        syncState { it.copy(
                            shotCount = event.shotCount,
                            llmFeedback = event.llmFeedback
                        ) }
                        setFeedback("Shots completed: ${event.shotCount}")
                    }

                    is SessionInfoEvent -> {
                        syncState { it.copy(sessionInfo = event.info) }
                    }

                    is SocketErrorEvent -> {
                        val message = event.message?.takeIf { it.isNotBlank() } ?: "Socket error"
                        syncState { it.copy(isConnected = false, errorMessage = message) }
                        setFeedback(message)
                        Log.e("TrackingViewModel", "socket error: $message")
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketClient.disconnect()
        audioPlayer.release()
    }

    private fun JSONObject.toLiveAnglesDto(): LiveAnglesDto {
        return LiveAnglesDto(
            elbowAngle = optNullableDouble("elbowAngle"),
            shoulderAngle = optNullableDouble("shoulderAngle"),
            kneeAngle = optNullableDouble("kneeAngle"),
            backAngle = optNullableDouble("backAngle"),
            shootingHand = optString("shootingHand").takeIf { it.isNotBlank() }
        )
    }

    private fun JSONObject.toShotStatsDto(): ShotStatsDto {
        return ShotStatsDto(
            avgElbowAngle = optNullableDouble("avgElbowAngle"),
            avgKneeAngle = optNullableDouble("avgKneeAngle"),
            avgShoulderAngle = optNullableDouble("avgShoulderAngle"),
            avgBackAngle = optNullableDouble("avgBackAngle"),
            frameCount = if (has("frameCount") && !isNull("frameCount")) optInt("frameCount") else null,
            shootingHand = optString("shootingHand").takeIf { it.isNotBlank() },
            tone = optString("tone").takeIf { it.isNotBlank() }
        )
    }

    private fun JSONObject.toExerciseProgressDto(): ExerciseProgressDto {
        return ExerciseProgressDto(
            exerciseId = optIntOrNull("exerciseId"),
            tone = optString("tone").takeIf { it.isNotBlank() },
            name = optString("name").takeIf { it.isNotBlank() },
            category = optString("category").takeIf { it.isNotBlank() },
            set = optIntOrNull("set"),
            targetSets = optIntOrNull("targetSets"),
            reps = optIntOrNull("reps"),
            targetReps = optIntOrNull("targetReps"),
            phase = optString("phase").takeIf { it.isNotBlank() },
            phaseIndex = optIntOrNull("phaseIndex"),
            totalPhases = optIntOrNull("totalPhases"),
            currentPhaseCue = optString("currentPhaseCue").takeIf { it.isNotBlank() },
            completed = optBooleanOrNull("completed") ?: false,
            angle = optNullableDouble("angle"),
            restRemainingMs = if (has("restRemainingMs") && !isNull("restRemainingMs")) optLong("restRemainingMs") else null,
            restSeconds = optIntOrNull("restSeconds"),
            timestamp = if (has("timestamp") && !isNull("timestamp")) optLong("timestamp") else null
        )
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key)
    }

    private fun JSONObject.optBooleanOrNull(key: String): Boolean? {
        if (!has(key) || isNull(key)) return null
        return optBoolean(key)
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val value = optDouble(key, Double.NaN)
        return value.takeUnless { it.isNaN() }
    }
}
