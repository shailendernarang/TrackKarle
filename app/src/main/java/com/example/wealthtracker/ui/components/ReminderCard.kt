package com.example.wealthtracker.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wealthtracker.data.Reminder
import com.example.wealthtracker.util.FormatUtils

@Composable
fun DashboardRemindersSection(
    reminders: List<Reminder>,
    onGotIt: (Reminder) -> Unit,
    onRemindLater: (Reminder) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (reminders.isEmpty()) return
    
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Upcoming Maturities",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            TextButton(onClick = onViewAll) {
                Text("View All")
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(reminders.take(3), key = { it.id }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onGotIt = { onGotIt(reminder) },
                    onRemindLater = { onRemindLater(reminder) }
                )
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    onGotIt: () -> Unit,
    onRemindLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showActions by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .width(300.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = getUrgencyColor(getDaysUntilMaturity(reminder.maturityDate)).copy(alpha = 0.1f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 2.dp,
            brush = androidx.compose.ui.graphics.SolidColor(
                getUrgencyColor(getDaysUntilMaturity(reminder.maturityDate))
            )
        )
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        reminder.investmentType,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Urgency badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = getUrgencyColor(getDaysUntilMaturity(reminder.maturityDate)),
                    tonalElevation = 0.dp
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
            
            Spacer(Modifier.height(8.dp))
            
            // Investment name & amount
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
            
            // Maturity date
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Matures on ${reminder.maturityDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onGotIt,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Got it", style = MaterialTheme.typography.labelMedium)
                }
                
                if (reminder.snoozeCount < 3) {
                    OutlinedButton(
                        onClick = onRemindLater,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Later", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun getUrgencyColor(daysUntil: Int): Color {
    return when {
        daysUntil <= 7 -> Color(0xFFF44336) // Red - Very urgent
        daysUntil <= 30 -> Color(0xFFFF9800) // Orange - Urgent
        else -> Color(0xFF4CAF50) // Green - Normal
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
