package com.example.hoopmaster.viewmodels

import android.app.Application
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.hoopmaster.network.SessionManager
import com.example.hoopmaster.network.WebSocketManager
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class TrackingViewModel(application: Application) : AndroidViewModel(application) {

    // Các State để UI (Compose) tự động cập nhật
    val poseResult = mutableStateOf<PoseLandmarkerResult?>(null)
    val feedbackText = mutableStateOf("Đang kết nối Server...")
    val selectedTone = mutableStateOf("neutral")

    private val sessionManager = SessionManager(application)
    private var webSocketManager: WebSocketManager? = null

    // Thêm MediaPlayer để phát âm thanh phản hồi từ AI
    private var mediaPlayer: MediaPlayer? = null

    // Bộ kiểm soát tần suất gửi dữ liệu (Throttle)
    private var lastSendTime = 0L
    private val SEND_INTERVAL = 100L // Gửi tối đa 10 khung hình/giây để tránh nghẽn mạng

    init {
        // Khởi tạo Socket.IO Manager
        webSocketManager = WebSocketManager(
            onConnected = { message ->
                feedbackText.value = message
            },
            onFeedbackReceived = { text, audioBase64 ->
                // 1. Hiện chữ lên màn hình
                if (text.isNotEmpty()) {
                    feedbackText.value = text
                }
                // 2. Phát âm thanh nếu có chuỗi Base64
                if (!audioBase64.isNullOrEmpty()) {
                    playAudioFromBase64(audioBase64)
                }
            }
        )

        // Lấy User ID đã đăng nhập và bắt đầu kết nối
        webSocketManager?.connect(sessionManager.getUserId())
    }

    // Hàm chuyển đổi và phát âm thanh từ chuỗi Base64
    private fun playAudioFromBase64(base64String: String) {
        try {
            val cleanBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }

            Log.d("AudioPlayer", "Đã nhận chuỗi Base64 có độ dài: ${cleanBase64.length}")

            // Nếu chuỗi quá ngắn, chắc chắn không phải file Audio thật
            if (cleanBase64.length < 100) {
                Log.e("AudioPlayer", "Chuỗi Base64 quá ngắn, Backend chưa tạo được âm thanh!")
                return
            }

            val audioBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            Log.d("AudioPlayer", "Đã dịch thành mảng byte có kích thước: ${audioBytes.size} bytes")

            val tempFile = File.createTempFile("ai_coach_feedback", ".mp3", getApplication<Application>().cacheDir)
            tempFile.deleteOnExit()

            val fos = FileOutputStream(tempFile)
            fos.write(audioBytes)
            fos.close()

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setOnErrorListener { mp, what, extra ->
                    Log.e("AudioPlayer", "MediaPlayer lỗi phát nhạc! What: $what, Extra: $extra")
                    true
                }

                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                Log.d("AudioPlayer", "Đang phát âm thanh thành công!")
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Lỗi Exception khi phát nhạc: ${e.message}")
        }
    }

    // Hàm đóng gói tọa độ khung xương MediaPipe và bắn lên Backend
    fun streamPoseToServer(result: PoseLandmarkerResult) {
        val currentTime = System.currentTimeMillis()

        // 1. Kiểm soát tần suất gửi
        if (currentTime - lastSendTime < SEND_INTERVAL) return
        lastSendTime = currentTime

        if (result.landmarks().isEmpty()) return

        val landmarks = result.landmarks()[0]
        val jsonArray = JSONArray()

        // Lặp qua 33 điểm khớp trên cơ thể
        landmarks.forEachIndexed { index, landmark ->
            val point = JSONObject().apply {
                put("id", index)
                put("x", landmark.x())
                put("y", landmark.y())
                put("z", landmark.z())
                put("visibility", landmark.visibility().orElse(0f)) // Đã đổi thành visibility
            }
            jsonArray.put(point)
        }

        // 2. TẠO PAYLOAD PHẲNG (Khớp 100% với Backend)
        val payload = JSONObject().apply {
            put("landmarks", jsonArray)        // Backend gọi: const { landmarks } = poseData;
            put("exerciseType", "shooting")    // Backend gọi: const { exerciseType } = poseData;
            put("tone", selectedTone.value)
            put("timestamp", currentTime)
            put("action", "pose_data")
        }

        // Gọi Manager để đẩy data qua mạng
        webSocketManager?.sendPoseData(payload)
    }

    // Hàm cập nhật thái độ (tone) của AI Coach khi người dùng chọn trên màn hình
    fun updateTone(tone: String) {
        selectedTone.value = when (tone) {
            "neutral", "cheerful", "strict" -> tone
            else -> "neutral"
        }
    }

    // Hàm dọn dẹp bộ nhớ khi tắt app hoặc chuyển màn hình
    override fun onCleared() {
        super.onCleared()
        webSocketManager?.disconnect()
        // Dọn dẹp MediaPlayer để không bị rò rỉ bộ nhớ (memory leak)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
