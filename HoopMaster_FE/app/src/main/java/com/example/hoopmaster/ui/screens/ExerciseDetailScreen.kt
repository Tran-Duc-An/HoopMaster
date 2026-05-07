package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.example.hoopmaster.ui.theme.ActiveOrange
import com.example.hoopmaster.viewmodels.ExerciseDetailAction
import com.example.hoopmaster.viewmodels.ExerciseDetailUiState
import com.example.hoopmaster.viewmodels.ExerciseDetailViewModel

@Composable
fun ExerciseDetailScreen(
    exerciseId: Int,
    onBack: () -> Unit,
    onStartTracking: () -> Unit,
    viewModel: ExerciseDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId)
    }

    HoopScreenScaffold(
        title = uiState.title.ifBlank { "Exercise" },
        onBack = onBack,
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                HoopActionButton(
                    text = "Start tracking",
                    icon = Icons.Outlined.PlayArrow,
                    onClick = {
                        viewModel.onAction(ExerciseDetailAction.StartExercise)
                        onStartTracking()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HoopStatusPanel(
                    title = uiState.category.ifBlank { "Shooting" },
                    message = uiState.description.ifBlank { "Loading exercise..." },
                    status = if (uiState.isLoading) HoopStatus.Active else HoopStatus.Info
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Targets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HoopMetricCard(
                            label = "Sets",
                            value = uiState.sets?.toString() ?: "-",
                            accentColor = ActiveOrange
                        )
                        HoopMetricCard(
                            label = "Reps",
                            value = uiState.reps?.toString() ?: "-",
                            accentColor = MaterialTheme.colorScheme.primary
                        )
                        HoopMetricCard(
                            label = "Rest",
                            value = uiState.restSeconds?.let { "${it}s" } ?: "-",
                            accentColor = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = buildTargets(uiState),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (uiState.steps.isNotEmpty()) {
                item {
                    Text(
                        text = "Steps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(uiState.steps) { step ->
                    HoopCard {
                        Text(text = step)
                    }
                }
            }
            if (uiState.warnings.isNotEmpty()) {
                item {
                    Text(
                        text = "Warnings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(uiState.warnings) { warning ->
                    HoopStatusPanel(
                        title = "Warning",
                        message = warning,
                        status = HoopStatus.Error
                    )
                }
            }
            if (uiState.isLoading) {
                item {
                    HoopStatusPanel(
                        title = "Loading exercise",
                        message = "Pulling drill details and targets.",
                        status = HoopStatus.Active
                    )
                }
            }
            if (uiState.errorMessage != null) {
                item {
                    HoopStatusPanel(
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
