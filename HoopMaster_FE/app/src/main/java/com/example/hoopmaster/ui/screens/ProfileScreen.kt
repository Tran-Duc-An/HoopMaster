package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hoopmaster.ui.responsive.HoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.HoopWindowInfo
import com.example.hoopmaster.ui.responsive.rememberHoopResponsiveTokens
import com.example.hoopmaster.ui.responsive.rememberHoopWindowInfo
import com.example.hoopmaster.ui.responsive.responsiveContentWidth
import com.example.hoopmaster.viewmodels.ProfileAction
import com.example.hoopmaster.viewmodels.ProfileUiState
import com.example.hoopmaster.viewmodels.ProfileViewModel

import com.example.hoopmaster.ui.theme.*

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val windowInfo = rememberHoopWindowInfo()
    val tokens = rememberHoopResponsiveTokens(windowInfo)

    LaunchedEffect(Unit) {
        viewModel.loadProfile(null)
    }

    ProfileContent(
        uiState = uiState,
        onToneSelected = { tone -> viewModel.onAction(ProfileAction.ToneChanged(tone)) },
        onBack = onBack,
        onLogout = {
            viewModel.logout()
            onLogout()
        },
        windowInfo = windowInfo,
        tokens = tokens
    )
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onToneSelected: (String) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    windowInfo: HoopWindowInfo,
    tokens: HoopResponsiveTokens
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AthleticBackground)
    ) {
        ProfileBackground(modifier = Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            ProfileTopBar()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = tokens.spacing.screenMargin)
                    .padding(top = 56.dp, bottom = 116.dp)
                    .responsiveContentWidth(windowInfo, tokens),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                IdentityCard(uiState = uiState)
                CoachPersonaCard(
                    selectedTone = uiState.tone,
                    saving = uiState.toneSaving,
                    onToneSelected = onToneSelected
                )
                SettingsList()
                uiState.errorMessage?.let { error ->
                    ErrorStrip(message = error)
                }
                SignOutButton(onLogout = onLogout)
            }
        }

        ProfileBottomNav(
            onHome = onBack,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ProfileBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(NavyShadow, AthleticBackground, NavyShadow.copy(alpha = 0.5f))
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PrimaryContainer.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(size.width * 0.50f, size.height * 0.04f),
                radius = size.width * 0.78f
            ),
            radius = size.width * 0.78f,
            center = Offset(size.width * 0.50f, size.height * 0.04f)
        )
    }
}

@Composable
private fun ProfileTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLowest.copy(alpha = 0.90f))
            .statusBarsPadding()
            .border(1.dp, OutlineVariant.copy(alpha = 0.28f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Menu,
            contentDescription = null,
            tint = OnSurfaceVariant,
            modifier = Modifier.size(31.dp)
        )
        Text(
            text = "HOOPMASTER",
            style = athleticHeadline(34, italic = true).copy(fontWeight = FontWeight.Black),
            color = Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        MiniAvatar()
    }
}

@Composable
private fun MiniAvatar() {
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
private fun IdentityCard(uiState: ProfileUiState) {
    val displayName = uiState.displayName.takeIf { it.isNotBlank() }
        ?: uiState.email.takeIf { it.isNotBlank() }
        ?: uiState.userId?.takeLast(8)?.let { "Athlete $it" }
        ?: "HoopMaster Athlete"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface.copy(alpha = 0.96f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LargeAvatar()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = displayName,
                style = athleticHeadline(30),
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "PRO-ELITE TIER",
                style = technicalLabel(13),
                color = Primary
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = Secondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Account Verified",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LargeAvatar() {
    Box(
        modifier = Modifier
            .size(98.dp)
            .clip(CircleShape)
            .background(SurfaceHighest)
            .border(3.dp, PrimaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Primary.copy(alpha = 0.28f), SurfaceLowest),
                    center = Offset(size.width * 0.42f, size.height * 0.25f),
                    radius = size.width
                )
            )
        }
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(46.dp)
        )
    }
}

@Composable
private fun CoachPersonaCard(
    selectedTone: String,
    saving: Boolean,
    onToneSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface.copy(alpha = 0.96f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Psychology,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = if (saving) "AI COACH PERSONA • SAVING" else "AI COACH PERSONA",
                style = technicalLabel(13),
                color = OnSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PersonaOption(
                label = "Strict",
                value = "strict",
                icon = Icons.Filled.LocalFireDepartment,
                selected = selectedTone == "strict",
                enabled = !saving,
                onToneSelected = onToneSelected,
                modifier = Modifier.weight(1f)
            )
            PersonaOption(
                label = "Neutral",
                value = "neutral",
                icon = Icons.Outlined.Balance,
                selected = selectedTone == "neutral",
                enabled = !saving,
                onToneSelected = onToneSelected,
                modifier = Modifier.weight(1f)
            )
            PersonaOption(
                label = "Cheerful",
                value = "cheerful",
                icon = Icons.Outlined.Mood,
                selected = selectedTone == "cheerful",
                enabled = !saving,
                onToneSelected = onToneSelected,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = toneDescription(selectedTone),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontStyle = FontStyle.Italic,
                fontSize = 17.sp,
                lineHeight = 25.sp
            ),
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PersonaOption(
    label: String,
    value: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onToneSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) PrimaryContainer.copy(alpha = 0.10f) else SurfaceHigh)
            .border(
                width = 1.dp,
                color = if (selected) Primary else OutlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = { onToneSelected(value) })
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Primary else OnSurfaceVariant,
            modifier = Modifier.size(34.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label,
            style = technicalLabel(11),
            color = if (selected) Primary else OnSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun SettingsList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface.copy(alpha = 0.86f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.56f), RoundedCornerShape(18.dp))
    ) {
        SettingsRow(icon = Icons.Outlined.ManageAccounts, title = "Account Details")
        SettingsRow(icon = Icons.Outlined.Watch, title = "Connected Devices", trailing = "1 Active", trailingColor = Secondary)
        SettingsRow(icon = Icons.Outlined.Notifications, title = "Notifications")
        SettingsRow(icon = Icons.Outlined.Security, title = "Privacy & Data", showDivider = false)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    trailing: String? = null,
    trailingColor: Color = Outline,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 78.dp)
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 21.sp),
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailing != null) {
                Text(
                    text = trailing,
                    style = technicalLabel(12),
                    color = trailingColor
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Outline,
                modifier = Modifier.size(28.dp)
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OutlineVariant.copy(alpha = 0.44f))
            )
        }
    }
}

@Composable
private fun ErrorStrip(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Error.copy(alpha = 0.12f))
            .border(1.dp, Error.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = Error
    )
}

@Composable
private fun SignOutButton(onLogout: () -> Unit) {
    Button(
        onClick = onLogout,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 30.dp)
            .heightIn(min = 64.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = Primary
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Primary)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            modifier = Modifier.size(25.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "SIGN OUT",
            style = technicalLabel(13)
        )
    }
}

@Composable
private fun ProfileBottomNav(
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .background(SurfaceLowest.copy(alpha = 0.94f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.22f))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(label = "Home", icon = Icons.Filled.Home, selected = false, onClick = onHome)
        BottomNavItem(label = "Training", icon = Icons.Outlined.SportsBasketball, selected = false, enabled = false, onClick = {})
        BottomNavItem(label = "Analytics", icon = Icons.Outlined.Insights, selected = false, enabled = false, onClick = {})
        BottomNavItem(label = "Profile", icon = Icons.Outlined.Person, selected = true, onClick = {})
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val itemColor = when {
        selected -> OnPrimaryContainer
        enabled -> Outline
        else -> Outline.copy(alpha = 0.46f)
    }
    Column(
        modifier = Modifier
            .height(70.dp)
            .widthIn(min = 70.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) PrimaryContainer else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = itemColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = technicalLabel(11),
            color = itemColor,
            maxLines = 1
        )
    }
}

private fun toneDescription(tone: String): String {
    return when (tone.lowercase()) {
        "strict" -> "\"Pushing your limits with uncompromising feedback and intense motivation.\""
        "cheerful" -> "\"Keeping the session upbeat with positive cues and steady encouragement.\""
        else -> "\"Balanced coaching with clear corrections and calm performance guidance.\""
    }
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
