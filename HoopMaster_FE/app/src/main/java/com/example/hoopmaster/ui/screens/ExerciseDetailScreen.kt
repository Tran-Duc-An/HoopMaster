package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.hoopmaster.viewmodels.ExerciseDetailUiState
import com.example.hoopmaster.viewmodels.ExerciseDetailViewModel

@Composable
fun ExerciseDetailScreen(
    exerciseId: Int,
    onBack: () -> Unit,
    onStartTracking: () -> Unit,
    demoState: ExerciseDetailUiState? = null,
    viewModel: ExerciseDetailViewModel = viewModel()
) {
    val liveState by viewModel.uiState.collectAsState()
    val uiState = demoState ?: liveState

    LaunchedEffect(exerciseId, demoState) {
        if (demoState == null) {
            viewModel.loadExercise(exerciseId)
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = uiState.title.ifBlank { "Exercise" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        if (demoState == null) {
                            viewModel.onAction(com.example.hoopmaster.viewmodels.ExerciseDetailAction.StartExercise)
                        }
                        onStartTracking()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start tracking")
                }
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
                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = uiState.category.ifBlank { "Shooting" },
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = uiState.description.ifBlank { "Loading exercise..." },
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            item {
                DetailRow("Targets", buildTargets(uiState))
            }
            if (uiState.steps.isNotEmpty()) {
                item {
                    Text("Steps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(uiState.steps) { step ->
                    Card {
                        Text(
                            text = step,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            if (uiState.warnings.isNotEmpty()) {
                item {
                    Text("Warnings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(uiState.warnings) { warning ->
                    Surface(color = MaterialTheme.colorScheme.errorContainer) {
                        Text(
                            text = warning,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            if (uiState.isLoading) {
                item {
                    Text("Loading...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (uiState.errorMessage != null) {
                item {
                    Surface(color = MaterialTheme.colorScheme.errorContainer) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value)
    }
}

private fun buildTargets(uiState: ExerciseDetailUiState): String {
    val sets = uiState.sets?.toString() ?: "-"
    val reps = uiState.reps?.toString() ?: "-"
    val rest = uiState.restSeconds?.let { "${it}s" } ?: "-"
    return "$sets sets, $reps reps, $rest rest"
}
