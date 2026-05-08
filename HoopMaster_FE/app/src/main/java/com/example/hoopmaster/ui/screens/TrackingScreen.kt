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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoopmaster.R
import com.example.hoopmaster.utils.PoseAnalyzer
import com.example.hoopmaster.ui.theme.ActiveOrange
import com.example.hoopmaster.ui.theme.HoopRadius
import com.example.hoopmaster.ui.theme.HoopSpacing
import com.example.hoopmaster.ui.theme.NavyShadow
import com.example.hoopmaster.ui.responsive.rememberHoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.rememberHoopWindowInfo
import com.example.hoopmaster.viewmodels.TrackingViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    exerciseId: Int? = null,
    onEndSession: (socketId: String?) -> Unit,
    viewModel: TrackingViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val windowInfo = rememberHoopWindowInfo()
    val tokens = rememberHoopResponsiveTokens(windowInfo)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val uiStateValue by viewModel.uiState.collectAsState()
    val compactOverlay = windowInfo.isLandscape || windowInfo.isSmallHeight
    val tightLandscape = windowInfo.isLandscape && windowInfo.isSmallHeight
    val overlayMargin = tokens.spacing.screenMargin
    val shotModeBottomPadding = if (compactOverlay) {
        overlayMargin
    } else {
        156.dp
    }
    val analysisOnTop = tightLandscape

    // Trạng thái hiển thị
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var showSkeleton by remember { mutableStateOf(true) }
    var poseInitError by remember { mutableStateOf<String?>(null) }
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
    LaunchedEffect(exerciseId) {
        if (exerciseId != null) {
            viewModel.startExercise(exerciseId = exerciseId)
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
                            viewModel.streamPoseToServer(
                                result,
                                mirrorX = lensFacing == CameraSelector.LENS_FACING_FRONT
                            )
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

    val shotCount = uiStateValue.shotCount
    val analysisTitle = "Last Shot Analysis"
    val analysisMessage = resolveAnalysisMessage(uiStateValue)

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
                    factory = { previewView },
                    update = {
                        it.scaleX = if (lensFacing == CameraSelector.LENS_FACING_FRONT) -1f else 1f
                    }
                )
            } else {
                CameraFallbackBackground()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )

            if (hasCameraPermission) {
                val poseResult = viewModel.poseResult.value
                if (showSkeleton && poseResult != null && poseResult.landmarks().isNotEmpty()) {
                    val skeletonLineColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.92f)
                    val skeletonPointColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.96f)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val landmarks = poseResult.landmarks()[0]
                        fun displayX(x: Float): Float {
                            return if (lensFacing == CameraSelector.LENS_FACING_FRONT) 1f - x else x
                        }
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
                                    start = Offset(displayX(startPoint.x()) * size.width, startPoint.y() * size.height),
                                    end = Offset(displayX(endPoint.x()) * size.width, endPoint.y() * size.height),
                                    strokeWidth = 4f
                                )
                            }
                        }
                        landmarks.forEach { landmark ->
                            if (landmark.visibility().orElse(0f) > 0.5f) {
                                drawCircle(
                                    color = skeletonPointColor,
                                    radius = 7f,
                                    center = Offset(displayX(landmark.x()) * size.width, landmark.y() * size.height)
                                )
                            }
                        }
                    }
                }
            }

            TopTrackingBar(
                onClose = {
                    viewModel.requestSessionInfo()
                    onEndSession(uiStateValue.socketId)
                },
                onFlipCamera = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
                shotCount = shotCount,
                iconSize = tokens.sizing.iconButtonSize
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = if (compactOverlay) 72.dp else 92.dp,
                        end = overlayMargin
                    ),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(HoopSpacing.Xs)
            ) {
                GlassIconButton(
                    onClick = { showSkeleton = !showSkeleton },
                    icon = if (showSkeleton) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle skeleton",
                    containerColor = if (showSkeleton) {
                        ActiveOrange.copy(alpha = 0.2f)
                    } else {
                        NavyShadow.copy(alpha = 0.62f)
                    },
                    size = tokens.sizing.iconButtonSize
                )
                Column(
                    modifier = Modifier
                        .widthIn(max = if (compactOverlay) 188.dp else 220.dp)
                        .padding(horizontal = HoopSpacing.Sm, vertical = HoopSpacing.Xs),
                    verticalArrangement = Arrangement.spacedBy(HoopSpacing.Xs)
                ) {
                    Text(
                        text = "Tone",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(HoopSpacing.Xs)) {
                        listOf("strict", "neutral", "cheerful").forEach { tone ->
                            FilterChip(
                                selected = viewModel.selectedTone.value == tone,
                                onClick = {
                                    viewModel.updateTone(tone)
                                },
                                label = {
                                    Text(
                                        text = tone.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ActiveOrange.copy(alpha = 0.18f),
                                    selectedLabelColor = NavyShadow,
                                    selectedLeadingIconColor = NavyShadow,
                                    containerColor = Color.White.copy(alpha = 0.04f),
                                    labelColor = Color.White.copy(alpha = 0.82f),
                                    iconColor = Color.White.copy(alpha = 0.82f)
                                ),
                                modifier = Modifier.heightIn(min = 30.dp)
                            )
                        }
                    }
                }
            }

            LastShotAnalysisCard(
                modifier = Modifier
                    .align(if (analysisOnTop) Alignment.TopCenter else Alignment.BottomCenter)
                    .padding(
                        horizontal = overlayMargin,
                        vertical = if (analysisOnTop) 82.dp else tokens.spacing.contentGap
                    ),
                title = analysisTitle,
                message = analysisMessage,
                liveAnglesText = formatLiveAngles(uiStateValue.liveAngles),
                shotStatsText = formatShotStats(uiStateValue.lastShotStats),
                compact = compactOverlay,
                applyNavigationPadding = !analysisOnTop
            )

            if (poseInitError != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = if (compactOverlay) 132.dp else 144.dp)
                        .padding(horizontal = HoopSpacing.ScreenMargin),
                    color = NavyShadow.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(HoopRadius.Md),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Text(
                        text = poseInitError ?: "",
                        color = Color.White,
                        modifier = Modifier.padding(HoopSpacing.Md),
                        style = MaterialTheme.typography.labelMedium
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
    shotCount: Int,
    iconSize: Dp
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = HoopSpacing.ScreenMargin, vertical = HoopSpacing.Sm),
        color = NavyShadow.copy(alpha = 0.62f),
        shape = RoundedCornerShape(HoopRadius.Full),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HoopSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GlassIconButton(
                onClick = onClose,
                icon = Icons.Outlined.Close,
                contentDescription = "Close session",
                size = iconSize
            )

            CountPill(shotCount = shotCount)

            GlassIconButton(
                onClick = onFlipCamera,
                icon = Icons.Default.FlipCameraAndroid,
                contentDescription = "Flip camera",
                size = iconSize
            )
        }
    }
}

@Composable
private fun CountPill(shotCount: Int) {
    val pillShape = RoundedCornerShape(HoopRadius.Full)
    Row(
        modifier = Modifier
            .clip(pillShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, ActiveOrange.copy(alpha = 0.35f), pillShape)
            .padding(horizontal = HoopSpacing.Md, vertical = HoopSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Count",
            fontWeight = FontWeight.Bold,
            color = ActiveOrange,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.width(HoopSpacing.Md))
        Text(
            text = shotCount.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}

@Composable
private fun GlassIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    containerColor: Color = NavyShadow.copy(alpha = 0.64f),
    size: Dp = 48.dp
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(size),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = Color.White.copy(alpha = 0.38f)
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White
            )
        }
    }
}

@Composable
private fun LastShotAnalysisCard(
    modifier: Modifier,
    title: String,
    message: String,
    liveAnglesText: String?,
    shotStatsText: String?,
    compact: Boolean,
    applyNavigationPadding: Boolean
) {
    Surface(
        modifier = modifier
            .then(if (applyNavigationPadding) Modifier.navigationBarsPadding() else Modifier),
        shape = RoundedCornerShape(HoopRadius.Lg),
        color = NavyShadow.copy(alpha = 0.72f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.14f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) HoopSpacing.Sm else HoopSpacing.Md)
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
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                    Text(
                        text = message,
                        style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        color = ActiveOrange
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = ActiveOrange.copy(alpha = 0.14f),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        ActiveOrange.copy(alpha = 0.68f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (compact) 44.dp else 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = ActiveOrange,
                            modifier = Modifier.size(if (compact) 22.dp else 28.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(HoopSpacing.Sm))
            HorizontalDivider(
                Modifier,
                DividerDefaults.Thickness,
                color = Color.White.copy(alpha = 0.12f)
            )
            Spacer(modifier = Modifier.height(HoopSpacing.Sm))
            if (liveAnglesText != null) {
                Text(
                    text = liveAnglesText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.86f)
                )
            }
            if (shotStatsText != null) {
                if (liveAnglesText != null) {
                    Spacer(modifier = Modifier.height(HoopSpacing.Xs))
                }
                Text(
                    text = shotStatsText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.86f)
                )
            }
        }
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

private fun resolveAnalysisMessage(uiState: com.example.hoopmaster.viewmodels.TrackingUiState): String {
    return uiState.feedbackText.takeIf { it.isNotBlank() }
        ?: "Waiting for shot feedback"
}

private fun formatLiveAngles(liveAngles: com.example.hoopmaster.data.model.LiveAnglesDto?): String? {
    if (liveAngles == null) return null
    val parts = listOfNotNull(
        liveAngles.elbowAngle.toAnglePart("Elbow"),
        liveAngles.shoulderAngle.toAnglePart("Shoulder"),
        liveAngles.kneeAngle.toAnglePart("Knee"),
        liveAngles.backAngle.toAnglePart("Back")
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
}

private fun formatShotStats(stats: com.example.hoopmaster.data.model.ShotStatsDto?): String? {
    if (stats == null) return null
    val parts = listOfNotNull(
        stats.avgElbowAngle.toAnglePart("Elbow"),
        stats.avgShoulderAngle.toAnglePart("Shoulder"),
        stats.avgKneeAngle.toAnglePart("Knee"),
        stats.avgBackAngle.toAnglePart("Back"),
        stats.frameCount.toFramesPart("Frames")
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
}

private fun Double?.toAnglePart(label: String): String? {
    if (this == null || this.isNaN()) return null
    return "$label ${String.format("%.1f°", this)}"
}

private fun Int?.toFramesPart(label: String): String? {
    if (this == null || this <= 0) return null
    return "$label $this"
}
