package com.example.wealthtracker.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.example.wealthtracker.util.FormatUtils

/**
 * A locale-aware amount input field.
 * - Displays grouping separators as the user types (1,00,000 for India; 100,000 for US/others).
 * - [rawValue] is the unformatted string (digits + optional ".") kept by the caller.
 * - [onRawValueChange] delivers the stripped string back so the caller can parse it with
 *   FormatUtils.parseAmountInput() or toDoubleOrNull().
 */
@Composable
fun AmountTextField(
    rawValue: String,
    onRawValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    isError: Boolean = false
) {
    var tfv by remember(rawValue) {
        val formatted = FormatUtils.formatAmountInput(rawValue)
        mutableStateOf(TextFieldValue(formatted, TextRange(formatted.length)))
    }

    OutlinedTextField(
        value = tfv,
        onValueChange = { new ->
            val stripped = new.text.replace(",", "")
            val formatted = FormatUtils.formatAmountInput(new.text)
            tfv = TextFieldValue(formatted, TextRange(formatted.length))
            onRawValueChange(stripped)
        },
        label = label,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        placeholder = placeholder,
        supportingText = supportingText,
        enabled = enabled,
        isError = isError
    )
}
