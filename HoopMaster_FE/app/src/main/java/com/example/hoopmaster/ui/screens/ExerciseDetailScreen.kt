package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoopmaster.ui.components.HoopActionButton
import com.example.hoopmaster.ui.components.HoopCard
import com.example.hoopmaster.ui.components.HoopMetricCard
import com.example.hoopmaster.ui.components.HoopScreenScaffold
import com.example.hoopmaster.ui.components.HoopStatus
import com.example.hoopmaster.ui.components.HoopStatusPanel
import com.example.hoopmaster.ui.responsive.ResponsiveMetricGrid
import com.example.hoopmaster.ui.responsive.rememberHoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.rememberHoopWindowInfo
import com.example.hoopmaster.ui.responsive.responsiveContentWidth
import com.example.hoopmaster.ui.theme.ActiveOrange
import com.example.hoopmaster.viewmodels.ExerciseDetailAction
import com.example.hoopmaster.viewmodels.ExerciseDetailUiState
import com.example.hoopmaster.viewmodels.ExerciseDetailViewModel

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
    val compactBottomControls = windowInfo.isLandscape || windowInfo.isSmallHeight

    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId)
    }

    HoopScreenScaffold(
        title = uiState.title.ifBlank { "Exercise" },
        onBack = onBack,
        windowInfo = windowInfo,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = tokens.spacing.bottomBarHorizontal,
                        vertical = tokens.spacing.bottomBarVertical
                    )
            ) {
                HoopActionButton(
                    text = "Start tracking",
                    icon = Icons.Outlined.PlayArrow,
                    onClick = {
                        viewModel.onAction(ExerciseDetailAction.StartExercise)
                        onStartTracking(exerciseId)
                    },
                    compact = compactBottomControls,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = tokens.spacing.screenMargin),
            contentPadding = PaddingValues(vertical = tokens.spacing.contentGap),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap)
        ) {
            item {
                HoopStatusPanel(
                    modifier = Modifier.responsiveContentWidth(windowInfo, tokens),
                    title = uiState.category.ifBlank { "Shooting" },
                    message = uiState.description.ifBlank { "Loading exercise..." },
                    status = if (uiState.isLoading) HoopStatus.Active else HoopStatus.Info
                )
            }
            item {
                Column(
                    modifier = Modifier.responsiveContentWidth(windowInfo, tokens),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap)
                ) {
                    Text(
                        text = "Targets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    ResponsiveMetricGrid(
                        itemCount = 3,
                        windowInfo = windowInfo
                    ) { index, itemModifier ->
                        when (index) {
                            0 -> HoopMetricCard(
                                label = "Sets",
                                value = uiState.sets?.toString() ?: "-",
                                accentColor = ActiveOrange,
                                compact = compactBottomControls,
                                modifier = itemModifier
                            )
                            1 -> HoopMetricCard(
                                label = "Reps",
                                value = uiState.reps?.toString() ?: "-",
                                accentColor = MaterialTheme.colorScheme.primary,
                                compact = compactBottomControls,
                                modifier = itemModifier
                            )
                            else -> HoopMetricCard(
                                label = "Rest",
                                value = uiState.restSeconds?.let { "${it}s" } ?: "-",
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                compact = compactBottomControls,
                                modifier = itemModifier
                            )
                        }
                    }
                    Text(
                        text = buildTargets(uiState),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (uiState.steps.isNotEmpty()) {
                item {
                    Text(
                        modifier = Modifier.responsiveContentWidth(windowInfo, tokens),
                        text = "Steps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(uiState.steps) { step ->
                    HoopCard(
                        modifier = Modifier.responsiveContentWidth(windowInfo, tokens),
                        contentPadding = PaddingValues(tokens.spacing.cardPadding)
                    ) {
                        Text(text = step)
                    }
                }
            }
            if (uiState.warnings.isNotEmpty()) {
                item {
                    Text(
                        modifier = Modifier.responsiveContentWidth(windowInfo, tokens),
                        text = "Warnings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(uiState.warnings) { warning ->
                    HoopStatusPanel(
                        modifier = Modifier.responsiveContentWidth(windowInfo, tokens),
                        title = "Warning",
                        message = warning,
                        status = HoopStatus.Error
                    )
                }
            }
            if (uiState.isLoading) {
                item {
                    HoopStatusPanel(
                        modifier = Modifier.responsiveContentWidth(windowInfo, tokens),
                        title = "Loading exercise",
                        message = "Pulling drill details and targets.",
                        status = HoopStatus.Active
                    )
                }
            }
            if (uiState.errorMessage != null) {
                item {
                    HoopStatusPanel(
                        modifier = Modifier.responsiveContentWidth(windowInfo, tokens),
                        title = "Load error",
                        message = uiState.errorMessage ?: "",
                        status = HoopStatus.Error
                    )
                }
            }
        }
    }
}

private fun buildTargets(uiState: ExerciseDetailUiState): String {
    val sets = uiState.sets?.toString() ?: "-"
    val reps = uiState.reps?.toString() ?: "-"
    val rest = uiState.restSeconds?.let { "${it}s" } ?: "-"
    return "$sets sets, $reps reps, $rest rest"
}
