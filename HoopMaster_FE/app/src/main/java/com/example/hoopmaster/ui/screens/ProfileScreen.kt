package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoopmaster.ui.components.HoopCard
import com.example.hoopmaster.ui.components.HoopFilterChip
import com.example.hoopmaster.ui.components.HoopScreenScaffold
import com.example.hoopmaster.ui.components.HoopStatus
import com.example.hoopmaster.ui.components.HoopStatusPanel
import com.example.hoopmaster.ui.components.HoopSecondaryButton
import com.example.hoopmaster.ui.responsive.rememberHoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.rememberHoopWindowInfo
import com.example.hoopmaster.ui.responsive.responsiveContentWidth
import com.example.hoopmaster.viewmodels.ProfileAction
import com.example.hoopmaster.viewmodels.ProfileViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val windowInfo = rememberHoopWindowInfo()
    val tokens = rememberHoopResponsiveTokens(windowInfo)
    val compactBottomBar = windowInfo.isLandscape || windowInfo.isSmallHeight

    LaunchedEffect(Unit) {
        viewModel.loadProfile(null)
    }

    HoopScreenScaffold(
        title = "Profile",
        onBack = onBack,
        windowInfo = windowInfo,
        bottomBar = {
            Column(
                modifier = Modifier.padding(
                    horizontal = tokens.spacing.bottomBarHorizontal,
                    vertical = tokens.spacing.bottomBarVertical
                )
            ) {
                HoopSecondaryButton(
                    text = "Logout",
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    onClick = {
                        viewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .responsiveContentWidth(windowInfo, tokens),
                    compact = compactBottomBar
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(
                    if (windowInfo.isSmallHeight) {
                        Modifier.verticalScroll(rememberScrollState())
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = tokens.spacing.screenMargin, vertical = tokens.spacing.contentGap),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap)
        ) {
            HoopCard(
                modifier = Modifier.responsiveContentWidth(windowInfo, tokens),
                contentPadding = PaddingValues(tokens.spacing.cardPadding)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap)) {
                    Text("Profile summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("User ${uiState.userId ?: "guest"}")
                }
            }
            Column(
                modifier = Modifier.responsiveContentWidth(windowInfo, tokens),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap)
            ) {
                Text("Coach tone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap)
                ) {
                    listOf("strict", "neutral", "cheerful").forEach { tone ->
                        HoopFilterChip(
                            label = tone.replaceFirstChar { it.uppercase() },
                            selected = uiState.tone == tone,
                            onClick = { viewModel.onAction(ProfileAction.ToneChanged(tone)) },
                            enabled = true
                        )
                    }
                }
            }
            if (uiState.toneSaving) {
                HoopStatusPanel(
                    title = "Saving tone",
                    message = "Coach voice update in progress.",
                    status = HoopStatus.Active,
                    modifier = Modifier.responsiveContentWidth(windowInfo, tokens)
                )
            }
            if (uiState.errorMessage != null) {
                HoopStatusPanel(
                    title = "Profile error",
                    message = uiState.errorMessage ?: "",
                    status = HoopStatus.Error,
                    modifier = Modifier.responsiveContentWidth(windowInfo, tokens)
                )
            }
        }
    }
}
