package com.example.wealthtracker.util

import java.text.NumberFormat
import java.util.Locale

object FormatUtils {
    private val inLocale = Locale("en", "IN")
    private val currency: NumberFormat = NumberFormat.getCurrencyInstance(inLocale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    fun formatINR(amount: Double): String {
        // NumberFormat includes currency symbol; ensure it is the rupee sign
        val s = currency.format(amount)
        return s
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
