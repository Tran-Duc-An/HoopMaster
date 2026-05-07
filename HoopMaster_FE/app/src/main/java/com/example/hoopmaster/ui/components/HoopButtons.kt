@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.hoopmaster.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.hoopmaster.ui.theme.ActiveOrange
import com.example.hoopmaster.ui.theme.HoopElevation
import com.example.hoopmaster.ui.theme.HoopRadius
import com.example.hoopmaster.ui.theme.NavyShadow

private val HoopButtonShape = androidx.compose.foundation.shape.RoundedCornerShape(HoopRadius.Full)
private val HoopButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
private val HoopButtonCompactPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

@Composable
fun HoopActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.sizeIn(minHeight = if (compact) 40.dp else 48.dp),
        enabled = enabled,
        shape = HoopButtonShape,
        contentPadding = if (compact) HoopButtonCompactPadding else HoopButtonPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = ActiveOrange,
            contentColor = NavyShadow,
            disabledContainerColor = ActiveOrange.copy(alpha = 0.38f),
            disabledContentColor = NavyShadow.copy(alpha = 0.38f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = HoopElevation.Level1),
        content = {
            HoopButtonContent(text = text, icon = icon)
        }
    )
}

@Composable
fun HoopPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.sizeIn(minHeight = if (compact) 40.dp else 48.dp),
        enabled = enabled,
        shape = HoopButtonShape,
        contentPadding = if (compact) HoopButtonCompactPadding else HoopButtonPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = HoopElevation.Level1),
        content = {
            HoopButtonContent(text = text, icon = icon)
        }
    )
}

@Composable
fun HoopSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.sizeIn(minHeight = if (compact) 40.dp else 48.dp),
        enabled = enabled,
        shape = HoopButtonShape,
        contentPadding = if (compact) HoopButtonCompactPadding else HoopButtonPadding,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = NavyShadow,
            disabledContentColor = NavyShadow.copy(alpha = 0.38f)
        ),
        border = BorderStroke(1.dp, if (enabled) NavyShadow else NavyShadow.copy(alpha = 0.38f)),
        content = {
            HoopButtonContent(text = text, icon = icon)
        }
    )
}

@Composable
fun HoopIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    when {
        emphasized -> FilledIconButton(
            onClick = onClick,
            modifier = modifier.size(size),
            enabled = true
        ) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }

        else -> FilledTonalIconButton(
            onClick = onClick,
            modifier = modifier.size(size),
            enabled = true
        ) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}

@Composable
private fun HoopButtonContent(
    text: String,
    icon: ImageVector?
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.sizeIn(minWidth = 8.dp))
        }
        Text(text = text)
    }
}
