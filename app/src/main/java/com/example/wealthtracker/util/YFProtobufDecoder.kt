package com.example.wealthtracker.util

import android.util.Base64

/**
 * Manual protobuf decoder for Yahoo Finance WebSocket binary tick frames.
 * No external protobuf dependency required — decodes only the fields we need.
 *
 * Field map (from Yahoo Finance reverse-engineering):
 *   1  = id (string)
 *   2  = price (float)
 *   4  = currency (string)
 *   7  = marketHours (string)
 *   8  = changePercent (float)
 *   10 = dayHigh (float)
 *   11 = dayLow (float)
 *   12 = change (float)
 *   13 = shortName (string)
 */
object YFProtobufDecoder {

    data class Tick(
        val symbol: String,
        val price: Float,
        val currency: String = "",
        val marketHours: String = "",
        val changePercent: Float = 0f,
        val change: Float = 0f,
        val dayHigh: Float = 0f,
        val dayLow: Float = 0f,
        val shortName: String = ""
    )

    /** Decodes a base64-encoded protobuf frame from the Yahoo Finance WebSocket. */
    fun decode(base64Frame: String): Tick? = runCatching {
        val bytes = Base64.decode(base64Frame.trim(), Base64.DEFAULT)
        parse(bytes)
    }.getOrNull()

    private fun parse(bytes: ByteArray): Tick? {
        var pos = 0
        var symbol = ""; var price = 0f; var currency = ""; var marketHours = ""
        var changePercent = 0f; var change = 0f; var dayHigh = 0f; var dayLow = 0f
        var shortName = ""

        while (pos < bytes.size) {
            // Read varint tag
            var tag = 0L; var shift = 0
            while (pos < bytes.size) {
                val b = bytes[pos++].toLong() and 0xFF
                tag = tag or ((b and 0x7F) shl shift)
                shift += 7
                if (b and 0x80 == 0L) break
            }
            val fieldNum = (tag ushr 3).toInt()
            val wireType = (tag and 0x7).toInt()

            when (wireType) {
                0 -> { // varint — skip
                    while (pos < bytes.size && bytes[pos++].toInt() and 0x80 != 0) {}
                }
                1 -> { pos += 8 } // 64-bit — skip
                2 -> { // length-delimited
                    var len = 0L; var s = 0
                    while (pos < bytes.size) {
                        val b = bytes[pos++].toLong() and 0xFF
                        len = len or ((b and 0x7F) shl s); s += 7
                        if (b and 0x80 == 0L) break
                    }
                    val end = minOf(pos + len.toInt(), bytes.size)
                    val data = bytes.copyOfRange(pos, end)
                    when (fieldNum) {
                        1  -> symbol      = String(data, Charsets.UTF_8)
                        4  -> currency    = String(data, Charsets.UTF_8)
                        7  -> marketHours = String(data, Charsets.UTF_8)
                        13 -> shortName   = String(data, Charsets.UTF_8)
                    }
                    pos = end
                }
                5 -> { // 32-bit float
                    if (pos + 4 <= bytes.size) {
                        val bits = (bytes[pos].toInt() and 0xFF) or
                            ((bytes[pos + 1].toInt() and 0xFF) shl 8) or
                            ((bytes[pos + 2].toInt() and 0xFF) shl 16) or
                            ((bytes[pos + 3].toInt() and 0xFF) shl 24)
                        val v = java.lang.Float.intBitsToFloat(bits)
                        when (fieldNum) {
                            2  -> price         = v
                            8  -> changePercent = v
                            10 -> dayHigh       = v
                            11 -> dayLow        = v
                            12 -> change        = v
                        }
                    }
                    pos += 4
                }
                else -> break
            }
        }

        return if (symbol.isNotEmpty() && price > 0f)
            Tick(symbol, price, currency, marketHours, changePercent, change, dayHigh, dayLow, shortName)
        else null
    }
}
