package com.example.hoopmaster.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class PoseAnalyzer(
    context: Context,
    private val onPoseDetected: (PoseLandmarkerResult, Int, Int) -> Unit,
    private val onError: (String) -> Unit = {}
) : ImageAnalysis.Analyzer {

    private var poseLandmarker: PoseLandmarker? = null

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_full.task")
            .build()
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ ->
                // Trả kết quả về UI (Result, chiều rộng ảnh, chiều cao ảnh)
                onPoseDetected(result, 480, 640) // Kích thước mặc định của ImageAnalysis
            }
            .setErrorListener { error -> error.printStackTrace() }
            .build()

        try {
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
        } catch (e: UnsatisfiedLinkError) {
            val message = "MediaPipe native lib missing for ABI(s): ${Build.SUPPORTED_ABIS.joinToString()}"
            Log.e("PoseAnalyzer", message, e)
            onError(message)
        } catch (e: Exception) {
            val message = "Failed to init PoseLandmarker: ${e.message}"
            Log.e("PoseAnalyzer", message, e)
            onError(message)
        }
    }

    override fun analyze(image: ImageProxy) {
        if (poseLandmarker == null) {
            image.close()
            return
        }
        val bitmap = image.toBitmap()
        // Xoay ảnh cho đúng chiều camera
        val matrix = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        val timestampMs = image.imageInfo.timestamp / 1000000

        poseLandmarker?.detectAsync(mpImage, timestampMs)
        image.close()
    }
}