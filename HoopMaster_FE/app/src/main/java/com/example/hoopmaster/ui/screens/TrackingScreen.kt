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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoopmaster.R
import com.example.hoopmaster.utils.PoseAnalyzer
import com.example.hoopmaster.viewmodels.TrackingUiState
import com.example.hoopmaster.viewmodels.TrackingViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    onEndSession: () -> Unit,
    demoState: TrackingUiState? = null,
    demoHasCameraPermission: Boolean = true,
    viewModel: TrackingViewModel? = null
) {
    val isDemo = demoState != null
    val liveViewModel = viewModel ?: if (!isDemo) viewModel<TrackingViewModel>() else null
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val uiStateValue = demoState ?: requireNotNull(liveViewModel).uiState.value

    // Trạng thái hiển thị
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var showSkeleton by remember { mutableStateOf(true) }
    var poseInitError by remember { mutableStateOf<String?>(null) }
    var demoTone by remember(demoState?.selectedTone) { mutableStateOf(demoState?.selectedTone ?: "neutral") }

    // Quản lý quyền Camera
    var hasCameraPermission by remember {
        mutableStateOf(
            if (isDemo) {
                demoHasCameraPermission
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasCameraPermission = isGranted }
    )
    LaunchedEffect(Unit) {
        if (!isDemo && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 👉 CHÌA KHÓA SỬA LỖI: Tạo PreviewView 1 lần duy nhất
    val previewView = remember { PreviewView(context) }

    // 👉 CHÌA KHÓA SỬA LỖI: Dùng LaunchedEffect theo dõi sự thay đổi của lensFacing
    LaunchedEffect(lensFacing, hasCameraPermission) {
        if (!isDemo && hasCameraPermission) {
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
                            requireNotNull(liveViewModel).poseResult.value = result
                            requireNotNull(liveViewModel).streamPoseToServer(result)
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

    val accuracyValue = 0.78f
    val releaseValue = ".65s"
    val arcValue = "45°"
    val madeShots = 12
    val totalShots = 15
    val streakValue = 3
    val analysisTitle = "Last Shot Analysis"
    val analysisMessage = if (uiStateValue.feedbackText.isNotBlank()) {
        uiStateValue.feedbackText
    } else {
        "Perfect Arc!"
    }

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
                val poseResult = liveViewModel?.poseResult?.value
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

            AccuracyMeter(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp),
                progress = accuracyValue,
                label = "Accuracy"
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickStatCard(
                    icon = Icons.Outlined.Speed,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    value = releaseValue,
                    label = "Release"
                )
                QuickStatCard(
                    icon = Icons.Outlined.Architecture,
                    iconTint = MaterialTheme.colorScheme.primaryContainer,
                    value = arcValue,
                    label = "Arc"
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, end = 20.dp, bottom = 156.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Shot mode",
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("strict", "neutral", "cheerful").forEach { tone ->
                            FilterChip(
                                selected = if (isDemo) demoTone == tone else liveViewModel?.selectedTone?.value == tone,
                                onClick = {
                                    if (isDemo) {
                                        demoTone = tone
                                    } else {
                                        requireNotNull(liveViewModel).updateTone(tone)
                                    }
                                },
                                label = { Text(tone.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            if (!isDemo) {
                                requireNotNull(liveViewModel).onShotReleased()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Shot released")
                    }
                }
            }

            LastShotAnalysisCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                title = analysisTitle,
                message = analysisMessage,
                made = madeShots,
                total = totalShots,
                streak = streakValue
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
private fun AccuracyMeter(
    modifier: Modifier,
    progress: Float,
    label: String
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(220.dp)
                .width(44.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                .padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(progress)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 38.dp)
                    .background(Color.White.copy(alpha = 0.8f))
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${(progress * 100).toInt()}%",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondaryContainer
        )
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LastShotAnalysisCard(
    modifier: Modifier,
    title: String,
    message: String,
    made: Int,
    total: Int,
    streak: Int
) {
    Surface(
        modifier = modifier
            .navigationBarsPadding(),
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
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title.uppercase(),
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = message,
                        fontSize = 28.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(
                        3.dp,
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                Modifier,
                DividerDefaults.Thickness,
                color = Color.White.copy(alpha = 0.12f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatColumn(label = "Made", value = made.toString())
                StatColumn(label = "Total", value = total.toString())
                StatColumn(
                    label = "Streak",
                    value = streak.toString(),
                    valueColor = MaterialTheme.colorScheme.tertiary,
                    alignEnd = true
                )
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    alignEnd: Boolean = false
) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
        )
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun CameraFallbackBackground() {
    val logoId = R.drawable.hoopmaster_logo
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = logoId),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            alpha = 0.18f
        )
        Text(
            text = "Camera permission needed",
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center),
            fontWeight = FontWeight.Medium
        )
    }
}
