package com.example.wealthtracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

enum class SnackbarType {
    SUCCESS, ERROR, WARNING, INFO
}

@Composable
fun ModernSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 16.dp // Add padding to position above FAB
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(bottom = bottomPadding),
        snackbar = { data ->
            ModernSnackbar(
                message = data.visuals.message,
                actionLabel = data.visuals.actionLabel,
                onActionClick = { data.performAction() },
                onDismiss = { data.dismiss() },
                type = determineSnackbarType(data.visuals.message)
            )
        }
    )
}

@Composable
private fun ModernSnackbar(
    message: String,
    actionLabel: String?,
    onActionClick: () -> Unit,
    onDismiss: () -> Unit,
    type: SnackbarType,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, contentColor, icon) = when (type) {
        SnackbarType.SUCCESS -> Triple(
            Color(0xFF4CAF50),
            Color.White,
            Icons.Default.CheckCircle
        )
        SnackbarType.ERROR -> Triple(
            Color(0xFFF44336),
            Color.White,
            Icons.Default.Error
        )
        SnackbarType.WARNING -> Triple(
            Color(0xFFFF9800),
            Color.White,
            Icons.Default.Warning
        )
        SnackbarType.INFO -> Triple(
            MaterialTheme.colorScheme.inverseSurface,
            MaterialTheme.colorScheme.inverseOnSurface,
            Icons.Default.Info
        )
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            
            Text(
                text = message,
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            
            if (actionLabel != null) {
                TextButton(
                    onClick = onActionClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = contentColor
                    )
                ) {
                    Text(
                        text = actionLabel,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = contentColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun determineSnackbarType(message: String): SnackbarType {
    return when {
        message.contains("successfully", ignoreCase = true) ||
        message.contains("added", ignoreCase = true) ||
        message.contains("saved", ignoreCase = true) ||
        message.contains("updated", ignoreCase = true) -> SnackbarType.SUCCESS
        
        message.contains("error", ignoreCase = true) ||
        message.contains("failed", ignoreCase = true) ||
        message.contains("invalid", ignoreCase = true) -> SnackbarType.ERROR
        
        message.contains("warning", ignoreCase = true) ||
        message.contains("caution", ignoreCase = true) -> SnackbarType.WARNING
        
        else -> SnackbarType.INFO
    }
}

// Extension functions for better snackbar messages
suspend fun SnackbarHostState.showSuccessSnackbar(message: String) {
    showSnackbar("✅ $message")
}

suspend fun SnackbarHostState.showErrorSnackbar(message: String) {
    showSnackbar("❌ $message")
}

suspend fun SnackbarHostState.showInfoSnackbar(message: String) {
    showSnackbar("ℹ️ $message")
}

suspend fun SnackbarHostState.showDeleteSnackbar(itemName: String, onUndo: (() -> Unit)? = null) {
    val result = showSnackbar(
        message = "🗑️ $itemName deleted successfully",
        actionLabel = if (onUndo != null) "UNDO" else null
    )
    if (result == SnackbarResult.ActionPerformed && onUndo != null) {
        onUndo()
    }
}
