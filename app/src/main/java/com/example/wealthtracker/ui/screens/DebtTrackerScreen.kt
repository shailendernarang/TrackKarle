package com.example.wealthtracker.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.wealthtracker.data.local.DebtEntity
import com.example.wealthtracker.ui.DebtViewModel
import com.example.wealthtracker.ui.components.AmountTextField
import com.example.wealthtracker.util.FormatUtils
import com.example.wealthtracker.util.XirrCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val DEBT_TYPES = listOf("Home Loan", "Personal Loan", "Car Loan", "Credit Card", "Other")
private fun isCreditCard(type: String) = type == "Credit Card"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtTrackerScreen(
    onBack: () -> Unit,
    autoOpenAdd: Boolean = false,
    viewModel: DebtViewModel = hiltViewModel()
) {
    val debts by viewModel.debts.collectAsState()
    val message by viewModel.message.collectAsState()
    var editingDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (autoOpenAdd) showAddSheet = true
    }
    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it); viewModel.consumeMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debt Tracker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Debt")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (debts.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                DebtSummaryCard(debts = debts)
                Spacer(Modifier.height(12.dp))
            }

            if (debts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text("No debts tracked", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Track loans, credit cards & more", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        Spacer(Modifier.height(4.dp))
                        Button(onClick = { showAddSheet = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Debt")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(debts, key = { it.id }) { debt ->
                        SwipeToDeleteDebtCard(
                            debt = debt,
                            onDelete = { viewModel.delete(debt) },
                            onEdit = { editingDebt = debt }
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        DebtFormSheet(
            title = "Add Debt",
            initial = null,
            onDismiss = { showAddSheet = false },
            onSave = { entity ->
                viewModel.addDebt(
                    entity.name, entity.debtType, entity.principalAmount,
                    entity.outstandingBalance, entity.interestRate, entity.emiAmount,
                    entity.startDate, entity.tenureMonths
                )
                showAddSheet = false
            }
        )
    }

    editingDebt?.let { debt ->
        DebtFormSheet(
            title = "Edit Debt",
            initial = debt,
            onDismiss = { editingDebt = null },
            onSave = { updated ->
                viewModel.updateDebt(updated.copy(id = debt.id, createdAt = debt.createdAt))
                editingDebt = null
            }
        )
    }
}

// ── Summary ─────────────────────────────────────────────────────────────────

@Composable
private fun DebtSummaryCard(debts: List<DebtEntity>) {
    val totalOutstanding = debts.sumOf { it.outstandingBalance }
    val loans = debts.filter { !isCreditCard(it.debtType) }
    val cards = debts.filter { isCreditCard(it.debtType) }
    val totalEmi = loans.sumOf { it.emiAmount }
    val totalMinDue = cards.sumOf { it.emiAmount }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column {
                Text("Total Outstanding", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                Text(FormatUtils.formatINR(totalOutstanding), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.15f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (totalEmi > 0) SummaryItem("Monthly EMI", FormatUtils.formatINR(totalEmi), MaterialTheme.colorScheme.onErrorContainer)
                if (totalMinDue > 0) SummaryItem("Min Due", FormatUtils.formatINR(totalMinDue), MaterialTheme.colorScheme.onErrorContainer)
                SummaryItem("Debts", FormatUtils.formatInt(debts.size), MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

// ── Card ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteDebtCard(
    debt: DebtEntity,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                label = "swipe_bg"
            )
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(color),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(end = 16.dp))
            }
        }
    ) {
        if (isCreditCard(debt.debtType)) CreditCardCard(debt = debt, onEdit = onEdit, onDelete = onDelete)
        else LoanCard(debt = debt, onEdit = onEdit, onDelete = onDelete)
    }
}

@Composable
private fun LoanCard(debt: DebtEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val now = LocalDate.now()
    val startDate = XirrCalculator.parseDate(debt.startDate.takeIf { it.isNotBlank() })
    val monthsElapsed = if (startDate != null) ChronoUnit.MONTHS.between(startDate, now).toInt().coerceAtLeast(0) else 0
    val monthsRemaining = if (debt.tenureMonths > 0) (debt.tenureMonths - monthsElapsed).coerceAtLeast(0) else null
    val totalInterestRemaining = if (monthsRemaining != null && monthsRemaining > 0 && debt.interestRate > 0)
        debt.outstandingBalance * (debt.interestRate / 1200.0) * monthsRemaining else 0.0
    val paidFraction = if (debt.principalAmount > 0)
        (1f - (debt.outstandingBalance / debt.principalAmount).toFloat()).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(debt.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SuggestionChip(onClick = {}, label = { Text(debt.debtType, style = MaterialTheme.typography.labelSmall) })
                        if (debt.interestRate > 0)
                            AssistChip(onClick = {}, label = { Text(String.format(Locale.ENGLISH, "%.1f%% p.a.", debt.interestRate), style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("outstanding", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FormatUtils.formatINR(debt.outstandingBalance), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }

            if (debt.principalAmount > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Paid off", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.ENGLISH, "%.0f%%", paidFraction * 100), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                    }
                    LinearProgressIndicator(
                        progress = { paidFraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.errorContainer
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (debt.emiAmount > 0) {
                    Column {
                        Text("Monthly Payment", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(FormatUtils.formatINR(debt.emiAmount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (monthsRemaining != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${FormatUtils.formatInt(monthsRemaining)} months", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (totalInterestRemaining > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    "Est. total interest remaining: ${FormatUtils.formatINR(totalInterestRemaining)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun CreditCardCard(debt: DebtEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    // principalAmount = credit limit, emiAmount = min due, tenureMonths = billing due day
    val creditLimit = debt.principalAmount
    val utilization = if (creditLimit > 0) (debt.outstandingBalance / creditLimit).toFloat().coerceIn(0f, 1f) else 0f
    val utilizationColor = when {
        utilization > 0.75f -> MaterialTheme.colorScheme.error
        utilization > 0.40f -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.primary
    }
    val billingDay = if (debt.tenureMonths in 1..31) debt.tenureMonths else null

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(debt.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SuggestionChip(onClick = {}, label = { Text("Credit Card", style = MaterialTheme.typography.labelSmall) })
                        if (debt.interestRate > 0)
                            AssistChip(onClick = {}, label = { Text(String.format(Locale.ENGLISH, "%.0f%% p.a.", debt.interestRate), style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("outstanding", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(FormatUtils.formatINR(debt.outstandingBalance), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }

            if (creditLimit > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Credit utilization", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            String.format(Locale.ENGLISH, "%.0f%% of %s", utilization * 100, FormatUtils.formatINRShort(creditLimit)),
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = utilizationColor
                        )
                    }
                    LinearProgressIndicator(
                        progress = { utilization },
                        modifier = Modifier.fillMaxWidth(),
                        color = utilizationColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (debt.emiAmount > 0) {
                    Column {
                        Text("Min Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(FormatUtils.formatINR(debt.emiAmount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (billingDay != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Due Day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${billingDay}th of month", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ── Form Sheet ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtFormSheet(
    title: String,
    initial: DebtEntity?,
    onDismiss: () -> Unit,
    onSave: (DebtEntity) -> Unit
) {
    var selectedType by remember { mutableStateOf(initial?.debtType ?: DEBT_TYPES[0]) }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    // Raw values (digits only, no grouping separators) — AmountTextField formats display
    var outstanding by remember { mutableStateOf(if ((initial?.outstandingBalance ?: 0.0) > 0) initial!!.outstandingBalance.toBigDecimal().stripTrailingZeros().toPlainString() else "") }
    var rate by remember { mutableStateOf(if ((initial?.interestRate ?: 0.0) > 0) String.format(Locale.ENGLISH, "%.1f", initial!!.interestRate) else "") }
    // Loan-specific
    var principal by remember { mutableStateOf(if ((initial?.principalAmount ?: 0.0) > 0 && initial?.debtType != "Credit Card") initial!!.principalAmount.toBigDecimal().stripTrailingZeros().toPlainString() else "") }
    var emi by remember { mutableStateOf(if ((initial?.emiAmount ?: 0.0) > 0 && initial?.debtType != "Credit Card") initial!!.emiAmount.toBigDecimal().stripTrailingZeros().toPlainString() else "") }
    var startDate by remember { mutableStateOf(initial?.startDate ?: "") }
    var tenure by remember { mutableStateOf(if ((initial?.tenureMonths ?: 0) > 0 && initial?.debtType != "Credit Card") initial!!.tenureMonths.toString() else "") }
    // Credit card-specific
    var creditLimit by remember { mutableStateOf(if ((initial?.principalAmount ?: 0.0) > 0 && initial?.debtType == "Credit Card") initial!!.principalAmount.toBigDecimal().stripTrailingZeros().toPlainString() else "") }
    var minDue by remember { mutableStateOf(if ((initial?.emiAmount ?: 0.0) > 0 && initial?.debtType == "Credit Card") initial!!.emiAmount.toBigDecimal().stripTrailingZeros().toPlainString() else "") }
    var billingDay by remember { mutableStateOf(if ((initial?.tenureMonths ?: 0) in 1..31 && initial?.debtType == "Credit Card") initial!!.tenureMonths.toString() else "") }

    var typeExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = XirrCalculator.parseDate(initial?.startDate)
            ?.let { java.time.ZoneId.systemDefault().let { z -> it.atStartOfDay(z).toInstant().toEpochMilli() } }
    )
    val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

    val isCC = isCreditCard(selectedType)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val ld = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        startDate = ld.format(dateFmt)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            // Type selector as chips
            Text("Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DEBT_TYPES.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            HorizontalDivider()

            // Name
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(if (isCC) "Card Name / Bank" else "Lender / Bank Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            // Outstanding — always shown
            AmountTextField(rawValue = outstanding, onRawValueChange = { outstanding = it }, label = { Text("Outstanding Balance") }, modifier = Modifier.fillMaxWidth())

            // Interest rate — plain field (% not currency)
            OutlinedTextField(
                value = rate,
                onValueChange = { rate = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Interest Rate (% p.a.)") },
                placeholder = { Text(if (isCC) "36" else "8.5") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            if (isCC) {
                AmountTextField(rawValue = creditLimit, onRawValueChange = { creditLimit = it }, label = { Text("Credit Limit (optional)") }, modifier = Modifier.fillMaxWidth())
                AmountTextField(rawValue = minDue, onRawValueChange = { minDue = it }, label = { Text("Minimum Due Amount (optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = billingDay,
                    onValueChange = { v -> if (v.length <= 2 && (v.toIntOrNull() ?: 0) <= 31) billingDay = v },
                    label = { Text("Billing Due Day (1–31, optional)") },
                    placeholder = { Text("e.g. 15") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            } else {
                AmountTextField(rawValue = emi, onRawValueChange = { emi = it }, label = { Text("EMI / month") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = startDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Loan Start Date (optional)") },
                    trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarMonth, contentDescription = null) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(value = tenure, onValueChange = { tenure = it.filter { c -> c.isDigit() } }, label = { Text("Total Tenure (months, optional)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                AmountTextField(rawValue = principal, onRawValueChange = { principal = it }, label = { Text("Original Loan Amount (optional)") }, modifier = Modifier.fillMaxWidth(), supportingText = { Text("Needed to show repayment progress") })
            }

            val canSave = name.isNotBlank() && (outstanding.toDoubleOrNull() ?: 0.0) > 0
            Button(
                onClick = {
                    if (!canSave) return@Button
                    val entity = if (isCC) DebtEntity(
                        name = name.trim(), debtType = selectedType,
                        principalAmount = creditLimit.toDoubleOrNull() ?: 0.0,
                        outstandingBalance = outstanding.toDouble(),
                        interestRate = rate.toDoubleOrNull() ?: 0.0,
                        emiAmount = minDue.toDoubleOrNull() ?: 0.0,
                        startDate = "",
                        tenureMonths = billingDay.toIntOrNull()?.coerceIn(1, 31) ?: 0
                    ) else DebtEntity(
                        name = name.trim(), debtType = selectedType,
                        principalAmount = principal.toDoubleOrNull() ?: 0.0,
                        outstandingBalance = outstanding.toDouble(),
                        interestRate = rate.toDoubleOrNull() ?: 0.0,
                        emiAmount = emi.toDoubleOrNull() ?: 0.0,
                        startDate = startDate,
                        tenureMonths = tenure.toIntOrNull() ?: 0
                    )
                    onSave(entity)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave
            ) {
                Text(title)
            }
        }
    }
}
