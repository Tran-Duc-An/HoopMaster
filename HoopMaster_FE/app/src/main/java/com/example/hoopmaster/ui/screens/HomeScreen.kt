package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.hoopmaster.viewmodels.HomeAction
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

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "HOOPMASTER",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.activePlanTitle ?: "Default training plan",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Card(colors = CardDefaults.cardColors()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = uiState.activePlanTitle ?: "No plan loaded",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = uiState.activePlanDescription ?: "Training will fall back to the default plan when nothing is active.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onPersonalizePlan) {
                                Icon(Icons.Outlined.Edit, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Personalize")
                            }
                            Button(onClick = onStartShooting) {
                                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start session")
                            }
                            IconButton(onClick = onOpenProfile) {
                                Icon(Icons.Outlined.Person, contentDescription = "Profile")
                            }
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    Text(
                        text = "Loading plan...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
        }
    }
}

@Composable
private fun ExercisePlanCard(
    exercise: PlanExerciseDto,
    onClick: () -> Unit
) {
    val exerciseId = exercise.exerciseId ?: exercise.exercise?.id
    Card(
        modifier = Modifier.fillMaxWidth(),
        enabled = exerciseId != null,
        onClick = onClick,
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name ?: exercise.exercise?.name ?: "Exercise",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = exercise.category ?: exercise.exercise?.category ?: "Work"
                    )
                }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
            }
            Text(
                text = exercise.description ?: exercise.exercise?.description ?: "",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sets ${exercise.sets ?: exercise.exercise?.sets ?: "-"}")
                Text("Reps ${exercise.reps ?: exercise.exercise?.reps ?: "-"}")
                Text("Rest ${exercise.target?.restSeconds ?: exercise.exercise?.target?.restSeconds ?: "-"}s")
            }
            if (exerciseId == null) {
                Text(
                    text = "No exercise id",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
