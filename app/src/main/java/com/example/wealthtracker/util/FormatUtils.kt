package com.example.wealthtracker.util

import android.content.Context
import java.text.NumberFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object FormatUtils {
    @Volatile private var useHindiNumerals: Boolean = false
    @Volatile private var currencySymbol: String = "₹"
    @Volatile private var currencyCode: String = "INR"
    @Volatile private var countryCode: String = "IN"

    fun setUseHindiNumerals(enabled: Boolean) {
        useHindiNumerals = enabled
    }
    
    fun setCurrency(symbol: String, code: String, country: String) {
        currencySymbol = symbol
        currencyCode = code
        countryCode = country
    }
    
    fun getCurrencySymbol(): String = currencySymbol

    private fun currency(): NumberFormat {
        val language = if (useHindiNumerals) "hi" else "en"
        val loc = Locale(language, countryCode)
        val nf = NumberFormat.getCurrencyInstance(loc).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
        
        // Override currency symbol if using custom currency
        if (nf is DecimalFormat) {
            val dfs = nf.decimalFormatSymbols
            dfs.currencySymbol = currencySymbol
            
            if (useHindiNumerals) {
                // Set Devanagari digits for Hindi
                dfs.zeroDigit = '\u0966' // ०
            }
            
            nf.decimalFormatSymbols = dfs
        }
        return nf
    }

    fun formatCurrency(amount: Double): String {
        return currency().format(amount)
    }
    
    // Backward compatibility
    fun formatINR(amount: Double): String {
        return formatCurrency(amount)
    }

    fun formatCurrencyShort(amount: Double): String {
        val abs = kotlin.math.abs(amount)
        
        // Use different abbreviations based on country
        return when (countryCode) {
            "IN" -> {
                // Indian numbering system (Lakhs, Crores)
                when {
                    abs >= 1_00_00_000 -> {
                        val cr = amount / 1_00_00_000.0
                        "${currencySymbol}${String.format(Locale.ENGLISH, "%.2f", cr)} Cr"
                    }
                    abs >= 1_00_000 -> {
                        val lakh = amount / 1_00_000.0
                        "${currencySymbol}${String.format(Locale.ENGLISH, "%.2f", lakh)} L"
                    }
                    else -> formatCurrency(amount)
                }
            }
            else -> {
                // International numbering system (K, M, B)
                when {
                    abs >= 1_000_000_000 -> {
                        val b = amount / 1_000_000_000.0
                        "${currencySymbol}${String.format(Locale.ENGLISH, "%.2f", b)}B"
                    }
                    abs >= 1_000_000 -> {
                        val m = amount / 1_000_000.0
                        "${currencySymbol}${String.format(Locale.ENGLISH, "%.2f", m)}M"
                    }
                    abs >= 1_000 -> {
                        val k = amount / 1_000.0
                        "${currencySymbol}${String.format(Locale.ENGLISH, "%.2f", k)}K"
                    }
                    else -> formatCurrency(amount)
                }
            }
        }
    }
    
    // Backward compatibility
    fun formatINRShort(amount: Double): String {
        return formatCurrencyShort(amount)
    }

    fun formatPercent(value: Double): String {
        val language = if (useHindiNumerals) "hi" else "en"
        val loc = Locale(language, countryCode)
        val pattern = "#,##0.0%"
        val df = DecimalFormat(pattern, DecimalFormatSymbols(loc)).apply {
            maximumFractionDigits = 1
            minimumFractionDigits = 1
        }
        if (useHindiNumerals) {
            val dfs = df.decimalFormatSymbols
            dfs.zeroDigit = '\u0966'
            df.decimalFormatSymbols = dfs
        }
        // value is expected as 0..100 or 0..1? We'll accept 0..100 and divide if needed
        val normalized = if (value > 1.0) value / 100.0 else value
        return df.format(normalized)
    }

    fun formatInt(value: Int): String {
        val language = if (useHindiNumerals) "hi" else "en"
        val loc = Locale(language, countryCode)
        val nf = NumberFormat.getIntegerInstance(loc)
        if (useHindiNumerals && nf is DecimalFormat) {
            val dfs = DecimalFormatSymbols(loc).apply { zeroDigit = '\u0966' }
            nf.decimalFormatSymbols = dfs
        }
        return nf.format(value)
    }
    
    /**
     * Formats a raw amount string (digits + optional decimal) with locale-aware grouping separators.
     * India: 1,00,000  |  Others: 100,000
     * Preserves trailing "." so the user can keep typing decimals.
     */
    fun formatAmountInput(raw: String): String {
        val clean = raw.replace(",", "").trim()
        if (clean.isBlank()) return ""
        val hasDot = clean.contains('.')
        val parts = clean.split('.')
        val intPart = parts[0].filter { it.isDigit() }
        val decPart = parts.getOrNull(1)?.filter { it.isDigit() }?.take(2) ?: ""
        if (intPart.isEmpty()) return if (hasDot) "." else ""
        val num = intPart.toLongOrNull() ?: return clean
        val pattern = if (countryCode == "IN") "#,##,##0" else "#,###,###,###"
        val dfs = DecimalFormatSymbols(Locale.ENGLISH)
        val df = DecimalFormat(pattern, dfs)
        var result = df.format(num)
        if (hasDot) result += "." + decPart
        return result
    }

    /** Strips grouping separators and parses to Double. */
    fun parseAmountInput(formatted: String): Double? =
        formatted.replace(",", "").toDoubleOrNull()

    // Initialize currency from UserPreferences
    fun init(context: Context) {
        val prefs = com.example.wealthtracker.data.UserPreferences(context)
        if (prefs.isCountrySet()) {
            currencySymbol = prefs.getCurrencySymbol()
            currencyCode = prefs.getCurrencyCode()
            countryCode = prefs.getCountryCode()
        }
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
        "Canara Bank",
        "Others (Custom Bank)"
    )
}
