package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackHome) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                }
                Text("Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Button(onClick = onBackHome, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Back home")
                }
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
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Shots", style = MaterialTheme.typography.labelLarge)
                    Text("${uiState.madeShots} / ${uiState.totalShots}", style = MaterialTheme.typography.headlineMedium)
                    Text("Duration ${uiState.durationSeconds}s")
                }
            }
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Feedback", style = MaterialTheme.typography.labelLarge)
                    Text(uiState.lastFeedback.ifBlank { "No feedback yet." })
                }
            }
            if (uiState.highlight.isNotBlank()) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        text = uiState.highlight,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
