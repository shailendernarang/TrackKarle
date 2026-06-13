package com.example.wealthtracker.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.google.firebase.perf.metrics.Trace

// Extension for ViewModels to easily access analytics
inline fun <reified T : ViewModel> T.withAnalytics(analyticsManager: AnalyticsManager): T {
    return this
}

// Composable for automatic screen tracking
@Composable
fun TrackScreen(
    screenName: String,
    screenClass: String? = null,
    analyticsManager: AnalyticsManager
) {
    DisposableEffect(screenName) {
        analyticsManager.logScreenView(screenName, screenClass)
        onDispose { }
    }
}

// Performance tracing composable
@Composable
fun TracePerformance(
    traceName: String,
    performanceManager: PerformanceManager,
    content: @Composable (Trace) -> Unit
) {
    val trace = remember(traceName) {
        performanceManager.startTrace(traceName)
    }
    
    DisposableEffect(trace) {
        onDispose {
            performanceManager.stopTrace(trace)
        }
    }
    
    content(trace)
}

// Inline functions for common analytics patterns
inline fun AnalyticsManager.trackUserAction(
    action: String,
    screen: String,
    additionalParams: Map<String, Any> = emptyMap()
) {
    logFeatureUsed(action, screen)
}

inline fun AnalyticsManager.trackError(
    error: Throwable,
    screen: String,
    context: String? = null
) {
    logUserError(
        errorType = error.javaClass.simpleName,
        screenName = screen,
        errorCode = context
    )
}

// Performance tracking extensions
inline fun <T> PerformanceManager.measurePerformance(
    traceName: String,
    attributes: Map<String, String> = emptyMap(),
    block: (Trace) -> T
): T {
    val trace = startTrace(traceName)
    attributes.forEach { (key, value) ->
        addTraceAttribute(trace, key, value)
    }
    return try {
        block(trace)
    } finally {
        stopTrace(trace)
    }
}

// Common screen names as constants
object ScreenNames {
    const val HOME = "home_screen"
    const val ADD_INVESTMENT = "add_investment_screen"
    const val INVESTMENT_DETAILS = "investment_details_screen"
    const val PORTFOLIO_OVERVIEW = "portfolio_overview_screen"
    const val SETTINGS = "settings_screen"
    const val CHARTS = "charts_screen"
    const val CALCULATOR = "calculator_screen"
    const val EXPORT_DATA = "export_data_screen"
    const val IMPORT_DATA = "import_data_screen"
}

// Common event names
object EventNames {
    const val INVESTMENT_CREATION_STARTED = "investment_creation_started"
    const val INVESTMENT_ADDED = "investment_added_successfully"
    const val COUNTRY_PREFERENCE_SET = "country_preference_set"
    const val FILTER_APPLIED = "filter_applied"
    const val SEARCH_PERFORMED = "search_performed"
    const val CHART_VIEWED = "chart_viewed"
    const val DATA_EXPORTED = "data_exported"
    const val SETTINGS_CHANGED = "settings_changed"
}

// Investment types
object InvestmentTypes {
    const val FIXED_DEPOSIT = "fixed_deposit"
    const val RECURRING_DEPOSIT = "recurring_deposit"
    const val SAVINGS_ACCOUNT = "savings_account"
    const val MUTUAL_FUND = "mutual_fund"
    const val STOCKS = "stocks"
    const val BONDS = "bonds"
    const val PPF = "ppf"
    const val NSC = "nsc"
    const val OTHER = "other"
}
