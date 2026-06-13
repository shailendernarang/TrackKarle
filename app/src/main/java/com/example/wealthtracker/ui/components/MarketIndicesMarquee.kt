package com.example.wealthtracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wealthtracker.network.StocksApiProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.runtime.saveable.listSaver

data class MarketIndex(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double
) {
    companion object {
        val Saver = listSaver<MarketIndex, Any>(
            save = { listOf(it.symbol, it.name, it.price, it.change, it.changePercent) },
            restore = { 
                MarketIndex(
                    symbol = it[0] as String,
                    name = it[1] as String,
                    price = it[2] as Double,
                    change = it[3] as Double,
                    changePercent = it[4] as Double
                )
            }
        )
    }
}

@Composable
fun MarketIndicesMarquee(
    modifier: Modifier = Modifier,
    refreshIntervalSeconds: Int = 30
) {
    var indices by rememberSaveable(stateSaver = listSaver(
        save = { it.map { index -> with(MarketIndex.Saver) { save(index) } } },
        restore = { it.map { saved -> MarketIndex.Saver.restore(saved as List<Any>)!! } }
    )) { mutableStateOf<List<MarketIndex>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Check if market is open based on timezone
    fun isMarketHours(): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        // Market hours: 9 AM to 3 PM (15:00)
        return hour in 9..14
    }
    
    // Fetch market indices data
    suspend fun fetchIndices(): List<MarketIndex> {
        val symbols = listOf(
            // India
            "^NSEI" to "NIFTY 50",
            "^BSESN" to "SENSEX",
            // US
            "^GSPC" to "S&P 500",
            "^DJI" to "DOW JONES",
            "^IXIC" to "NASDAQ",
            // Europe
            "^FTSE" to "FTSE 100",      // UK
            "^GDAXI" to "DAX",          // Germany
            "^FCHI" to "CAC 40",        // France
            // Asia Pacific
            "^N225" to "NIKKEI 225",    // Japan
            "^HSI" to "HANG SENG",      // Hong Kong
            "000001.SS" to "SSE",       // Shanghai
            "^AXJO" to "ASX 200"        // Australia
        )

        val results = mutableListOf<MarketIndex>()
        
        symbols.forEach { (symbol, name) ->
            runCatching {
                // Use chart endpoint with 1 day range to get latest data
                val response = StocksApiProvider.service.chart(symbol, range = "1d", interval = "1d")
                val result = response.chart.result.firstOrNull()
                
                if (result != null) {
                    val meta = result.meta
                    val currentPrice = meta?.regularMarketPrice ?: 0.0
                    val previousClose = meta?.chartPreviousClose ?: meta?.previousClose ?: 0.0
                    
                    val change = currentPrice - previousClose
                    val changePercent = if (previousClose != 0.0) {
                        (change / previousClose) * 100.0
                    } else 0.0
                    
                    results.add(
                        MarketIndex(
                            symbol = symbol,
                            name = name,
                            price = currentPrice,
                            change = change,
                            changePercent = changePercent
                        )
                    )
                }
            }.onFailure { e ->
                Log.e("MarketMarquee", "Failed to fetch $symbol: ${e.message}")
            }
        }
        
        return results
    }

    // Auto-refresh effect - only during market hours
    LaunchedEffect(Unit) {
        // Initial fetch only if we don't have data
        if (indices.isEmpty()) {
            isLoading = true
            val data = fetchIndices()
            if (data.isNotEmpty()) {
                indices = data
            }
            isLoading = false
        }
        
        while (true) {
            if (isMarketHours()) {
                // During market hours - refresh every 30 seconds
                delay(refreshIntervalSeconds * 1000L)
                val data = fetchIndices()
                if (data.isNotEmpty()) {
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
