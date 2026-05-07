package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import com.example.hoopmaster.viewmodels.ProfileAction
import com.example.hoopmaster.viewmodels.ProfileUiState
import com.example.hoopmaster.viewmodels.ProfileViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    demoState: ProfileUiState? = null,
    viewModel: ProfileViewModel = viewModel()
) {
    val liveState by viewModel.uiState.collectAsState()
    val uiState = demoState ?: liveState

    LaunchedEffect(demoState) {
        if (demoState == null) {
            viewModel.loadProfile(null)
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
                Text("Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (demoState == null) {
                            viewModel.logout()
                        }
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Logout")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Coach tone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("strict", "neutral", "cheerful").forEach { tone ->
                            FilterChip(
                                selected = uiState.tone == tone,
                                onClick = {
                                    if (demoState == null) {
                                        viewModel.onAction(ProfileAction.ToneChanged(tone))
                                    }
                                },
                                enabled = demoState == null,
                                label = { Text(tone.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                    if (uiState.toneSaving) {
                        Text("Saving tone...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (uiState.errorMessage != null) {
                        Text(uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Session profile", style = MaterialTheme.typography.labelLarge)
                    Text("User ${uiState.userId ?: "guest"}")
                    Text("Tone ${uiState.tone}")
                }
            }
        }
    }
}
