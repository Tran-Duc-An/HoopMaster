package com.example.hoopmaster.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val Success = Color(0xFF0B7D3B)
val OnSuccess = Color(0xFFFFFFFF)
val SuccessContainer = Color(0xFFC8F7D6)
val ActiveOrange = Color(0xFFFF8F00)
val NavyShadow = Color(0xFF000666)

val HoopMasterLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF000666),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1A237E),
    onPrimaryContainer = Color(0xFF8690EE),
    inversePrimary = Color(0xFFBDC2FF),
    secondary = Color(0xFF8F4E00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFF8F00),
    onSecondaryContainer = Color(0xFF623400),
    tertiary = Color(0xFF001944),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF002C6E),
    onTertiaryContainer = Color(0xFF6B95F3),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF9F9F9),
    onBackground = Color(0xFF1A1C1C),
    surface = Color(0xFFF9F9F9),
    onSurface = Color(0xFF1A1C1C),
    surfaceVariant = Color(0xFFE2E2E2),
    onSurfaceVariant = Color(0xFF454652),
    outline = Color(0xFF767683),
    outlineVariant = Color(0xFFC6C5D4),
    inverseSurface = Color(0xFF2F3131),
    inverseOnSurface = Color(0xFFF1F1F1),
    surfaceTint = Color(0xFF4C56AF)
)

val HoopMasterColorScheme: ColorScheme = HoopMasterLightColorScheme
