package com.example.hoopmaster.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import java.io.File
import java.io.FileOutputStream

class AudioPlayerHelper(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun playBase64Audio(base64String: String) {
        try {
            // 1. Giải mã chuỗi Base64 thành mảng byte âm thanh
            val audioBytes = Base64.decode(base64String, Base64.DEFAULT)

            // 2. Tạo một file tạm thời trong bộ nhớ cache của điện thoại
            val tempAudioFile = File.createTempFile("ai_feedback", ".mp3", context.cacheDir)
            tempAudioFile.deleteOnExit()

            // 3. Ghi mảng byte vào file tạm
            val fos = FileOutputStream(tempAudioFile)
            fos.write(audioBytes)
            fos.close()

            // 4. Phát âm thanh bằng MediaPlayer
            mediaPlayer?.release() // Dọn dẹp âm thanh cũ nếu đang phát dở
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempAudioFile.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}