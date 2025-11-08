package com.example.wealthtracker.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

object SettingsStore {
    private val DARK_MODE = booleanPreferencesKey("dark_mode")
    private val REQUIRE_LOCK = booleanPreferencesKey("require_device_lock")
    private val HINDI_NUMERALS = booleanPreferencesKey("hindi_numerals")

    fun darkModeFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[DARK_MODE] ?: false }

    fun requireLockFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[REQUIRE_LOCK] ?: false }

    fun hindiNumeralsFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[HINDI_NUMERALS] ?: false }

    suspend fun setDarkMode(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE] = enabled
        }
    }

    suspend fun setRequireLock(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[REQUIRE_LOCK] = enabled
        }
    }

    suspend fun setHindiNumerals(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HINDI_NUMERALS] = enabled
        }
    }
}
