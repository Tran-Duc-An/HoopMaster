package com.example.hoopmaster.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hoopmaster.R

@Composable
fun MainScreen(onNavigateToTracking: () -> Unit) {
    val logoId = R.drawable.hoopmaster_logo

    val surfaceLow = Color(0xFF1C1B1C)
    val surfaceHigh = Color(0xFF2A2A2B)
    val surfaceHighest = Color(0xFF353436)
    val electricOrange = Color(0xFFFF5F00)
    val courtGreen = Color(0xFF79FF5B)
    val technicalBlue = Color(0xFF00A2B9)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavigationBar(
                activeColor = electricOrange,
                inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            TopHeader(logoId = logoId, accentColor = electricOrange)
            Spacer(modifier = Modifier.height(16.dp))
            HeroCard(
                logoId = logoId,
                accentColor = electricOrange,
                onPrimaryAction = onNavigateToTracking
            )
            Spacer(modifier = Modifier.height(16.dp))
            StatusCardsRow(
                surfaceHigh = surfaceHigh,
                surfaceHighest = surfaceHighest,
                courtGreen = courtGreen,
                technicalBlue = technicalBlue
            )
            Spacer(modifier = Modifier.height(24.dp))
            HighlightsSection(
                surfaceLow = surfaceLow,
                surfaceHighest = surfaceHighest,
                accentColor = electricOrange,
                courtGreen = courtGreen,
                technicalBlue = technicalBlue,
                logoId = logoId
            )
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}

@Composable
private fun TopHeader(logoId: Int, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (logoId != 0) {
                Image(
                    painter = painterResource(id = logoId),
                    contentDescription = "HoopMaster logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "HOOPMASTER",
                style = MaterialTheme.typography.headlineLarge,
                color = accentColor,
                letterSpacing = (-0.5).sp
            )
        }
        Surface(
            shape = CircleShape,
            color = Color.Transparent
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun HeroCard(
    logoId: Int,
    accentColor: Color,
    onPrimaryAction: () -> Unit
) {
    val heroShape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(heroShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A0B),
                        Color(0xFF1C1B1C)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), heroShape)
    ) {
        if (logoId != 0) {
            Image(
                painter = painterResource(id = logoId),
                contentDescription = null,
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 40.dp)
                    .background(Color.Transparent),
                alpha = 0.2f
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "Ready to Work?",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Set up your camera and let AI track your form, accuracy, and mechanics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onPrimaryAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color(0xFF0E0E0F)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
            ) {
                Icon(
                    imageVector = Icons.Outlined.Videocam,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "START PRACTICE",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun StatusCardsRow(
    surfaceHigh: Color,
    surfaceHighest: Color,
    courtGreen: Color,
    technicalBlue: Color
) {
    Column {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "CURRENT STATUS",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Pro Level",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontStyle = FontStyle.Italic,
                            color = courtGreen
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.WorkspacePremium,
                        contentDescription = null,
                        tint = courtGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "XP: 8,450",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Next Tier: 10,000",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { 0.84f },
                    color = courtGreen,
                    trackColor = surfaceHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LAST SESSION SUMMARY",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = technicalBlue.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Today, 8:00 AM",
                            style = MaterialTheme.typography.labelLarge,
                            color = technicalBlue,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "18/25",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Shots Made",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(
                            text = "72%",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = courtGreen
                        )
                        Text(
                            text = "Accuracy",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightsSection(
    surfaceLow: Color,
    surfaceHighest: Color,
    accentColor: Color,
    courtGreen: Color,
    technicalBlue: Color,
    logoId: Int
) {
    Text(
        text = "Recent Highlights",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(12.dp))
    HighlightItem(
        title = "Perfect Swish Streak",
        subtitle = "3-Point Line • 5 in a row",
        icon = Icons.Outlined.Star,
        iconColor = technicalBlue,
        surfaceLow = surfaceLow,
        surfaceHighest = surfaceHighest,
        accentColor = accentColor,
        logoId = logoId
    )
    Spacer(modifier = Modifier.height(12.dp))
    HighlightItem(
        title = "Form Correction: Elbow",
        subtitle = "Free Throw • AI Analysis",
        icon = Icons.Outlined.AutoGraph,
        iconColor = courtGreen,
        surfaceLow = surfaceLow,
        surfaceHighest = surfaceHighest,
        accentColor = accentColor,
        logoId = logoId
    )
}

@Composable
private fun HighlightItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    surfaceLow: Color,
    surfaceHighest: Color,
    accentColor: Color,
    logoId: Int
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceHighest),
                contentAlignment = Alignment.Center
            ) {
                if (logoId != 0) {
                    Image(
                        painter = painterResource(id = logoId),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        alpha = 0.6f
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor
            )
        }
    }
}

@Composable
private fun BottomNavigationBar(activeColor: Color, inactiveColor: Color) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                label = "HOME",
                icon = Icons.Outlined.Home,
                selected = true,
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )
            BottomNavItem(
                label = "HISTORY",
                icon = Icons.Outlined.Insights,
                selected = false,
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )
            BottomNavItem(
                label = "PROFILE",
                icon = Icons.Outlined.Person,
                selected = false,
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )
            BottomNavItem(
                label = "SETTINGS",
                icon = Icons.Outlined.Settings,
                selected = false,
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    activeColor: Color,
    inactiveColor: Color
) {
    val background = if (selected) activeColor else Color.Transparent
    val contentColor = if (selected) Color(0xFF0E0E0F) else inactiveColor
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}
