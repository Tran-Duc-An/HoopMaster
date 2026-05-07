package com.example.hoopmaster.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hoopmaster.core.di.AppContainer
import com.example.hoopmaster.data.model.AnglesUpdateEvent
import com.example.hoopmaster.data.model.AudioFeedbackEvent
import com.example.hoopmaster.data.model.ConnectedEvent
import com.example.hoopmaster.data.model.ExerciseProgressEvent
import com.example.hoopmaster.data.model.PostShotFeedbackEvent
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
    val isExerciseActive: Boolean = false,
    val lastShotReleasedAt: Long? = null,
    val errorMessage: String? = null
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

    fun onPoseDetected(result: PoseLandmarkerResult) {
        poseResult.value = result
        syncState { it.copy(poseResult = result) }
        streamPoseToServer(result)
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
        syncState { it.copy(isConnected = false, isExerciseActive = false) }
    }

    private fun setFeedback(text: String) {
        feedbackText.value = text
        syncState { it.copy(feedbackText = text) }
    }

    fun streamPoseToServer(result: PoseLandmarkerResult) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSendTime < sendInterval) return
        lastSendTime = currentTime

        if (result.landmarks().isEmpty()) return

        val landmarks = result.landmarks()[0]
        val jsonArray = JSONArray()

        landmarks.forEachIndexed { index, landmark ->
            val point = JSONObject().apply {
                put("id", index)
                put("x", landmark.x())
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
                                errorMessage = null
                            )
                        }
                    }

                    is AudioFeedbackEvent -> {
                        event.text?.takeIf { it.isNotBlank() }?.let { setFeedback(it) }
                        event.audioBase64?.takeIf { it.isNotBlank() }?.let { audioPlayer.playBase64Audio(it) }
                    }

                    is AnglesUpdateEvent -> {
                        event.raw?.optString("feedback")?.takeIf { it.isNotBlank() }?.let { setFeedback(it) }
                    }

                    is ExerciseProgressEvent -> {
                        val active = event.raw?.optBoolean("isActive")
                        if (active != null) {
                            syncState { it.copy(isExerciseActive = active) }
                        }
                        event.raw?.optString("message")?.takeIf { it.isNotBlank() }?.let { setFeedback(it) }
                    }

                    is PostShotFeedbackEvent -> {
                        event.text?.takeIf { it.isNotBlank() }?.let { setFeedback(it) }
                        event.audioBase64?.takeIf { it.isNotBlank() }?.let { audioPlayer.playBase64Audio(it) }
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
}
