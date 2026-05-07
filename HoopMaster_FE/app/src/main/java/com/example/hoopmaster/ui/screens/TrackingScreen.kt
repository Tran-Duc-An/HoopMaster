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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
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
                            viewModel.streamPoseToServer(result)
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

    val feedbackMessage = if (viewModel.feedbackText.value.isNotBlank()) {
        viewModel.feedbackText.value
    } else {
        "Waiting for coach feedback..."
    }
    val selectedTone = viewModel.selectedTone.value

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { previewView }
                )
            } else {
                CameraFallbackBackground()
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )

            if (hasCameraPermission) {
                val poseResult = viewModel.poseResult.value
                if (showSkeleton && poseResult != null && poseResult.landmarks().isNotEmpty()) {
                    val skeletonLineColor = MaterialTheme.colorScheme.tertiary
                    val skeletonPointColor = MaterialTheme.colorScheme.tertiary
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
                                    strokeWidth = 4f
                                )
                            }
                        }
                        landmarks.forEach { landmark ->
                            if (landmark.visibility().orElse(0f) > 0.5f) {
                                drawCircle(
                                    color = skeletonPointColor,
                                    radius = 7f,
                                    center = Offset(landmark.x() * size.width, landmark.y() * size.height)
                                )
                            }
                        }
                    }
                }
            }

            TopTrackingBar(
                onClose = onEndSession,
                onFlipCamera = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
                timerText = "00:59"
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 96.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlassIconButton(
                    onClick = { showSkeleton = !showSkeleton },
                    icon = if (showSkeleton) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle skeleton",
                    containerColor = if (showSkeleton) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    }
                )
            }

            CoachFeedbackPanel(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                selectedTone = selectedTone,
                feedbackMessage = feedbackMessage,
                onToneSelected = viewModel::updateTone
            )

            if (poseInitError != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 88.dp)
                        .padding(horizontal = 16.dp),
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
        }
    }
}

@Composable
private fun TopTrackingBar(
    onClose: () -> Unit,
    onFlipCamera: () -> Unit,
    timerText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        GlassIconButton(
            onClick = onClose,
            icon = Icons.Outlined.Close,
            contentDescription = "Close session"
        )

        RecordingPill(timerText = timerText)

        GlassIconButton(
            onClick = onFlipCamera,
            icon = Icons.Default.FlipCameraAndroid,
            contentDescription = "Flip camera"
        )
    }
}

@Composable
private fun RecordingPill(timerText: String) {
    val pillShape = RoundedCornerShape(999.dp)
    Row(
        modifier = Modifier
            .clip(pillShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f), pillShape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "REC",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = timerText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun GlassIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    containerColor: Color = Color.White.copy(alpha = 0.08f),
    size: Dp = 48.dp
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(size)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CoachFeedbackPanel(
    modifier: Modifier,
    selectedTone: String,
    feedbackMessage: String,
    onToneSelected: (String) -> Unit
) {
    Surface(
        modifier = modifier.navigationBarsPadding(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "COACH TONE",
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToneChip(
                    modifier = Modifier.weight(1f),
                    label = "Neutral",
                    tone = "neutral",
                    selectedTone = selectedTone,
                    onToneSelected = onToneSelected
                )
                ToneChip(
                    modifier = Modifier.weight(1f),
                    label = "Cheerful",
                    tone = "cheerful",
                    selectedTone = selectedTone,
                    onToneSelected = onToneSelected
                )
                ToneChip(
                    modifier = Modifier.weight(1f),
                    label = "Strict",
                    tone = "strict",
                    selectedTone = selectedTone,
                    onToneSelected = onToneSelected
                )
            }
            Text(
                text = "LIVE FEEDBACK",
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = feedbackMessage,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

@Composable
private fun ToneChip(
    modifier: Modifier,
    label: String,
    tone: String,
    selectedTone: String,
    onToneSelected: (String) -> Unit
) {
    val isSelected = selectedTone == tone
    val shape = RoundedCornerShape(999.dp)
    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .clickable { onToneSelected(tone) },
        shape = shape,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        } else {
            Color.White.copy(alpha = 0.08f)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = 0.12f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun CameraFallbackBackground() {
    val context = LocalContext.current
    val logoId = remember(context) {
        context.resources.getIdentifier("hoopmaster_logo", "drawable", context.packageName)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (logoId != 0) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = logoId),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                alpha = 0.18f
            )
        }
        Text(
            text = "Camera permission needed",
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center),
            fontWeight = FontWeight.Medium
        )
    }
}
