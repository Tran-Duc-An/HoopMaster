package com.example.hoopmaster.ui.responsive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hoopmaster.ui.theme.HoopSpacing

@Composable
fun ResponsiveScreenContainer(
    modifier: Modifier = Modifier,
    windowInfo: HoopWindowInfo = rememberHoopWindowInfo(),
    tokens: HoopResponsiveTokens = rememberHoopResponsiveTokens(windowInfo),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = tokens.spacing.screenMargin),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap),
        content = {
            Column(
                modifier = Modifier.responsiveContentWidth(windowInfo, tokens),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.contentGap),
                content = content
            )
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResponsiveActionRow(
    modifier: Modifier = Modifier,
    windowInfo: HoopWindowInfo = rememberHoopWindowInfo(),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(HoopSpacing.Sm),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(HoopSpacing.Sm),
    content: @Composable FlowRowScope.() -> Unit
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResponsiveMetricGrid(
    modifier: Modifier = Modifier,
    itemCount: Int,
    windowInfo: HoopWindowInfo = rememberHoopWindowInfo(),
    content: @Composable (index: Int, itemModifier: Modifier) -> Unit
) {
    val columns = when {
        windowInfo.phoneSizeClass == HoopPhoneSizeClass.Small -> 1
        windowInfo.phoneSizeClass == HoopPhoneSizeClass.Large && !windowInfo.isLandscape && windowInfo.width >= 430.dp -> 3
        else -> 2
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        maxItemsInEachRow = columns,
        horizontalArrangement = Arrangement.spacedBy(HoopSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(HoopSpacing.Sm)
    ) {
        repeat(itemCount) { index ->
            val itemModifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
            content(index, itemModifier)
        }
    }
}

fun Modifier.responsiveContentWidth(
    windowInfo: HoopWindowInfo,
    tokens: HoopResponsiveTokens
): Modifier {
    return this
        .fillMaxWidth()
        .widthIn(max = tokens.sizing.contentMaxWidth)
}
