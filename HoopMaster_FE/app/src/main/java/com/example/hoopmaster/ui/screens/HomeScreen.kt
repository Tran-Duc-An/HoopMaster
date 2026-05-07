@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoopmaster.data.model.PlanExerciseDto
import com.example.hoopmaster.ui.components.HoopActionButton
import com.example.hoopmaster.ui.components.HoopCard
import com.example.hoopmaster.ui.components.HoopErrorBanner
import com.example.hoopmaster.ui.components.HoopFilterChip
import com.example.hoopmaster.ui.components.HoopIconButton
import com.example.hoopmaster.ui.components.HoopLoadingRow
import com.example.hoopmaster.ui.components.HoopMetricCard
import com.example.hoopmaster.ui.components.HoopSecondaryButton
import com.example.hoopmaster.ui.components.HoopStatus
import com.example.hoopmaster.ui.components.HoopStatusBadge
import com.example.hoopmaster.ui.theme.ActiveOrange
import com.example.hoopmaster.ui.theme.HoopSpacing
import com.example.hoopmaster.ui.theme.NavyShadow
import com.example.hoopmaster.viewmodels.HomeViewModel

@Composable
fun HomeScreen(
    onPersonalizePlan: () -> Unit,
    onStartShooting: () -> Unit,
    onOpenExercise: (Int) -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHome()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HoopSpacing.ScreenMargin, vertical = HoopSpacing.Md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(HoopSpacing.Xs)) {
                    Text(
                        text = "Good to see you",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.userName?.let { "Hi, $it" } ?: "Hi, Coach",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                HoopIconButton(
                    icon = Icons.Outlined.Person,
                    contentDescription = "Open profile",
                    onClick = onOpenProfile,
                    emphasized = true
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = HoopSpacing.ScreenMargin,
                end = HoopSpacing.ScreenMargin,
                bottom = HoopSpacing.Section
            ),
            verticalArrangement = Arrangement.spacedBy(HoopSpacing.Lg)
        ) {
            item {
                HoopCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(HoopSpacing.Md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(HoopSpacing.Xs)
                            ) {
                                Text(
                                    text = uiState.activePlanTitle ?: "Active plan",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = uiState.activePlanDescription
                                        ?: "Ready to move from warm-up into live reps.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HoopStatusBadge(
                                label = if (uiState.isLoading) "Loading" else "Active",
                                status = if (uiState.isLoading) HoopStatus.Info else HoopStatus.Active
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(HoopSpacing.Sm),
                            verticalArrangement = Arrangement.spacedBy(HoopSpacing.Sm)
                        ) {
                            HoopFilterChip(
                                label = uiState.plan?.goal ?: "Game prep",
                                selected = true,
                                onClick = {}
                            )
                            HoopFilterChip(
                                label = uiState.plan?.status ?: "Custom plan",
                                selected = false,
                                onClick = {}
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(HoopSpacing.Sm)) {
                            HoopActionButton(
                                text = "Start session",
                                icon = Icons.Outlined.PlayArrow,
                                onClick = onStartShooting,
                                modifier = Modifier.weight(1f)
                            )
                            HoopSecondaryButton(
                                text = "Personalize",
                                icon = Icons.Outlined.Edit,
                                onClick = onPersonalizePlan,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(HoopSpacing.Sm)) {
                    HoopMetricCard(
                        label = "Streak",
                        value = "${uiState.streakDays}",
                        icon = Icons.Outlined.Whatshot,
                        accentColor = ActiveOrange,
                        modifier = Modifier.weight(1f)
                    )
                    HoopMetricCard(
                        label = "Session",
                        value = when (uiState.currentSessionState) {
                            "active" -> "Active"
                            "ready" -> "Ready"
                            else -> "Idle"
                        },
                        icon = Icons.Outlined.Timer,
                        modifier = Modifier.weight(1f)
                    )
                    HoopMetricCard(
                        label = "Exercises",
                        value = "${uiState.exercises.size}",
                        icon = Icons.Outlined.SportsBasketball,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (uiState.isLoading) {
                item {
                    HoopLoadingRow(
                        text = "Loading your plan...",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (uiState.errorMessage != null) {
                item {
                    HoopErrorBanner(
                        message = uiState.errorMessage.orEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (uiState.exercises.isNotEmpty()) {
                item {
                    Text(
                        text = "Exercises",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(uiState.exercises) { exercise ->
                    ExercisePlanCard(
                        exercise = exercise,
                        onClick = {
                            exercise.exerciseId?.let(onOpenExercise)
                        }
                    )
                }
            }

            if (!uiState.isLoading && uiState.exercises.isEmpty() && uiState.errorMessage == null) {
                item {
                    HoopCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(HoopSpacing.Sm)) {
                            Text(
                                text = "No exercises loaded",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Your active plan will appear here once it is available.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExercisePlanCard(
    exercise: PlanExerciseDto,
    onClick: () -> Unit
) {
    val exerciseId = exercise.exerciseId ?: exercise.exercise?.id
    val title = exercise.name ?: exercise.exercise?.name ?: "Exercise"
    val category = exercise.category ?: exercise.exercise?.category ?: "Training"
    val status = if (exerciseId != null) HoopStatus.Success else HoopStatus.Info
    val target = exercise.target ?: exercise.exercise?.target
    val sets = exercise.sets ?: exercise.exercise?.sets
    val reps = exercise.reps ?: exercise.exercise?.reps
    val restSeconds = target?.restSeconds

    HoopCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (exerciseId != null) onClick else null,
        enabled = exerciseId != null
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(HoopSpacing.Md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(HoopSpacing.Xs)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = exercise.description ?: exercise.exercise?.description ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = exerciseId?.toString() ?: "draft",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(HoopSpacing.Sm)) {
                HoopFilterChip(
                    label = category,
                    selected = true,
                    onClick = {}
                )
                HoopStatusBadge(
                    label = if (exerciseId != null) "Ready" else "Draft",
                    status = status
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Target",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = buildTargetRow(sets, reps, restSeconds),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (exerciseId != null) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = NavyShadow
                    )
                }
            }
        }
    }
}

private fun buildTargetRow(sets: Int?, reps: Int?, restSeconds: Int?): String {
    val safeSets = sets?.toString() ?: "-"
    val safeReps = reps?.toString() ?: "-"
    val safeRest = restSeconds?.let { "${it}s" } ?: "-"
    return "$safeSets sets · $safeReps reps · $safeRest rest"
}
