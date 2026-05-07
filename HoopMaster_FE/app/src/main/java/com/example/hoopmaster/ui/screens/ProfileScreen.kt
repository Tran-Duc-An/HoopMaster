package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
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
import com.example.hoopmaster.ui.components.HoopFilterChip
import com.example.hoopmaster.ui.components.HoopScreenScaffold
import com.example.hoopmaster.ui.components.HoopStatus
import com.example.hoopmaster.ui.components.HoopStatusPanel
import com.example.hoopmaster.ui.components.HoopSecondaryButton
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

    HoopScreenScaffold(
        title = "Profile",
        onBack = onBack,
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                HoopSecondaryButton(
                    text = "Logout",
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    onClick = {
                        if (demoState == null) {
                            viewModel.logout()
                        }
                        onLogout()
                    },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HoopCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Profile summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("User ${uiState.userId ?: "guest"}")
                    if (uiState.displayName.isNotBlank()) {
                        Text(uiState.displayName)
                    }
                    if (uiState.email.isNotBlank()) {
                        Text(uiState.email)
                    }
                    Text("Tone ${uiState.tone}")
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Coach tone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("strict", "neutral", "cheerful").forEach { tone ->
                        HoopFilterChip(
                            label = tone.replaceFirstChar { it.uppercase() },
                            selected = uiState.tone == tone,
                            onClick = {
                                if (demoState == null) {
                                    viewModel.onAction(ProfileAction.ToneChanged(tone))
                                }
                            },
                            enabled = demoState == null
                        )
                    }
                }
            }
            if (uiState.toneSaving) {
                HoopStatusPanel(
                    title = "Saving tone",
                    message = "Coach voice update in progress.",
                    status = HoopStatus.Active
                )
            }
            if (uiState.errorMessage != null) {
                HoopStatusPanel(
                    title = "Profile error",
                    message = uiState.errorMessage ?: "",
                    status = HoopStatus.Error
                )
            }
        }
    }
}
