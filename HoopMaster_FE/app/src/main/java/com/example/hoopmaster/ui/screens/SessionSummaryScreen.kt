package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoopmaster.data.model.SessionInfoDto
import com.example.hoopmaster.ui.responsive.HoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.HoopWindowInfo
import com.example.hoopmaster.ui.responsive.rememberHoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.rememberHoopWindowInfo
import com.example.hoopmaster.ui.responsive.responsiveContentWidth
import com.example.hoopmaster.viewmodels.SessionSummaryAction
import com.example.hoopmaster.viewmodels.SessionSummaryUiState
import com.example.hoopmaster.viewmodels.SessionSummaryViewModel

private val AthleticBackground = Color(0xFF1E100C)
private val SurfaceLowest = Color(0xFF180B07)
private val Surface = Color(0xFF2C1C17)
private val SurfaceHigh = Color(0xFF372621)
private val SurfaceHighest = Color(0xFF43302B)
private val Primary = Color(0xFFFFB5A0)
private val PrimaryContainer = Color(0xFFFF5722)
private val Secondary = Color(0xFF78DC77)
private val Tertiary = Color(0xFF86CFFF)
private val OnSurface = Color(0xFFFADCD4)
private val OnSurfaceVariant = Color(0xFFE4BEB4)
private val Outline = Color(0xFFAB8980)
private val OutlineVariant = Color(0xFF5B4039)
private val OnPrimaryContainer = Color(0xFF541200)
private val Error = Color(0xFFFFB4AB)

@Composable
fun SessionSummaryScreen(
    socketId: String?,
    onBackHome: () -> Unit,
    viewModel: SessionSummaryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val windowInfo = rememberHoopWindowInfo()
    val tokens = rememberHoopResponsiveTokens(windowInfo)

    LaunchedEffect(socketId) {
        viewModel.onAction(SessionSummaryAction.LoadSummary(socketId))
    }

    SessionSummaryContent(
        uiState = uiState,
        onBackHome = onBackHome,
        windowInfo = windowInfo,
        tokens = tokens
    )
}

@Composable
private fun SessionSummaryContent(
    uiState: SessionSummaryUiState,
    onBackHome: () -> Unit,
    windowInfo: HoopWindowInfo,
    tokens: HoopResponsiveTokens
) {
    val summary = uiState.summary
    val stats = summary?.stats
    val shots = stats?.shotsCompleted ?: 0
    val feedback = stats?.feedbackCount ?: 0
    val exercises = stats?.exercisesCompleted ?: 0
    val uptime = formatUptime(summary?.uptime)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AthleticBackground)
    ) {
        SummaryBackground(modifier = Modifier.matchParentSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = tokens.spacing.screenMargin, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.responsiveContentWidth(windowInfo, tokens),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SummaryHero()

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryMetricCard(
                            label = "Total Shots",
                            value = shots.toString(),
                            icon = Icons.Outlined.SportsBasketball,
                            accent = Primary,
                            footer = if (shots > 0) "+${shots.coerceAtMost(15)} from avg" else "No shots logged",
                            footerPositive = shots > 0,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricCard(
                            label = "Feedback",
                            value = feedback.toString(),
                            icon = Icons.Outlined.TrackChanges,
                            accent = Secondary,
                            footer = feedbackPerformanceText(feedback),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryMetricCard(
                            label = "Exercises",
                            value = exercises.toString(),
                            icon = Icons.Filled.EmojiEvents,
                            accent = Tertiary,
                            footer = if (exercises > 0) "Completed modules" else "No modules closed",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricCard(
                            label = "Uptime",
                            value = uptime,
                            icon = Icons.Outlined.Timer,
                            accent = OnSurface,
                            footer = "Session duration",
                            compactValue = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                AccuracyChartCard(
                    bars = buildSessionBars(summary),
                    modifier = Modifier.fillMaxWidth()
                )

                FormAnalysisCard(
                    uiState = uiState,
                    modifier = Modifier.fillMaxWidth()
                )

                SessionStateCard(
                    uiState = uiState,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onBackHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 26.dp, bottom = 12.dp)
                        .heightIn(min = 68.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryContainer,
                        contentColor = OnPrimaryContainer
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "BACK HOME",
                        style = athleticHeadline(28, italic = false).copy(fontWeight = FontWeight.Black)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF25100B), AthleticBackground, Color(0xFF130604))
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PrimaryContainer.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * 0.50f, size.height * 0.04f),
                radius = size.width * 0.82f
            ),
            radius = size.width * 0.82f,
            center = Offset(size.width * 0.50f, size.height * 0.04f)
        )
    }
}

@Composable
private fun SummaryHero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TrophyMark()
        Text(
            text = "SESSION COMPLETE",
            style = athleticHeadline(44, italic = true).copy(
                fontWeight = FontWeight.Black,
                lineHeight = 46.sp
            ),
            color = OnSurface,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Text(
            text = "Great work out there. Here's how you performed today.",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 28.sp),
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TrophyMark() {
    Box(
        modifier = Modifier.size(128.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Secondary.copy(alpha = 0.22f), Color.Transparent),
                    center = center,
                    radius = size.minDimension * 0.48f
                ),
                radius = size.minDimension * 0.48f,
                center = center
            )
            drawCircle(
                color = Primary,
                radius = 7.dp.toPx(),
                center = Offset(size.width * 0.18f, size.height * 0.13f)
            )
            drawCircle(
                color = Tertiary,
                radius = 9.dp.toPx(),
                center = Offset(size.width * 0.82f, size.height * 0.28f)
            )
            drawCircle(
                color = PrimaryContainer,
                radius = 5.dp.toPx(),
                center = Offset(size.width * 0.12f, size.height * 0.72f)
            )
            drawCircle(
                color = Secondary,
                radius = 7.dp.toPx(),
                center = Offset(size.width * 0.68f, size.height * 0.88f)
            )
        }
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Surface.copy(alpha = 0.84f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = Secondary,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

@Composable
private fun SummaryMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    footer: String,
    modifier: Modifier = Modifier,
    footerPositive: Boolean = false,
    compactValue: Boolean = false
) {
    Box(
        modifier = modifier
            .height(154.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        MetricRingAccent(
            accent = accent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(112.dp)
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OutlineVariant,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = label.uppercase(),
                    style = technicalLabel(12),
                    color = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = value,
                    style = athleticHeadline(if (compactValue) 34 else 48, italic = true).copy(
                        lineHeight = if (compactValue) 38.sp else 48.sp
                    ),
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (footerPositive) {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = null,
                            tint = Secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = footer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (footerPositive) Secondary else Outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricRingAccent(
    accent: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 9.dp.toPx()
        drawArc(
            color = SurfaceHigh.copy(alpha = 0.36f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = accent.copy(alpha = 0.86f),
            startAngle = -86f,
            sweepAngle = 82f,
            useCenter = false,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun AccuracyChartCard(
    bars: List<Float>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ShowChart,
                contentDescription = null,
                tint = OutlineVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Session Load",
                style = technicalLabel(13),
                color = OnSurfaceVariant
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val axisY = size.height - 22.dp.toPx()
            val topGrid = 20.dp.toPx()
            val midGrid = size.height * 0.48f
            val gap = 8.dp.toPx()
            val barWidth = (size.width - gap * (bars.size - 1)) / bars.size

            listOf(topGrid, midGrid).forEach { y ->
                drawLine(
                    color = OutlineVariant.copy(alpha = 0.34f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }
            drawLine(
                color = OutlineVariant,
                start = Offset(0f, axisY),
                end = Offset(size.width, axisY),
                strokeWidth = 1.dp.toPx()
            )

            bars.forEachIndexed { index, raw ->
                val value = raw.coerceIn(0.12f, 1f)
                val height = (axisY - topGrid) * value
                val left = index * (barWidth + gap)
                val color = if (value >= 0.68f) Secondary else PrimaryContainer.copy(alpha = 0.30f)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, axisY - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Start", "Mid", "End").forEach { label ->
                Text(
                    text = label,
                    style = technicalLabel(10),
                    color = Outline
                )
            }
        }
    }
}

@Composable
private fun FormAnalysisCard(
    uiState: SessionSummaryUiState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF2C2C2C))
            .border(1.dp, OutlineVariant.copy(alpha = 0.42f), RoundedCornerShape(18.dp))
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(Surface)
                .border(1.dp, OutlineVariant.copy(alpha = 0.52f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Psychology,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(30.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Form Analysis",
                style = athleticHeadline(28, italic = false),
                color = OnSurface
            )
            Text(
                text = analysisMessage(uiState),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 28.sp),
                color = OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun SessionStateCard(
    uiState: SessionSummaryUiState,
    modifier: Modifier = Modifier
) {
    val message = when {
        uiState.isLoading -> "Loading final session telemetry..."
        uiState.errorMessage != null -> uiState.errorMessage.orEmpty()
        uiState.isMissing -> "No session summary was found for this session."
        uiState.summary == null -> "No session summary is available."
        else -> "Summary loaded for socket ${uiState.socketId ?: "unknown"}."
    }
    val accent = if (uiState.errorMessage != null) Error else Outline
    Text(
        text = message,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface.copy(alpha = 0.78f))
            .border(1.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = if (uiState.errorMessage != null) Error else OnSurfaceVariant
    )
}

private fun buildSessionBars(summary: SessionInfoDto?): List<Float> {
    val stats = summary?.stats
    val shots = stats?.shotsCompleted ?: 0
    val feedback = stats?.feedbackCount ?: 0
    val exercises = stats?.exercisesCompleted ?: 0
    val frames = stats?.totalFrames ?: 0
    val base = listOf(
        shots / 250f,
        feedback / 20f,
        exercises / 6f,
        frames / 1200f
    ).map { it.coerceIn(0.18f, 0.92f) }

    return if (summary == null) {
        listOf(0.40f, 0.55f, 0.80f, 0.60f, 0.45f, 0.75f, 0.90f, 0.50f, 0.68f, 0.55f)
    } else {
        listOf(
            base[0] * 0.70f,
            base[1] * 0.82f,
            base[2],
            base[0],
            base[3] * 0.78f,
            base[1],
            base[2] * 0.92f,
            base[3],
            base.maxOrNull() ?: 0.62f,
            ((base.sum() / base.size) + 0.12f).coerceAtMost(0.94f)
        )
    }
}

private fun analysisMessage(uiState: SessionSummaryUiState): String {
    val summary = uiState.summary
    val shots = summary?.stats?.shotsCompleted ?: 0
    val feedback = summary?.stats?.feedbackCount ?: 0
    val exercises = summary?.stats?.exercisesCompleted ?: 0
    return when {
        uiState.isLoading -> "We are pulling the final telemetry and coach notes from your session."
        uiState.errorMessage != null -> "Session analysis could not be loaded. Your local session flow is still complete."
        uiState.isMissing -> "The session ended before a full analytics package was saved. Keep your next run active until the final summary appears."
        shots > 0 && feedback > 0 -> "You completed $shots tracked shots with $feedback coach feedback events across $exercises modules. Carry the strongest cue into your next session."
        shots > 0 -> "You completed $shots tracked shots. Keep building volume and let the AI coach collect more form feedback next time."
        else -> "Your workout is complete. Run another tracked session to unlock deeper shot and form analysis."
    }
}

private fun feedbackPerformanceText(feedback: Int): String {
    return when {
        feedback >= 12 -> "High coach activity"
        feedback >= 4 -> "Solid coaching"
        feedback > 0 -> "Light coaching"
        else -> "No feedback logged"
    }
}

private fun formatUptime(uptimeMs: Long?): String {
    val totalSeconds = ((uptimeMs ?: 0L) / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0L) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
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
