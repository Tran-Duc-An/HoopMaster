package com.example.hoopmaster.network

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class WebSocketManager(
    private val onConnected: (String) -> Unit,
    private val onFeedbackReceived: (String, String?) -> Unit
) {
    private var mSocket: Socket? = null

    // Đảm bảo IP này đúng với IP máy tính chạy Node.js
    private val SOCKET_URL = "http://10.122.3.182:3000"

    fun connect(userId: String?) {
        if (mSocket?.connected() == true) return

        try {
            val options = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 2000
                // Backend của bạn không thấy dùng userId ở handshake,
                // nhưng cứ giữ lại nếu sau này bạn cần xác thực.
                if (userId != null) {
                    query = "userId=$userId"
                }
            }

            mSocket = IO.socket(SOCKET_URL, options)

            // --- 1. Lắng nghe sự kiện kết nối thành công ---
            mSocket?.on("connected") { args ->
                if (args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    val msg = data.optString("message", "Đã kết nối!")
                    onConnected(msg)
                    Log.i("SocketIO", "✅ Server chào: $msg")
                }
            }

            // --- 2. Lắng nghe Feedback (Welcome, Instruction, và Live Feedback) ---
            // Backend của bạn emit duy nhất sự kiện 'audio_feedback'
            mSocket?.on("audio_feedback") { args ->
                try {
                    if (args.isNotEmpty()) {
                        val data = args[0] as JSONObject
                        Log.d("SocketIO_IN", "⬇️ Nhận Feedback: $data")

                        val text = data.optString("text", "")
                        val audio = data.optString("audioBase64", "")

                        onFeedbackReceived(text, audio)
                    }
                } catch (e: Exception) {
                    Log.e("SocketIO_IN", "❌ Lỗi parse dữ liệu: ${e.message}")
                }
            }

            mSocket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("SocketIO", "❌ Lỗi kết nối Socket: ${args.getOrNull(0)}")
            }

            mSocket?.on(Socket.EVENT_DISCONNECT) {
                Log.w("SocketIO", "⚠️ Đã ngắt kết nối")
            }

            mSocket?.connect()
        } catch (e: Exception) {
            Log.e("SocketIO", "❌ Lỗi khởi tạo: ${e.message}")
        }
    }

    /**
     * Gửi dữ liệu Pose lên Backend
     */
    fun sendPoseData(payload: JSONObject) {
        if (mSocket?.connected() == true) {
            // ĐÃ SỬA: Dùng đúng tên 'pose_data' như trong code Node.js của bạn
            mSocket?.emit("pose_data", payload)
        } else {
            // Nếu log này hiện ra, hãy kiểm tra lại Firewall trên máy tính
            Log.v("SocketIO_OUT", "⚠️ Chờ kết nối để gửi dữ liệu...")
        }
    }

    /**
     * Thông báo cho Backend là bóng đã được ném
     */
    fun sendShotReleased() {
        if (mSocket?.connected() == true) {
            mSocket?.emit("shot_released")
            Log.i("SocketIO_OUT", "🏀 Đã gửi sự kiện: shot_released")
        }
    }

    fun disconnect() {
        mSocket?.off()
        mSocket?.disconnect()
        mSocket = null
    }
}