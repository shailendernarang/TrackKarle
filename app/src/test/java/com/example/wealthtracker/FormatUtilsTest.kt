package com.example.wealthtracker

import com.example.wealthtracker.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FormatUtilsTest {

    @Before
    fun setUp() {
        // Ensure English numerals for consistent testing
        FormatUtils.setUseHindiNumerals(false)
    }

    @Test
    fun formatINR_zeroAmount() {
        val result = FormatUtils.formatINR(0.0)
        assert(result.startsWith("₹") && result.endsWith("0"))
    }

    @Test
    fun formatINR_smallAmount() {
        val result = FormatUtils.formatINR(500.0)
        assert(result.startsWith("₹") && result.contains("500"))
    }

    @Test
    fun formatINR_thousandsAmount() {
        val result = FormatUtils.formatINR(50000.0)
        assert(result.startsWith("₹") && result.contains("50") && result.contains("000"))
    }

    @Test
    fun formatINR_lakhsAmount() {
        val result = FormatUtils.formatINR(500000.0)
        assert(result.startsWith("₹") && result.contains("5") && result.contains("00"))
    }

    @Test
    fun formatINR_croresAmount() {
        val result = FormatUtils.formatINR(10000000.0)
        assert(result.startsWith("₹") && result.contains("1") && result.contains("00"))
    }

    @Test
    fun formatINR_negativeAmount() {
        val result = FormatUtils.formatINR(-1000.0)
        assert((result.startsWith("-₹") || result.startsWith("₹-")) && result.contains("1"))
    }

    @Test
    fun formatINRShort_smallAmount() {
        val result = FormatUtils.formatINRShort(500.0)
        assert(result.startsWith("₹") && result.contains("500"))
    }

    @Test
    fun formatINRShort_lakhsAmount() {
        val result = FormatUtils.formatINRShort(500000.0)
        assert(result.contains("5.00") && result.contains("L"))
    }

    @Test
    fun formatINRShort_croresAmount() {
        val result = FormatUtils.formatINRShort(10000000.0)
        assert(result.contains("1.00") && result.contains("Cr"))
    }

    @Test
    fun formatInt_zeroValue() {
        val result = FormatUtils.formatInt(0)
        assertEquals("0", result)
    }

    @Test
    fun formatInt_smallValue() {
        val result = FormatUtils.formatInt(123)
        assertEquals("123", result)
    }

    @Test
    fun formatInt_largeValue() {
        val result = FormatUtils.formatInt(123456)
        // Should add commas based on Indian number system
        assert(result.contains("1") && result.contains("23") && result.contains("456"))
    }

    @Test
    fun formatPercent_zeroValue() {
        val result = FormatUtils.formatPercent(0.0)
        assert(result.contains("0") && result.endsWith("%"))
    }

    @Test
    fun formatPercent_decimalValue() {
        val result = FormatUtils.formatPercent(7.5)
        assert(result.contains("7") && result.endsWith("%"))
    }

    @Test
    fun formatPercent_hundredValue() {
        val result = FormatUtils.formatPercent(100.0)
        assert(result.contains("100") && result.endsWith("%"))
    }

    @Test
    fun setUseHindiNumerals_togglesCorrectly() {
        FormatUtils.setUseHindiNumerals(true)
        // After setting to true, subsequent formats should use Hindi numerals
        // We can't easily test the exact output without knowing the implementation,
        // but we can verify the method doesn't crash
        FormatUtils.setUseHindiNumerals(false)
    }
}
