package com.example.wealthtracker.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        Firebase.analytics
    }

    // Screen tracking events
    fun logScreenView(screenName: String, screenClass: String? = null) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            screenClass?.let { putString(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    // Investment related events
    fun logInvestmentCreationStarted(investmentType: String) {
        val bundle = Bundle().apply {
            putString("investment_type", investmentType)
            putString("action", "investment_creation_started")
        }
        firebaseAnalytics.logEvent("investment_management", bundle)
    }

    fun logInvestmentAdded(investmentType: String) {
        val bundle = Bundle().apply {
            putString("investment_type", investmentType)
            putString("action", "investment_added_successfully")
        }
        firebaseAnalytics.logEvent("investment_management", bundle)
    }

    fun logCountryPreferenceSet(country: String, context: String = "first_time_setup") {
        val bundle = Bundle().apply {
            putString("country", country)
            putString("selection_context", context) // "first_time_setup", "settings_change"
        }
        firebaseAnalytics.logEvent("country_preference_set", bundle)
    }

    fun logInvestmentTypeSelected(investmentType: String) {
        val bundle = Bundle().apply {
            putString("investment_type", investmentType)
        }
        firebaseAnalytics.logEvent("investment_type_selection", bundle)
    }

    // Filter and search events
    fun logFilterUsed(filterType: String, filterValue: String, screenName: String) {
        val bundle = Bundle().apply {
            putString("filter_type", filterType)
            putString("filter_value", filterValue)
            putString("screen_name", screenName)
        }
        firebaseAnalytics.logEvent("filter_usage", bundle)
    }

    fun logSearchPerformed(searchQuery: String, screenName: String, resultsCount: Int = 0) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SEARCH_TERM, searchQuery)
            putString("screen_name", screenName)
            putInt("results_count", resultsCount)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
    }

    // User engagement events
    fun logPlayStoreRedirect(source: String = "rating_prompt") {
        val bundle = Bundle().apply {
            putString("source", source)
            putString("destination", "play_store")
        }
        firebaseAnalytics.logEvent("external_redirect", bundle)
    }

    fun logRatingPromptShown() {
        firebaseAnalytics.logEvent("rating_prompt_shown", Bundle())
    }

    fun logRatingPromptInteraction(action: String) {
        val bundle = Bundle().apply {
            putString("action", action) // "rate_now", "later", "dismiss"
        }
        firebaseAnalytics.logEvent("rating_prompt_interaction", bundle)
    }

    // Portfolio and calculation events
    fun logPortfolioCalculation(
        calculationType: String,
        investmentCount: Int,
        totalValue: Double? = null
    ) {
        val bundle = Bundle().apply {
            putString("calculation_type", calculationType)
            putInt("investment_count", investmentCount)
            totalValue?.let { putDouble("total_value", it) }
        }
        firebaseAnalytics.logEvent("portfolio_calculation", bundle)
    }

    fun logChartViewed(chartType: String, timeRange: String? = null) {
        val bundle = Bundle().apply {
            putString("chart_type", chartType)
            timeRange?.let { putString("time_range", it) }
        }
        firebaseAnalytics.logEvent("chart_interaction", bundle)
    }

    // Settings and preferences
    fun logSettingChanged(settingName: String, newValue: String) {
        val bundle = Bundle().apply {
            putString("setting_name", settingName)
            putString("new_value", newValue)
        }
        firebaseAnalytics.logEvent("setting_changed", bundle)
    }

    fun logCurrencyChanged(oldCurrency: String, newCurrency: String) {
        val bundle = Bundle().apply {
            putString("old_currency", oldCurrency)
            putString("new_currency", newCurrency)
        }
        firebaseAnalytics.logEvent("currency_changed", bundle)
    }

    // Data management events
    fun logDataExport(exportFormat: String, recordCount: Int) {
        val bundle = Bundle().apply {
            putString("export_format", exportFormat)
            putInt("record_count", recordCount)
        }
        firebaseAnalytics.logEvent("data_export", bundle)
    }

    fun logDataImport(importSource: String, recordCount: Int, success: Boolean) {
        val bundle = Bundle().apply {
            putString("import_source", importSource)
            putInt("record_count", recordCount)
            putBoolean("success", success)
        }
        firebaseAnalytics.logEvent("data_import", bundle)
    }

    // Error tracking (non-PII)
    fun logUserError(errorType: String, screenName: String, errorCode: String? = null) {
        val bundle = Bundle().apply {
            putString("error_type", errorType)
            putString("screen_name", screenName)
            errorCode?.let { putString("error_code", it) }
        }
        firebaseAnalytics.logEvent("user_error", bundle)
    }

    // Biometric authentication events
    fun logBiometricAuthAttempt(success: Boolean, errorReason: String? = null) {
        val bundle = Bundle().apply {
            putBoolean("success", success)
            errorReason?.let { putString("error_reason", it) }
        }
        firebaseAnalytics.logEvent("biometric_auth_attempt", bundle)
    }

    fun logBiometricAvailability(isAvailable: Boolean, deviceApiLevel: Int) {
        val bundle = Bundle().apply {
            putBoolean("biometric_available", isAvailable)
            putInt("device_api_level", deviceApiLevel)
        }
        firebaseAnalytics.logEvent("biometric_availability", bundle)
    }

    // Database migration and integrity events
    fun logDatabaseMigration(fromVersion: Int, toVersion: Int, success: Boolean, recordCount: Int = 0) {
        val bundle = Bundle().apply {
            putInt("from_version", fromVersion)
            putInt("to_version", toVersion)
            putBoolean("success", success)
            putInt("record_count", recordCount)
        }
        firebaseAnalytics.logEvent("database_migration", bundle)
    }

    fun logDatabaseIntegrityCheck(totalRecords: Int, validRecords: Int, isHealthy: Boolean) {
        val bundle = Bundle().apply {
            putInt("total_records", totalRecords)
            putInt("valid_records", validRecords)
            putBoolean("is_healthy", isHealthy)
        }
        firebaseAnalytics.logEvent("database_integrity_check", bundle)
    }

    fun logDatabaseBackend(backend: String, encrypted: Boolean) {
        val bundle = Bundle().apply {
            putString("backend_type", backend)
            putBoolean("encrypted", encrypted)
        }
        firebaseAnalytics.logEvent("database_backend", bundle)
    }

    // Chart error tracking
    fun logChartError(chartType: String, errorType: String, errorMessage: String? = null) {
        val bundle = Bundle().apply {
            putString("chart_type", chartType)
            putString("error_type", errorType)
            errorMessage?.let { putString("error_message", it.take(100)) } // Limit message length
        }
        firebaseAnalytics.logEvent("chart_error", bundle)
    }

    // Feature usage tracking
    fun logFeatureUsed(featureName: String, context: String? = null) {
        val bundle = Bundle().apply {
            putString("feature_name", featureName)
            context?.let { putString("context", it) }
        }
        firebaseAnalytics.logEvent("feature_usage", bundle)
    }

    // User properties (non-PII)
    fun setUserProperty(propertyName: String, propertyValue: String) {
        firebaseAnalytics.setUserProperty(propertyName, propertyValue)
    }

    // Set user preferences as properties
    fun setUserPreferences(
        preferredCurrency: String? = null,
        appTheme: String? = null,
        notificationsEnabled: Boolean? = null
    ) {
        preferredCurrency?.let { setUserProperty("preferred_currency", it) }
        appTheme?.let { setUserProperty("app_theme", it) }
        notificationsEnabled?.let { setUserProperty("notifications_enabled", it.toString()) }
    }
}
