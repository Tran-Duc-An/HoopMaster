package com.example.hoopmaster.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.hoopmaster.ui.theme.ActiveOrange
import com.example.hoopmaster.ui.theme.HoopElevation
import com.example.hoopmaster.ui.theme.HoopRadius
import com.example.hoopmaster.ui.theme.Success
import com.example.hoopmaster.ui.theme.SuccessContainer

enum class HoopStatus {
    Info,
    Active,
    Success,
    Error
}

private val HoopCardShape = RoundedCornerShape(HoopRadius.Lg)

@Composable
fun HoopCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        ElevatedCard(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = HoopCardShape,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = HoopElevation.Level1)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(
            modifier = modifier,
            shape = HoopCardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = HoopElevation.Level0)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun HoopMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.secondary
) {
    HoopCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (icon != null) {
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    contentColor = accentColor,
                    shape = RoundedCornerShape(HoopRadius.Md)
                ) {
                    Box(modifier = Modifier.padding(10.dp)) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null
                        )
                    }
                }
                Spacer(modifier = Modifier.size(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
fun HoopStatusPanel(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    status: HoopStatus = HoopStatus.Info
) {
    val colors = hoopStatusColors(status)
    Surface(
        modifier = modifier,
        shape = HoopCardShape,
        color = colors.container,
        contentColor = colors.content
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            HoopStatusBadge(label = status.name, status = status)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun HoopStatusBadge(
    label: String,
    status: HoopStatus,
    modifier: Modifier = Modifier
) {
    val colors = hoopStatusColors(status)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(HoopRadius.Full),
        color = colors.container,
        contentColor = colors.content
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private data class HoopStatusColors(
    val container: Color,
    val content: Color
)

@Composable
private fun hoopStatusColors(status: HoopStatus): HoopStatusColors {
    return when (status) {
        HoopStatus.Info -> HoopStatusColors(
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer
        )
        HoopStatus.Active -> HoopStatusColors(
            container = ActiveOrange.copy(alpha = 0.16f),
            content = ActiveOrange
        )
        HoopStatus.Success -> HoopStatusColors(
            container = SuccessContainer,
            content = Success
        )
        HoopStatus.Error -> HoopStatusColors(
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
