package com.example.wealthtracker.data

import android.content.Context
import android.content.SharedPreferences
import com.example.wealthtracker.util.CountryCurrency

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "wealth_tracker_prefs",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_COUNTRY_CODE = "country_code"
        private const val KEY_CURRENCY_CODE = "currency_code"
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        private const val KEY_COUNTRY_NAME = "country_name"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_APP_VERSION = "app_version"
        private const val KEY_FIRST_LAUNCH = "first_launch_timestamp"
    }
    
    // Track first launch to detect fresh installs
    init {
        if (prefs.getLong(KEY_FIRST_LAUNCH, 0L) == 0L) {
            // This is truly the first launch - mark it
            prefs.edit().putLong(KEY_FIRST_LAUNCH, System.currentTimeMillis()).apply()
        }
    }
    
    // Country and Currency
    fun setCountry(countryCode: String, countryName: String, currencyCode: String, currencySymbol: String) {
        prefs.edit().apply {
            putString(KEY_COUNTRY_CODE, countryCode)
            putString(KEY_COUNTRY_NAME, countryName)
            putString(KEY_CURRENCY_CODE, currencyCode)
            putString(KEY_CURRENCY_SYMBOL, currencySymbol)
            apply()
        }
    }
    
    fun getCountryCode(): String {
        return prefs.getString(KEY_COUNTRY_CODE, null) ?: "IN"
    }
    
    fun getCountryName(): String {
        return prefs.getString(KEY_COUNTRY_NAME, null) ?: "India"
    }
    
    fun getCurrencyCode(): String {
        return prefs.getString(KEY_CURRENCY_CODE, null) ?: "INR"
    }
    
    fun getCurrencySymbol(): String {
        return prefs.getString(KEY_CURRENCY_SYMBOL, null) ?: "₹"
    }
    
    fun isCountrySet(): Boolean {
        return prefs.getString(KEY_COUNTRY_CODE, null) != null
    }
    
    // Onboarding
    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }
    
    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }
    
    // App Version (for detecting updates)
    fun setAppVersion(version: String) {
        prefs.edit().putString(KEY_APP_VERSION, version).apply()
    }
    
    fun getAppVersion(): String? {
        return prefs.getString(KEY_APP_VERSION, null)
    }
    
    fun shouldShowCountrySelection(currentVersion: String): Boolean {
        // Show if:
        // 1. Country is not set
        // 2. OR if app version changed and country wasn't set in previous version
        val savedVersion = getAppVersion()
        val countryNotSet = !isCountrySet()
        val versionChanged = savedVersion != null && savedVersion != currentVersion
        
        return countryNotSet || (versionChanged && countryNotSet)
    }
    
    // Clear all preferences
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
