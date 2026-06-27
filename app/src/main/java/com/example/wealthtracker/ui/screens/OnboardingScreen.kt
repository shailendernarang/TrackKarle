package com.example.wealthtracker.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ss.wealthtracker.R
import kotlinx.coroutines.launch

private val screenGradients = listOf(
    // Screen 1 — Welcome (deep navy)
    listOf(Color(0xFF0B1426), Color(0xFF1A1240)),
    // Screen 2 — Privacy (dark forest green)
    listOf(Color(0xFF0A1C10), Color(0xFF0D3320)),
    // Screen 3 — Track Everything (dark amber)
    listOf(Color(0xFF1C0B04), Color(0xFF6B2D06)),
    // Screen 4 — Smart Analytics (dark blue-slate)
    listOf(Color(0xFF0A1525), Color(0xFF101A3A)),
    // Screen 5 — Built for You (dark emerald)
    listOf(Color(0xFF072918), Color(0xFF0C4428))
)

private val screenTitles = listOf(
    "Welcome to TrackKaro",
    "Privacy First",
    "Track Everything",
    "Smart Analytics",
    "Built for You"
)

private val screenSubtitles = listOf(
    "Your personal wealth tracker. Private, powerful, and built for India.",
    "Your data stays on your device. No cloud sync, no accounts, no tracking.",
    "FDs, Mutual Funds, Stocks, Gold, PPF, Crypto — all in one place.",
    "Beautiful charts, portfolio insights, and calculators to understand your wealth.",
    "Multi-currency, global markets, and features designed for investors everywhere."
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val scope = rememberCoroutineScope()
    val totalPages = 5
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }

    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        val pageColors = screenGradients[page]
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    if (!size.isEmpty()) drawRect(Brush.verticalGradient(pageColors, 0f, size.height))
                }
        ) {
            // Skip button row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 24.dp, start = 24.dp)
                    .height(40.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (page < totalPages - 1) {
                    TextButton(onClick = onComplete) {
                        Text(
                            text = "Skip",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Visual content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (page) {
                    0 -> WelcomeVisual()
                    1 -> PrivacyVisual()
                    2 -> TrackEverythingVisual()
                    3 -> AnalyticsVisual()
                    4 -> BuiltForYouVisual()
                }
            }

            // Bottom section: dots + title + subtitle + button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page indicator dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(totalPages) { index ->
                        val isActive = index == page
                        val width: Dp by animateDpAsState(
                            targetValue = if (isActive) 24.dp else 8.dp,
                            animationSpec = tween(300),
                            label = "dot_width"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isActive) Color.White
                                    else Color.White.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = screenTitles[page],
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Subtitle
                Text(
                    text = screenSubtitles[page],
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // CTA button
                Button(
                    onClick = {
                        if (currentPage == totalPages - 1) {
                            onComplete()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = screenGradients[page].last()
                    )
                ) {
                    Text(
                        text = if (page == totalPages - 1) "Get Started 🚀" else "Next →",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = screenGradients[page].first()
                    )
                }
            }
        }
    }
}

// ── Screen Visuals ─────────────────────────────────────────────────────────────

@Composable
private fun WelcomeVisual() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App icon
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.mipmap.ic_launcher_legacy),
            contentDescription = "TrackKaro",
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Investment type chips — row 1
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            InvestmentChip(emoji = "🏦", label = "Fixed Deposit")
            Spacer(modifier = Modifier.width(10.dp))
            InvestmentChip(emoji = "📊", label = "Mutual Funds")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            InvestmentChip(emoji = "📈", label = "Stocks")
            Spacer(modifier = Modifier.width(10.dp))
            InvestmentChip(emoji = "🥇", label = "Gold")
            Spacer(modifier = Modifier.width(10.dp))
            InvestmentChip(emoji = "🏛️", label = "PPF")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 3
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            InvestmentChip(emoji = "₿", label = "Crypto")
            Spacer(modifier = Modifier.width(10.dp))
            InvestmentChip(emoji = "🛡️", label = "Insurance")
            Spacer(modifier = Modifier.width(10.dp))
            InvestmentChip(emoji = "🏠", label = "Real Estate")
        }
    }
}

@Composable
private fun InvestmentChip(emoji: String, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PrivacyVisual() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Shield/lock icon
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Privacy feature rows
        PrivacyFeatureRow(
            icon = Icons.Default.VisibilityOff,
            title = "No cloud sync",
            subtitle = "Your data never leaves the device"
        )
        Spacer(modifier = Modifier.height(4.dp))
        PrivacyFeatureRow(
            icon = Icons.Default.Verified,
            title = "No accounts",
            subtitle = "Start instantly, zero sign-up"
        )
        Spacer(modifier = Modifier.height(4.dp))
        PrivacyFeatureRow(
            icon = Icons.Default.Lock,
            title = "No tracking",
            subtitle = "Zero analytics or data collection"
        )
    }
}

@Composable
private fun PrivacyFeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(text = subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun TrackEverythingVisual() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 3x2 investment category grid
        val categories = listOf(
            Triple("🏦", "Fixed Deposit", "Banking"),
            Triple("📊", "Mutual Funds", "AMC"),
            Triple("📈", "Stocks", "Broker"),
            Triple("🥇", "Gold", "Commodity"),
            Triple("🏛️", "PPF / NPS", "Govt"),
            Triple("₿", "Crypto", "Exchange")
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            categories.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { (emoji, name, category) ->
                        InvestmentCategoryCard(
                            emoji = emoji,
                            name = name,
                            category = category,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // "+ 500 more" pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "+ 500 more investment types",
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun InvestmentCategoryCard(
    emoji: String,
    name: String,
    category: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = name, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(text = category, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun AnalyticsVisual() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // Portfolio card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(text = "Total Portfolio", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    Text(
                        text = "₹7,24,860",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF22C55E).copy(alpha = 0.25f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(text = "+12.4%", fontSize = 13.sp, color = Color(0xFF86EFAC), fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bar chart
            val barHeights = listOf(0.33f, 0.54f, 0.37f, 0.57f, 0.49f, 0.65f, 0.47f, 0.74f, 0.61f, 0.78f, 0.70f, 0.82f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                barHeights.forEach { fraction ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF60A5FA), Color(0xFF3B82F6).copy(alpha = 0.5f))
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Jan", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                Text(text = "Dec", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            // Stats row
            Row(modifier = Modifier.fillMaxWidth()) {
                StatTile(label = "Invested", value = "₹5.8L", modifier = Modifier.weight(1f))
                Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color.White.copy(alpha = 0.15f)).align(Alignment.CenterVertically))
                StatTile(label = "Returns", value = "₹1.4L", modifier = Modifier.weight(1f))
                Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color.White.copy(alpha = 0.15f)).align(Alignment.CenterVertically))
                StatTile(label = "Holdings", value = "18", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun BuiltForYouVisual() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Globe icon with currency symbols floating around it
        Box(
            modifier = Modifier.size(144.dp),
            contentAlignment = Alignment.Center
        ) {
            // Main circle
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
            // Currency symbols around the circle
            val symbols = listOf("₹", "$", "£", "€", "¥")
            val positions = listOf(
                Alignment.TopStart, Alignment.TopEnd,
                Alignment.CenterStart, Alignment.CenterEnd,
                Alignment.BottomCenter
            )
            positions.zip(symbols).forEach { (alignment, symbol) ->
                Box(
                    modifier = Modifier
                        .align(alignment)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = symbol, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Currency flag chips — row 1
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("🇮🇳" to "INR", "🇺🇸" to "USD", "🇬🇧" to "GBP", "🇪🇺" to "EUR").forEach { (flag, code) ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = flag, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = code, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Currency flag chips — row 2
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("🇦🇺" to "AUD", "🇸🇬" to "SGD").forEach { (flag, code) ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = flag, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = code, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Global markets info row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Global stock markets — NSE, BSE, NYSE, NASDAQ and more",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )
        }
    }
}

// ── Onboarding completion preferences ─────────────────────────────────────────

object OnboardingPrefs {
    private const val PREFS_NAME = "onboarding_prefs"
    private const val KEY_COMPLETED = "onboarding_completed"

    fun setCompleted(context: android.content.Context, completed: Boolean) {
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETED, completed)
            .apply()
    }

    fun isCompleted(context: android.content.Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getBoolean(KEY_COMPLETED, false)
    }
}
