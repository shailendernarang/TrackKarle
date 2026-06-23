package com.example.wealthtracker.util

import java.util.Calendar
import java.util.TimeZone

object MarketHours {

    private data class Exchange(
        val timezone: String,
        val openHour: Int, val openMin: Int,
        val closeHour: Int, val closeMin: Int
    )

    private val byCountry = mapOf(
        "IN" to Exchange("Asia/Kolkata",      9, 15, 15, 30),  // NSE/BSE
        "US" to Exchange("America/New_York",  9, 30, 16,  0),  // NYSE/NASDAQ
        "GB" to Exchange("Europe/London",     8,  0, 16, 30),  // LSE
        "JP" to Exchange("Asia/Tokyo",        9,  0, 15, 30),  // TSE/Nikkei
        "HK" to Exchange("Asia/Hong_Kong",    9, 30, 16,  0),  // HKEX
        "DE" to Exchange("Europe/Berlin",     9,  0, 17, 30),  // XETRA/DAX
        "FR" to Exchange("Europe/Paris",      9,  0, 17, 30),  // Euronext/CAC
        "AU" to Exchange("Australia/Sydney", 10,  0, 16,  0),  // ASX
        "CN" to Exchange("Asia/Shanghai",     9, 30, 15,  0),  // SSE/Shanghai
        "CA" to Exchange("America/Toronto",   9, 30, 16,  0),  // TSX
    )

    private fun isOpen(ex: Exchange): Boolean {
        val cal = Calendar.getInstance(TimeZone.getTimeZone(ex.timezone))
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) return false
        val nowMin  = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val openMin = ex.openHour * 60 + ex.openMin
        val closeMin = ex.closeHour * 60 + ex.closeMin
        return nowMin in openMin until closeMin
    }

    /** True if the primary exchange for this country is currently trading. */
    fun isMarketOpen(countryCode: String): Boolean =
        isOpen(byCountry[countryCode] ?: byCountry["US"]!!)

    /** True if ANY of the global exchanges in the marquee is currently trading. */
    fun isAnyMarketOpen(): Boolean = byCountry.values.any { isOpen(it) }
}
