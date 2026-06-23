package com.example.wealthtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wealthtracker.data.local.InvestmentEntity
import com.example.wealthtracker.util.FormatUtils
import com.example.wealthtracker.util.IndianBanks
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.TextRange

// Delegates to FormatUtils so grouping matches the user's selected country
private fun formatIndianNumber(input: String): String = FormatUtils.formatAmountInput(input)

private fun formatIndianNumberTF(input: TextFieldValue): TextFieldValue {
    val formatted = formatIndianNumber(input.text)
    return TextFieldValue(formatted, selection = TextRange(formatted.length))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernInvestmentForm(
    amount: TextFieldValue,
    onAmountChange: (TextFieldValue) -> Unit,
    investmentType: String,
    onInvestmentTypeChange: (String) -> Unit,
    onAddInvestment: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String = ""
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Add New Investment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Modern Amount Field
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { 
                    Text(
                        "Investment Amount",
                        style = MaterialTheme.typography.bodyMedium
                    ) 
                },
                placeholder = { 
                    Text(
                        "${FormatUtils.getCurrencySymbol()} 10,00,000",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    ) 
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp) // Increased height for better comma visibility
            )
            
            if (isError && errorMessage.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Investment Type Selector
            ModernInvestmentTypeSelector(
                selectedType = investmentType,
                onTypeSelected = onInvestmentTypeChange
            )
            
            // Add Button
            Button(
                onClick = onAddInvestment,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Investment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ModernInvestmentTypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val investmentTypes = listOf(
        "Fixed Deposit" to Icons.Default.AccountBalance,
        "Mutual Fund" to Icons.AutoMirrored.Filled.TrendingUp,
        "Stocks" to Icons.AutoMirrored.Filled.ShowChart,
        "PPF" to Icons.Default.Savings,
        "Gold" to Icons.Default.Star,
        "NPS" to Icons.Default.AccountBalance,
        "Others" to Icons.Default.Category
    )
    
    Column(modifier = modifier) {
        Text(
            text = "Investment Type",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(investmentTypes.size) { index ->
                val (type, icon) = investmentTypes[index]
                val isSelected = selectedType == type
                
                FilterChip(
                    selected = isSelected,
                    onClick = { onTypeSelected(type) },
                    label = { 
                        Text(
                            text = type,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditInvestmentBottomSheet(
    investment: InvestmentEntity,
    onDismiss: () -> Unit,
    onSave: (InvestmentEntity) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editAmount by remember { mutableStateOf(TextFieldValue(FormatUtils.formatINR(investment.amount))) }
    var editType by remember { mutableStateOf(investment.investmentType) }
    var editBankName by remember { mutableStateOf(investment.bankName ?: "") }
    var editCustomBankName by remember { mutableStateOf("") }
    var editFdRate by remember { mutableStateOf(investment.fdRate?.toString() ?: "") }
    var editFdTenure by remember { mutableStateOf(investment.fdTenure ?: "") }
    var editFdStartDate by remember { mutableStateOf(investment.fdStartDate ?: "") }
    var editFdMaturityDate by remember { mutableStateOf(investment.fdMaturityDate ?: "") }
    
    // All other investment type fields with proper auto-population
    var editMfName by remember { mutableStateOf(investment.type) }
    var editMfDate by remember { mutableStateOf(investment.mfDate ?: "") }
    var editStockName by remember { mutableStateOf(investment.stockName ?: investment.type) }
    var editStockDate by remember { mutableStateOf(investment.stockDate ?: "") }
    var editPpfFy by remember { mutableStateOf(investment.ppfFy ?: "") }
    var editPpfDate by remember { mutableStateOf(investment.ppfDate ?: "") }
    var editNpsTier by remember { mutableStateOf(investment.npsTier ?: "") }
    var editNpsDate by remember { mutableStateOf(investment.npsDate ?: "") }
    var editGoldType by remember { mutableStateOf(investment.goldType ?: "") }
    var editGoldDate by remember { mutableStateOf(investment.goldDate ?: "") }
    var editPolicyName by remember { mutableStateOf(investment.hiPolicyName ?: "") }
    var editRenewalDate by remember { mutableStateOf(investment.hiRenewalDate ?: "") }
    var editOthersName by remember { mutableStateOf(investment.type) }
    
    val createdDate = remember {
        try {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            sdf.format(Date(investment.createdAt))
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding() // Adjust for keyboard
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Edit Investment",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Update your investment details",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            
            // Added On Date (Disabled)
            OutlinedTextField(
                value = "Added on $createdDate",
                onValueChange = { },
                label = { Text("Added On") },
                enabled = false,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Amount Field with proper formatting
            OutlinedTextField(
                value = editAmount,
                onValueChange = { newValue -> 
                    editAmount = formatIndianNumberTF(newValue)
                },
                label = { Text("Investment Amount") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
            )
            
            // Investment Type Dropdown
            var typeDropdownExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = editType,
                    onValueChange = { },
                    label = { Text("Investment Type") },
                    readOnly = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { typeDropdownExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { typeDropdownExpanded = true }
                )
                DropdownMenu(
                    expanded = typeDropdownExpanded,
                    onDismissRequest = { typeDropdownExpanded = false }
                ) {
                    listOf(
                        "Fixed Deposit", "Mutual Fund", "Stocks", "Equity", "PPF", "EPF", 
                        "NPS", "Gold", "Health Insurance", "Term Insurance", "Others"
                    ).forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                editType = type
                                typeDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            
            // Conditional Fields based on type
            if (editType == "Fixed Deposit" || editType == "FD") {
                // Bank Name Dropdown
                var bankDropdownExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editBankName.ifEmpty { "Select Bank" },
                        onValueChange = { },
                        label = { Text("Bank Name") },
                        readOnly = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { bankDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { bankDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = bankDropdownExpanded,
                        onDismissRequest = { bankDropdownExpanded = false }
                    ) {
                        IndianBanks.all.forEach { bank ->
                            DropdownMenuItem(
                                text = { Text(bank) },
                                onClick = {
                                    editBankName = bank
                                    // Auto-load FD rate based on bank selection
                                    // You can add rate loading logic here
                                    bankDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Show custom bank name field if "Others" is selected
                if (editBankName == "Others (Custom Bank)") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editCustomBankName,
                        onValueChange = { editCustomBankName = it },
                        label = { Text("Enter Bank Name") },
                        placeholder = { Text("e.g., Chase Bank, HSBC, etc.") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editFdRate,
                    onValueChange = { editFdRate = it },
                    label = { Text("Interest Rate (%)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Percent,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = editFdTenure,
                    onValueChange = { editFdTenure = it },
                    label = { Text("Tenure (Optional)") },
                    placeholder = { Text("e.g., 5 years, 36 months") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                var showFdStartPicker by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = editFdStartDate,
                    onValueChange = { },
                    label = { Text("Start Date") },
                    placeholder = { Text("DD MMM YYYY") },
                    readOnly = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showFdStartPicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFdStartPicker = true }
                )
                if (showFdStartPicker) {
                    val dpState = rememberDatePickerState()
                    DatePickerDialog(
                        onDismissRequest = { showFdStartPicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                dpState.selectedDateMillis?.let { millis ->
                                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                                    editFdStartDate = sdf.format(Date(millis))
                                }
                                showFdStartPicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showFdStartPicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = dpState)
                    }
                }
                
                var showFdMaturityPicker by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = editFdMaturityDate,
                    onValueChange = { },
                    label = { Text("Maturity Date") },
                    placeholder = { Text("DD MMM YYYY") },
                    readOnly = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showFdMaturityPicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFdMaturityPicker = true }
                )
                if (showFdMaturityPicker) {
                    val dpState = rememberDatePickerState()
                    DatePickerDialog(
                        onDismissRequest = { showFdMaturityPicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                dpState.selectedDateMillis?.let { millis ->
                                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                                    editFdMaturityDate = sdf.format(Date(millis))
                                }
                                showFdMaturityPicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showFdMaturityPicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = dpState)
                    }
                }
            }
            
            // Gold Fields
            if (editType == "Gold") {
                // Gold Type Dropdown
                var goldTypeDropdownExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editGoldType.ifEmpty { "Select Type" },
                        onValueChange = { },
                        label = { Text("Gold Type") },
                        readOnly = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { goldTypeDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { goldTypeDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = goldTypeDropdownExpanded,
                        onDismissRequest = { goldTypeDropdownExpanded = false }
                    ) {
                        listOf(
                            "24K Coins/Bars", "22K Jewellery", "Sovereign Gold Bond", 
                            "Gold ETF", "Digital Gold", "Gold Mutual Fund", "Others"
                        ).forEach { goldType ->
                            DropdownMenuItem(
                                text = { Text(goldType) },
                                onClick = {
                                    editGoldType = goldType
                                    goldTypeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                var showGoldDatePicker by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = editGoldDate,
                    onValueChange = { },
                    label = { Text("Purchase Date (Optional)") },
                    placeholder = { Text("DD MMM YYYY") },
                    readOnly = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showGoldDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showGoldDatePicker = true }
                )
                if (showGoldDatePicker) {
                    val dpState = rememberDatePickerState()
                    DatePickerDialog(
                        onDismissRequest = { showGoldDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                dpState.selectedDateMillis?.let { millis ->
                                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                                    editGoldDate = sdf.format(Date(millis))
                                }
                                showGoldDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showGoldDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = dpState)
                    }
                }
            }
            
            // Mutual Fund Fields
            if (editType == "Mutual Fund" || editType == "MF") {
                OutlinedTextField(
                    value = editMfName,
                    onValueChange = { editMfName = it },
                    label = { Text("Fund Name") },
                    placeholder = { Text("e.g., SBI Bluechip Fund") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                var showMfDatePicker by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = editMfDate,
                    onValueChange = { },
                    label = { Text("Investment Date (Optional)") },
                    placeholder = { Text("DD MMM YYYY") },
                    readOnly = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showMfDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMfDatePicker = true }
                )
                if (showMfDatePicker) {
                    val dpState = rememberDatePickerState()
                    DatePickerDialog(
                        onDismissRequest = { showMfDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                dpState.selectedDateMillis?.let { millis ->
                                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                                    editMfDate = sdf.format(Date(millis))
                                }
                                showMfDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showMfDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = dpState)
                    }
                }
            }
            
            // Stocks/Equity Fields
            if (editType == "Stocks" || editType == "Equity" || editType == "Stock") {
                OutlinedTextField(
                    value = editStockName,
                    onValueChange = { editStockName = it },
                    label = { Text("Stock Name") },
                    placeholder = { Text("e.g., Reliance Industries") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                var showStockDatePicker by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = editStockDate,
                    onValueChange = { },
                    label = { Text("Purchase Date (Optional)") },
                    placeholder = { Text("DD MMM YYYY") },
                    readOnly = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showStockDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStockDatePicker = true }
                )
                if (showStockDatePicker) {
                    val dpState = rememberDatePickerState()
                    DatePickerDialog(
                        onDismissRequest = { showStockDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                dpState.selectedDateMillis?.let { millis ->
                                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                                    editStockDate = sdf.format(Date(millis))
                                }
                                showStockDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showStockDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = dpState)
                    }
                }
            }
            
            // PPF Fields
            if (editType == "PPF") {
                OutlinedTextField(
                    value = editPpfFy,
                    onValueChange = { editPpfFy = it },
                    label = { Text("Financial Year (Optional)") },
                    placeholder = { Text("e.g., FY 2023-24") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                var showPpfDatePicker by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = editPpfDate,
                    onValueChange = { },
                    label = { Text("Deposit Date (Optional)") },
                    placeholder = { Text("DD MMM YYYY") },
                    readOnly = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPpfDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPpfDatePicker = true }
                )
                if (showPpfDatePicker) {
                    val dpState = rememberDatePickerState()
                    DatePickerDialog(
                        onDismissRequest = { showPpfDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                dpState.selectedDateMillis?.let { millis ->
                                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                                    editPpfDate = sdf.format(Date(millis))
                                }
                                showPpfDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showPpfDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = dpState)
                    }
                }
            }
            
            // NPS Fields
            if (editType == "NPS") {
                var npsTierDropdownExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editNpsTier.ifEmpty { "Select Tier" },
                        onValueChange = { },
                        label = { Text("NPS Tier") },
                        readOnly = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { npsTierDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { npsTierDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = npsTierDropdownExpanded,
                        onDismissRequest = { npsTierDropdownExpanded = false }
                    ) {
                        listOf("Tier I", "Tier II").forEach { tier ->
                            DropdownMenuItem(
                                text = { Text(tier) },
                                onClick = {
                                    editNpsTier = tier
                                    npsTierDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                var showNpsDatePicker by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = editNpsDate,
                    onValueChange = { },
                    label = { Text("Contribution Date (Optional)") },
                    placeholder = { Text("DD MMM YYYY") },
                    readOnly = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showNpsDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNpsDatePicker = true }
                )
                if (showNpsDatePicker) {
                    val dpState = rememberDatePickerState()
                    DatePickerDialog(
                        onDismissRequest = { showNpsDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                dpState.selectedDateMillis?.let { millis ->
                                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                                    editNpsDate = sdf.format(Date(millis))
                                }
                                showNpsDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNpsDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = dpState)
                    }
                }
            }
            
            // Health Insurance & Term Insurance Fields
            if (editType == "Health Insurance" || editType == "Term Insurance") {
                OutlinedTextField(
                    value = editPolicyName,
                    onValueChange = { editPolicyName = it },
                    label = { Text("Policy Name/Number") },
                    placeholder = { Text("e.g., Star Health Policy") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                var showRenewalDatePicker by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = editRenewalDate,
                    onValueChange = { },
                    label = { Text("Renewal Date (Optional)") },
                    placeholder = { Text("DD MMM YYYY") },
                    readOnly = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showRenewalDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRenewalDatePicker = true }
                )
                if (showRenewalDatePicker) {
                    val dpState = rememberDatePickerState()
                    DatePickerDialog(
                        onDismissRequest = { showRenewalDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                dpState.selectedDateMillis?.let { millis ->
                                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                                    editRenewalDate = sdf.format(Date(millis))
                                }
                                showRenewalDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRenewalDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = dpState)
                    }
                }
            }
            
            // Others Fields
            if (editType == "Others") {
                OutlinedTextField(
                    value = editOthersName,
                    onValueChange = { editOthersName = it },
                    label = { Text("Investment Name") },
                    placeholder = { Text("e.g., Real Estate, Bonds") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Save Button (Primary action first)
                Button(
                    onClick = {
                        // Determine final bank name
                        val finalBankName = if (editBankName == "Others (Custom Bank)" && editCustomBankName.isNotBlank()) {
                            editCustomBankName
                        } else {
                            editBankName
                        }
                        
                        val updatedInvestment = investment.copy(
                            amount = editAmount.text.replace(",", "").replace(FormatUtils.getCurrencySymbol(), "").replace("$", "").replace("£", "").replace("€", "").trim().toDoubleOrNull() ?: investment.amount,
                            investmentType = editType,
                            type = when (editType) {
                                "Mutual Fund", "MF" -> editMfName
                                "Stocks", "Equity", "Stock" -> editStockName
                                "Others" -> editOthersName
                                else -> investment.type
                            },
                            // Fixed Deposit fields
                            bankName = if (editType == "Fixed Deposit" || editType == "FD") finalBankName.takeIf { it.isNotBlank() } else null,
                            fdRate = if (editType == "Fixed Deposit" || editType == "FD") editFdRate.toDoubleOrNull() else null,
                            fdTenure = if (editType == "Fixed Deposit" || editType == "FD") editFdTenure.takeIf { it.isNotBlank() } else null,
                            fdStartDate = if (editType == "Fixed Deposit" || editType == "FD") editFdStartDate.takeIf { it.isNotBlank() } else null,
                            fdMaturityDate = if (editType == "Fixed Deposit" || editType == "FD") editFdMaturityDate.takeIf { it.isNotBlank() } else null,
                            // Mutual Fund fields
                            mfDate = if (editType == "Mutual Fund" || editType == "MF") editMfDate.takeIf { it.isNotBlank() } else null,
                            // Stock fields
                            stockName = if (editType == "Stocks" || editType == "Equity" || editType == "Stock") editStockName.takeIf { it.isNotBlank() } else null,
                            stockDate = if (editType == "Stocks" || editType == "Equity" || editType == "Stock") editStockDate.takeIf { it.isNotBlank() } else null,
                            // PPF fields
                            ppfFy = if (editType == "PPF") editPpfFy.takeIf { it.isNotBlank() } else null,
                            ppfDate = if (editType == "PPF") editPpfDate.takeIf { it.isNotBlank() } else null,
                            // NPS fields
                            npsTier = if (editType == "NPS") editNpsTier.takeIf { it.isNotBlank() } else null,
                            npsDate = if (editType == "NPS") editNpsDate.takeIf { it.isNotBlank() } else null,
                            // Gold fields
                            goldType = if (editType == "Gold") editGoldType.takeIf { it.isNotBlank() } else null,
                            goldDate = if (editType == "Gold") editGoldDate.takeIf { it.isNotBlank() } else null,
                            // Insurance fields
                            hiPolicyName = if (editType == "Health Insurance" || editType == "Term Insurance") editPolicyName.takeIf { it.isNotBlank() } else null,
                            hiRenewalDate = if (editType == "Health Insurance" || editType == "Term Insurance") editRenewalDate.takeIf { it.isNotBlank() } else null
                        )
                        onSave(updatedInvestment)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Save Changes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Delete Button (Secondary action)
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }
            }
            } // Close item block
        }
    }
}
