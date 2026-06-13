package com.example.wealthtracker.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import com.ss.wealthtracker.BuildConfig

object AverageRatesProvider {
    private const val PREFS = "avg_rates_cache"
    private const val KEY_JSON = "json"
    private const val KEY_TIME = "time"

    // Default fallback rates
    val defaults: Map<String, Double> = mapOf(
        "FD" to 6.5,
        "Mutual Fund" to 10.0,
        "Equity" to 12.0,
        "PPF" to 7.1,
        "EPF" to 8.15,
        "NPS" to 9.0,
        "Gold" to 10.0,
        "Others" to 0.0
    )

    suspend fun getAverages(context: Context, maxAgeHours: Long = 24): Map<String, Double> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val cachedAt = prefs.getLong(KEY_TIME, 0L)
        val cachedJson = prefs.getString(KEY_JSON, null)
        if (cachedJson != null && (now - cachedAt) <= TimeUnit.HOURS.toMillis(maxAgeHours)) {
            parseRates(cachedJson) ?: defaults
        } else {
            val fresh = fetchRemote()
            if (fresh != null) {
                prefs.edit().putString(KEY_JSON, toJson(fresh)).putLong(KEY_TIME, now).apply()
                fresh
            } else {
                parseRates(cachedJson) ?: defaults
            }
        }
    }

    private fun toJson(map: Map<String, Double>): String {
        val obj = JSONObject()
        for ((k, v) in map) obj.put(k, v)
        return obj.toString()
    }

    private fun parseRates(json: String?): Map<String, Double>? {
        if (json.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(json)
            val res = mutableMapOf<String, Double>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = obj.optDouble(k, Double.NaN)
                if (!v.isNaN()) res[k] = v
            }
            res
        } catch (t: Throwable) {
            null
        }
    }

    private fun fetchRemote(): Map<String, Double>? {
        return try {
            val url = URL(BuildConfig.RATES_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            conn.inputStream.use { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                val sb = StringBuilder()
                var line: String? = reader.readLine()
                while (line != null) {
                    sb.append(line)
                    line = reader.readLine()
                }
                parseRates(sb.toString())
            }
        } catch (t: Throwable) {
            Log.w("AvgRates", "fetch failed", t)
            null
        }
    }
}
