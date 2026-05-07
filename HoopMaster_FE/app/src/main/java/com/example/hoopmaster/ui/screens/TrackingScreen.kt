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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.style.TextAlign
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
    onEndSession: () -> Unit,
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
                timerText = "00:59",
                iconSize = tokens.sizing.iconButtonSize
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = if (compactOverlay) 76.dp else 96.dp,
                        end = overlayMargin
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
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
            }

            AccuracyMeter(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = overlayMargin),
                progress = accuracyValue,
                label = "Accuracy",
                meterHeight = tokens.sizing.trackingMeterHeight
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = overlayMargin),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap)
            ) {
                QuickStatCard(
                    icon = Icons.Outlined.Speed,
                    iconTint = MaterialTheme.colorScheme.tertiaryContainer,
                    value = releaseValue,
                    label = "Release",
                    compact = compactOverlay
                )
                QuickStatCard(
                    icon = Icons.Outlined.Architecture,
                    iconTint = ActiveOrange,
                    value = arcValue,
                    label = "Arc",
                    compact = compactOverlay
                )
            }

            Surface(
                modifier = Modifier
                    .align(if (compactOverlay) Alignment.BottomEnd else Alignment.BottomStart)
                    .padding(
                        start = overlayMargin,
                        end = overlayMargin,
                        bottom = shotModeBottomPadding
                    ),
                shape = RoundedCornerShape(HoopRadius.Lg),
                color = NavyShadow.copy(alpha = 0.66f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
            ) {
                Column(
                    modifier = Modifier
                        .width(if (compactOverlay) tokens.sizing.trackingControlSize * 2f else tokens.sizing.trackingControlSize * 2.4f)
                        .padding(tokens.spacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap)
                ) {
                    Text(
                        text = "Shot mode",
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.84f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(HoopSpacing.Sm)) {
                        listOf("strict", "neutral", "cheerful").forEach { tone ->
                            FilterChip(
                                selected = viewModel.selectedTone.value == tone,
                                onClick = {
                                    viewModel.updateTone(tone)
                                },
                                label = { Text(tone.replaceFirstChar { it.uppercase() }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ActiveOrange.copy(alpha = 0.18f),
                                    selectedLabelColor = NavyShadow,
                                    selectedLeadingIconColor = NavyShadow,
                                    containerColor = Color.White.copy(alpha = 0.04f),
                                    labelColor = Color.White.copy(alpha = 0.82f),
                                    iconColor = Color.White.copy(alpha = 0.82f)
                                ),
                                modifier = Modifier.heightIn(min = tokens.sizing.buttonMinHeight)
                            )
                        }
                    }
                    Button(
                        onClick = {
                            viewModel.onShotReleased()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = tokens.sizing.buttonMinHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ActiveOrange,
                            contentColor = NavyShadow,
                            disabledContainerColor = ActiveOrange.copy(alpha = 0.38f),
                            disabledContentColor = NavyShadow.copy(alpha = 0.38f)
                        ),
                        shape = RoundedCornerShape(HoopRadius.Full)
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(HoopSpacing.Xs))
                        Text("Shot released")
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
                made = madeShots,
                total = totalShots,
                streak = streakValue,
                compact = compactOverlay,
                applyNavigationPadding = !analysisOnTop
            )

            if (poseInitError != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 88.dp)
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
    timerText: String,
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

            RecordingPill(timerText = timerText)

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
private fun RecordingPill(timerText: String) {
    val pillShape = RoundedCornerShape(HoopRadius.Full)
    Row(
        modifier = Modifier
            .clip(pillShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, ActiveOrange.copy(alpha = 0.35f), pillShape)
            .padding(horizontal = HoopSpacing.Md, vertical = HoopSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(ActiveOrange)
        )
        Spacer(modifier = Modifier.width(HoopSpacing.Sm))
        Text(
            text = "REC",
            fontWeight = FontWeight.Bold,
            color = ActiveOrange,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.width(HoopSpacing.Md))
        Text(
            text = timerText,
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
private fun AccuracyMeter(
    modifier: Modifier,
    progress: Float,
    label: String,
    meterHeight: Dp
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(meterHeight)
                .width(44.dp)
                .clip(RoundedCornerShape(HoopRadius.Full))
                .background(NavyShadow.copy(alpha = 0.64f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(HoopRadius.Full))
                .padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(progress)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(HoopRadius.Full))
                    .background(ActiveOrange)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 38.dp)
                    .background(Color.White.copy(alpha = 0.86f))
            )
        }
        Spacer(modifier = Modifier.height(HoopSpacing.Sm))
        Text(
            text = "${(progress * 100).toInt()}%",
            fontWeight = FontWeight.Bold,
            color = ActiveOrange
        )
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            color = Color.White.copy(alpha = 0.78f)
        )
    }
}

@Composable
private fun QuickStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    compact: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = NavyShadow.copy(alpha = 0.66f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compact) HoopSpacing.Sm else HoopSpacing.Md,
                vertical = if (compact) HoopSpacing.Xs else HoopSpacing.Sm
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(if (compact) 16.dp else 20.dp)
            )
            Spacer(modifier = Modifier.height(HoopSpacing.Xs))
            Text(
                text = value,
                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = Color.White.copy(alpha = 0.78f)
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
    streak: Int,
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
            color = Color.White.copy(alpha = 0.78f),
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
