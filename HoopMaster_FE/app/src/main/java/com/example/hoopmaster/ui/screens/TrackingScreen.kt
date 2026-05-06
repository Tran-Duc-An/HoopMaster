package com.example.hoopmaster.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoopmaster.utils.PoseAnalyzer
import com.example.hoopmaster.viewmodels.TrackingViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    onEndSession: () -> Unit,
    viewModel: TrackingViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Trạng thái hiển thị
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var showSkeleton by remember { mutableStateOf(true) }
    var poseInitError by remember { mutableStateOf<String?>(null) }

    // Quản lý quyền Camera
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasCameraPermission = isGranted }
    )
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 👉 CHÌA KHÓA SỬA LỖI: Tạo PreviewView 1 lần duy nhất
    val previewView = remember { PreviewView(context) }

    // 👉 CHÌA KHÓA SỬA LỖI: Dùng LaunchedEffect theo dõi sự thay đổi của lensFacing
    LaunchedEffect(lensFacing, hasCameraPermission) {
        if (hasCameraPermission) {
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(executor, PoseAnalyzer(
                        context,
                        onPoseDetected = { result, _, _ ->
                            viewModel.poseResult.value = result
                            if (result != null) {
                                viewModel.streamPoseToServer(result)
                            }
                        },
                        onError = { message ->
                            poseInitError = message
                        }
                    ))

                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                // Hủy camera cũ và gắn camera mới ngay lập tức
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("TrackingScreen", "Lỗi xoay camera", e)
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- PHẦN 1: CAMERA & SKELETON OVERLAY ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission) {
                    // 1.1 Layer Camera Preview (Rất gọn gàng vì logic đã đưa lên trên)
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { previewView }
                    )

                    // 1.2 Layer vẽ Khung xương
                    val poseResult = viewModel.poseResult.value
                    if (showSkeleton && poseResult != null && poseResult.landmarks().isNotEmpty()) {
                        val skeletonLineColor = MaterialTheme.colorScheme.tertiary
                        val skeletonPointColor = MaterialTheme.colorScheme.secondaryContainer
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val landmarks = poseResult.landmarks()[0]
                            val connections = listOf(
                                Pair(11, 12), Pair(11, 13), Pair(13, 15),
                                Pair(12, 14), Pair(14, 16),
                                Pair(11, 23), Pair(12, 24), Pair(23, 24),
                                Pair(23, 25), Pair(25, 27),
                                Pair(24, 26), Pair(26, 28)
                            )
                            connections.forEach { (start, end) ->
                                val startPoint = landmarks[start]
                                val endPoint = landmarks[end]
                                if (startPoint.visibility().orElse(0f) > 0.5f && endPoint.visibility().orElse(0f) > 0.5f) {
                                    drawLine(
                                        color = skeletonLineColor,
                                        start = Offset(startPoint.x() * size.width, startPoint.y() * size.height),
                                        end = Offset(endPoint.x() * size.width, endPoint.y() * size.height),
                                        strokeWidth = 5f
                                    )
                                }
                            }
                            landmarks.forEach { landmark ->
                                if (landmark.visibility().orElse(0f) > 0.5f) {
                                    drawCircle(
                                        color = skeletonPointColor,
                                        radius = 8f,
                                        center = Offset(landmark.x() * size.width, landmark.y() * size.height)
                                    )
                                }
                            }
                        }
                    }

                    // 1.3 Nút Điều Khiển
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Nút Lật Camera
                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            },
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Lật Camera", tint = Color.White)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Nút Bật/Tắt Khung Xương
                        IconButton(
                            onClick = { showSkeleton = !showSkeleton },
                            modifier = Modifier
                                .background(
                                    if (showSkeleton) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (showSkeleton) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Bật tắt khung xương",
                                tint = Color.White
                            )
                        }
                    }

                    if (poseInitError != null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = poseInitError ?: "",
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Text(
                        "Vui lòng cấp quyền Camera",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 1.4 Floating AI Feedback
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = viewModel.feedbackText.value,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // --- PHẦN 2: BẢNG ĐIỀU KHIỂN (CONTROL PANEL) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "AI COACH TONE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val tones = listOf("neutral", "strict", "cheerful")
                        tones.forEach { tone ->
                            FilterChip(
                                selected = viewModel.selectedTone.value == tone,
                                onClick = { viewModel.updateTone(tone) },
                                label = { Text(tone.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onEndSession,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("END SESSION", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}