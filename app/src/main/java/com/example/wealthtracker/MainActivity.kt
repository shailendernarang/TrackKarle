package com.example.wealthtracker

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.wealthtracker.util.FormatUtils
import com.example.wealthtracker.data.SettingsStore
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.ss.wealthtracker.R
import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import android.content.res.Configuration
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.KeyguardManager
import android.provider.Settings
import android.content.Intent
import android.app.Activity.RESULT_OK
import com.google.firebase.crashlytics.FirebaseCrashlytics
import android.content.pm.ApplicationInfo

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var pendingLocaleTag: String? = null
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
        // Notifications permission (Android 13+)
        requestNotificationPermission()
        // Log current FCM token for testing
        logCurrentFcmToken()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var darkMode by remember { mutableStateOf(systemDark) }
            var requireDeviceLock by remember { mutableStateOf(false) }
            var useHindiNumerals by remember { mutableStateOf(false) }
            var authenticated by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                // Load persisted settings
                darkMode = SettingsStore.darkModeFlow(applicationContext).first()
                requireDeviceLock = SettingsStore.requireLockFlow(applicationContext).first()
                useHindiNumerals = SettingsStore.hindiNumeralsFlow(applicationContext).first()
                FormatUtils.setUseHindiNumerals(useHindiNumerals)
                authenticated = !requireDeviceLock
            }
            val scope = rememberCoroutineScope()
            // Re-evaluate auth on toggle
            LaunchedEffect(requireDeviceLock) {
                if (requireDeviceLock) {
                    authenticated = false
                } else {
                    authenticated = true
                }
            }
            WealthTrackerTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showComposeSplash by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
                    val fromNotification = intent?.getBooleanExtra("from_notification", false) == true
                    LaunchedEffect(showComposeSplash, fromNotification) {
                        if (!showComposeSplash) return@LaunchedEffect
                        if (fromNotification) {
                            // Skip splash delay when launched from a notification
                            showComposeSplash = false
                        } else {
                            // Faster splash to reduce perceived sluggishness
                            delay(1000)
                            showComposeSplash = false
                        }
                    }
                    if (showComposeSplash) {
                        val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.money_investment))
                        val cfgTop = androidx.compose.ui.platform.LocalConfiguration.current
                        val isCompact = cfgTop.screenWidthDp < 600
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
                                    modifier = Modifier.fillMaxSize(if (isCompact) 0.28f else 0.35f)
                                )
                                if (comp != null) {
                                    // Force Montserrat explicitly on splash texts
                                    val montserrat = FontFamily(
                                        Font(com.ss.wealthtracker.R.font.montserrat_regular, weight = FontWeight.Normal),
                                        Font(com.ss.wealthtracker.R.font.montserrat_semibold, weight = FontWeight.SemiBold),
                                        Font(com.ss.wealthtracker.R.font.montserrat_bold, weight = FontWeight.Bold)
                                    )
                                    androidx.compose.material3.Text(
                                        text = "TrackKaro",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = (
                                            if (isCompact) androidx.compose.material3.MaterialTheme.typography.headlineLarge
                                            else androidx.compose.material3.MaterialTheme.typography.displaySmall
                                        ).copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontFamily = montserrat)
                                    )
                                    androidx.compose.material3.Text(
                                        text = "Nivesh Track Karo",
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                                        style = (
                                            if (isCompact) androidx.compose.material3.MaterialTheme.typography.titleSmall
                                            else androidx.compose.material3.MaterialTheme.typography.titleMedium
                                        ).copy(fontFamily = montserrat)
                                    )
                                }
                            }
                            // Bottom privacy/offline line (show when composition is ready)
                            if (comp != null) {
                                androidx.compose.material3.Text(
                                    text = "Private • Offline • No cloud sync",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                    style = (
                                        if (isCompact) androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                        else androidx.compose.material3.MaterialTheme.typography.titleMedium
                                    ).copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 24.dp)
                                )
                            }
                        }
                    } else {
                        val nav = rememberNavController()
                        // Create Hilt VM outside any localized provider to keep Activity context intact
                        val vm: InvestmentViewModel = hiltViewModel()
                        val startRoute = rememberStartRoute(fromNotification, vm)
                        if (startRoute != null) {
                            NavHost(navController = nav, startDestination = startRoute) {
                                composable("dashboard") {
                                    WithLocalizedContext(useHindiNumerals) {
                                        DashboardScreen(
                                            viewModel = vm,
                                            onAddClick = { nav.navigate("invest") },
                                            onOpenInvestments = { nav.navigate("invest") },
                                            onOpenCalculators = { nav.navigate("calculators?tab=fd") },
                                            onOpenSettings = { nav.navigate("settings") }
                                        )
                                    }
                                }
                                composable("stocks") {
                                    WithLocalizedContext(useHindiNumerals) {
                                        com.example.wealthtracker.ui.screens.StockAnalysisScreen(
                                            onBack = { nav.popBackStack() }
                                        )
                                    }
                                }
                                composable("invest") {
                                    val canGoBack = nav.previousBackStackEntry != null
                                    WithLocalizedContext(useHindiNumerals) {
                                        InvestmentScreen(
                                            viewModel = vm,
                                            onOpenDashboard = { nav.navigate("dashboard") },
                                            onOpenCalculators = { nav.navigate("calculators") },
                                            onOpenStocks = { nav.navigate("stocks") },
                                            onOpenSettings = { nav.navigate("settings") },
                                            onBack = { nav.popBackStack() },
                                            showBack = canGoBack,
                                            onToggleDarkMode = {
                                                darkMode = !darkMode
                                                scope.launch { SettingsStore.setDarkMode(applicationContext, darkMode) }
                                            },
                                            requireDeviceLock = requireDeviceLock,
                                            onToggleRequireDeviceLock = {
                                                requireDeviceLock = !requireDeviceLock
                                                scope.launch { SettingsStore.setRequireLock(applicationContext, requireDeviceLock) }
                                                if (requireDeviceLock) {
                                                    authenticated = false
                                                }
                                            },
                                            useHindiNumerals = useHindiNumerals,
                                            onToggleHindiNumerals = {
                                                useHindiNumerals = !useHindiNumerals
                                                FormatUtils.setUseHindiNumerals(useHindiNumerals)
                                                scope.launch { SettingsStore.setHindiNumerals(applicationContext, useHindiNumerals) }
                                                val tagNow = if (useHindiNumerals) "hi-IN" else "en-IN"
                                                pendingLocaleTag = tagNow
                                            }
                                        )
                                    }
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
                                    WithLocalizedContext(useHindiNumerals) {
                                        com.example.wealthtracker.ui.screens.CalculatorsScreen(
                                            onBack = { nav.popBackStack() },
                                            initialTab = tab,
                                            showBack = canGoBack
                                        )
                                    }
                                }
                                composable("settings") {
                                    com.example.wealthtracker.ui.screens.SettingsScreen(
                                        darkMode = darkMode,
                                        onToggleDarkMode = {
                                            darkMode = !darkMode
                                            scope.launch { com.example.wealthtracker.data.SettingsStore.setDarkMode(applicationContext, darkMode) }
                                        },
                                        requireDeviceLock = requireDeviceLock,
                                        onToggleRequireDeviceLock = {
                                            requireDeviceLock = !requireDeviceLock
                                            scope.launch { com.example.wealthtracker.data.SettingsStore.setRequireLock(applicationContext, requireDeviceLock) }
                                            if (requireDeviceLock) {
                                                authenticated = false
                                            }
                                        },
                                        useHindiNumerals = useHindiNumerals,
                                        onToggleHindiNumerals = {
                                            useHindiNumerals = !useHindiNumerals
                                            com.example.wealthtracker.util.FormatUtils.setUseHindiNumerals(useHindiNumerals)
                                            scope.launch { com.example.wealthtracker.data.SettingsStore.setHindiNumerals(applicationContext, useHindiNumerals) }
                                            pendingLocaleTag = if (useHindiNumerals) "hi-IN" else "en-IN"
                                        },
                                        onBack = { nav.popBackStack() }
                                    )
                                }
                            }
                            }
                            // App lock overlay when required and not authenticated: system device credential
                            if (requireDeviceLock && !authenticated) {
                                // Launcher for Keyguard confirm intent
                                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                                    if (result.resultCode == RESULT_OK) authenticated = true
                                }
                                val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
                                LaunchedEffect(Unit) {
                                    if (km.isDeviceSecure) {
                                        val intent = km.createConfirmDeviceCredentialIntent("Unlock", "Confirm device lock to continue")
                                        if (intent != null) launcher.launch(intent)
                                    }
                                }
                                Box(Modifier.fillMaxSize()) {
                                    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f), modifier = Modifier.fillMaxSize()) {}
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = "Unlock", style = MaterialTheme.typography.titleLarge)
                                        Spacer(Modifier.height(12.dp))
                                        if (!km.isDeviceSecure) {
                                            Text(text = "Set a device screen lock to protect the app")
                                            Spacer(Modifier.height(12.dp))
                                            androidx.compose.material3.Button(onClick = { startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }) {
                                                Text("Open Security Settings")
                                            }
                                        } else {
                                            androidx.compose.material3.Button(onClick = {
                                                val intent = km.createConfirmDeviceCredentialIntent("Unlock", "Confirm device lock to continue")
                                                if (intent != null) launcher.launch(intent)
                                            }) {
                                                Text("Unlock")
                                            }
                                        }
                                    }
                                }
                            }
                    }
                }
            }
        }
    }

@Composable
private fun rememberStartRoute(fromNotification: Boolean, vm: InvestmentViewModel): String? {
    var route by remember(fromNotification) { mutableStateOf<String?>(null) }
    LaunchedEffect(fromNotification) {
        if (fromNotification) {
            val firstList = vm.investments.first()
            route = if (firstList.isNotEmpty()) "dashboard" else "invest"
        } else {
            kotlinx.coroutines.coroutineScope {
                val collector = launch {
                    vm.investments.collect { list ->
                        if (list.isNotEmpty() && route == null) {
                            route = "dashboard"
                        }
                    }
                }
                launch {
                    delay(600)
                    if (route == null) route = "invest"
                    collector.cancel()
                }
                // Both run; the first to set route wins
            }
        }
    }
    return route
}

@Composable
private fun WithLocalizedContext(useHindiNumerals: Boolean, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val localized = remember(useHindiNumerals, base) {
        val tag = if (useHindiNumerals) "hi-IN" else "en-IN"
        val conf = Configuration(base.resources.configuration)
        conf.setLocales(android.os.LocaleList.forLanguageTags(tag))
        base.createConfigurationContext(conf)
    }
    key(useHindiNumerals) {
        CompositionLocalProvider(LocalContext provides localized) {
            content()
        }
    }
}

    override fun onStop() {
        super.onStop()
        // Apply pending locale in background so next resume shows updated language without flicker
        pendingLocaleTag?.let { tag ->
            val toApply = LocaleListCompat.forLanguageTags(tag)
            AppCompatDelegate.setApplicationLocales(toApply)
            pendingLocaleTag = null
        }
    }

    // No transition suppression needed; we avoid recreation altogether

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    private fun logCurrentFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM", "Token: $token")
        }
    }

}
