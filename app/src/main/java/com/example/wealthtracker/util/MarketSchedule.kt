package com.example.wealthtracker.util

import android.content.Context
import java.util.Calendar
import java.util.TimeZone

/**
 * Reads market_config.csv + market_schedule.csv from assets and resolves:
 *   - Which 4 symbols to show in the ticker for a given country + current IST time
 *   - Metadata for each symbol (display name, source type, display unit)
 */
object MarketSchedule {

    data class SymbolConfig(
        val yahooSymbol: String,    // used for REST API
        val wsSymbol: String,       // used for WebSocket (may differ for ETF proxies)
        val displayName: String,
        val sourceType: SourceType,
        val unitForCountry: Map<String, String>  // country_code → unit string
    )

    enum class SourceType { EQUITY, MCX_GOLD, MCX_SILVER, COMEX_COPPER, COMEX_CRUDE }

    private val configCache = mutableMapOf<String, SymbolConfig>()  // keyed by yahooSymbol
    private var configLoaded = false

    // Early-Asia slot name → override display names (GIFT NIFTY proxy)
    private val earlyAsiaOverrides = mapOf("^NSEI" to "GIFT NIFTY")

    fun load(context: Context) {
        if (configLoaded) return
        loadConfig(context)
        configLoaded = true
    }

    private fun loadConfig(context: Context) {
        context.assets.open("market_config.csv").bufferedReader().useLines { lines ->
            lines.filter { !it.startsWith("#") && it.isNotBlank() }.drop(1) // skip header
                .forEach { line ->
                    val cols = line.split(",")
                    if (cols.size < 4) return@forEach
                    val yahoo = cols[0].trim()
                    // For equities: wsSymbol = yahooSymbol (indices stream directly)
                    // For commodities: wsSymbol = ETF proxy (GLD, SLV) or blank (REST-only)
                    val wsRaw = cols[1].trim()
                    val ws = if (wsRaw.isBlank()) yahoo else wsRaw
                    val name = cols[2].trim()
                    val type = when (cols[3].trim()) {
                        "MCX_GOLD"     -> SourceType.MCX_GOLD
                        "MCX_SILVER"   -> SourceType.MCX_SILVER
                        "COMEX_COPPER" -> SourceType.COMEX_COPPER
                        "COMEX_CRUDE"  -> SourceType.COMEX_CRUDE
                        else           -> SourceType.EQUITY
                    }
                    val countryKeys = listOf(
                        "IN","US","GB","JP","AU","HK","DE","CN","CA","SG","KR","NZ","TW","DEFAULT"
                    )
                    val unitMap = mutableMapOf<String, String>()
                    countryKeys.forEachIndexed { i, key ->
                        val colIdx = 4 + i
                        if (colIdx < cols.size) unitMap[key] = cols[colIdx].trim()
                    }
                    configCache[yahoo] = SymbolConfig(yahoo, ws, name, type, unitMap)
                }
        }
    }

    /**
     * Returns the WS symbols to subscribe to for [countryCode]'s active time slot.
     * Excludes REST-only sources (COMEX_COPPER, COMEX_CRUDE).
     */
    fun wsSymbolsForCountry(context: Context, countryCode: String): List<String> {
        val active = activeSymbols(context, countryCode)
        return active.mapNotNull { (cfg, _) ->
            when (cfg.sourceType) {
                SourceType.COMEX_COPPER, SourceType.COMEX_CRUDE -> null
                else -> cfg.wsSymbol.ifBlank { null }
            }
        }.distinct()
    }

    /**
     * Returns the 4 active [SymbolConfig]s for [countryCode] at the current moment,
     * applying India's IST time-slot rules if applicable.
     */
    fun activeSymbols(context: Context, countryCode: String): List<Pair<SymbolConfig, String?>> {
        if (!configLoaded) load(context)

        val istHHMM = currentIstHHMM()
        val slots = readSchedule(context, countryCode)
        val slot = slots.firstOrNull { isInSlot(istHHMM, it.start, it.end) } ?: slots.firstOrNull()
        val symbols = slot?.symbols ?: listOf("^GSPC", "^IXIC", "^DJI", "^GDAXI")

        return symbols.mapNotNull { sym ->
            val cfg = configCache[sym] ?: return@mapNotNull null
            val displayNameOverride = if (slot?.id == "EARLY_ASIA") earlyAsiaOverrides[sym] else null
            cfg to displayNameOverride
        }
    }

    /** Returns the yahoo REST symbols + ws symbols needed to fetch prices for [countryCode]. */
    fun requiredRestSymbols(context: Context, countryCode: String): List<String> {
        val active = activeSymbols(context, countryCode).map { it.first }
        val result = active.map { it.yahooSymbol }.toMutableList()
        // Always add FX rates needed for commodity conversion
        if (active.any { it.sourceType != SourceType.EQUITY }) {
            result += "USDINR=X"
            if (countryCode != "IN") result += "USDUSD=X" // placeholder — handled in converter
        }
        return result.distinct()
    }

    fun getConfig(yahooSymbol: String): SymbolConfig? = configCache[yahooSymbol]

    // ── Private helpers ────────────────────────────────────────────────────────

    private data class SlotRow(val id: String, val start: Int, val end: Int, val symbols: List<String>)

    private val scheduleCache = mutableMapOf<String, List<SlotRow>>()

    private fun readSchedule(context: Context, countryCode: String): List<SlotRow> {
        scheduleCache[countryCode]?.let { return it }
        val rows = mutableListOf<SlotRow>()
        val fallback = mutableListOf<SlotRow>()
        context.assets.open("market_schedule.csv").bufferedReader().useLines { lines ->
            lines.filter { !it.startsWith("#") && it.isNotBlank() }.drop(1).forEach { line ->
                val cols = line.split(",")
                if (cols.size < 8) return@forEach
                val cc = cols[0].trim()
                val id = cols[1].trim()
                val start = parseHHMM(cols[2].trim())
                val end = parseHHMM(cols[3].trim())
                val syms = cols.drop(4).map { it.trim() }.filter { it.isNotBlank() }
                val row = SlotRow(id, start, end, syms)
                when (cc) {
                    countryCode -> rows += row
                    "DEFAULT"   -> fallback += row
                }
            }
        }
        val result = if (rows.isNotEmpty()) rows else fallback
        scheduleCache[countryCode] = result
        return result
    }

    private fun parseHHMM(s: String): Int {
        val parts = s.split(":")
        if (parts.size != 2) return 0
        return parts[0].trim().toIntOrNull()?.times(60)?.plus(parts[1].trim().toIntOrNull() ?: 0) ?: 0
    }

    /** Returns true if [nowMin] (minutes since midnight IST) is in [start]..[end], wrapping midnight. */
    private fun isInSlot(nowMin: Int, start: Int, end: Int): Boolean {
        return if (end > start) nowMin in start until end          // normal e.g. 09:00–15:30
        else nowMin >= start || nowMin < end                        // overnight e.g. 23:30–05:30
    }

    private fun currentIstHHMM(): Int {
        val ist = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        return ist.get(Calendar.HOUR_OF_DAY) * 60 + ist.get(Calendar.MINUTE)
    }
}
