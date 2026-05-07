package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
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
import com.example.hoopmaster.ui.components.HoopCard
import com.example.hoopmaster.ui.components.HoopMetricCard
import com.example.hoopmaster.ui.components.HoopScreenScaffold
import com.example.hoopmaster.ui.components.HoopPrimaryButton
import com.example.hoopmaster.ui.components.HoopStatus
import com.example.hoopmaster.ui.components.HoopStatusPanel
import com.example.hoopmaster.ui.theme.ActiveOrange
import com.example.hoopmaster.viewmodels.SessionSummaryAction
import com.example.hoopmaster.viewmodels.SessionSummaryViewModel

@Composable
fun SessionSummaryScreen(
    onBackHome: () -> Unit,
    viewModel: SessionSummaryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onAction(
            SessionSummaryAction.LoadSummary(
                sessionId = null,
                totalShots = 15,
                madeShots = 12,
                durationSeconds = 60,
                lastFeedback = "Last shot looked clean."
            )
        )
    }

    HoopScreenScaffold(
        title = "Summary",
        onBack = onBackHome,
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                HoopPrimaryButton(
                    text = "Back home",
                    icon = Icons.Outlined.Home,
                    onClick = onBackHome,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Session stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                HoopMetricCard(
                    label = "Made shots",
                    value = "${uiState.madeShots}",
                    accentColor = ActiveOrange
                )
                HoopMetricCard(
                    label = "Total shots",
                    value = "${uiState.totalShots}",
                    accentColor = MaterialTheme.colorScheme.primary
                )
                HoopMetricCard(
                    label = "Duration",
                    value = "${uiState.durationSeconds}s",
                    accentColor = MaterialTheme.colorScheme.tertiary
                )
            }
            HoopCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Feedback", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(uiState.lastFeedback.ifBlank { "No feedback yet." })
                }
            }
            if (uiState.highlight.isNotBlank()) {
                HoopStatusPanel(
                    title = "Highlight",
                    message = uiState.highlight,
                    status = HoopStatus.Success
                )
            }
        }
    }
}
