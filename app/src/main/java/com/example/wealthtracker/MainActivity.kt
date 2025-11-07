package com.example.wealthtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.wealthtracker.ui.screens.InvestmentScreen
import com.example.wealthtracker.ui.screens.DashboardScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.wealthtracker.ui.InvestmentViewModel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.wealthtracker.ui.theme.WealthTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 12+ system splash
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Initialize Google Mobile Ads SDK
        MobileAds.initialize(this)
        // Enable test ads on this device to avoid policy violations during development
        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(listOf("FBDCD822095FC6704834758AED843A43"))
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)
        setContent {
            WealthTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showComposeSplash by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        delay(2200) // keep visible but < 3s total including system splash
                        showComposeSplash = false
                    }
                    if (showComposeSplash) {
                        val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.money_investment))
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Center content: animation + app title
                            androidx.compose.foundation.layout.Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LottieAnimation(
                                    composition = comp,
                                    iterations = Int.MAX_VALUE,
                                    isPlaying = true,
                                    modifier = Modifier.fillMaxSize(0.35f)
                                )
                                val inf = androidx.compose.animation.core.rememberInfiniteTransition(label = "splash_text")
                                val scale by inf.animateFloat(
                                    initialValue = 0.96f,
                                    targetValue = 1.06f,
                                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                        animation = androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.LinearEasing),
                                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                    ),
                                    label = "scale"
                                )
                                if (comp != null) {
                                    androidx.compose.material3.Text(
                                        text = "TrackKarle",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = androidx.compose.material3.MaterialTheme.typography.displaySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                                    )
                                }
                            }
                            // Bottom privacy/offline line (show when composition is ready)
                            if (comp != null) {
                                androidx.compose.material3.Text(
                                    text = "Private • Offline • No cloud sync",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 24.dp)
                                )
                            }
                        }
                    } else {
                        val nav = rememberNavController()
                        val vm: InvestmentViewModel = hiltViewModel()
                        var startRoute by remember { mutableStateOf<String?>(null) }
                        LaunchedEffect(Unit) {
                            // winner: first non-empty list OR timeout fallback
                            val scope = this
                            launch {
                                vm.investments.collect { list ->
                                    if (list.isNotEmpty() && startRoute == null) {
                                        startRoute = "dashboard"
                                    }
                                }
                            }
                            launch {
                                delay(600)
                                if (startRoute == null) startRoute = "invest"
                            }
                        }
                        if (startRoute != null) {
                            NavHost(navController = nav, startDestination = startRoute!!) {
                                composable("dashboard") {
                                    DashboardScreen(
                                        onAddClick = { nav.navigate("invest") },
                                        onOpenInvestments = { nav.navigate("invest") },
                                        onOpenCalculators = { nav.navigate("calculators?tab=fd") }
                                    )
                                }
                                composable("invest") {
                                    val canGoBack = nav.previousBackStackEntry != null
                                    InvestmentScreen(
                                        onOpenDashboard = { nav.navigate("dashboard") },
                                        onOpenCalculators = { nav.navigate("calculators") },
                                        onBack = { nav.popBackStack() },
                                        showBack = canGoBack
                                    )
                                }
                                composable(
                                    route = "calculators?tab={tab}",
                                    arguments = listOf(androidx.navigation.navArgument("tab") {
                                        defaultValue = ""
                                        type = androidx.navigation.NavType.StringType
                                    })
                                ) { backStackEntry ->
                                    val tab = backStackEntry.arguments?.getString("tab")
                                    val canGoBack = nav.previousBackStackEntry != null
                                    com.example.wealthtracker.ui.screens.CalculatorsScreen(
                                        onBack = { nav.popBackStack() },
                                        initialTab = tab,
                                        showBack = canGoBack
                                    )
                                }
                            }
                        } else {
                            // lightweight placeholder while computing start
                            Box(Modifier.fillMaxSize()) {}
                        }
                    }
                }
            }
        }
    }
}
