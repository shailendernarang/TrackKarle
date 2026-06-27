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
import com.example.wealthtracker.ui.screens.AddInvestmentScreen
import com.example.wealthtracker.ui.screens.OnboardingScreen
import com.example.wealthtracker.ui.screens.OnboardingPrefs
import com.example.wealthtracker.ui.screens.ReferralScreen
import com.example.wealthtracker.ui.screens.CountrySelectionScreen
import com.example.wealthtracker.ui.screens.ModernSplashScreen
import com.example.wealthtracker.ui.screens.ReviewInvestmentScreen
import com.example.wealthtracker.ui.screens.InvestmentSuccessScreen
import com.example.wealthtracker.ui.screens.PortfolioScreen
import com.example.wealthtracker.data.UserPreferences
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
 
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.wealthtracker.ui.InvestmentViewModel
import com.example.wealthtracker.ui.DebtViewModel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.first
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.wealthtracker.ui.theme.WealthTrackerTheme
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.wealthtracker.util.FormatUtils
import com.example.wealthtracker.util.LocalActivity
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.core.app.ActivityCompat
import com.appodeal.ads.Appodeal
import com.appodeal.ads.initializing.ApdInitializationCallback
import com.appodeal.ads.initializing.ApdInitializationError
import com.google.firebase.messaging.FirebaseMessaging
import com.ss.wealthtracker.R
import com.ss.wealthtracker.BuildConfig
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
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.core.content.ContextCompat
import com.example.wealthtracker.ui.components.RatingPromptManager
import com.example.wealthtracker.util.BiometricUtils
import com.example.wealthtracker.ui.components.PrivacyConsentDialog
import com.example.wealthtracker.ui.components.PrivacyPreferences
import com.example.wealthtracker.util.MarketWebSocket

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var pendingLocaleTag: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 12+ system splash
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Initialize Appodeal SDK for ads
        Appodeal.setTesting(BuildConfig.DEBUG)
        Appodeal.setLogLevel(com.appodeal.ads.utils.Log.LogLevel.none)
        
        // Smart banners fill the full screen width instead of the fixed 320dp standard
        Appodeal.setSmartBanners(true)

        // Enable 728x90 banners for tablets (devices > 7 inches)
        Appodeal.set728x90Banners(true)
        
        // Enable shared ads instance to prevent reload on tab changes
        Appodeal.setSharedAdsInstanceAcrossActivities(true)
        
        // Auto-caching is enabled by default - banners load automatically
        // This prevents reload on navigation between screens
        
        // Initialize Appodeal SDK
        // Appodeal auto-CMP handles GDPR/CCPA consent (enabled by support team)
        Appodeal.initialize(
            this,
            com.ss.wealthtracker.BuildConfig.APPODEAL_API_KEY,
            Appodeal.BANNER or Appodeal.NATIVE,
            object : ApdInitializationCallback {
                override fun onInitializationFinished(errors: List<ApdInitializationError>?) {
                    if (BuildConfig.DEBUG) {
                        val initResult = if (errors.isNullOrEmpty()) "successfully" else "with ${errors.size} errors"
                        Log.d("Appodeal", "SDK Initialized $initResult")
                        errors?.forEach { error -> Log.e("Appodeal", "Init error: $error") }
                        Log.d("Appodeal", "Auto-cache BANNER: ${Appodeal.isAutoCacheEnabled(Appodeal.BANNER)}")
                        Log.d("Appodeal", "Auto-cache NATIVE: ${Appodeal.isAutoCacheEnabled(Appodeal.NATIVE)}")
                    }
                    // Explicitly cache native ads — auto-cache doesn't always trigger for native
                    Appodeal.cache(this@MainActivity, Appodeal.NATIVE)
                }
            }
        )
        
        Appodeal.setBannerCallbacks(object : com.appodeal.ads.BannerCallbacks {
            override fun onBannerLoaded(height: Int, isPrecache: Boolean) {
                if (BuildConfig.DEBUG) Log.d("Appodeal", "Banner loaded height=$height precache=$isPrecache")
            }
            override fun onBannerFailedToLoad() {
                if (BuildConfig.DEBUG) Log.e("Appodeal", "Banner failed to load")
            }
            override fun onBannerShown() {}
            override fun onBannerShowFailed() {
                if (BuildConfig.DEBUG) Log.e("Appodeal", "Banner failed to show")
            }
            override fun onBannerClicked() {}
            override fun onBannerExpired() {}
        })
        // Notifications permission (Android 13+)
        requestNotificationPermission()
        // Log current FCM token for testing
        logCurrentFcmToken()
        
        // Track app opens for rating prompt
        RatingPromptManager.incrementAppOpenCount(this)
        RatingPromptManager.startNewSession(this)
        
        setContent {
            val systemDark = isSystemInDarkTheme()
            var darkMode by remember { mutableStateOf(systemDark) }
            var requireDeviceLock by remember { mutableStateOf(false) }
            var useHindiNumerals by remember { mutableStateOf(false) }
            var authenticated by remember { mutableStateOf(true) }
            var showPrivacyConsent by remember { mutableStateOf(false) }
            var analyticsEnabled by remember { mutableStateOf(true) }
            var crashReportingEnabled by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                // Load persisted settings
                darkMode = SettingsStore.darkModeFlow(applicationContext).first()
                requireDeviceLock = SettingsStore.requireLockFlow(applicationContext).first()
                useHindiNumerals = SettingsStore.hindiNumeralsFlow(applicationContext).first()
                FormatUtils.setUseHindiNumerals(useHindiNumerals)
                
                // Initialize FormatUtils with user's selected currency
                FormatUtils.init(applicationContext)
                
                authenticated = !requireDeviceLock

                // Load privacy preferences first
                analyticsEnabled = PrivacyPreferences.isAnalyticsEnabled(applicationContext)
                crashReportingEnabled = PrivacyPreferences.isCrashReportingEnabled(applicationContext)

                // Delay consent dialog so it doesn't stack on top of the system notification
                // permission dialog that fires immediately at first launch
                kotlinx.coroutines.delay(1800)
                showPrivacyConsent = !PrivacyPreferences.hasConsentBeenGiven(applicationContext)
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
            CompositionLocalProvider(LocalActivity provides this@MainActivity) {
            WealthTrackerTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showComposeSplash by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
                    val fromNotification = intent?.getBooleanExtra("from_notification", false) == true
                    
                    // Initialize navigation early to avoid blank screen during transition
                    val nav = rememberNavController()
                    val vm: InvestmentViewModel = hiltViewModel()
                    val debtVm: DebtViewModel = hiltViewModel()
                    val userPrefs = remember { UserPreferences(this@MainActivity) }
                    
                    // Show Privacy Consent Dialog if not yet given
                    if (showPrivacyConsent) {
                        PrivacyConsentDialog(
                            onAccept = { analyticsEnabled, crashReportingEnabled ->
                                // Save consent preferences
                                PrivacyPreferences.setConsentGiven(applicationContext, true)
                                PrivacyPreferences.setAnalyticsEnabled(applicationContext, analyticsEnabled)
                                PrivacyPreferences.setCrashReportingEnabled(applicationContext, crashReportingEnabled)
                                
                                // Enable/disable Firebase Analytics
                                com.google.firebase.analytics.FirebaseAnalytics.getInstance(applicationContext)
                                    .setAnalyticsCollectionEnabled(analyticsEnabled)
                                
                                // Enable/disable Crashlytics
                                FirebaseCrashlytics.getInstance()
                                    .setCrashlyticsCollectionEnabled(crashReportingEnabled)
                                
                                showPrivacyConsent = false
                                Log.d("Privacy", "Consent accepted - Analytics: $analyticsEnabled, Crashlytics: $crashReportingEnabled")
                            },
                            onDecline = {
                                // User declined all tracking
                                PrivacyPreferences.setConsentGiven(applicationContext, true)
                                PrivacyPreferences.setAnalyticsEnabled(applicationContext, false)
                                PrivacyPreferences.setCrashReportingEnabled(applicationContext, false)
                                
                                // Disable Firebase Analytics
                                com.google.firebase.analytics.FirebaseAnalytics.getInstance(applicationContext)
                                    .setAnalyticsCollectionEnabled(false)
                                
                                // Disable Crashlytics
                                FirebaseCrashlytics.getInstance()
                                    .setCrashlyticsCollectionEnabled(false)
                                
                                showPrivacyConsent = false
                                Log.d("Privacy", "Consent declined - All tracking disabled")
                            }
                        )
                    }
                    
                    androidx.compose.animation.Crossfade(
                        targetState = showComposeSplash,
                        animationSpec = tween(150),
                        label = "splash_transition"
                    ) { isSplash ->
                        if (isSplash) {
                            ModernSplashScreen(
                                onSplashComplete = {
                                    showComposeSplash = false
                                }
                            )
                        } else {
                            // Check if onboarding is completed and country is set
                            val isOnboardingCompleted = OnboardingPrefs.isCompleted(this@MainActivity)
                            val isCountrySet = userPrefs.isCountrySet()
                            
                            val startRoute = if (!isOnboardingCompleted) {
                                "onboarding"
                            } else if (!isCountrySet) {
                                "countrySelection"
                            } else {
                                rememberStartRoute(fromNotification, vm)
                            }
                            
                            if (startRoute != null) {
                                NavHost(navController = nav, startDestination = startRoute) {
                                // Onboarding screen
                                composable("onboarding") {
                                    OnboardingScreen(
                                        onComplete = {
                                            OnboardingPrefs.setCompleted(this@MainActivity, true)
                                            // After onboarding, go to country selection
                                            nav.navigate("countrySelection") {
                                                popUpTo("onboarding") { inclusive = true }
                                            }
                                        }
                                    )
                                }
                                
                                // Country Selection screen
                                composable("countrySelection") {
                                    CountrySelectionScreen(
                                        onCountrySelected = { country ->
                                            // Save country selection
                                            userPrefs.setCountry(
                                                countryCode = country.countryCode,
                                                countryName = country.countryName,
                                                currencyCode = country.currencyCode,
                                                currencySymbol = country.currencySymbol
                                            )
                                            
                                            // Update FormatUtils with selected currency
                                            FormatUtils.setCurrency(
                                                symbol = country.currencySymbol,
                                                code = country.currencyCode,
                                                country = country.countryCode
                                            )
                                            
                                            // Navigate to dashboard
                                            nav.navigate("dashboard") {
                                                popUpTo("countrySelection") { inclusive = true }
                                            }
                                        }
                                    )
                                }
                                // Shared bottom-nav helper: pops to root and avoids duplicate entries
                                val navTab: (String) -> Unit = { route ->
                                    nav.navigate(route) {
                                        popUpTo("dashboard") { inclusive = false; saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }

                                composable(
                                    "dashboard",
                                    enterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                                ) {
                                    WithLocalizedContext(useHindiNumerals) {
                                        DashboardScreen(
                                            viewModel = vm,
                                            debtViewModel = debtVm,
                                            onAddClick = { nav.navigate("add_investment") },
                                            onOpenInvestments = { navTab("portfolio") },
                                            onOpenCalculators = { navTab("calculators") },
                                            onOpenSettings = { navTab("settings") },
                                            onOpenReminders = { nav.navigate("reminders") },
                                            onOpenDebt = { nav.navigate("debt") },
                                            onAddDebt  = { nav.navigate("debt?autoAdd=true") },
                                            onOpenStocks = { nav.navigate("stocks") }
                                        )
                                    }
                                }
                                composable(
                                    "debt?autoAdd={autoAdd}",
                                    arguments = listOf(androidx.navigation.navArgument("autoAdd") {
                                        defaultValue = false
                                        type = androidx.navigation.NavType.BoolType
                                    }),
                                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                                ) { backStack ->
                                    val autoAdd = backStack.arguments?.getBoolean("autoAdd") ?: false
                                    com.example.wealthtracker.ui.screens.DebtTrackerScreen(
                                        onBack = { nav.popBackStack() },
                                        autoOpenAdd = autoAdd
                                    )
                                }
                                composable(
                                    "stocks",
                                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                                ) {
                                    WithLocalizedContext(useHindiNumerals) {
                                        com.example.wealthtracker.ui.screens.StockAnalysisScreen(
                                            onBack = { nav.popBackStack() }
                                        )
                                    }
                                }
                                // Add Investment discovery flow
                                composable(
                                    "add_investment",
                                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                                ) {
                                    AddInvestmentScreen(
                                        viewModel = vm,
                                        onBack = {
                                            if (!nav.popBackStack()) {
                                                nav.navigate("dashboard") {
                                                    popUpTo(0) { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                            }
                                        },
                                        onTypeSelected = { investmentType, preOthersName ->
                                            val preOthersEncoded = preOthersName?.let {
                                                java.net.URLEncoder.encode(it, "UTF-8")
                                            } ?: ""
                                            nav.navigate("invest?preType=$investmentType&preOthers=$preOthersEncoded")
                                        }
                                    )
                                }
                                composable(
                                    route = "invest?preType={preType}&preOthers={preOthers}",
                                    arguments = listOf(
                                        androidx.navigation.navArgument("preType") {
                                            defaultValue = ""
                                            type = androidx.navigation.NavType.StringType
                                        },
                                        androidx.navigation.navArgument("preOthers") {
                                            defaultValue = ""
                                            type = androidx.navigation.NavType.StringType
                                        }
                                    ),
                                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                                ) { backStack ->
                                    val preType = backStack.arguments?.getString("preType")?.takeIf { it.isNotEmpty() }
                                    val preOthers = backStack.arguments?.getString("preOthers")
                                        ?.takeIf { it.isNotEmpty() }
                                        ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                                    val canGoBack = nav.previousBackStackEntry != null
                                    WithLocalizedContext(useHindiNumerals) {
                                        InvestmentScreen(
                                            viewModel = vm,
                                            onOpenDashboard = { navTab("dashboard") },
                                            onOpenPortfolio = { navTab("portfolio") },
                                            onOpenCalculators = { navTab("calculators") },
                                            onOpenStocks = { nav.navigate("stocks") },
                                            onOpenSettings = { navTab("settings") },
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
                                            },
                                            preSelectedType = preType,
                                            preOthersName = preOthers,
                                            onReviewInvestment = { nav.navigate("review_investment") },
                                            onAddClick = { nav.navigate("add_investment") }
                                        )
                                    }
                                }
                                // Review investment before saving
                                composable(
                                    "review_investment",
                                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                                ) {
                                    ReviewInvestmentScreen(
                                        viewModel = vm,
                                        onBack = { nav.popBackStack() },
                                        onConfirm = { nav.navigate("investment_success") }
                                    )
                                }
                                // Success screen after adding investment
                                composable(
                                    "investment_success",
                                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                                ) {
                                    InvestmentSuccessScreen(
                                        viewModel = vm,
                                        onViewPortfolio = {
                                            // Clear entire stack, land on dashboard (establishes
                                            // back-stack root), then immediately go to portfolio
                                            nav.navigate("dashboard") {
                                                popUpTo(0) { inclusive = true }
                                                launchSingleTop = true
                                            }
                                            nav.navigate("portfolio") {
                                                popUpTo("dashboard") { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        },
                                        onAddAnother = {
                                            nav.navigate("add_investment") {
                                                popUpTo(0) { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                                composable(
                                    "portfolio",
                                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                                ) {
                                    PortfolioScreen(
                                        viewModel = vm,
                                        debtViewModel = debtVm,
                                        onGoHome = { navTab("dashboard") },
                                        onGoCalculators = { navTab("calculators") },
                                        onGoSettings = { navTab("settings") },
                                        onAddClick = { nav.navigate("add_investment") }
                                    )
                                }
                                composable(
                                    route = "calculators?tab={tab}",
                                    arguments = listOf(androidx.navigation.navArgument("tab") {
                                        defaultValue = ""
                                        type = androidx.navigation.NavType.StringType
                                    }),
                                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                                ) { backStackEntry ->
                                    val tab = backStackEntry.arguments?.getString("tab")
                                    val canGoBack = nav.previousBackStackEntry != null
                                    WithLocalizedContext(useHindiNumerals) {
                                        com.example.wealthtracker.ui.screens.CalculatorsScreen(
                                            onBack = { nav.popBackStack() },
                                            initialTab = tab,
                                            showBack = canGoBack,
                                            onGoHome = { navTab("dashboard") },
                                            onGoPortfolio = { navTab("portfolio") },
                                            onGoSettings = { navTab("settings") },
                                            onAddClick = { nav.navigate("add_investment") }
                                        )
                                    }
                                }
                                composable(
                                    "settings",
                                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                                ) {
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
                                        analyticsEnabled = analyticsEnabled,
                                        onToggleAnalytics = {
                                            analyticsEnabled = !analyticsEnabled
                                            PrivacyPreferences.setAnalyticsEnabled(applicationContext, analyticsEnabled)
                                            com.google.firebase.analytics.FirebaseAnalytics.getInstance(applicationContext)
                                                .setAnalyticsCollectionEnabled(analyticsEnabled)
                                            Log.d("Privacy", "Analytics ${if (analyticsEnabled) "enabled" else "disabled"}")
                                        },
                                        crashReportingEnabled = crashReportingEnabled,
                                        onToggleCrashReporting = {
                                            crashReportingEnabled = !crashReportingEnabled
                                            PrivacyPreferences.setCrashReportingEnabled(applicationContext, crashReportingEnabled)
                                            FirebaseCrashlytics.getInstance()
                                                .setCrashlyticsCollectionEnabled(crashReportingEnabled)
                                            Log.d("Privacy", "Crash reporting ${if (crashReportingEnabled) "enabled" else "disabled"}")
                                        },
                                        onBack = { nav.popBackStack() },
                                        onOpenReferral = { nav.navigate("referral") },
                                        onGoHome = { navTab("dashboard") },
                                        onGoPortfolio = { navTab("portfolio") },
                                        onGoCalculators = { navTab("calculators") },
                                        onAddClick = { nav.navigate("add_investment") }
                                    )
                                }
                                composable(
                                    "referral",
                                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                                ) {
                                    ReferralScreen(
                                        onBack = { nav.popBackStack() }
                                    )
                                }
                                composable(
                                    "reminders",
                                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                                ) {
                                    val ctx = LocalContext.current
                                    val reminderManager = remember { com.example.wealthtracker.data.ReminderManager.getInstance(ctx) }
                                    
                                    val activeReminders by reminderManager.activeReminders.collectAsState()
                                    val snoozedReminders by reminderManager.snoozedReminders.collectAsState()
                                    val dismissedReminders by reminderManager.dismissedReminders.collectAsState()
                                    
                                    com.example.wealthtracker.ui.screens.RemindersScreen(
                                        activeReminders = activeReminders,
                                        snoozedReminders = snoozedReminders,
                                        dismissedReminders = dismissedReminders,
                                        onGotIt = { reminderManager.dismissReminder(it.id) },
                                        onRemindLater = { reminderManager.snoozeReminder(it.id) },
                                        onDismiss = { reminderManager.archiveReminder(it.id) },
                                        onReactivate = { reminderManager.reactivateReminder(it.id) },
                                        onBack = { nav.popBackStack() }
                                    )
                                }

                            }
                            }
                            // App lock overlay when required and not authenticated: system device credential
                            if (requireDeviceLock && !authenticated) {
                                val ctx = androidx.compose.ui.platform.LocalContext.current
                                val activity = remember(ctx) { ctx as? androidx.appcompat.app.AppCompatActivity }
                                val executor = remember(activity) { activity?.let { ContextCompat.getMainExecutor(it) } }
                                val prompt = remember(activity, executor) {
                                    activity?.let {
                                        BiometricPrompt(
                                            it,
                                            executor!!,
                                            object : BiometricPrompt.AuthenticationCallback() {
                                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                                    authenticated = true
                                                }
                                            }
                                        )
                                    }
                                }
                                val promptInfo = remember {
                                    BiometricPrompt.PromptInfo.Builder()
                                        .setTitle("Unlock")
                                        .setSubtitle("Confirm device lock to continue")
                                        .setAllowedAuthenticators(BiometricUtils.getCompatibleAuthenticators())
                                        .apply {
                                            // For older versions, we need setNegativeButtonText when using DEVICE_CREDENTIAL
                                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                                                setNegativeButtonText("Cancel")
                                            }
                                        }
                                        .build()
                                }
                                val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
                                LaunchedEffect(Unit) {
                                    if (km.isDeviceSecure) {
                                        try {
                                            // Use BiometricUtils for consistent availability checking
                                            if (BiometricUtils.isDeviceLockReady(this@MainActivity)) {
                                                prompt?.authenticate(promptInfo)
                                            } else {
                                                // Device lock not ready, skip authentication
                                                Log.w("BiometricAuth", "Device lock not ready: ${BiometricUtils.getDeviceLockStatusMessage(this@MainActivity)}")
                                                authenticated = true
                                            }
                                        } catch (e: Exception) {
                                            // Fallback: Skip biometric auth if it fails
                                            Log.e("BiometricAuth", "Biometric authentication failed", e)
                                            authenticated = true
                                        }
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
                                            androidx.compose.material3.Button(onClick = { 
                                                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                startActivity(intent)
                                            }) {
                                                Text("Open Security Settings")
                                            }
                                        } else {
                                            androidx.compose.material3.Button(onClick = {
                                                prompt?.authenticate(promptInfo)
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
            } // end CompositionLocalProvider(LocalActivity)
        }
    }

@Composable
private fun rememberStartRoute(fromNotification: Boolean, vm: InvestmentViewModel): String? {
    var route by remember(fromNotification) { mutableStateOf<String?>(null) }
    LaunchedEffect(fromNotification) {
        // Fast count query - doesn't load all investment data
        route = if (vm.hasInvestments()) "dashboard" else "add_investment"
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

    override fun onStart() {
        super.onStart()
        // Pre-load CSV config so it's ready when MarketIndicesMarquee renders.
        // WS symbols + connect are owned by MarketIndicesMarquee (country-filtered).
        lifecycleScope.launch(Dispatchers.IO) {
            com.example.wealthtracker.util.MarketSchedule.load(applicationContext)
        }
    }

    override fun onStop() {
        super.onStop()
        // Only disconnect when truly going to background — not during screen rotation
        if (!isChangingConfigurations) MarketWebSocket.disconnect()
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
