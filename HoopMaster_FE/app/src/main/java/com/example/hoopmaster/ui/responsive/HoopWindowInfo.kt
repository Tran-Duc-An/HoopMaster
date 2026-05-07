package com.example.hoopmaster.ui.responsive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class HoopOrientation { Portrait, Landscape }

enum class HoopPhoneSizeClass { Small, Standard, Large }

data class HoopWindowInfo(
    val width: Dp,
    val height: Dp,
    val orientation: HoopOrientation,
    val phoneSizeClass: HoopPhoneSizeClass,
    val fontScale: Float,
    val isLandscape: Boolean,
    val isSmallHeight: Boolean
)

@Composable
fun rememberHoopWindowInfo(): HoopWindowInfo {
    val configuration = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
    val width = configuration.screenWidthDp.dp
    val height = configuration.screenHeightDp.dp
    val isLandscape = width > height
    val orientation = if (isLandscape) HoopOrientation.Landscape else HoopOrientation.Portrait
    val phoneSizeClass = classifyPhoneSize(width = width, height = height)
    val isSmallHeight = height < 560.dp

    return remember(width, height, fontScale) {
        HoopWindowInfo(
            width = width,
            height = height,
            orientation = orientation,
            phoneSizeClass = phoneSizeClass,
            fontScale = fontScale,
            isLandscape = isLandscape,
            isSmallHeight = isSmallHeight
        )
    }
}

fun classifyPhoneSize(width: Dp, height: Dp): HoopPhoneSizeClass {
    val shortestSide = minOf(width, height)
    val isSmall = shortestSide < 360.dp || width < 360.dp
    if (isSmall) return HoopPhoneSizeClass.Small

    val isLandscape = width > height
    val isLarge = (!isLandscape && width >= 430.dp) || (isLandscape && height >= 430.dp)
    if (isLarge) return HoopPhoneSizeClass.Large

    return HoopPhoneSizeClass.Standard
}
