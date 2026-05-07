package com.example.hoopmaster.media

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun playBase64Audio(base64String: String) {
        val trimmed = base64String.trim()
        val cleanBase64 = if (trimmed.startsWith("data:")) {
            trimmed.substringAfter(",")
        } else {
            trimmed
        }

        if (cleanBase64.isBlank() || cleanBase64.length < 100) {
            Log.e(TAG, "skip short audio")
            return
        }

        runCatching {
            val audioBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val tempAudioFile = File.createTempFile("hoopmaster_audio_", ".mp3", context.cacheDir)
            FileOutputStream(tempAudioFile).use { it.write(audioBytes) }

            stop()

            mediaPlayer = MediaPlayer().apply {
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "player err $what/$extra")
                    true
                }
                setOnCompletionListener {
                    release()
                }
                setDataSource(tempAudioFile.absolutePath)
                prepare()
                start()
            }
        }.onFailure { error ->
            Log.e(TAG, "play fail: ${error.message}")
        }
    }

    fun stop() {
        runCatching {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
            }
        }.onFailure {
            Log.e(TAG, "stop fail: ${it.message}")
        }
        release()
    }

    fun release() {
        try {
            mediaPlayer?.release()
        } catch (error: Exception) {
            Log.e(TAG, "release fail: ${error.message}")
        } finally {
            mediaPlayer = null
        }
    }

    private companion object {
        const val TAG = "AudioPlayer"
    }
}
