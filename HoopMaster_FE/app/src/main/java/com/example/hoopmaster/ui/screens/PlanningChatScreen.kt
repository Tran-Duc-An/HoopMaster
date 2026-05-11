package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoopmaster.data.model.PlanExerciseDto
import com.example.hoopmaster.data.model.TrainingPlanDto
import com.example.hoopmaster.ui.responsive.HoopPhoneSizeClass
import com.example.hoopmaster.ui.responsive.HoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.HoopWindowInfo
import com.example.hoopmaster.ui.responsive.rememberHoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.rememberHoopWindowInfo
import com.example.hoopmaster.ui.responsive.responsiveContentWidth
import com.example.hoopmaster.viewmodels.PlanningChatAction
import com.example.hoopmaster.viewmodels.PlanningChatEntry
import com.example.hoopmaster.viewmodels.PlanningChatSession
import com.example.hoopmaster.viewmodels.PlanningChatViewModel

private val AthleticBackground = Color(0xFF1E100C)
private val SurfaceLowest = Color(0xFF180B07)
private val SurfaceLow = Color(0xFF271813)
private val Surface = Color(0xFF2C1C17)
private val SurfaceHigh = Color(0xFF372621)
private val SurfaceHighest = Color(0xFF43302B)
private val Primary = Color(0xFFFFB5A0)
private val PrimaryContainer = Color(0xFFFF5722)
private val Secondary = Color(0xFF78DC77)
private val OnSurface = Color(0xFFFADCD4)
private val OnSurfaceVariant = Color(0xFFE4BEB4)
private val Outline = Color(0xFFAB8980)
private val OutlineVariant = Color(0xFF5B4039)
private val OnPrimaryContainer = Color(0xFF541200)
private val Error = Color(0xFFFFB4AB)

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AthleticBackground)
    ) {
        ChatBackground(modifier = Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            PlanningTopBar(onBack = onBack)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = tokens.spacing.screenMargin)
                    .responsiveContentWidth(windowInfo, tokens),
                contentPadding = PaddingValues(top = 18.dp, bottom = 126.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    SessionStrip(
                        sessions = uiState.sessions,
                        activeSessionId = uiState.activeSessionId,
                        isLoading = uiState.isLoading,
                        onSelectSession = { viewModel.onAction(PlanningChatAction.SelectSession(it)) },
                        onCreateSession = { viewModel.onAction(PlanningChatAction.CreateSession) }
                    )
                }

                item {
                    TimePill(
                        text = when {
                            uiState.isLoading -> "WORKING"
                            uiState.statusMessage != null -> uiState.statusMessage.orEmpty().uppercase()
                            else -> "TODAY"
                        },
                        error = uiState.errorMessage != null
                    )
                }

                if (uiState.errorMessage != null) {
                    item {
                        StatusBubble(
                            title = "Problem",
                            message = uiState.errorMessage.orEmpty(),
                            error = true
                        )
                    }
                } else if (uiState.messages.isEmpty() && !uiState.isLoading) {
                    item {
                        ChatBubble(
                            message = PlanningChatEntry(
                                id = "prompt",
                                role = "assistant",
                                text = "Ready to lock in. What specific skills or situations do you want to attack in today's session?"
                            ),
                            windowInfo = windowInfo,
                            tokens = tokens
                        )
                    }
                }

                items(uiState.messages) { message ->
                    ChatBubble(
                        message = message,
                        windowInfo = windowInfo,
                        tokens = tokens
                    )
                    val planDraft = message.planDraft
                    if (planDraft != null) {
                        DraftPlanCard(
                            plan = planDraft,
                            canConfirm = canConfirm && planDraft.id == uiState.draftPlan?.id,
                            isLoading = uiState.isLoading,
                            onConfirm = {
                                if (canConfirm) {
                                    viewModel.confirmPlan()
                                }
                            }
                        )
                    }
                }

                if (uiState.isLoading) {
                    item {
                        StatusBubble(
                            title = "AI Coach",
                            message = "Curating the next training draft.",
                            error = false
                        )
                    }
                }

                val draftPlan = uiState.draftPlan
                if (draftPlan != null && uiState.messages.none { it.planDraft?.id == draftPlan.id }) {
                    item {
                        DraftPlanCard(
                            plan = draftPlan,
                            canConfirm = canConfirm,
                            isLoading = uiState.isLoading,
                            onConfirm = {
                                if (canConfirm) {
                                    viewModel.confirmPlan()
                                }
                            }
                        )
                    }
                }
            }
        }

        PlanningInputBar(
            input = uiState.input,
            isLoading = uiState.isLoading,
            canSend = canSend,
            onInputChange = { viewModel.onAction(PlanningChatAction.InputChanged(it)) },
            onSend = {
                if (canSend) {
                    viewModel.sendMessage()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
            tokens = tokens
        )
    }
}

@Composable
private fun ChatBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF23100B), AthleticBackground, Color(0xFF130604))
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PrimaryContainer.copy(alpha = 0.11f), Color.Transparent),
                center = Offset(size.width * 0.50f, size.height * 0.18f),
                radius = size.width * 0.85f
            ),
            radius = size.width * 0.85f,
            center = Offset(size.width * 0.50f, size.height * 0.18f)
        )
    }
}

@Composable
private fun PlanningTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLowest.copy(alpha = 0.88f))
            .statusBarsPadding()
            .border(1.dp, OutlineVariant.copy(alpha = 0.26f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Menu,
            contentDescription = "Back",
            tint = OnSurfaceVariant,
            modifier = Modifier
                .size(30.dp)
                .clickable(onClick = onBack)
        )
        Text(
            text = "HOOPMASTER",
            style = athleticHeadline(34, italic = true).copy(fontWeight = FontWeight.Black),
            color = Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        AthleteAvatar()
    }
}

@Composable
private fun AthleteAvatar() {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(SurfaceHighest)
            .border(1.dp, OutlineVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.SportsBasketball,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SessionStrip(
    sessions: List<PlanningChatSession>,
    activeSessionId: String,
    isLoading: Boolean,
    onSelectSession: (String) -> Unit,
    onCreateSession: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sessionLabel(activeSessionId).uppercase(),
                style = technicalLabel(12),
                color = Primary
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Surface)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.50f), RoundedCornerShape(999.dp))
                    .clickable(enabled = !isLoading, onClick = onCreateSession)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "NEW",
                    style = technicalLabel(11),
                    color = Primary
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sessions) { session ->
                SessionChip(
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
private fun SessionChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Primary.copy(alpha = 0.16f) else SurfaceLow)
            .border(
                width = 1.dp,
                color = if (selected) Primary.copy(alpha = 0.70f) else OutlineVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            style = technicalLabel(10),
            color = if (selected) Primary else OnSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun TimePill(
    text: String,
    error: Boolean
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (error) Error.copy(alpha = 0.16f) else SurfaceHigh)
                .border(1.dp, OutlineVariant.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
                .padding(horizontal = 18.dp, vertical = 8.dp),
            style = technicalLabel(12),
            color = if (error) Error else OnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusBubble(
    title: String,
    message: String,
    error: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        AssistantMark()
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .widthIn(max = 470.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp))
                .background(if (error) Error.copy(alpha = 0.12f) else SurfaceLow)
                .border(
                    1.dp,
                    if (error) Error.copy(alpha = 0.34f) else OutlineVariant.copy(alpha = 0.28f),
                    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp)
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title.uppercase(), style = technicalLabel(11), color = if (error) Error else Primary)
            Text(text = message, style = MaterialTheme.typography.bodyLarge, color = OnSurface)
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
    val maxWidth = if (windowInfo.phoneSizeClass == HoopPhoneSizeClass.Large) {
        tokens.sizing.contentMaxWidth * 0.84f
    } else {
        tokens.sizing.contentMaxWidth * 0.9f
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            AssistantMark()
            Spacer(modifier = Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp
                    )
                )
                .background(if (isUser) PrimaryContainer else SurfaceLow)
                .border(
                    width = 1.dp,
                    color = if (isUser) PrimaryContainer else OutlineVariant.copy(alpha = 0.26f),
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp
                    )
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 31.sp),
                color = if (isUser) OnPrimaryContainer else OnSurface,
                textAlign = if (isUser) TextAlign.Start else TextAlign.Start
            )
        }
    }
}

@Composable
private fun AssistantMark() {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(SurfaceHigh)
            .border(1.dp, OutlineVariant.copy(alpha = 0.40f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.SmartToy,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun DraftPlanCard(
    plan: TrainingPlanDto,
    canConfirm: Boolean,
    isLoading: Boolean,
    onConfirm: () -> Unit
) {
    val modules = plan.exercises.orEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 46.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceHigh.copy(alpha = 0.78f))
            .border(1.dp, Primary.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.FlashOn,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "AI GENERATED DRAFT",
                        style = technicalLabel(12),
                        color = Primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (plan.title ?: "Draft Plan").uppercase(),
                    style = athleticHeadline(28, italic = true).copy(lineHeight = 32.sp),
                    color = OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            DurationMetric(plan = plan)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlanMetricTile(
                label = "Intensity",
                value = estimateIntensity(modules),
                valueColor = Primary,
                modifier = Modifier.weight(1f)
            )
            PlanMetricTile(
                label = "Modules",
                value = modules.size.toString(),
                valueColor = OnSurface,
                modifier = Modifier.weight(1f)
            )
        }

        if (!plan.description.isNullOrBlank()) {
            Text(
                text = plan.description,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            modules.take(3).forEachIndexed { index, exercise ->
                DrillPreviewRow(
                    exercise = exercise,
                    accent = if (index == 0) Secondary else Primary
                )
            }
            if (modules.size > 3) {
                Text(
                    text = "VIEW FULL PLAN  +${modules.size - 3} MORE",
                    modifier = Modifier.fillMaxWidth(),
                    style = technicalLabel(11),
                    color = Primary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Button(
            onClick = onConfirm,
            enabled = canConfirm && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = Color(0xFF3B0900),
                disabledContainerColor = Primary.copy(alpha = 0.35f),
                disabledContentColor = Color(0xFF3B0900).copy(alpha = 0.50f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "CONFIRM & SAVE PLAN", style = technicalLabel(12))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun DurationMetric(plan: TrainingPlanDto) {
    val duration = plan.schedule?.sessionDurationMinutes ?: estimateDurationMinutes(plan.exercises.orEmpty())
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = duration.toString(),
            style = athleticHeadline(46, italic = true).copy(lineHeight = 46.sp),
            color = OnSurface
        )
        Text(
            text = "MIN",
            modifier = Modifier.padding(top = 8.dp),
            style = technicalLabel(11),
            color = OnSurfaceVariant
        )
    }
}

@Composable
private fun PlanMetricTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceLowest.copy(alpha = 0.82f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(text = label.uppercase(), style = technicalLabel(11), color = OnSurfaceVariant)
        Text(text = value.uppercase(), style = athleticHeadline(20), color = valueColor)
    }
}

@Composable
private fun DrillPreviewRow(
    exercise: PlanExerciseDto,
    accent: Color
) {
    val name = exercise.name ?: exercise.exercise?.name ?: "Training module"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Surface.copy(alpha = 0.76f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = moduleDuration(exercise),
            style = technicalLabel(10),
            color = OnSurfaceVariant
        )
    }
}

@Composable
private fun PlanningInputBar(
    input: String,
    isLoading: Boolean,
    canSend: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    tokens: HoopResponsiveTokens
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceLowest.copy(alpha = 0.94f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.34f))
            .navigationBarsPadding()
            .imePadding()
            .padding(
                horizontal = tokens.spacing.bottomBarHorizontal,
                vertical = 14.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 58.dp),
            enabled = !isLoading,
            singleLine = true,
            placeholder = {
                Text(
                    text = "Tweak this plan...",
                    color = Outline
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = OnSurface, fontSize = 18.sp),
            shape = RoundedCornerShape(999.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface,
                disabledContainerColor = Surface.copy(alpha = 0.58f),
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface,
                disabledTextColor = OnSurface.copy(alpha = 0.42f),
                cursorColor = Primary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(if (canSend) PrimaryContainer else SurfaceHigh)
                .clickable(enabled = canSend, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (canSend) OnPrimaryContainer else Outline,
                modifier = Modifier.size(29.dp)
            )
        }
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

private fun estimateDurationMinutes(exercises: List<PlanExerciseDto>): Int {
    if (exercises.isEmpty()) return 45
    return (exercises.size * 9).coerceIn(30, 75)
}

private fun estimateIntensity(exercises: List<PlanExerciseDto>): String {
    val totalSets = exercises.sumOf { it.sets ?: it.exercise?.sets ?: it.target?.sets ?: 0 }
    return when {
        totalSets >= 16 -> "High"
        totalSets >= 8 -> "Med"
        else -> "Base"
    }
}

private fun moduleDuration(exercise: PlanExerciseDto): String {
    exercise.duration?.takeIf { it.isNotBlank() }?.let { return it.uppercase() }
    val reps = exercise.reps ?: exercise.exercise?.reps ?: exercise.target?.reps
    return reps?.let { "$it REPS" } ?: "MODULE"
}

private fun athleticHeadline(size: Int, italic: Boolean = false): TextStyle {
    return TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        fontSize = size.sp,
        lineHeight = (size + 4).sp
    )
}

private fun technicalLabel(size: Int): TextStyle {
    return TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = size.sp,
        lineHeight = (size + 4).sp,
        letterSpacing = 1.1.sp
    )
}
