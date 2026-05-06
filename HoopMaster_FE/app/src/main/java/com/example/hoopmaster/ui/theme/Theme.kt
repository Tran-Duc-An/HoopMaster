package com.example.hoopmaster.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun HoopMasterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HoopMasterColorScheme,
        typography = HoopMasterTypography,
        shapes = HoopMasterShapes,
        content = content
    )
}

