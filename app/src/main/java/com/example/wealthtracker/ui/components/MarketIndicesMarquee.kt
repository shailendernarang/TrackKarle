package com.example.wealthtracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wealthtracker.network.StocksApiProvider
import com.example.wealthtracker.util.MarketHours
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log

data class MarketIndex(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val preMarketPrice: Double = 0.0,
    val preMarketChange: Double = 0.0,
    val preMarketChangePercent: Double = 0.0,
    val postMarketPrice: Double = 0.0,
    val postMarketChange: Double = 0.0,
    val postMarketChangePercent: Double = 0.0,
    val marketState: String = "REGULAR"
)

// In-memory cache — survives navigation, cleared only on process death
private object MarketDataCache {
    var indices: List<MarketIndex> = emptyList()
}

@Composable
fun MarketIndicesMarquee(
    modifier: Modifier = Modifier,
    refreshIntervalSeconds: Int = 30
) {
    // Seed from cache so navigating back shows data instantly with no API call
    var indices by remember { mutableStateOf(MarketDataCache.indices) }
    var isLoading by remember { mutableStateOf(false) }

    // Fetch market indices data
    suspend fun fetchIndices(): List<MarketIndex> {
        val symbols = listOf(
            "^NSEI"     to "NIFTY 50",   "^BSESN"    to "SENSEX",
            "^GSPC"     to "S&P 500",    "^DJI"      to "DOW Jones",
            "^IXIC"     to "NASDAQ",     "^FTSE"     to "FTSE 100",
            "^GDAXI"    to "DAX",        "^FCHI"     to "CAC 40",
            "^N225"     to "Nikkei 225", "^HSI"      to "Hang Seng",
            "000001.SS" to "Shanghai",   "^AXJO"     to "ASX 200"
        )
        val symbolToName = symbols.toMap()

        return runCatching {
            StocksApiProvider.ensureCrumb()
            val symbolStr = symbols.joinToString(",") { it.first }
            val quoteItems = StocksApiProvider.service.quotes(symbolStr).quoteResponse.result
            quoteItems.mapNotNull { item ->
                val sym  = item.symbol ?: return@mapNotNull null
                val name = symbolToName[sym] ?: return@mapNotNull null
                val price = item.regularMarketPrice ?: return@mapNotNull null
                MarketIndex(
                    symbol = sym, name = name,
                    price = price,
                    change = item.regularMarketChange ?: 0.0,
                    changePercent = item.regularMarketChangePercent ?: 0.0,
                    preMarketPrice = item.preMarketPrice ?: 0.0,
                    preMarketChange = item.preMarketChange ?: 0.0,
                    preMarketChangePercent = item.preMarketChangePercent ?: 0.0,
                    postMarketPrice = item.postMarketPrice ?: 0.0,
                    postMarketChange = item.postMarketChange ?: 0.0,
                    postMarketChangePercent = item.postMarketChangePercent ?: 0.0,
                    marketState = item.marketState ?: "REGULAR"
                )
            }
        }.onFailure { e ->
            Log.e("MarketMarquee", "fetchIndices failed: ${e.message}")
        }.getOrDefault(emptyList())
    }

    // Auto-refresh effect - only during market hours
    LaunchedEffect(Unit) {
        // Initial fetch only if we don't have data
        if (indices.isEmpty()) {
            isLoading = true
            val data = fetchIndices()
            if (data.isNotEmpty()) {
                MarketDataCache.indices = data
                indices = data
            }
            isLoading = false
        }

        while (true) {
            if (MarketHours.isAnyMarketOpen()) {
                // During market hours - refresh every 30 seconds
                delay(refreshIntervalSeconds * 1000L)
                val data = fetchIndices()
                if (data.isNotEmpty()) {
                    MarketDataCache.indices = data
                    indices = data
                }
            } else {
                // Outside market hours - just wait and check periodically
                // Don't fetch again if we already have data
                delay(5 * 60 * 1000L)
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        if (isLoading && indices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                indices.forEach { index ->
                    MarketIndexItem(index)
                }
            }
        }
    }
}

@Composable
private fun MarketIndexItem(index: MarketIndex) {
    val isPositive = index.change >= 0
    val changeColor = if (isPositive) Color(0xFF2E7D32) else Color(0xFFD32F2F)
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            Text(
                text = index.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = String.format("%.2f", index.price),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Surface(
            color = changeColor,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = String.format("%+.2f (%.2f%%)", index.change, index.changePercent),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
