package com.example.wealthtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wealthtracker.data.Reminder
import com.example.wealthtracker.data.ReminderStatus
import com.example.wealthtracker.util.FormatUtils

enum class ReminderFilter {
    ACTIVE, SNOOZED, DISMISSED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    activeReminders: List<Reminder>,
    snoozedReminders: List<Reminder>,
    dismissedReminders: List<Reminder>,
    onGotIt: (Reminder) -> Unit,
    onRemindLater: (Reminder) -> Unit,
    onDismiss: (Reminder) -> Unit,
    onReactivate: (Reminder) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(ReminderFilter.ACTIVE) }
    
    val displayedReminders = when (selectedFilter) {
        ReminderFilter.ACTIVE -> activeReminders
        ReminderFilter.SNOOZED -> snoozedReminders
        ReminderFilter.DISMISSED -> dismissedReminders
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Reminders",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == ReminderFilter.ACTIVE,
                    onClick = { selectedFilter = ReminderFilter.ACTIVE },
                    label = { Text("Active (${activeReminders.size})") },
                    leadingIcon = {
                        if (selectedFilter == ReminderFilter.ACTIVE) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                
                FilterChip(
                    selected = selectedFilter == ReminderFilter.SNOOZED,
                    onClick = { selectedFilter = ReminderFilter.SNOOZED },
                    label = { Text("Snoozed (${snoozedReminders.size})") },
                    leadingIcon = {
                        if (selectedFilter == ReminderFilter.SNOOZED) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                
                FilterChip(
                    selected = selectedFilter == ReminderFilter.DISMISSED,
                    onClick = { selectedFilter = ReminderFilter.DISMISSED },
                    label = { Text("Done (${dismissedReminders.size})", maxLines = 1) },
                    leadingIcon = {
                        if (selectedFilter == ReminderFilter.DISMISSED) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // List of reminders
            if (displayedReminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (selectedFilter) {
                                ReminderFilter.ACTIVE -> Icons.Default.NotificationsNone
                                ReminderFilter.SNOOZED -> Icons.Default.Schedule
                                ReminderFilter.DISMISSED -> Icons.Default.CheckCircleOutline
                            },
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            when (selectedFilter) {
                                ReminderFilter.ACTIVE -> "No active reminders"
                                ReminderFilter.SNOOZED -> "No snoozed reminders"
                                ReminderFilter.DISMISSED -> "No completed reminders"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(displayedReminders, key = { it.id }) { reminder ->
                        ReminderListItem(
                            reminder = reminder,
                            filter = selectedFilter,
                            onGotIt = { onGotIt(reminder) },
                            onRemindLater = { onRemindLater(reminder) },
                            onDismiss = { onDismiss(reminder) },
                            onReactivate = { onReactivate(reminder) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderListItem(
    reminder: Reminder,
    filter: ReminderFilter,
    onGotIt: () -> Unit,
    onRemindLater: () -> Unit,
    onDismiss: () -> Unit,
    onReactivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysUntil = getDaysUntilMaturity(reminder.maturityDate)
    
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (reminder.investmentType) {
                            "FD" -> Icons.Default.AccountBalance
                            "Health Insurance" -> Icons.Default.HealthAndSafety
                            else -> Icons.Default.Notifications
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            reminder.investmentType,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (filter == ReminderFilter.SNOOZED) {
                            Text(
                                "Snoozed ${reminder.snoozeCount}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                if (filter == ReminderFilter.ACTIVE) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = getUrgencyColor(daysUntil)
                    ) {
                        Text(
                            reminder.message,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Investment details
            Text(
                reminder.investmentName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                FormatUtils.formatINR(reminder.amount),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(Modifier.height(4.dp))
            
            // Date
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (filter == ReminderFilter.DISMISSED) 
                        "Completed" 
                    else 
                        "Matures on ${reminder.maturityDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Actions based on filter
            Spacer(Modifier.height(12.dp))
            
            when (filter) {
                ReminderFilter.ACTIVE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onGotIt,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Got it")
                        }
                        
                        if (reminder.snoozeCount < 3) {
                            OutlinedButton(
                                onClick = onRemindLater,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Later")
                            }
                        }
                    }
                }
                ReminderFilter.SNOOZED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReactivate,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reactivate")
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Remove")
                        }
                    }
                }
                ReminderFilter.DISMISSED -> {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Remove from History")
                    }
                }
            }
        }
    }
}

@Composable
private fun getUrgencyColor(daysUntil: Int): Color {
    return when {
        daysUntil <= 7 -> Color(0xFFF44336) // Red
        daysUntil <= 30 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFF4CAF50) // Green
    }
}

private fun getDaysUntilMaturity(maturityDate: String): Int {
    return try {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH)
        val maturity = sdf.parse(maturityDate) ?: return Int.MAX_VALUE
        val today = java.util.Calendar.getInstance().time
        val diffInMillis = maturity.time - today.time
        (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    } catch (e: Exception) {
        Int.MAX_VALUE
    }
}
