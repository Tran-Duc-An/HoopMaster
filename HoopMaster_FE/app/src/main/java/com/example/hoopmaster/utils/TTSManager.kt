package com.example.hoopmaster.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TTSManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        // Khởi tạo engine giọng nói của hệ điều hành
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Setup ngôn ngữ. Nếu backend trả tiếng Việt, bạn có thể thử Locale("vi", "VN")
            // Ở đây mình set mặc định tiếng Anh Mỹ (US) cho chuẩn giọng bóng rổ
            val result = tts?.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                isInitialized = false
            } else {
                isInitialized = true
            }
        }
    }

    // Hàm gọi để phát âm thanh
    fun speak(text: String) {
        if (isInitialized) {
            // QUEUE_FLUSH: Đang nói câu cũ mà có câu mới sẽ ngắt luôn câu cũ để nói câu mới
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // Nhớ tắt khi không dùng để giải phóng bộ nhớ
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}