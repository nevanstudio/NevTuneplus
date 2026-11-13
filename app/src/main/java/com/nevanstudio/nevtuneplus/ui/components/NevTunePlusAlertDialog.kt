// Copyright (c) 2026 Sachit. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/** Shared NevTune+ styling for native Compose alert dialogs. */
@Composable
fun NevTunePlusAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    properties: DialogProperties = DialogProperties(),
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title?.let { content ->
            { ProvideTextStyle(MaterialTheme.typography.titleMedium, content = content) }
        },
        text = text?.let { content ->
            { ProvideTextStyle(MaterialTheme.typography.bodyMedium, content = content) }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = containerColor,
        iconContentColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp,
        properties = properties,
    )
}

@Deprecated("Use NevTunePlusAlertDialog instead", ReplaceWith("NevTunePlusAlertDialog(onDismissRequest, confirmButton, modifier, dismissButton, icon, title, text, containerColor, properties)"))
@Composable
fun AudifyAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    properties: DialogProperties = DialogProperties(),
) = NevTunePlusAlertDialog(
    onDismissRequest = onDismissRequest,
    confirmButton = confirmButton,
    modifier = modifier,
    dismissButton = dismissButton,
    icon = icon,
    title = title,
    text = text,
    containerColor = containerColor,
    properties = properties,
)
