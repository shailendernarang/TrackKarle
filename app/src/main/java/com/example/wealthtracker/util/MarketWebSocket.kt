package com.example.wealthtracker.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Process-lifetime WebSocket singleton — wss://streamer.finance.yahoo.com
 *
 * Retry strategy (market hours only):
 *   Retries 1–3  : 5 s gaps — fast recovery for transient drops
 *   Retries 4–10 : 30 s gaps — background retry while REST takes over as primary
 *   Retry > 10   : give up; REST stays fast until user re-opens the screen
 *
 * Callers observe [retryCount] to switch REST polling intervals:
 *   retryCount == 0   → WS live, REST is backup
 *   retryCount in 1-2 → WS recovering, REST backup at normal rate
 *   retryCount >= 3   → WS degraded, REST should poll fast (30 s dashboard / 10 s marquee)
 *   retryCount == 0 again → WS recovered, REST returns to normal rate
 */
object MarketWebSocket {

    private const val TAG = "MarketWS"
    private const val WS_URL = "wss://streamer.finance.yahoo.com/?version=2"
    private const val THROTTLE_MS = 500L
    private const val MAX_RETRIES = 10
    private const val FAST_RETRY_MS = 5_000L   // retries 1–3
    private const val SLOW_RETRY_MS = 30_000L  // retries 4–10

    private val subscribedSymbols = CopyOnWriteArrayList<String>(listOf("GC=F", "SI=F"))

    private val _ticks = MutableStateFlow<Map<String, YFProtobufDecoder.Tick>>(emptyMap())
    val ticks: StateFlow<Map<String, YFProtobufDecoder.Tick>> = _ticks

    private val _retryCount = MutableStateFlow(0)
    /** Current WS reconnect attempt count. Resets to 0 on successful connect or explicit disconnect. */
    val retryCount: StateFlow<Int> = _retryCount

    private var webSocket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)
    private val retryCounter = AtomicInteger(0)
    @Volatile private var intentionalClose = false
    private var lastEmitMs = 0L
    private val pendingUpdates = mutableMapOf<String, YFProtobufDecoder.Tick>()

    // Singleton scope — survives Activity restarts, lives for the process lifetime
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }

    fun updateSymbols(symbols: List<String>) {
        val newSet = symbols.distinct().filter { it.isNotBlank() }
        subscribedSymbols.clear()
        subscribedSymbols.addAll(newSet)
        Log.d(TAG, "Symbol list updated: $newSet")
        if (isConnected.get()) subscribe()
    }

    /**
     * Connects only when at least one market is open.
     * Safe to call repeatedly — no-op if already connected or market closed.
     */
    fun connect() {
        if (isConnected.get()) return
        if (!MarketHours.isAnyMarketOpen()) {
            Log.d(TAG, "Market closed — skipping WS connect")
            return
        }
        val req = Request.Builder()
            .url(WS_URL)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13)")
            .header("Origin", "https://finance.yahoo.com")
            .build()
        webSocket = client.newWebSocket(req, Listener())
        Log.d(TAG, "Connecting…")
    }

    fun disconnect() {
        intentionalClose = true
        webSocket?.close(1000, "Disconnect")
        webSocket = null
        isConnected.set(false)
        retryCounter.set(0)
        _retryCount.value = 0
        Log.d(TAG, "Disconnected")
    }

    private fun subscribe() {
        val syms = subscribedSymbols.toList()
        // Send in batches of 10 — Yahoo Finance WS silently drops large subscriptions
        syms.chunked(10).forEach { batch ->
            val msg = JSONObject().put("subscribe", JSONArray(batch)).toString()
            webSocket?.send(msg)
        }
        Log.d(TAG, "Subscribed (${syms.size} symbols in batches): $syms")
    }

    private fun onTick(tick: YFProtobufDecoder.Tick) {
        pendingUpdates[tick.symbol] = tick
        val now = System.currentTimeMillis()
        if (now - lastEmitMs >= THROTTLE_MS) {
            lastEmitMs = now
            val merged = _ticks.value.toMutableMap().apply { putAll(pendingUpdates) }
            _ticks.value = merged
            pendingUpdates.clear()
        }
    }

    private class Listener : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            isConnected.set(true)
            retryCounter.set(0)
            _retryCount.value = 0
            Log.d(TAG, "Connected ✓ (retries reset)")
            subscribe()
        }

        override fun onMessage(ws: WebSocket, text: String) {
            // WS v2 wraps protobuf as {"type":"pricing","message":"<base64>"}
            val base64 = runCatching { org.json.JSONObject(text).getString("message") }.getOrElse { text }
            YFProtobufDecoder.decode(base64)?.let { onTick(it) }
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            isConnected.set(false)

            if (!MarketHours.isAnyMarketOpen()) {
                // Yahoo legitimately drops idle connections when markets are closed.
                // REST poll loop will call connect() when market reopens — no action needed.
                Log.d(TAG, "WS closed (market closed) — REST is primary")
                return
            }

            val attempt = retryCounter.incrementAndGet()
            _retryCount.value = attempt
            Log.w(TAG, "WS failure (attempt $attempt/$MAX_RETRIES): ${t.message}")

            if (attempt > MAX_RETRIES) {
                Log.w(TAG, "Max retries reached — WS suspended; REST continues as primary")
                return
            }

            val delayMs = if (attempt <= 3) FAST_RETRY_MS else SLOW_RETRY_MS
            Log.d(TAG, "Reconnecting WS in ${delayMs / 1000}s (retry $attempt/$MAX_RETRIES)")
            scope.launch {
                delay(delayMs)
                connect()
            }
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            isConnected.set(false)
            Log.d(TAG, "Closed ($code): $reason")
            if (intentionalClose) {
                intentionalClose = false
                return  // we called disconnect() ourselves — don't retry
            }
            // Yahoo sometimes cleanly closes mid-session (server-side idle reset).
            // Schedule a fast reconnect if the market is still open.
            if (MarketHours.isAnyMarketOpen()) {
                scope.launch {
                    delay(FAST_RETRY_MS)
                    connect()
                }
            }
        }
    }
}
