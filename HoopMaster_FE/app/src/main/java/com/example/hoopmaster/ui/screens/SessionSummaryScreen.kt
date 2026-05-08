package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoopmaster.ui.components.HoopCard
import com.example.hoopmaster.ui.components.HoopMetricCard
import com.example.hoopmaster.ui.components.HoopPrimaryButton
import com.example.hoopmaster.ui.components.HoopScreenScaffold
import com.example.hoopmaster.ui.components.HoopStatus
import com.example.hoopmaster.ui.components.HoopStatusPanel
import com.example.hoopmaster.ui.responsive.ResponsiveMetricGrid
import com.example.hoopmaster.ui.responsive.rememberHoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.rememberHoopWindowInfo
import com.example.hoopmaster.ui.responsive.responsiveContentWidth
import com.example.hoopmaster.ui.theme.ActiveOrange
import com.example.hoopmaster.viewmodels.SessionSummaryAction
import com.example.hoopmaster.viewmodels.SessionSummaryViewModel

@Composable
fun SessionSummaryScreen(
    socketId: String?,
    onBackHome: () -> Unit,
    viewModel: SessionSummaryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val windowInfo = rememberHoopWindowInfo()
    val tokens = rememberHoopResponsiveTokens(windowInfo)
    val compactBottomControls = windowInfo.isLandscape || windowInfo.isSmallHeight

    LaunchedEffect(socketId) {
        viewModel.onAction(SessionSummaryAction.LoadSummary(socketId))
    }

    HoopScreenScaffold(
        title = "Summary",
        onBack = onBackHome,
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
                HoopPrimaryButton(
                    text = "Back home",
                    icon = Icons.Outlined.Home,
                    onClick = onBackHome,
                    compact = compactBottomControls,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = tokens.spacing.screenMargin, vertical = tokens.spacing.contentGap)
                .responsiveContentWidth(windowInfo, tokens),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap)) {
                Text("Session stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                ResponsiveMetricGrid(
                    itemCount = 4,
                    windowInfo = windowInfo
                ) { index, itemModifier ->
                    when (index) {
                        0 -> HoopMetricCard(
                            label = "Shots completed",
                            value = "${uiState.summary?.stats?.shotsCompleted ?: 0}",
                            accentColor = ActiveOrange,
                            compact = compactBottomControls,
                            modifier = itemModifier
                        )
                        1 -> HoopMetricCard(
                            label = "Feedback count",
                            value = "${uiState.summary?.stats?.feedbackCount ?: 0}",
                            accentColor = MaterialTheme.colorScheme.primary,
                            compact = compactBottomControls,
                            modifier = itemModifier
                        )
                        2 -> HoopMetricCard(
                            label = "Exercises done",
                            value = "${uiState.summary?.stats?.exercisesCompleted ?: 0}",
                            accentColor = MaterialTheme.colorScheme.secondary,
                            compact = compactBottomControls,
                            modifier = itemModifier
                        )
                        else -> HoopMetricCard(
                            label = "Uptime",
                            value = formatUptime(uiState.summary?.uptime),
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            compact = compactBottomControls,
                            modifier = itemModifier
                        )
                    }
                }
            }
            HoopCard(contentPadding = PaddingValues(tokens.spacing.cardPadding)) {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap)) {
                    Text("Session state", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    when {
                        uiState.isLoading -> Text("Loading summary...")
                        uiState.errorMessage != null -> Text(uiState.errorMessage ?: "")
                        uiState.isMissing -> Text("No session summary found for this session.")
                        uiState.summary == null -> Text("No session summary available.")
                        else -> Text("Summary loaded for socket ${uiState.socketId ?: "unknown"}.")
                    }
                }
            }
            if (uiState.errorMessage != null) {
                HoopStatusPanel(
                    title = "Error",
                    message = uiState.errorMessage ?: "",
                    status = HoopStatus.Error
                )
            }
        }
    }
}

private fun formatUptime(uptimeMs: Long?): String {
    val totalSeconds = ((uptimeMs ?: 0L) / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0L) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
}
