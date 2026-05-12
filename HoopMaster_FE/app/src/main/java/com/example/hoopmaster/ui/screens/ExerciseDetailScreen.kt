package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoopmaster.ui.responsive.HoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.HoopWindowInfo
import com.example.hoopmaster.ui.responsive.rememberHoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.rememberHoopWindowInfo
import com.example.hoopmaster.ui.responsive.responsiveContentWidth
import com.example.hoopmaster.viewmodels.ExerciseDetailAction
import com.example.hoopmaster.viewmodels.ExerciseDetailUiState
import com.example.hoopmaster.viewmodels.ExerciseDetailViewModel

private val AthleticBackground = Color(0xFFFCF9F8)
private val SurfaceLowest = Color(0xFFFFFFFF)
private val Surface = Color(0xFFF0EDEC)
private val SurfaceLow = Color(0xFFF6F3F2)
private val SurfaceHigh = Color(0xFFEBE7E7)
private val Primary = Color(0xFFB02F00)
private val PrimaryContainer = Color(0xFFFF5722)
private val Secondary = Color(0xFF1B6D24)
private val OnSurface = Color(0xFF1C1B1B)
private val OnSurfaceVariant = Color(0xFF5B4039)
private val Outline = Color(0xFF907067)
private val OutlineVariant = Color(0xFFE4BEB4)
private val ErrorContainer = Color(0xFFFFDAD6)
private val OnPrimaryContainer = Color(0xFF541200)

@Composable
fun ExerciseDetailScreen(
    exerciseId: Int,
    onBack: () -> Unit,
    onStartTracking: (Int) -> Unit,
    viewModel: ExerciseDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val windowInfo = rememberHoopWindowInfo()
    val tokens = rememberHoopResponsiveTokens(windowInfo)

    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId)
    }

    ExerciseDetailContent(
        uiState = uiState,
        exerciseId = exerciseId,
        onBack = onBack,
        onStartTracking = {
            viewModel.onAction(ExerciseDetailAction.StartExercise)
            onStartTracking(exerciseId)
        },
        windowInfo = windowInfo,
        tokens = tokens
    )
}

@Composable
private fun ExerciseDetailContent(
    uiState: ExerciseDetailUiState,
    exerciseId: Int,
    onBack: () -> Unit,
    onStartTracking: () -> Unit,
    windowInfo: HoopWindowInfo,
    tokens: HoopResponsiveTokens
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AthleticBackground)
    ) {
        DetailBackground(modifier = Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = tokens.spacing.screenMargin)
                    .padding(top = 18.dp, bottom = 116.dp)
                    .responsiveContentWidth(windowInfo, tokens),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HeroCard(uiState = uiState)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TargetMetricCard(
                        label = "Target Sets",
                        value = uiState.sets?.toString() ?: "-",
                        suffix = "Sets",
                        icon = Icons.Filled.Repeat,
                        accent = Primary,
                        modifier = Modifier.weight(1f)
                    )
                    TargetMetricCard(
                        label = "Target Reps",
                        value = uiState.reps?.toString() ?: "-",
                        suffix = "Reps",
                        icon = Icons.Filled.Sync,
                        accent = Secondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (uiState.restSeconds != null) {
                    DetailInfoStrip(
                        label = "Rest Window",
                        value = "${uiState.restSeconds}s between sets"
                    )
                }

                DescriptionCard(
                    description = uiState.description.ifBlank {
                        if (uiState.isLoading) "Loading drill details and targets." else "No description available."
                    }
                )

                if (uiState.steps.isNotEmpty()) {
                    StepsCard(steps = uiState.steps)
                }

                FocusPointsCard(
                    warnings = uiState.warnings.ifEmpty {
                        listOf("Keep your mechanics controlled through every rep.")
                    }
                )

                uiState.errorMessage?.let { error ->
                    ErrorCard(message = error)
                }
            }
        }

        StartTrackingBar(
            onStartTracking = onStartTracking,
            enabled = !uiState.isLoading && uiState.errorMessage == null,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun DetailBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(SurfaceLow, AthleticBackground, SurfaceLowest)
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Primary.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(size.width * 0.50f, size.height * 0.10f),
                radius = size.width * 0.78f
            ),
            radius = size.width * 0.78f,
            center = Offset(size.width * 0.50f, size.height * 0.10f)
        )
    }
}

@Composable
private fun DetailTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLowest.copy(alpha = 0.90f))
            .statusBarsPadding()
            .border(1.dp, OutlineVariant.copy(alpha = 0.26f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Primary,
                modifier = Modifier
                    .size(31.dp)
                    .clickable(onClick = onBack)
            )
            Text(
                text = "Drill Details",
                style = athleticHeadline(30),
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Filled.BookmarkBorder,
            contentDescription = null,
            tint = OnSurfaceVariant,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun HeroCard(uiState: ExerciseDetailUiState) {
    val title = uiState.title.ifBlank { if (uiState.isLoading) "Loading Drill" else "Drill" }
    val category = uiState.category.ifBlank { "Training" }
    val level = uiState.exercise?.tracking?.type?.takeIf { it.isNotBlank() } ?: "Intermediate"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(1.dp, OutlineVariant.copy(alpha = 0.20f), RoundedCornerShape(18.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(SurfaceLowest, Surface, AthleticBackground.copy(alpha = 0.96f))
                )
            )
            drawCircle(
                color = Primary.copy(alpha = 0.08f),
                radius = size.width * 0.56f,
                center = Offset(size.width * 0.54f, size.height * 0.06f)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.20f),
                start = Offset(size.width * 0.18f, size.height * 0.16f),
                end = Offset(size.width * 0.08f, size.height * 0.50f),
                strokeWidth = 18.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.18f),
                start = Offset(size.width * 0.84f, size.height * 0.16f),
                end = Offset(size.width * 0.92f, size.height * 0.52f),
                strokeWidth = 18.dp.toPx()
            )
            val courtY = size.height * 0.86f
            drawLine(
                color = OutlineVariant.copy(alpha = 0.42f),
                start = Offset(size.width * 0.24f, courtY),
                end = Offset(size.width * 0.76f, courtY),
                strokeWidth = 1.dp.toPx()
            )
            drawRoundRect(
                color = OutlineVariant.copy(alpha = 0.22f),
                topLeft = Offset(size.width * 0.36f, size.height * 0.68f),
                size = Size(size.width * 0.28f, size.height * 0.17f),
                cornerRadius = CornerRadius(80f, 80f),
                style = Stroke(1.dp.toPx())
            )
            drawCircle(
                color = Primary.copy(alpha = 0.16f),
                radius = 36.dp.toPx(),
                center = Offset(size.width * 0.50f, size.height * 0.38f)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailChip(label = category)
                DetailChip(label = level)
            }
            Text(
                text = title.uppercase(),
                style = athleticHeadline(46, italic = true).copy(lineHeight = 48.sp),
                color = Primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailChip(label: String) {
    Text(
        text = label.uppercase(),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceLowest.copy(alpha = 0.54f))
            .border(1.dp, Outline.copy(alpha = 0.52f), RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        style = technicalLabel(12),
        color = Primary,
        maxLines = 1
    )
}

@Composable
private fun TargetMetricCard(
    label: String,
    value: String,
    suffix: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(138.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, OutlineVariant.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(18.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.05f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(84.dp)
        )
        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = technicalLabel(12),
                color = OnSurfaceVariant
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = athleticHeadline(44, italic = true).copy(lineHeight = 44.sp),
                    color = accent
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = suffix,
                    modifier = Modifier.padding(bottom = 6.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailInfoStrip(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceLow)
            .border(1.dp, OutlineVariant.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label.uppercase(), style = technicalLabel(11), color = OnSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
    }
}

@Composable
private fun DescriptionCard(description: String) {
    DetailCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "Description",
                style = athleticHeadline(28),
                color = OnSurface
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 30.sp),
            color = OnSurfaceVariant
        )
    }
}

@Composable
private fun StepsCard(steps: List<String>) {
    DetailCard {
        Text(
            text = "Execution Steps",
            style = athleticHeadline(28),
            color = OnSurface
        )
        steps.take(4).forEachIndexed { index, step ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(SurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = technicalLabel(10),
                        color = Primary
                    )
                }
                Text(
                    text = step,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 25.sp),
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FocusPointsCard(warnings: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceLow)
            .border(1.dp, OutlineVariant.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
    ) {
        Row {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .heightIn(min = 170.dp)
                    .background(ErrorContainer)
            )
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = ErrorContainer,
                        modifier = Modifier.size(29.dp)
                    )
                    Text(
                        text = "Focus Points",
                        style = athleticHeadline(28),
                        color = OnSurface
                    )
                }
                warnings.take(4).forEach { warning ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = ErrorContainer,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(18.dp)
                        )
                        Text(
                            text = warning,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 17.sp, lineHeight = 26.sp),
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ErrorContainer.copy(alpha = 0.18f))
            .border(1.dp, ErrorContainer.copy(alpha = 0.52f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = Primary
    )
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, OutlineVariant.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        content = content
    )
}

@Composable
private fun StartTrackingBar(
    onStartTracking: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceLowest.copy(alpha = 0.92f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.28f))
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Button(
            onClick = onStartTracking,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryContainer,
                contentColor = OnPrimaryContainer,
                disabledContainerColor = SurfaceHigh,
                disabledContentColor = Outline
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Videocam,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "START TRACKING",
                style = athleticHeadline(27).copy(fontWeight = FontWeight.Black)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )
        }
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
