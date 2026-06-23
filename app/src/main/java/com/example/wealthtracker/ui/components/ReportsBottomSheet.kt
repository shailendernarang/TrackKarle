package com.example.wealthtracker.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wealthtracker.data.local.InvestmentEntity
import com.example.wealthtracker.util.createCsvExport
import com.example.wealthtracker.util.createPdfExport
import com.example.wealthtracker.util.shareFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsBottomSheet(
    investments: List<InvestmentEntity>,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var isExporting by remember { mutableStateOf(false) }

    val count = investments.size
    val holdingLabel = if (count == 1) "Holding" else "Holdings"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Export Portfolio Report",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "$count $holdingLabel Available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            ExportCard(
                emoji = "📊",
                title = "CSV Spreadsheet",
                subtitle = "$count $holdingLabel · Portfolio Data",
                badge = ".CSV",
                badgeNote = "Excel & Sheets Compatible",
                accentColor = Color(0xFF16A34A),
                buttonLabel = "Export CSV",
                isLoading = isExporting,
                onClick = {
                    scope.launch {
                        isExporting = true
                        runCatching {
                            val file = withContext(Dispatchers.IO) { createCsvExport(ctx, investments) }
                            shareFile(ctx, file, "text/csv", "Export Portfolio CSV")
                        }.onFailure {
                            Toast.makeText(ctx, "Export failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                        isExporting = false
                    }
                }
            )

            ExportCard(
                emoji = "📄",
                title = "PDF Report",
                subtitle = "$count $holdingLabel · Summary Report",
                badge = ".PDF",
                badgeNote = "Printable Report",
                accentColor = Color(0xFF2563EB),
                buttonLabel = "Export PDF",
                isLoading = isExporting,
                onClick = {
                    scope.launch {
                        isExporting = true
                        runCatching {
                            val file = withContext(Dispatchers.IO) { createPdfExport(ctx, investments) }
                            shareFile(ctx, file, "application/pdf", "Export Portfolio Report")
                        }.onFailure {
                            Toast.makeText(ctx, "Export failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                        isExporting = false
                    }
                }
            )

            if (isExporting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Preparing export…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportCard(
    emoji: String,
    title: String,
    subtitle: String,
    badge: String,
    badgeNote: String,
    accentColor: Color,
    buttonLabel: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 22.sp)
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            badge,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    badgeNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor.copy(alpha = 0.8f)
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = if (!isLoading) 0.12f else 0.05f)
            ) {
                Text(
                    buttonLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (!isLoading) accentColor else accentColor.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
