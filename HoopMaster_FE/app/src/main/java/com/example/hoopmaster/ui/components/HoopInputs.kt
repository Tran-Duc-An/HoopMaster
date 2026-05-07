package com.example.hoopmaster.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.hoopmaster.ui.theme.ActiveOrange
import com.example.hoopmaster.ui.theme.HoopRadius

@Composable
fun HoopOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isLive: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val activeBorder = if (isLive) ActiveOrange else MaterialTheme.colorScheme.primary
    val unfocusedBorder = if (isLive) ActiveOrange else MaterialTheme.colorScheme.outline

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 56.dp),
        label = { Text(text = label) },
        placeholder = placeholder?.let { { Text(text = it) } },
        enabled = enabled,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(HoopRadius.Md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = activeBorder,
            unfocusedBorderColor = unfocusedBorder,
            focusedLabelColor = activeBorder,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = activeBorder,
            focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
