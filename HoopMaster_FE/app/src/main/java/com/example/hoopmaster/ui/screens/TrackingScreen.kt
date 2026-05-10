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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoopmaster.data.model.LiveAnglesDto
import com.example.hoopmaster.data.model.ShotStatsDto
import com.example.hoopmaster.utils.PoseAnalyzer
import com.example.hoopmaster.viewmodels.TrackingUiState
import com.example.hoopmaster.viewmodels.TrackingViewModel
import java.util.concurrent.Executors

private val AthleticBackground = Color(0xFF1E100C)
private val SurfaceLowest = Color(0xFF180B07)
private val Surface = Color(0xFF2C1C17)
private val SurfaceHigh = Color(0xFF372621)
private val SurfaceHighest = Color(0xFF43302B)
private val Primary = Color(0xFFFFB5A0)
private val PrimaryContainer = Color(0xFFFF5722)
private val Secondary = Color(0xFF78DC77)
private val OnSurface = Color(0xFFFADCD4)
private val OnSurfaceVariant = Color(0xFFE4BEB4)
private val Outline = Color(0xFFAB8980)
private val OutlineVariant = Color(0xFF5B4039)
private val ErrorContainer = Color(0xFF93000A)
private val OnErrorContainer = Color(0xFFFFDAD6)
private val OnPrimaryContainer = Color(0xFF541200)

@Composable
fun TrackingScreen(
    exerciseId: Int? = null,
    onEndSession: (socketId: String?) -> Unit,
    viewModel: TrackingViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val uiStateValue by viewModel.uiState.collectAsState()

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

    val previewView = remember { PreviewView(context) }

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
                    it.setAnalyzer(
                        executor,
                        PoseAnalyzer(
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
                        )
                    )
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("TrackingScreen", "Failed to rotate camera", e)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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

        CameraGradeOverlay()

        if (hasCameraPermission && showSkeleton) {
            PoseSkeletonOverlay(
                uiState = uiStateValue,
                lensFacing = lensFacing
            )
        }

        TrackingFrameOverlay()

        TrackingTopHud(
            uiState = uiStateValue,
            showSkeleton = showSkeleton,
            onToggleSkeleton = { showSkeleton = !showSkeleton },
            onFlipCamera = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            },
            onEnd = {
                viewModel.requestSessionInfo()
                onEndSession(uiStateValue.socketId)
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        )

        ToneControlStrip(
            selectedTone = viewModel.selectedTone.value,
            onToneSelected = viewModel::updateTone,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 84.dp, start = 20.dp, end = 20.dp)
        )

        TelemetryHud(
            liveAngles = uiStateValue.liveAngles,
            stats = uiStateValue.lastShotStats,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp)
        )

        BottomCoachPanel(
            uiState = uiStateValue,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        )

        poseInitError?.let { error ->
            HudErrorBanner(
                message = error,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 132.dp, start = 20.dp, end = 20.dp)
            )
        }
    }
}

@Composable
private fun CameraGradeOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.22f))
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.20f),
                    Color.Transparent,
                    AthleticBackground.copy(alpha = 0.64f)
                )
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Primary.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * 0.50f, size.height * 0.10f),
                radius = size.width * 0.65f
            ),
            radius = size.width * 0.65f,
            center = Offset(size.width * 0.50f, size.height * 0.10f)
        )
    }
}

@Composable
private fun TrackingTopHud(
    uiState: TrackingUiState,
    showSkeleton: Boolean,
    onToggleSkeleton: () -> Unit,
    onFlipCamera: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        EndButton(onClick = onEnd)
        MakesPill(
            shotCount = uiState.shotCount,
            target = uiState.sessionInfo?.exercise?.targetReps ?: 30
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HudCircleButton(
                icon = Icons.Filled.FlipCameraAndroid,
                contentDescription = "Flip camera",
                onClick = onFlipCamera
            )
            HudCircleButton(
                icon = if (showSkeleton) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                contentDescription = "Toggle skeleton",
                onClick = onToggleSkeleton,
                size = 44.dp,
                active = showSkeleton
            )
        }
    }
}

@Composable
private fun EndButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ErrorContainer.copy(alpha = 0.88f))
            .border(1.dp, Color(0xFFFFB4AB).copy(alpha = 0.34f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            tint = OnErrorContainer,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = "END",
            style = technicalLabel(14),
            color = OnErrorContainer
        )
    }
}

@Composable
private fun MakesPill(
    shotCount: Int,
    target: Int
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Surface.copy(alpha = 0.84f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.8f), RoundedCornerShape(999.dp))
            .padding(horizontal = 26.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "MAKES",
            style = technicalLabel(13),
            color = Primary
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = shotCount.toString(),
                style = athleticHeadline(48, italic = true).copy(lineHeight = 48.sp),
                color = OnSurface
            )
            Text(
                text = "/$target",
                modifier = Modifier.padding(bottom = 7.dp),
                style = athleticHeadline(24, italic = true),
                color = OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun HudCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 56.dp,
    active: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (active) SurfaceHigh.copy(alpha = 0.84f) else Surface.copy(alpha = 0.78f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.82f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) Primary else OnSurface,
            modifier = Modifier.size(if (size > 48.dp) 28.dp else 22.dp)
        )
    }
}

@Composable
private fun ToneControlStrip(
    selectedTone: String,
    onToneSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(SurfaceLowest.copy(alpha = 0.70f))
                .border(1.dp, OutlineVariant.copy(alpha = 0.50f), RoundedCornerShape(999.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("strict", "neutral", "cheerful").forEach { tone ->
                ToneChip(
                    label = tone,
                    selected = selectedTone == tone,
                    onClick = { onToneSelected(tone) }
                )
            }
        }
    }
}

@Composable
private fun ToneChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Primary.copy(alpha = 0.16f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) Primary.copy(alpha = 0.55f) else Color.Transparent,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            style = technicalLabel(10),
            color = if (selected) Primary else OnSurfaceVariant
        )
    }
}

@Composable
private fun PoseSkeletonOverlay(
    uiState: TrackingUiState,
    lensFacing: Int
) {
    val poseResult = uiState.poseResult ?: return
    if (poseResult.landmarks().isEmpty()) return

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
                    color = Secondary.copy(alpha = 0.82f),
                    start = Offset(displayX(startPoint.x()) * size.width, startPoint.y() * size.height),
                    end = Offset(displayX(endPoint.x()) * size.width, endPoint.y() * size.height),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
        }
        landmarks.forEachIndexed { index, landmark ->
            if (landmark.visibility().orElse(0f) > 0.5f) {
                drawCircle(
                    color = if (index in listOf(13, 14, 15, 16)) Primary else Secondary,
                    radius = 7f,
                    center = Offset(displayX(landmark.x()) * size.width, landmark.y() * size.height)
                )
            }
        }
    }
}

@Composable
private fun TrackingFrameOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val frameWidth = size.width * 0.68f
        val frameHeight = size.height * 0.46f
        val left = (size.width - frameWidth) / 2f
        val top = size.height * 0.27f
        val right = left + frameWidth
        val bottom = top + frameHeight
        val corner = 30.dp.toPx()
        val line = 2.dp.toPx()

        drawRoundRect(
            color = Color.White.copy(alpha = 0.16f),
            topLeft = Offset(left, top),
            size = Size(frameWidth, frameHeight),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )
        listOf(
            Pair(Offset(left, top + corner), Offset(left, top)),
            Pair(Offset(left, top), Offset(left + corner, top)),
            Pair(Offset(right - corner, top), Offset(right, top)),
            Pair(Offset(right, top), Offset(right, top + corner)),
            Pair(Offset(left, bottom - corner), Offset(left, bottom)),
            Pair(Offset(left, bottom), Offset(left + corner, bottom)),
            Pair(Offset(right - corner, bottom), Offset(right, bottom)),
            Pair(Offset(right, bottom), Offset(right, bottom - corner))
        ).forEach { (start, end) ->
            drawLine(
                color = Primary,
                start = start,
                end = end,
                strokeWidth = line,
                cap = StrokeCap.Square
            )
        }
        drawCircle(
            color = Primary.copy(alpha = 0.82f),
            radius = 8.dp.toPx(),
            center = Offset(left + frameWidth * 0.32f, top + frameHeight * 0.27f)
        )
        drawCircle(
            color = Secondary.copy(alpha = 0.90f),
            radius = 8.dp.toPx(),
            center = Offset(left + frameWidth * 0.78f, top + frameHeight * 0.42f)
        )
        drawLine(
            color = Primary.copy(alpha = 0.62f),
            start = Offset(left + frameWidth * 0.32f, top + frameHeight * 0.27f),
            end = Offset(left + frameWidth * 0.48f, top + frameHeight * 0.34f),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Secondary.copy(alpha = 0.62f),
            start = Offset(left + frameWidth * 0.48f, top + frameHeight * 0.34f),
            end = Offset(left + frameWidth * 0.78f, top + frameHeight * 0.42f),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun TelemetryHud(
    liveAngles: LiveAnglesDto?,
    stats: ShotStatsDto?,
    modifier: Modifier = Modifier
) {
    val elbow = liveAngles?.elbowAngle ?: stats?.avgElbowAngle
    val knee = liveAngles?.kneeAngle ?: stats?.avgKneeAngle

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.54f)
    ) {
        TelemetryCard(
            label = "Elbow Splay",
            value = elbow.toAngleText(),
            accent = Primary,
            warning = true,
            alignEnd = false,
            modifier = Modifier.align(Alignment.TopStart)
        )
        TelemetryCard(
            label = "Knee Bend",
            value = knee.toAngleText(),
            accent = Secondary,
            warning = false,
            alignEnd = true,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun TelemetryCard(
    label: String,
    value: String,
    accent: Color,
    warning: Boolean,
    alignEnd: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .width(154.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceLowest.copy(alpha = 0.70f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
    ) {
        if (!alignEnd) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(accent)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(14.dp),
            horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
        ) {
            Text(
                text = label.uppercase(),
                style = technicalLabel(12),
                color = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = athleticHeadline(34, italic = true).copy(lineHeight = 34.sp),
                    color = accent
                )
                if (warning) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .size(18.dp)
                    )
                }
            }
        }
        if (alignEnd) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(accent)
            )
        }
    }
}

@Composable
private fun BottomCoachPanel(
    uiState: TrackingUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AiActiveBadge(
            connected = uiState.isConnected,
            active = uiState.isExerciseActive
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Surface.copy(alpha = 0.82f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(PrimaryContainer)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(SurfaceHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RecordVoiceOver,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = resolveAnalysisTitle(uiState),
                        style = athleticHeadline(29, italic = false).copy(lineHeight = 32.sp),
                        color = OnSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = resolveAnalysisMessage(uiState),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
                        color = OnSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    val details = listOfNotNull(
                        formatLiveAngles(uiState.liveAngles),
                        formatShotStats(uiState.lastShotStats)
                    ).joinToString("  •  ")
                    if (details.isNotBlank()) {
                        Text(
                            text = details,
                            style = technicalLabel(10),
                            color = Outline,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiActiveBadge(
    connected: Boolean,
    active: Boolean
) {
    val accent = if (connected) PrimaryContainer else Outline
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(SurfaceHighest.copy(alpha = 0.86f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Text(
            text = when {
                connected && active -> "AI ACTIVE"
                connected -> "AI READY"
                else -> "AI OFFLINE"
            },
            style = technicalLabel(12),
            color = OnSurface
        )
    }
}

@Composable
private fun HudErrorBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = message,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ErrorContainer.copy(alpha = 0.84f))
            .border(1.dp, Color(0xFFFFB4AB).copy(alpha = 0.38f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        style = MaterialTheme.typography.labelMedium,
        color = OnErrorContainer,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun CameraFallbackBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AthleticBackground),
        contentAlignment = Alignment.Center
    ) {
        BasketballFallbackMark()
        Text(
            text = "Camera permission needed",
            color = OnSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BasketballFallbackMark() {
    Canvas(
        modifier = Modifier
            .size(190.dp)
            .background(Color.Transparent)
    ) {
        val strokeWidth = 4.dp.toPx()
        val radius = size.minDimension / 2f - strokeWidth
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = Primary.copy(alpha = 0.16f),
            radius = radius,
            center = center,
            style = Stroke(strokeWidth)
        )
        drawLine(
            color = Primary.copy(alpha = 0.12f),
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Primary.copy(alpha = 0.12f),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun resolveAnalysisTitle(uiState: TrackingUiState): String {
    val elbow = uiState.liveAngles?.elbowAngle ?: uiState.lastShotStats?.avgElbowAngle
    return when {
        elbow != null && elbow > 0.0 -> "Tuck your elbow on release"
        uiState.isConnected -> "Hold your shooting window"
        else -> "Waiting for AI coach"
    }
}

private fun resolveAnalysisMessage(uiState: TrackingUiState): String {
    val feedback = uiState.feedbackText.takeIf { it.isNotBlank() && it != "Đang kết nối Server..." }
    if (feedback != null) return feedback
    return if (uiState.isConnected) {
        "Keep your body centered in frame and release when the tracking box is stable."
    } else {
        "Connecting live pose tracking and coaching feedback."
    }
}

private fun formatLiveAngles(liveAngles: LiveAnglesDto?): String? {
    if (liveAngles == null) return null
    val parts = listOfNotNull(
        liveAngles.elbowAngle.toAnglePart("Elbow"),
        liveAngles.shoulderAngle.toAnglePart("Shoulder"),
        liveAngles.kneeAngle.toAnglePart("Knee"),
        liveAngles.backAngle.toAnglePart("Back")
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
}

private fun formatShotStats(stats: ShotStatsDto?): String? {
    if (stats == null) return null
    val parts = listOfNotNull(
        stats.avgElbowAngle.toAnglePart("Elbow avg"),
        stats.avgShoulderAngle.toAnglePart("Shoulder avg"),
        stats.avgKneeAngle.toAnglePart("Knee avg"),
        stats.avgBackAngle.toAnglePart("Back avg"),
        stats.frameCount.toFramesPart("Frames")
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
}

private fun Double?.toAnglePart(label: String): String? {
    if (this == null || this.isNaN()) return null
    return "$label ${String.format("%.1f°", this)}"
}

private fun Double?.toAngleText(): String {
    if (this == null || this.isNaN()) return "--°"
    return String.format("%.0f°", this)
}

private fun Int?.toFramesPart(label: String): String? {
    if (this == null || this <= 0) return null
    return "$label $this"
}

private fun athleticHeadline(size: Int, italic: Boolean = false): TextStyle {
    return TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        fontSize = size.sp,
        lineHeight = (size + 4).sp
    )
}

private fun technicalLabel(size: Int): TextStyle {
    return TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = size.sp,
        lineHeight = (size + 4).sp,
        letterSpacing = 1.1.sp
    )
}
