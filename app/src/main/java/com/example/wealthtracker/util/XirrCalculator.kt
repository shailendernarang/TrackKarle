package com.example.wealthtracker.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object XirrCalculator {

    private val DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

    fun parseDate(s: String?): LocalDate? = runCatching {
        s?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it.trim(), DATE_FMT) }
    }.getOrNull()

    fun realReturn(nominalRate: Double, inflationRate: Double = 0.06): Double {
        return ((1.0 + nominalRate) / (1.0 + inflationRate)) - 1.0
    }

    fun weightedAvgRate(items: List<Pair<Double, Double>>): Double? {
        val totalAmount = items.sumOf { it.first }
        if (totalAmount <= 0.0) return null
        val weighted = items.sumOf { (amount, rate) -> amount * rate }
        return weighted / totalAmount
    }

    fun xirr(cashFlows: List<Double>, dates: List<LocalDate>): Double? {
        if (cashFlows.size != dates.size || cashFlows.size < 2) return null
        val baseDate = dates.first()
        val years = dates.map { ChronoUnit.DAYS.between(baseDate, it) / 365.0 }

        fun npv(rate: Double): Double {
            return cashFlows.indices.sumOf { i ->
                val exp = years[i]
                if (rate <= -1.0) Double.NaN
                else cashFlows[i] / Math.pow(1.0 + rate, exp)
            }
        }

        fun dnpv(rate: Double): Double {
            return cashFlows.indices.sumOf { i ->
                val exp = years[i]
                if (rate <= -1.0 || exp == 0.0) 0.0
                else -exp * cashFlows[i] / Math.pow(1.0 + rate, exp + 1.0)
            }
        }

        var guess = 0.1
        repeat(100) {
            val f = npv(guess)
            val df = dnpv(guess)
            if (!f.isFinite() || !df.isFinite() || df == 0.0) return null
            val next = guess - f / df
            if (Math.abs(next - guess) < 1e-7) {
                return if (next.isFinite()) next else null
            }
            guess = next
        }
        return if (guess.isFinite()) guess else null
    }
}
