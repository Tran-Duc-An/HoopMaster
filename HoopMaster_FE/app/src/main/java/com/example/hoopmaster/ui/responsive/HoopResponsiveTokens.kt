package com.example.hoopmaster.ui.responsive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hoopmaster.ui.theme.HoopSpacing

data class HoopResponsiveSpacing(
    val screenMargin: Dp,
    val contentGap: Dp,
    val sectionGap: Dp,
    val cardPadding: Dp,
    val bottomBarHorizontal: Dp,
    val bottomBarVertical: Dp
)

data class HoopResponsiveSizing(
    val contentMaxWidth: Dp,
    val buttonMinHeight: Dp,
    val iconButtonSize: Dp,
    val logoSize: Dp,
    val trackingControlSize: Dp,
    val trackingMeterHeight: Dp
)

data class HoopResponsiveTokens(
    val spacing: HoopResponsiveSpacing,
    val sizing: HoopResponsiveSizing
)

@Composable
fun rememberHoopResponsiveTokens(
    windowInfo: HoopWindowInfo = rememberHoopWindowInfo()
): HoopResponsiveTokens {
    return remember(windowInfo) {
        val compact = windowInfo.phoneSizeClass == HoopPhoneSizeClass.Small || windowInfo.isSmallHeight
        val roomy = windowInfo.phoneSizeClass == HoopPhoneSizeClass.Large
        val landscapeCompact = windowInfo.isLandscape || windowInfo.isSmallHeight

        val spacing = HoopResponsiveSpacing(
            screenMargin = if (compact) HoopSpacing.Sm else HoopSpacing.ScreenMargin,
            contentGap = if (compact) HoopSpacing.Sm else HoopSpacing.Md,
            sectionGap = if (compact) HoopSpacing.Md else HoopSpacing.Section,
            cardPadding = if (compact) HoopSpacing.Sm else HoopSpacing.Md,
            bottomBarHorizontal = if (compact) HoopSpacing.Sm else HoopSpacing.Md,
            bottomBarVertical = if (compact) HoopSpacing.Xs else HoopSpacing.Sm
        )

        val sizing = HoopResponsiveSizing(
            contentMaxWidth = when {
                roomy -> 560.dp
                windowInfo.isLandscape -> 520.dp
                else -> 460.dp
            },
            buttonMinHeight = when {
                landscapeCompact -> 40.dp
                compact -> 44.dp
                else -> 48.dp
            },
            iconButtonSize = when {
                landscapeCompact -> 36.dp
                compact -> 40.dp
                else -> 44.dp
            },
            logoSize = when {
                landscapeCompact -> 44.dp
                compact -> 52.dp
                else -> 60.dp
            },
            trackingControlSize = when {
                landscapeCompact -> 92.dp
                compact -> 104.dp
                else -> 120.dp
            },
            trackingMeterHeight = when {
                landscapeCompact -> 140.dp
                compact -> 180.dp
                else -> 220.dp
            }
        )

        HoopResponsiveTokens(
            spacing = spacing,
            sizing = sizing
        )
    }
}
