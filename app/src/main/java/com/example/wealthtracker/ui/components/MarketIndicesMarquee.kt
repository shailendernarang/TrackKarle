package com.example.wealthtracker.ui.components

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wealthtracker.data.UserPreferences
import com.example.wealthtracker.network.StocksApiProvider
import com.example.wealthtracker.util.*
import com.example.wealthtracker.util.LocalActivity
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

// ── Data model ─────────────────────────────────────────────────────────────────

enum class BadgeType { LIVE, COMEX, OFFLINE, CLOSED }

data class MarketIndex(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val displayPrice: String = "",
    val unit: String = "",
    val badgeType: BadgeType = BadgeType.LIVE,
    val sourceType: String = "EQUITY",
    val preMarketPrice: Double = 0.0,
    val preMarketChange: Double = 0.0,
    val preMarketChangePercent: Double = 0.0,
    val postMarketPrice: Double = 0.0,
    val postMarketChange: Double = 0.0,
    val postMarketChangePercent: Double = 0.0,
    val marketState: String = "REGULAR"
)

// ── Process-lifetime cache ──────────────────────────────────────────────────────

internal object MarketDataCache {
    var indices: List<MarketIndex> = emptyList()
}

// ── Sparkline helper (used by StockAnalysisScreen) ─────────────────────────────

@Composable
fun MiniSparkline(points: List<Float>, color: Color, modifier: Modifier = Modifier) {
    if (points.size < 2) return
    val minV = points.min(); val maxV = points.max(); val range = maxV - minV
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val step = if (points.size > 1) w / (points.size - 1) else w
        val path = Path()
        points.forEachIndexed { i, v ->
            val x = i * step
            val y = if (range > 0f) h - ((v - minV) / range * h) else h / 2f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ── Animated ticker price (only changed digits animate) ────────────────────────

@Composable
fun AnimatedPriceText(
    text: String,
    isRising: Boolean,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val changeColor = if (isRising) Color(0xFF16A34A) else Color(0xFFDC2626)
    val dir = if (isRising) 1 else -1  // +1 = enter from below (rising), -1 = from above (falling)

    Row(modifier = modifier.clipToBounds(), verticalAlignment = Alignment.CenterVertically) {
        text.forEachIndexed { idx, char ->
            key(idx) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        if (targetState.isDigit()) {
                            (slideInVertically(spring(stiffness = Spring.StiffnessMedium)) { dir * it } +
                             fadeIn(tween(120))) togetherWith
                            (slideOutVertically(spring(stiffness = Spring.StiffnessMedium)) { -dir * it } +
                             fadeOut(tween(80)))
                        } else {
                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        }
                    },
                    label = "ticker_$idx"
                ) { c ->
                    val onSurface = MaterialTheme.colorScheme.onSurface
                    val normalColor = if (style.color == Color.Unspecified) onSurface else style.color
                    var colorTarget by remember(c) {
                        mutableStateOf(if (c.isDigit()) changeColor else normalColor)
                    }
                    val animColor by animateColorAsState(colorTarget, tween(700), label = "char_color")
                    LaunchedEffect(c) {
                        if (!c.isDigit()) return@LaunchedEffect
                        delay(150)
                        colorTarget = normalColor
                    }
                    Text(text = c.toString(), style = style.copy(color = animColor))
                }
            }
        }
    }
}

// ── Card accent colors + icons ─────────────────────────────────────────────────

private val equityPalette = listOf(
    Color(0xFF3B82F6), // blue
    Color(0xFF22C55E), // green
    Color(0xFF14B8A6), // teal
    Color(0xFF6366F1)  // indigo
)

private fun accentFor(sourceType: String, position: Int): Pair<Color, ImageVector> = when (sourceType) {
    "MCX_GOLD"     -> Color(0xFFF59E0B) to Icons.Default.WorkspacePremium
    "MCX_SILVER"   -> Color(0xFF8B5CF6) to Icons.Default.WorkspacePremium
    "COMEX_COPPER" -> Color(0xFFF97316) to Icons.Default.Grain
    "COMEX_CRUDE"  -> Color(0xFF6B7280) to Icons.Default.LocalGasStation
    else           -> equityPalette[position % equityPalette.size] to Icons.AutoMirrored.Filled.TrendingUp
}

// ── Main composable ────────────────────────────────────────────────────────────

@Composable
fun MarketIndicesMarquee(modifier: Modifier = Modifier) {
    val ctx         = LocalContext.current
    var indices     by remember { mutableStateOf(MarketDataCache.indices) }
    var isLoading   by remember { mutableStateOf(false) }
    val countryCode = remember { UserPreferences(ctx).getCountryCode() }
    var lastUsdInr  by remember { mutableDoubleStateOf(84.0) }
    val wsTicks     by MarketWebSocket.ticks.collectAsState()

    // LocalActivity is the real AppCompatActivity (provided at the root CompositionLocalProvider).
    // LocalContext inside NavHost / WithLocalizedContext is a ContextWrapper — cannot be cast to Activity.
    val activityLifecycle = (LocalActivity.current as? LifecycleOwner)?.lifecycle
    var isForegrounded by remember { mutableStateOf(true) }
    DisposableEffect(activityLifecycle) {
        val lc = activityLifecycle ?: return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> { isForegrounded = true;  Log.d("MarketMarquee", "foregrounded — resuming polls") }
                Lifecycle.Event.ON_STOP  -> { isForegrounded = false; Log.d("MarketMarquee", "backgrounded — pausing polls") }
                else -> {}
            }
        }
        lc.addObserver(observer)
        onDispose { lc.removeObserver(observer) }
    }

    // Reconnect WS immediately when coming back to foreground — don't wait for the next poll cycle
    LaunchedEffect(isForegrounded) {
        if (isForegrounded && indices.isNotEmpty()) {
            MarketWebSocket.updateSymbols(MarketSchedule.wsSymbolsForCountry(ctx, countryCode))
            MarketWebSocket.connect()
        }
    }

    LaunchedEffect(Unit) {
        suspend fun fetch() {
            runCatching {
                MarketSchedule.load(ctx)
                val activeConfigs = MarketSchedule.activeSymbols(ctx, countryCode)
                // Subscribe WS only to symbols for this country's active time slot
                MarketWebSocket.updateSymbols(MarketSchedule.wsSymbolsForCountry(ctx, countryCode))
                MarketWebSocket.connect()

                val restSymbols = buildSet {
                    activeConfigs.forEach { (cfg, _) -> add(cfg.yahooSymbol) }
                    if (activeConfigs.any { it.first.sourceType != MarketSchedule.SourceType.EQUITY })
                        add("USDINR=X")
                }
                if (restSymbols.isEmpty()) return

                StocksApiProvider.ensureCrumb()
                val bySymbol = StocksApiProvider.service
                    .quotes(restSymbols.joinToString(","))
                    .quoteResponse.result.associateBy { it.symbol ?: "" }

                val usdInr = bySymbol["USDINR=X"]?.regularMarketPrice ?: lastUsdInr
                lastUsdInr = usdInr

                val fetched = activeConfigs.mapNotNull { (cfg, nameOverride) ->
                    val q     = bySymbol[cfg.yahooSymbol] ?: return@mapNotNull null
                    val price = q.regularMarketPrice ?: return@mapNotNull null
                    val unitHint = cfg.unitForCountry[countryCode] ?: cfg.unitForCountry["DEFAULT"] ?: "NATIVE"

                    val (displayPrice, unit, badge) = when (cfg.sourceType) {
                        MarketSchedule.SourceType.MCX_GOLD -> {
                            val c = CommodityConverter.gold(price, usdInr, countryCode, unitHint)
                            Triple(c.formatted, c.unit, BadgeType.COMEX)
                        }
                        MarketSchedule.SourceType.MCX_SILVER -> {
                            val c = CommodityConverter.silver(price, usdInr, countryCode, unitHint)
                            Triple(c.formatted, c.unit, BadgeType.COMEX)
                        }
                        MarketSchedule.SourceType.COMEX_COPPER -> {
                            val c = CommodityConverter.copper(price, usdInr, countryCode, unitHint)
                            Triple(c.formatted, c.unit, BadgeType.COMEX)
                        }
                        MarketSchedule.SourceType.COMEX_CRUDE -> {
                            val c = CommodityConverter.crude(price, usdInr, unitHint)
                            Triple(c.formatted, c.unit, BadgeType.COMEX)
                        }
                        MarketSchedule.SourceType.EQUITY -> {
                            val b = when (q.marketState ?: "REGULAR") {
                                "REGULAR", "PRE", "POST" -> BadgeType.LIVE
                                else -> BadgeType.OFFLINE
                            }
                            Triple("", "", b)
                        }
                    }

                    MarketIndex(
                        symbol        = cfg.yahooSymbol,
                        name          = nameOverride ?: cfg.displayName,
                        price         = price,
                        change        = q.regularMarketChange ?: 0.0,
                        changePercent = q.regularMarketChangePercent ?: 0.0,
                        displayPrice  = displayPrice,
                        unit          = unit,
                        badgeType     = badge,
                        sourceType    = cfg.sourceType.name,
                        preMarketPrice = q.preMarketPrice ?: 0.0,
                        preMarketChange = q.preMarketChange ?: 0.0,
                        preMarketChangePercent = q.preMarketChangePercent ?: 0.0,
                        postMarketPrice = q.postMarketPrice ?: 0.0,
                        postMarketChange = q.postMarketChange ?: 0.0,
                        postMarketChangePercent = q.postMarketChangePercent ?: 0.0,
                        marketState   = q.marketState ?: "REGULAR"
                    )
                }

                if (fetched.isNotEmpty()) {
                    MarketDataCache.indices = fetched
                    indices = fetched
                }
            }.onFailure { e ->
                val tag = if (e is java.net.UnknownHostException) "no network (Doze/offline)" else "fetch failed"
                Log.e("MarketMarquee", "$tag — ${e::class.simpleName}: ${e.message}")
            }
        }

        if (indices.isEmpty()) { isLoading = true; fetch(); isLoading = false }

        while (true) {
            // Pause polls while in background — Doze mode blocks DNS → UnknownHostException.
            // WS ticks still update live data when foregrounded.
            val delayMs = when {
                !isForegrounded               -> 5 * 60 * 1000L   // background: check every 5 min
                MarketHours.isAnyMarketOpen() -> 15_000L
                else                          -> 15 * 60 * 1000L
            }
            delay(delayMs)
            if (isForegrounded) fetch()
        }
    }

    // Merge WS ticks
    val displayIndices = remember(indices, wsTicks, lastUsdInr) {
        if (wsTicks.isEmpty()) return@remember indices
        indices.map { idx ->
            val cfg  = MarketSchedule.getConfig(idx.symbol) ?: return@map idx
            val tick = wsTicks[cfg.wsSymbol] ?: return@map idx
            val unitHint = cfg.unitForCountry[countryCode] ?: cfg.unitForCountry["DEFAULT"] ?: "NATIVE"
            val tickPrice = tick.price.toDouble()
            when (idx.sourceType) {
                MarketSchedule.SourceType.MCX_GOLD.name -> {
                    val c = CommodityConverter.gold(tickPrice, lastUsdInr, countryCode, unitHint)
                    idx.copy(price = tickPrice, change = tick.change.toDouble(),
                        changePercent = tick.changePercent.toDouble(),
                        displayPrice = c.formatted, unit = c.unit, badgeType = BadgeType.LIVE)
                }
                MarketSchedule.SourceType.MCX_SILVER.name -> {
                    val c = CommodityConverter.silver(tickPrice, lastUsdInr, countryCode, unitHint)
                    idx.copy(price = tickPrice, change = tick.change.toDouble(),
                        changePercent = tick.changePercent.toDouble(),
                        displayPrice = c.formatted, unit = c.unit, badgeType = BadgeType.LIVE)
                }
                else -> idx.copy(price = tickPrice, change = tick.change.toDouble(),
                    changePercent = tick.changePercent.toDouble(), badgeType = BadgeType.LIVE)
            }
        }
    }

    Surface(
        modifier       = modifier.fillMaxWidth(),
        color          = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        if (isLoading && displayIndices.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        } else {
            val bgColor = MaterialTheme.colorScheme.background
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    displayIndices.forEachIndexed { i, idx ->
                        val (accent, icon) = accentFor(idx.sourceType, i)
                        MarketIndexCard(idx, accent, icon, hero = i == 0)
                    }
                }
                // Right-edge fade to hint at horizontal scrollability
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                0.78f to Color.Transparent,
                                1.00f to bgColor
                            )
                        )
                )
            }
        }
    }
}

// ── Market index card ──────────────────────────────────────────────────────────

@Composable
private fun MarketIndexCard(index: MarketIndex, accentColor: Color, icon: ImageVector, hero: Boolean = false) {
    val isPositive  = index.changePercent >= 0
    val changeColor = if (isPositive) Color(0xFF16A34A) else Color(0xFFDC2626)
    val arrowIcon   = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    val priceText = if (index.displayPrice.isNotBlank()) {
        index.displayPrice
    } else {
        runCatching {
            NumberFormat.getNumberInstance(Locale.ENGLISH).apply {
                maximumFractionDigits = 2
                minimumFractionDigits = 2
            }.format(index.price)
        }.getOrElse { String.format("%.2f", index.price) }
    }

    Card(
        modifier  = Modifier.width(if (hero) 184.dp else 160.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Colored top accent bar
            Box(Modifier.fillMaxWidth().height(3.dp).background(accentColor))

            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Icon circle + name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(accentColor.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector    = icon,
                            contentDescription = null,
                            tint           = accentColor,
                            modifier       = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text       = index.name,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f)
                    )
                }

                // Price — only changed digits animate
                AnimatedPriceText(
                    text     = priceText,
                    isRising = isPositive,
                    style    = (if (hero) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium)
                                   .copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth()
                )

                // Change: arrow + amount | (pct%) pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(arrowIcon, null, tint = changeColor, modifier = Modifier.size(13.dp))
                    Text(
                        text       = String.format("%+.2f", index.change),
                        style      = MaterialTheme.typography.bodySmall,
                        color      = changeColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 11.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(changeColor.copy(alpha = 0.10f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text     = "(${String.format("%.2f", kotlin.math.abs(index.changePercent))}%)",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = changeColor,
                            fontSize = 9.sp
                        )
                    }
                }

                // Badge
                IndexBadge(badge = index.badgeType, accentColor = accentColor)
            }
        }
    }
}

// ── Status badge (color-aware for COMEX) ──────────────────────────────────────

@Composable
private fun IndexBadge(badge: BadgeType, accentColor: Color) {
    val (label, bgColor, textColor, pulse) = when (badge) {
        BadgeType.LIVE    -> Quadruple("● LIVE",  Color(0xFFFEE2E2), Color(0xFFDC2626), true)
        BadgeType.COMEX   -> Quadruple("COMEX",   accentColor.copy(alpha = 0.12f), accentColor, false)
        BadgeType.OFFLINE -> Quadruple("OFFLINE", Color(0xFFF1F5F9), Color(0xFF94A3B8), false)
        BadgeType.CLOSED  -> Quadruple("CLOSED",  Color(0xFFF1F5F9), Color(0xFF94A3B8), false)
    }

    val pulseAlpha = remember { Animatable(1f) }
    if (pulse) {
        LaunchedEffect(Unit) {
            while (true) { pulseAlpha.animateTo(0.3f, tween(600)); pulseAlpha.animateTo(1f, tween(600)) }
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelSmall,
            color      = if (pulse) textColor.copy(alpha = pulseAlpha.value) else textColor,
            fontSize   = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── StatusBadge (exported, used by StockAnalysisScreen) ───────────────────────

@Composable
fun StatusBadge(badge: BadgeType) {
    IndexBadge(badge = badge, accentColor = Color(0xFF6B7280))
}

// ── Helpers ────────────────────────────────────────────────────────────────────

internal fun formatIndexPrice(price: Double): String = runCatching {
    NumberFormat.getNumberInstance(Locale.ENGLISH).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }.format(price)
}.getOrElse { String.format("%.2f", price) }

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
