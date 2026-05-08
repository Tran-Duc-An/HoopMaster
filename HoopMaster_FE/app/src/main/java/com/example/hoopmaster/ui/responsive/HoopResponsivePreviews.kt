package com.example.hoopmaster.ui.responsive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.hoopmaster.ui.components.HoopActionButton
import com.example.hoopmaster.ui.components.HoopCard
import com.example.hoopmaster.ui.components.HoopMetricCard
import com.example.hoopmaster.ui.components.HoopSecondaryButton
import com.example.hoopmaster.ui.components.HoopStatus
import com.example.hoopmaster.ui.components.HoopStatusPanel
import com.example.hoopmaster.ui.theme.HoopMasterTheme

@Composable
private fun HoopResponsivePreviewSurface() {
    val windowInfo = rememberHoopWindowInfo()
    val tokens = rememberHoopResponsiveTokens(windowInfo)

    ResponsiveScreenContainer(
        windowInfo = windowInfo,
        tokens = tokens
    ) {
        HoopStatusPanel(
            title = "Session Ready",
            message = "Adaptive layout active for current phone size.",
            status = HoopStatus.Active,
            modifier = Modifier.fillMaxWidth()
        )

        ResponsiveMetricGrid(
            itemCount = 3,
            windowInfo = windowInfo
        ) { index, itemModifier ->
            when (index) {
                0 -> HoopMetricCard(
                    label = "Accuracy",
                    value = "82%",
                    modifier = itemModifier,
                    compact = windowInfo.phoneSizeClass == HoopPhoneSizeClass.Small || windowInfo.isSmallHeight
                )
                1 -> HoopMetricCard(
                    label = "Makes",
                    value = "37",
                    modifier = itemModifier,
                    compact = windowInfo.phoneSizeClass == HoopPhoneSizeClass.Small || windowInfo.isSmallHeight
                )
                else -> HoopMetricCard(
                    label = "Streak",
                    value = "9",
                    modifier = itemModifier,
                    compact = windowInfo.phoneSizeClass == HoopPhoneSizeClass.Small || windowInfo.isSmallHeight
                )
            }
        }

        HoopCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            ResponsiveActionRow(
                windowInfo = windowInfo,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap)
            ) {
                HoopActionButton(
                    text = "Start Drill",
                    onClick = {},
                    compact = windowInfo.phoneSizeClass == HoopPhoneSizeClass.Small || windowInfo.isSmallHeight
                )
                HoopSecondaryButton(
                    text = "View Plan",
                    onClick = {},
                    compact = windowInfo.phoneSizeClass == HoopPhoneSizeClass.Small || windowInfo.isSmallHeight
                )
            }
        }
    }
}

@Preview(name = "Small Portrait Phone", widthDp = 320, heightDp = 568, showBackground = true)
@Composable
private fun PreviewSmallPortraitPhone() {
    HoopMasterTheme {
        HoopResponsivePreviewSurface()
    }
}

@Preview(name = "Standard Portrait Phone", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PreviewStandardPortraitPhone() {
    HoopMasterTheme {
        HoopResponsivePreviewSurface()
    }
}

@Preview(name = "Large Portrait Phone", widthDp = 430, heightDp = 932, showBackground = true)
@Composable
private fun PreviewLargePortraitPhone() {
    HoopMasterTheme {
        HoopResponsivePreviewSurface()
    }
}

@Preview(name = "Landscape Phone", widthDp = 800, heightDp = 360, showBackground = true)
@Composable
private fun PreviewLandscapePhone() {
    HoopMasterTheme {
        HoopResponsivePreviewSurface()
    }
}

@Preview(name = "Small-Height Landscape Phone", widthDp = 640, heightDp = 320, showBackground = true)
@Composable
private fun PreviewSmallHeightLandscapePhone() {
    HoopMasterTheme {
        HoopResponsivePreviewSurface()
    }
}
