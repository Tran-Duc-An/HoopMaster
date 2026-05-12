package com.example.hoopmaster.network

import com.example.hoopmaster.core.config.AppConfig
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class WebSocketManager(
    private val onConnected: (String) -> Unit,
    private val onFeedbackReceived: (String, String?) -> Unit
) {
    private var mSocket: Socket? = null

    // Sử dụng cấu hình chung từ AppConfig
    private val SOCKET_URL = AppConfig.SOCKET_URL

    fun connect(userId: String?) {
        if (mSocket?.connected() == true) return

        try {
            val options = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = 10
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                timeout = 20000
                transports = arrayOf("websocket") // Chỉ dùng websocket, tránh xhr poll error
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

            // --- 2. Lắng nghe Feedback ---
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

    fun sendPoseData(payload: JSONObject) {
        if (mSocket?.connected() == true) {
            mSocket?.emit("pose_data", payload)
        } else {
            Log.v("SocketIO_OUT", "⚠️ Chờ kết nối để gửi dữ liệu...")
        }
    }

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
