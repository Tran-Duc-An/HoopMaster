package com.example.hoopmaster.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun HoopMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> HoopMasterDarkColorScheme
        else -> HoopMasterLightColorScheme
    }
    applyHoopThemeTokens(darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HoopMasterTypography,
        shapes = HoopMasterShapes,
        content = content
    )
}
