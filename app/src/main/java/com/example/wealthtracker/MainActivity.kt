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
import com.example.wealthtracker.ui.screens.OnboardingScreen
import com.example.wealthtracker.ui.screens.OnboardingPrefs
import com.example.wealthtracker.ui.screens.ReferralScreen
import com.example.wealthtracker.ui.screens.CountrySelectionScreen
import com.example.wealthtracker.ui.screens.ModernSplashScreen
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

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var pendingLocaleTag: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 12+ system splash
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Initialize Appodeal SDK for ads
        Appodeal.setTesting(false) // Production mode - real ads
        Appodeal.setLogLevel(com.appodeal.ads.utils.Log.LogLevel.none) // Production - no verbose logging
        
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
            Appodeal.BANNER,
            object : ApdInitializationCallback {
                override fun onInitializationFinished(errors: List<ApdInitializationError>?) {
                    val initResult = if (errors.isNullOrEmpty()) "successfully" else "with ${errors.size} errors"
                    Log.d("Appodeal", "✅ SDK Initialized $initResult")
                    errors?.forEach { error ->
                        Log.e("Appodeal", "❌ Init error: $error")
                    }
                    // Auto-cache is enabled by default and optimal for banners
                    // Banners auto-refresh every 15 seconds - no manual cache needed
                    Log.d("Appodeal", "📊 Auto-cache enabled: ${Appodeal.isAutoCacheEnabled(Appodeal.BANNER)}")
                    Log.d("Appodeal", "🔄 Shared instance enabled: ${Appodeal.isSharedAdsInstanceAcrossActivities()}")
                }
            }
        )
        
        // Set banner callbacks for debugging
        Appodeal.setBannerCallbacks(object : com.appodeal.ads.BannerCallbacks {
            override fun onBannerLoaded(height: Int, isPrecache: Boolean) {
                Log.d("Appodeal", "✅ Banner LOADED - height: $height, isPrecache: $isPrecache")
                Log.d("Appodeal", "📊 Banner isLoaded: ${Appodeal.isLoaded(Appodeal.BANNER)}")
            }
            
            override fun onBannerFailedToLoad() {
                Log.e("Appodeal", "❌ Banner FAILED to load")
                Log.e("Appodeal", "📊 SDK initialized: ${Appodeal.isInitialized(Appodeal.BANNER)}")
            }
            
            override fun onBannerShown() {
                Log.d("Appodeal", "Banner shown")
            }
            
            override fun onBannerShowFailed() {
                Log.e("Appodeal", "Banner failed to show")
            }
            
            override fun onBannerClicked() {
                Log.d("Appodeal", "Banner clicked")
            }
            
            override fun onBannerExpired() {
                Log.d("Appodeal", "Banner expired")
            }
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
            var isPremium by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                // Load persisted settings
                darkMode = SettingsStore.darkModeFlow(applicationContext).first()
                requireDeviceLock = SettingsStore.requireLockFlow(applicationContext).first()
                useHindiNumerals = SettingsStore.hindiNumeralsFlow(applicationContext).first()
                FormatUtils.setUseHindiNumerals(useHindiNumerals)
                
                // Initialize FormatUtils with user's selected currency
                FormatUtils.init(applicationContext)
                
                authenticated = !requireDeviceLock
                
                // Check if privacy consent has been given
                showPrivacyConsent = !PrivacyPreferences.hasConsentBeenGiven(applicationContext)
                
                // Load privacy preferences
                analyticsEnabled = PrivacyPreferences.isAnalyticsEnabled(applicationContext)
                crashReportingEnabled = PrivacyPreferences.isCrashReportingEnabled(applicationContext)
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
                    
                    // Initialize navigation early to avoid blank screen during transition
                    val nav = rememberNavController()
                    val vm: InvestmentViewModel = hiltViewModel()
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
                                            onAddClick = { nav.navigate("invest") },
                                            onOpenInvestments = { nav.navigate("invest") },
                                            onOpenCalculators = { nav.navigate("calculators?tab=fd") },
                                            onOpenSettings = { nav.navigate("settings") },
                                            onOpenReminders = { nav.navigate("reminders") }
                                        )
                                    }
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
                                composable(
                                    "invest",
                                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                                ) {
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
                                            showBack = canGoBack
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
                                        isPremium = isPremium,
                                        onBack = { nav.popBackStack() },
                                        onOpenReferral = { nav.navigate("referral") },
                                        onOpenPremium = { nav.navigate("premium") }
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
                                composable(
                                    "premium",
                                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                                ) {
                                    com.example.wealthtracker.ui.screens.PremiumScreen(
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
        }
    }

@Composable
private fun rememberStartRoute(fromNotification: Boolean, vm: InvestmentViewModel): String? {
    var route by remember(fromNotification) { mutableStateOf<String?>(null) }
    LaunchedEffect(fromNotification) {
        // Fast count query - doesn't load all investment data
        route = if (vm.hasInvestments()) "dashboard" else "invest"
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
