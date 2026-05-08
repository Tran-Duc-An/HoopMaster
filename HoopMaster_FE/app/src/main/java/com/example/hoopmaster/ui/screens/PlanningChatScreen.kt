package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoopmaster.data.model.TrainingPlanDto
import com.example.hoopmaster.ui.components.HoopCard
import com.example.hoopmaster.ui.components.HoopFilterChip
import com.example.hoopmaster.ui.components.HoopOutlinedTextField
import com.example.hoopmaster.ui.components.HoopScreenScaffold
import com.example.hoopmaster.ui.components.HoopStatus
import com.example.hoopmaster.ui.components.HoopStatusBadge
import com.example.hoopmaster.ui.components.HoopStatusPanel
import com.example.hoopmaster.ui.responsive.HoopPhoneSizeClass
import com.example.hoopmaster.ui.responsive.HoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.HoopWindowInfo
import com.example.hoopmaster.ui.responsive.ResponsiveActionRow
import com.example.hoopmaster.ui.responsive.rememberHoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.rememberHoopWindowInfo
import com.example.hoopmaster.ui.responsive.responsiveContentWidth
import com.example.hoopmaster.ui.theme.ActiveOrange
import com.example.hoopmaster.ui.theme.HoopRadius
import com.example.hoopmaster.ui.theme.NavyShadow
import com.example.hoopmaster.viewmodels.PlanningChatAction
import com.example.hoopmaster.viewmodels.PlanningChatEntry
import com.example.hoopmaster.viewmodels.PlanningChatSession
import com.example.hoopmaster.viewmodels.PlanningChatViewModel

@Composable
fun PlanningChatScreen(
    onBack: () -> Unit,
    viewModel: PlanningChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val windowInfo = rememberHoopWindowInfo()
    val tokens = rememberHoopResponsiveTokens(windowInfo)
    val canSend = !uiState.isLoading && uiState.input.isNotBlank()
    val canConfirm = !uiState.isLoading && uiState.draftPlan != null

    LaunchedEffect(Unit) {
        viewModel.loadSessions()
    }

    HoopScreenScaffold(
        title = "Planning",
        onBack = onBack,
        windowInfo = windowInfo,
        bottomBar = {
            PlanningChatBottomBar(
                input = uiState.input,
                isInteractive = true,
                isLoading = uiState.isLoading,
                canSend = canSend,
                canConfirm = canConfirm,
                onInputChange = { viewModel.onAction(PlanningChatAction.InputChanged(it)) },
                onSend = {
                    if (canSend) {
                        viewModel.sendMessage()
                    }
                },
                onConfirm = {
                    if (canConfirm) {
                        viewModel.confirmPlan()
                    }
                },
                windowInfo = windowInfo,
                tokens = tokens
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = tokens.spacing.screenMargin)
                .responsiveContentWidth(windowInfo, tokens),
            contentPadding = PaddingValues(
                vertical = tokens.spacing.contentGap
            ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap)
        ) {
            item {
                SessionSelector(
                    sessions = uiState.sessions,
                    activeSessionId = uiState.activeSessionId,
                    isLoading = uiState.isLoading,
                    onSelectSession = { viewModel.onAction(PlanningChatAction.SelectSession(it)) },
                    onCreateSession = { viewModel.onAction(PlanningChatAction.CreateSession) }
                )
            }

            when {
                uiState.errorMessage != null -> item {
                    HoopStatusPanel(
                        title = "Problem",
                        message = uiState.errorMessage.orEmpty(),
                        status = HoopStatus.Error
                    )
                }

                uiState.isLoading -> item {
                    HoopStatusPanel(
                        title = "Working",
                        message = "Updating the planning chat and draft plan.",
                        status = HoopStatus.Active
                    )
                }

                uiState.statusMessage != null -> item {
                    HoopStatusPanel(
                        title = statusTitle(uiState.statusMessage.orEmpty()),
                        message = uiState.statusMessage.orEmpty(),
                        status = statusForMessage(uiState.statusMessage.orEmpty())
                    )
                }
            }

            if (uiState.messages.isEmpty()) {
                item {
                    HoopStatusPanel(
                        title = "Planning chat",
                        message = "Send a prompt to shape the next plan draft.",
                        status = HoopStatus.Info
                    )
                }
            } else {
                items(uiState.messages) { message ->
                    ChatBubble(
                        message = message,
                        windowInfo = windowInfo,
                        tokens = tokens
                    )
                    if (message.planDraft != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        DraftPlanCard(plan = message.planDraft)
                    }
                }
            }

            val draftPlan = uiState.draftPlan
            if (draftPlan != null && uiState.messages.none { it.planDraft?.id == draftPlan.id }) {
                item {
                    DraftPlanCard(plan = draftPlan)
                }
            }
        }
    }
}

@Composable
private fun SessionSelector(
    sessions: List<PlanningChatSession>,
    activeSessionId: String,
    isLoading: Boolean,
    onSelectSession: (String) -> Unit,
    onCreateSession: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Chat sessions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = sessionLabel(activeSessionId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onCreateSession,
                enabled = !isLoading,
                shape = RoundedCornerShape(HoopRadius.Full),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActiveOrange,
                    contentColor = NavyShadow,
                    disabledContainerColor = ActiveOrange.copy(alpha = 0.38f),
                    disabledContentColor = NavyShadow.copy(alpha = 0.38f)
                )
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "New")
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            items(sessions) { session ->
                HoopFilterChip(
                    label = sessionChipLabel(session),
                    selected = session.sessionId == activeSessionId,
                    enabled = !isLoading,
                    onClick = { onSelectSession(session.sessionId) }
                )
            }
        }
    }
}

@Composable
private fun PlanningChatBottomBar(
    input: String,
    isInteractive: Boolean,
    isLoading: Boolean,
    canSend: Boolean,
    canConfirm: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onConfirm: () -> Unit,
    windowInfo: HoopWindowInfo,
    tokens: HoopResponsiveTokens
) {
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding()
            .padding(
                horizontal = tokens.spacing.bottomBarHorizontal,
                vertical = tokens.spacing.bottomBarVertical
            ),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap)
    ) {
        HoopOutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            label = "Prompt",
            placeholder = "Ask for a plan update",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = isInteractive && !isLoading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = { if (canSend) onSend() }
            )
        )
        ResponsiveActionRow(windowInfo = windowInfo) {
            Button(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .weight(1f, fill = true)
                    .widthIn(min = 140.dp),
                shape = RoundedCornerShape(HoopRadius.Full),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyShadow,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = NavyShadow.copy(alpha = 0.38f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
                )
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Send")
            }

            Button(
                onClick = onConfirm,
                enabled = canConfirm,
                modifier = Modifier
                    .weight(1f, fill = true)
                    .widthIn(min = 140.dp),
                shape = RoundedCornerShape(HoopRadius.Full),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActiveOrange,
                    contentColor = NavyShadow,
                    disabledContainerColor = ActiveOrange.copy(alpha = 0.38f),
                    disabledContentColor = NavyShadow.copy(alpha = 0.38f)
                )
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Confirm")
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: PlanningChatEntry,
    windowInfo: HoopWindowInfo,
    tokens: HoopResponsiveTokens
) {
    val isUser = message.role.equals("user", ignoreCase = true)
    val containerColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(
                max = if (windowInfo.phoneSizeClass == HoopPhoneSizeClass.Large) {
                    tokens.sizing.contentMaxWidth * 0.82f
                } else {
                    tokens.sizing.contentMaxWidth * 0.9f
                }
            ),
            color = containerColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(HoopRadius.Lg)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isUser) "You" else "Assistant",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.78f)
                )
                Text(
                    text = message.text,
                    color = contentColor,
                    textAlign = if (isUser) TextAlign.End else TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun DraftPlanCard(plan: TrainingPlanDto) {
    HoopCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.title ?: "Draft plan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!plan.goal.isNullOrBlank()) {
                        Text(
                            text = plan.goal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                HoopStatusBadge(
                    label = planStatusLabel(plan),
                    status = planStatus(plan)
                )
            }

            if (!plan.description.isNullOrBlank()) {
                Text(
                    text = plan.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlanStat(
                    modifier = Modifier.weight(1f),
                    label = "Exercises",
                    value = plan.exercises?.size?.toString() ?: "0"
                )
            }

            if (!plan.injuryConstraints.isNullOrEmpty()) {
                Text(
                    text = "Constraints: ${plan.injuryConstraints.joinToString(", ")}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PlanStat(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun statusTitle(message: String): String {
    return when {
        message.startsWith("Saved ", ignoreCase = true) -> "Plan saved"
        message.equals("Draft ready", ignoreCase = true) -> "Draft ready"
        else -> "Status"
    }
}

private fun statusForMessage(message: String): HoopStatus {
    return when {
        message.startsWith("Saved ", ignoreCase = true) -> HoopStatus.Success
        message.equals("Draft ready", ignoreCase = true) -> HoopStatus.Active
        message.equals("New chat ready", ignoreCase = true) -> HoopStatus.Success
        else -> HoopStatus.Info
    }
}

private fun sessionChipLabel(session: PlanningChatSession): String {
    val base = sessionLabel(session.sessionId)
    return if (session.messageCount > 0) "$base (${session.messageCount})" else base
}

private fun sessionLabel(sessionId: String): String {
    if (sessionId == "default") return "Default chat"
    val parts = sessionId.split("-")
    return parts.drop(1).firstOrNull()?.takeIf { it.isNotBlank() }?.let { "Chat $it" } ?: "Chat"
}

private fun planStatusLabel(plan: TrainingPlanDto): String {
    return plan.status?.takeIf { it.isNotBlank() } ?: plan.metadata?.status?.takeIf { it.isNotBlank() } ?: "Draft"
}

private fun planStatus(plan: TrainingPlanDto): HoopStatus {
    val status = (plan.status ?: plan.metadata?.status).orEmpty()
    return when {
        status.equals("saved", ignoreCase = true) -> HoopStatus.Success
        status.equals("active", ignoreCase = true) -> HoopStatus.Active
        status.equals("error", ignoreCase = true) -> HoopStatus.Error
        else -> HoopStatus.Info
    }
}
