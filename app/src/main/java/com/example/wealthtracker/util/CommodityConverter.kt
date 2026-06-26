package com.example.wealthtracker.util

import kotlin.math.abs

/**
 * Converts ETF / COMEX USD prices into the display unit for a given country.
 *
 * Gold  (GLD ETF):   1 GLD share ≈ 0.0932786 troy oz → gold_usd_oz = gld_price / 0.0932786
 * Silver (SLV ETF):  1 SLV share ≈ 0.9330 troy oz    → silver_usd_oz = slv_price / 0.9330
 * Copper (HG=F):     COMEX price is USD/lb            → copper_usd_lb = hgf_price
 * Crude (CL=F):      COMEX price is USD/barrel        → crude_usd_bbl = clf_price
 *
 * Duty factors (post Jul-2024 India budget):
 *   Gold/Silver: BCD 6% + AIDC 1% + GST 3% → ×1.103 compounded
 *   Copper:      BCD 5% + SWS 0.5%          → ×1.055
 *   Crude:       MCX futures embed no import duty; use 1:1 with FX
 */
object CommodityConverter {

    private const val GLD_OZ_RATIO    = 0.0932786   // oz per 1 GLD share
    private const val SLV_OZ_RATIO    = 0.9330       // oz per 1 SLV share
    private const val TROY_OZ_TO_G    = 31.1035
    private const val LB_TO_KG        = 0.453592

    private const val DUTY_GOLD_SILVER = 1.103
    private const val DUTY_COPPER      = 1.055

    data class ConvertedPrice(
        val value: Double,
        val unit: String,
        val formatted: String
    )

    // ── Gold ────────────────────────────────────────────────────────────────────

    fun goldFromEtf(gldEtfPrice: Double, usdInr: Double, countryCode: String, unitHint: String): ConvertedPrice {
        val goldUsdPerOz = gldEtfPrice / GLD_OZ_RATIO
        return gold(goldUsdPerOz, usdInr, countryCode, unitHint)
    }

    fun gold(goldUsdPerOz: Double, usdInr: Double, countryCode: String, unitHint: String): ConvertedPrice {
        val (value, unit) = when {
            unitHint == "INR/10g" -> (goldUsdPerOz * usdInr * (10.0 / TROY_OZ_TO_G) * DUTY_GOLD_SILVER) to "INR/10g"
            unitHint == "JPY/g"   -> (goldUsdPerOz * (1.0 / TROY_OZ_TO_G)) to "JPY/g"   // caller supplies JPY rate via usdInr
            unitHint == "CNY/g"   -> (goldUsdPerOz * usdInr * (1.0 / TROY_OZ_TO_G)) to "CNY/g"
            unitHint == "KRW/g"   -> (goldUsdPerOz * usdInr * (1.0 / TROY_OZ_TO_G)) to "KRW/g"
            unitHint == "TWD/g"   -> (goldUsdPerOz * usdInr * (1.0 / TROY_OZ_TO_G)) to "TWD/g"
            unitHint.contains("GBP") -> (goldUsdPerOz * usdInr) to "GBP/oz"   // caller supplies GBP rate via usdInr
            unitHint.contains("EUR") -> (goldUsdPerOz * usdInr) to "EUR/oz"
            unitHint.contains("AUD") -> (goldUsdPerOz * usdInr) to "AUD/oz"
            unitHint.contains("HKD") -> (goldUsdPerOz * usdInr) to "HKD/oz"
            unitHint.contains("CAD") -> (goldUsdPerOz * usdInr) to "CAD/oz"
            unitHint.contains("SGD") -> (goldUsdPerOz * usdInr) to "SGD/oz"
            unitHint.contains("NZD") -> (goldUsdPerOz * usdInr) to "NZD/oz"
            else                      -> goldUsdPerOz to "USD/oz"
        }
        return ConvertedPrice(value, unit, formatValue(value, unit))
    }

    // ── Silver ──────────────────────────────────────────────────────────────────

    fun silverFromEtf(slvEtfPrice: Double, usdInr: Double, countryCode: String, unitHint: String): ConvertedPrice {
        val silverUsdPerOz = slvEtfPrice / SLV_OZ_RATIO
        return silver(silverUsdPerOz, usdInr, countryCode, unitHint)
    }

    fun silver(silverUsdPerOz: Double, usdInr: Double, countryCode: String, unitHint: String): ConvertedPrice {
        val (value, unit) = when {
            unitHint == "INR/kg" -> (silverUsdPerOz * usdInr * (1000.0 / TROY_OZ_TO_G) * DUTY_GOLD_SILVER) to "INR/kg"
            unitHint == "JPY/kg" -> (silverUsdPerOz * usdInr * (1000.0 / TROY_OZ_TO_G)) to "JPY/kg"
            unitHint == "CNY/kg" -> (silverUsdPerOz * usdInr * (1000.0 / TROY_OZ_TO_G)) to "CNY/kg"
            unitHint == "KRW/kg" -> (silverUsdPerOz * usdInr * (1000.0 / TROY_OZ_TO_G)) to "KRW/kg"
            unitHint == "EUR/kg" -> (silverUsdPerOz * usdInr * (1000.0 / TROY_OZ_TO_G)) to "EUR/kg"
            unitHint == "GBP/oz" -> (silverUsdPerOz * usdInr) to "GBP/oz"
            unitHint.contains("AUD") -> (silverUsdPerOz * usdInr) to "AUD/oz"
            unitHint.contains("CAD") -> (silverUsdPerOz * usdInr) to "CAD/oz"
            unitHint.contains("SGD") -> (silverUsdPerOz * usdInr) to "SGD/oz"
            unitHint.contains("NZD") -> (silverUsdPerOz * usdInr) to "NZD/oz"
            else -> silverUsdPerOz to "USD/oz"
        }
        return ConvertedPrice(value, unit, formatValue(value, unit))
    }

    // ── Copper ──────────────────────────────────────────────────────────────────

    fun copper(copperUsdPerLb: Double, usdInr: Double, countryCode: String, unitHint: String): ConvertedPrice {
        val (value, unit) = when {
            unitHint == "INR/kg" -> (copperUsdPerLb * usdInr * (1.0 / LB_TO_KG) * DUTY_COPPER) to "INR/kg"
            unitHint == "JPY/kg" -> (copperUsdPerLb * usdInr * (1.0 / LB_TO_KG)) to "JPY/kg"
            unitHint == "GBP/kg" -> (copperUsdPerLb * usdInr * (1.0 / LB_TO_KG)) to "GBP/kg"
            unitHint == "EUR/kg" -> (copperUsdPerLb * usdInr * (1.0 / LB_TO_KG)) to "EUR/kg"
            unitHint == "AUD/kg" -> (copperUsdPerLb * usdInr * (1.0 / LB_TO_KG)) to "AUD/kg"
            unitHint == "CNY/kg" -> (copperUsdPerLb * usdInr * (1.0 / LB_TO_KG)) to "CNY/kg"
            unitHint == "CAD/lb" -> (copperUsdPerLb * usdInr) to "CAD/lb"
            unitHint == "SGD/kg" -> (copperUsdPerLb * usdInr * (1.0 / LB_TO_KG)) to "SGD/kg"
            unitHint == "KRW/kg" -> (copperUsdPerLb * usdInr * (1.0 / LB_TO_KG)) to "KRW/kg"
            unitHint == "NZD/kg" -> (copperUsdPerLb * usdInr * (1.0 / LB_TO_KG)) to "NZD/kg"
            else -> copperUsdPerLb to "USD/lb"
        }
        return ConvertedPrice(value, unit, formatValue(value, unit))
    }

    // ── Crude Oil ───────────────────────────────────────────────────────────────

    fun crude(crudeUsdPerBbl: Double, usdInr: Double, unitHint: String): ConvertedPrice {
        val (value, unit) = when {
            unitHint.startsWith("INR") -> (crudeUsdPerBbl * usdInr) to "INR/bbl"
            unitHint.startsWith("GBP") -> (crudeUsdPerBbl * usdInr) to "GBP/bbl"
            unitHint.startsWith("EUR") -> (crudeUsdPerBbl * usdInr) to "EUR/bbl"
            unitHint.startsWith("JPY") -> (crudeUsdPerBbl * usdInr) to "JPY/bbl"
            unitHint.startsWith("AUD") -> (crudeUsdPerBbl * usdInr) to "AUD/bbl"
            unitHint.startsWith("HKD") -> (crudeUsdPerBbl * usdInr) to "HKD/bbl"
            unitHint.startsWith("CNY") -> (crudeUsdPerBbl * usdInr) to "CNY/bbl"
            unitHint.startsWith("CAD") -> (crudeUsdPerBbl * usdInr) to "CAD/bbl"
            unitHint.startsWith("SGD") -> (crudeUsdPerBbl * usdInr) to "SGD/bbl"
            unitHint.startsWith("KRW") -> (crudeUsdPerBbl * usdInr) to "KRW/bbl"
            unitHint.startsWith("NZD") -> (crudeUsdPerBbl * usdInr) to "NZD/bbl"
            else -> crudeUsdPerBbl to "USD/bbl"
        }
        return ConvertedPrice(value, unit, formatValue(value, unit))
    }

    // ── Formatting ──────────────────────────────────────────────────────────────

    private fun formatValue(value: Double, unit: String): String {
        val prefix = when {
            unit.startsWith("INR") -> "₹"
            unit.startsWith("USD") -> "$"
            unit.startsWith("GBP") -> "£"
            unit.startsWith("EUR") -> "€"
            unit.startsWith("JPY") -> "¥"
            unit.startsWith("CNY") -> "¥"
            unit.startsWith("AUD") -> "A$"
            unit.startsWith("HKD") -> "HK$"
            unit.startsWith("CAD") -> "CA$"
            unit.startsWith("SGD") -> "S$"
            unit.startsWith("KRW") -> "₩"
            unit.startsWith("NZD") -> "NZ$"
            unit.startsWith("TWD") -> "NT$"
            else -> ""
        }
        val suffix = unit.substringAfter("/", "")
        val formatted = when {
            value >= 100_000 -> String.format("%.0f", value)
            value >= 1_000   -> String.format("%.1f", value)
            value >= 10      -> String.format("%.2f", value)
            else             -> String.format("%.4f", value)
        }
        return if (suffix.isNotEmpty()) "$prefix$formatted/$suffix" else "$prefix$formatted"
    }
}
