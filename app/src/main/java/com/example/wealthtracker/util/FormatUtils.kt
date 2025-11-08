package com.example.wealthtracker.util

import java.text.NumberFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object FormatUtils {
    @Volatile private var useHindiNumerals: Boolean = false

    fun setUseHindiNumerals(enabled: Boolean) {
        useHindiNumerals = enabled
    }

    private fun currency(): NumberFormat {
        val loc = if (useHindiNumerals) Locale("hi", "IN") else Locale("en", "IN")
        val nf = NumberFormat.getCurrencyInstance(loc).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
        if (useHindiNumerals && nf is DecimalFormat) {
            val dfs = DecimalFormatSymbols(loc).apply {
                // Set Devanagari digits
                zeroDigit = '\u0966' // ०
            }
            nf.decimalFormatSymbols = dfs
        }
        return nf
    }

    fun formatINR(amount: Double): String {
        return currency().format(amount)
    }

    fun formatInt(value: Int): String {
        val loc = if (useHindiNumerals) Locale("hi", "IN") else Locale("en", "IN")
        val nf = NumberFormat.getIntegerInstance(loc)
        if (useHindiNumerals && nf is DecimalFormat) {
            val dfs = DecimalFormatSymbols(loc).apply { zeroDigit = '\u0966' }
            nf.decimalFormatSymbols = dfs
        }
        return nf.format(value)
    }
}

object InvestmentTypes {
    val all = listOf(
        "FD",
        "Mutual Fund",
        "NPS",
        "Equity",
        "Gold",
        "PPF",
        "EPF",
        "Term Insurance",
        "Health Insurance",
        "Others"
    )
}

object IndianBanks {
    val all = listOf(
        "State Bank of India (SBI)",
        "HDFC Bank",
        "ICICI Bank",
        "Axis Bank",
        "Kotak Mahindra Bank",
        "Punjab National Bank",
        "Bank of Baroda",
        "IDFC FIRST Bank",
        "Union Bank of India",
        "Canara Bank"
    )
}
