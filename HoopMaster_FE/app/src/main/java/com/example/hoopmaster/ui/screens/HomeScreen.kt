@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoopmaster.data.model.PlanExerciseDto
import com.example.hoopmaster.data.model.TrainingPlanDto
import com.example.hoopmaster.ui.components.HoopErrorBanner
import com.example.hoopmaster.ui.components.HoopLoadingRow
import com.example.hoopmaster.ui.responsive.HoopPhoneSizeClass
import com.example.hoopmaster.ui.responsive.HoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.HoopWindowInfo
import com.example.hoopmaster.ui.responsive.rememberHoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.rememberHoopWindowInfo
import com.example.hoopmaster.ui.responsive.responsiveContentWidth
import com.example.hoopmaster.viewmodels.HomeAction
import com.example.hoopmaster.viewmodels.HomeUiState
import com.example.hoopmaster.viewmodels.HomeViewModel
import com.example.hoopmaster.viewmodels.HomeViewModel.Companion.EXERCISE_TAG_DEFAULT
import com.example.hoopmaster.viewmodels.HomeViewModel.Companion.EXERCISE_TAG_PERSONAL

import com.example.hoopmaster.ui.theme.*

@Composable
fun HomeScreen(
    onPersonalizePlan: () -> Unit,
    onStartShooting: () -> Unit,
    onOpenExercise: (Int) -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val windowInfo = rememberHoopWindowInfo()
    val tokens = rememberHoopResponsiveTokens(windowInfo)

    LaunchedEffect(Unit) {
        viewModel.loadHome()
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadHome()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    HomeScreenContent(
        uiState = uiState,
        onSelectExerciseTag = { tag -> viewModel.onAction(HomeAction.SelectExerciseTag(tag)) },
        onPersonalizePlan = onPersonalizePlan,
        onStartShooting = onStartShooting,
        onOpenExercise = onOpenExercise,
        onOpenProfile = onOpenProfile,
        windowInfo = windowInfo,
        tokens = tokens
    )
}

@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    onSelectExerciseTag: (String) -> Unit,
    onPersonalizePlan: () -> Unit,
    onStartShooting: () -> Unit,
    onOpenExercise: (Int) -> Unit,
    onOpenProfile: () -> Unit,
    windowInfo: HoopWindowInfo,
    tokens: HoopResponsiveTokens
) {
    val compact = windowInfo.phoneSizeClass == HoopPhoneSizeClass.Small || windowInfo.isSmallHeight
    val userName = uiState.userName?.takeIf { it.isNotBlank() } ?: "Coach"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AthleticBackground)
    ) {
        HomeBackground(modifier = Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            HomeTopBar(onOpenProfile = onOpenProfile)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = tokens.spacing.screenMargin,
                    end = tokens.spacing.screenMargin,
                    top = if (compact) 18.dp else 24.dp,
                    bottom = 104.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    WelcomeHeader(
                        userName = userName,
                        plan = uiState.plan,
                        modifier = Modifier.responsiveContentWidth(windowInfo, tokens)
                    )
                }

                item {
                    ActivePlanCard(
                        uiState = uiState,
                        compact = compact,
                        onStartShooting = onStartShooting,
                        modifier = Modifier.responsiveContentWidth(windowInfo, tokens)
                    )
                }

                item {
                    PlanFilters(
                        selectedExerciseTag = uiState.selectedExerciseTag,
                        defaultEnabled = uiState.defaultExercises.isNotEmpty(),
                        personalEnabled = uiState.personalExercises.isNotEmpty(),
                        onSelectExerciseTag = onSelectExerciseTag,
                        onPersonalizePlan = onPersonalizePlan,
                        modifier = Modifier.responsiveContentWidth(windowInfo, tokens)
                    )
                }

                if (uiState.isLoading) {
                    item {
                        HoopLoadingRow(
                            text = "Loading your plan...",
                            modifier = Modifier.responsiveContentWidth(windowInfo, tokens)
                        )
                    }
                }

                if (uiState.errorMessage != null) {
                    item {
                        HoopErrorBanner(
                            message = uiState.errorMessage.orEmpty(),
                            modifier = Modifier.responsiveContentWidth(windowInfo, tokens)
                        )
                    }
                }

                if (uiState.exercises.isNotEmpty()) {
                    item {
                        Text(
                            text = "Today's Modules",
                            modifier = Modifier
                                .responsiveContentWidth(windowInfo, tokens)
                                .fillMaxWidth(),
                            style = athleticHeadline(28, italic = true),
                            color = OnSurface
                        )
                    }
                    items(uiState.exercises) { exercise ->
                        ExerciseModuleCard(
                            exercise = exercise,
                            modifier = Modifier.responsiveContentWidth(windowInfo, tokens),
                            onClick = {
                                val exerciseId = exercise.exerciseId ?: exercise.exercise?.id
                                exerciseId?.let(onOpenExercise)
                            }
                        )
                    }
                }

                if (!uiState.isLoading && uiState.exercises.isEmpty() && uiState.errorMessage == null) {
                    item {
                        EmptyPlanCard(
                            modifier = Modifier.responsiveContentWidth(windowInfo, tokens)
                        )
                    }
                }
            }
        }

        HomeBottomNav(
            onStartShooting = onStartShooting,
            onOpenProfile = onOpenProfile,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun HomeBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Primary.copy(alpha = 0.10f),
                    AthleticBackground,
                    Color.Black.copy(alpha = 0.08f)
                )
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Primary.copy(alpha = 0.13f), Color.Transparent),
                center = Offset(size.width * 0.50f, 0f),
                radius = size.width * 0.85f
            ),
            radius = size.width * 0.85f,
            center = Offset(size.width * 0.50f, 0f)
        )
    }
}

@Composable
private fun HomeTopBar(onOpenProfile: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLowest.copy(alpha = 0.88f))
            .border(width = 1.dp, color = OutlineVariant.copy(alpha = 0.30f))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Menu,
            contentDescription = null,
            tint = OnSurfaceVariant,
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = "HOOPMASTER",
            style = athleticHeadline(34, italic = true).copy(fontWeight = FontWeight.Black),
            color = Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceHighest)
                .border(width = 1.dp, color = Outline, shape = CircleShape)
                .clickable(onClick = onOpenProfile),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Open profile",
                tint = Primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun WelcomeHeader(
    userName: String,
    plan: TrainingPlanDto?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = buildPlanMeta(plan).uppercase(),
            style = technicalLabel(14),
            color = Primary
        )
        Text(
            text = "Welcome back, $userName",
            style = athleticHeadline(44, italic = true).copy(lineHeight = 46.sp),
            color = OnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActivePlanCard(
    uiState: HomeUiState,
    compact: Boolean,
    onStartShooting: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = uiState.activePlanTitle ?: "Active Plan"
    val description = uiState.activePlanDescription ?: "Ready to move from warm-up into live reps."
    val duration = uiState.plan?.schedule?.sessionDurationMinutes ?: estimateDurationMinutes(uiState.exercises)
    val intensity = estimateIntensity(uiState.exercises)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Surface.copy(alpha = 0.96f))
            .border(1.dp, SurfaceHigh, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = athleticHeadline(28, italic = true),
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            ReadyBadge(loading = uiState.isLoading, status = uiState.plan?.status)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(
                label = "Est. Time",
                value = duration.toString(),
                suffix = "min",
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Intensity",
                value = intensity,
                valueColor = Primary,
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = onStartShooting,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (compact) 58.dp else 64.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryContainer,
                contentColor = OnPrimaryContainer,
                disabledContainerColor = PrimaryContainer.copy(alpha = 0.46f),
                disabledContentColor = OnPrimaryContainer.copy(alpha = 0.62f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = OnPrimaryContainer,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "START SESSION",
                    style = athleticHeadline(if (compact) 25 else 29, italic = true),
                    color = OnPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun ReadyBadge(
    loading: Boolean,
    status: String?
) {
    val ready = !loading && status.equals("active", ignoreCase = true)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = if (ready) Secondary else Outline,
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = if (loading) "LOADING" else if (ready) "READY" else (status ?: "PLAN").uppercase(),
            style = technicalLabel(12),
            color = if (ready) Secondary else OnSurfaceVariant
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    valueColor: Color = OnSurface
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceHighest)
            .border(1.dp, Outline.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = technicalLabel(12),
            color = OnSurfaceVariant
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = athleticHeadline(46, italic = true).copy(lineHeight = 46.sp),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (suffix != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun PlanFilters(
    selectedExerciseTag: String,
    defaultEnabled: Boolean,
    personalEnabled: Boolean,
    onSelectExerciseTag: (String) -> Unit,
    onPersonalizePlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SessionChip(
            label = "Default Plan",
            selected = selectedExerciseTag == EXERCISE_TAG_DEFAULT,
            enabled = defaultEnabled,
            onClick = { onSelectExerciseTag(EXERCISE_TAG_DEFAULT) }
        )
        SessionChip(
            label = "Personal Plan",
            selected = selectedExerciseTag == EXERCISE_TAG_PERSONAL,
            enabled = personalEnabled,
            onClick = { onSelectExerciseTag(EXERCISE_TAG_PERSONAL) }
        )
        SessionChip(
            label = "Personalize",
            selected = false,
            enabled = true,
            icon = Icons.Outlined.Tune,
            onClick = onPersonalizePlan
        )
    }
}

@Composable
private fun SessionChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) Primary.copy(alpha = 0.10f) else Surface)
            .border(
                width = 1.dp,
                color = when {
                    selected -> Primary
                    enabled -> OutlineVariant
                    else -> OutlineVariant.copy(alpha = 0.4f)
                },
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) OnSurfaceVariant else Outline.copy(alpha = 0.55f),
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            style = technicalLabel(13),
            color = when {
                selected -> Primary
                enabled -> OnSurfaceVariant
                else -> Outline.copy(alpha = 0.55f)
            },
            maxLines = 1
        )
    }
}

@Composable
private fun ExerciseModuleCard(
    exercise: PlanExerciseDto,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val exerciseId = exercise.exerciseId ?: exercise.exercise?.id
    val title = exercise.name ?: exercise.exercise?.name ?: "Training Module"
    val category = exercise.category ?: exercise.exercise?.category ?: "Training"
    val target = exercise.target ?: exercise.exercise?.target
    val sets = exercise.sets ?: exercise.exercise?.sets
    val reps = exercise.reps ?: exercise.exercise?.reps
    val restSeconds = target?.restSeconds
    val enabled = exerciseId != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface.copy(alpha = if (enabled) 0.96f else 0.56f))
            .border(1.dp, SurfaceHigh.copy(alpha = if (enabled) 1f else 0.5f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModuleThumbnail(
            category = category,
            enabled = enabled
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                color = if (enabled) OnSurface else OnSurfaceVariant.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TargetMeta(
                    icon = Icons.Outlined.Repeat,
                    text = buildSetRepLabel(sets, reps, exercise.duration),
                    enabled = enabled
                )
                if (restSeconds != null) {
                    TargetMeta(
                        icon = Icons.Outlined.Timer,
                        text = "${restSeconds}s rest",
                        enabled = enabled
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = null,
            tint = if (enabled) OnSurfaceVariant else Outline.copy(alpha = 0.55f),
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun ModuleThumbnail(
    category: String,
    enabled: Boolean
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceHighest)
            .border(1.dp, Outline.copy(alpha = 0.26f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryContainer.copy(alpha = 0.34f), Color.Transparent),
                    center = Offset(size.width * 0.25f, size.height * 0.92f),
                    radius = size.width * 0.95f
                )
            )
            drawLine(
                color = Outline.copy(alpha = if (enabled) 0.36f else 0.18f),
                start = Offset(size.width * 0.20f, size.height * 0.78f),
                end = Offset(size.width * 0.82f, size.height * 0.36f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        Icon(
            imageVector = if (category.contains("strength", ignoreCase = true)) {
                Icons.Outlined.FitnessCenter
            } else {
                Icons.Outlined.SportsBasketball
            },
            contentDescription = null,
            tint = if (enabled) Primary else Outline,
            modifier = Modifier.size(28.dp)
        )
        if (enabled) {
            Text(
                text = "AI TRK",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(SurfaceLowest.copy(alpha = 0.84f))
                    .padding(horizontal = 5.dp, vertical = 3.dp),
                style = technicalLabel(9),
                color = Primary
            )
        }
    }
}

@Composable
private fun TargetMeta(
    icon: ImageVector,
    text: String,
    enabled: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) OnSurfaceVariant else Outline.copy(alpha = 0.55f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = technicalLabel(12),
            color = if (enabled) OnSurfaceVariant else Outline.copy(alpha = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyPlanCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, SurfaceHigh, RoundedCornerShape(12.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "No modules loaded",
            style = athleticHeadline(24, italic = true),
            color = OnSurface
        )
        Text(
            text = "Your active plan will appear here once it is available.",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant
        )
    }
}

@Composable
private fun HomeBottomNav(
    onStartShooting: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .background(SurfaceLowest.copy(alpha = 0.94f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.22f))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            label = "Home",
            icon = Icons.Filled.Home,
            selected = true,
            onClick = {}
        )
        BottomNavItem(
            label = "Training",
            icon = Icons.Outlined.SportsBasketball,
            selected = false,
            onClick = onStartShooting
        )
        BottomNavItem(
            label = "Analytics",
            icon = Icons.Outlined.Insights,
            selected = false,
            enabled = false,
            onClick = {}
        )
        BottomNavItem(
            label = "Profile",
            icon = Icons.Outlined.Person,
            selected = false,
            onClick = onOpenProfile
        )
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val itemColor = when {
        selected -> OnPrimaryContainer
        enabled -> Outline
        else -> Outline.copy(alpha = 0.46f)
    }
    Column(
        modifier = Modifier
            .height(70.dp)
            .widthIn(min = 70.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) PrimaryContainer else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = itemColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = technicalLabel(11),
            color = itemColor,
            maxLines = 1
        )
    }
}

private fun buildPlanMeta(plan: TrainingPlanDto?): String {
    val days = plan?.schedule?.daysPerWeek
    val source = when (plan?.source?.lowercase()) {
        "personalized" -> "Personal Plan"
        "default" -> "Default Plan"
        else -> plan?.goal ?: "Training Plan"
    }
    return if (days != null) "$days days/week • $source" else source
}

private fun estimateDurationMinutes(exercises: List<PlanExerciseDto>): Int {
    if (exercises.isEmpty()) return 45
    return (exercises.size * 12).coerceIn(20, 75)
}

private fun estimateIntensity(exercises: List<PlanExerciseDto>): String {
    val totalSets = exercises.sumOf { it.sets ?: it.exercise?.sets ?: 0 }
    return when {
        totalSets >= 16 -> "High"
        totalSets >= 8 -> "Med"
        else -> "Base"
    }
}

private fun buildSetRepLabel(sets: Int?, reps: Int?, duration: String?): String {
    return when {
        sets != null && reps != null -> "${sets}x${reps}"
        sets != null && duration != null -> "${sets}x$duration"
        duration != null -> duration
        sets != null -> "$sets sets"
        reps != null -> "$reps reps"
        else -> "Target"
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
