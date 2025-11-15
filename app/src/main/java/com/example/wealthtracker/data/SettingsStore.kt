package com.example.wealthtracker.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)

object SettingsStore {
    private val DARK_MODE = booleanPreferencesKey("dark_mode")
    private val REQUIRE_LOCK = booleanPreferencesKey("require_device_lock")
    private val HINDI_NUMERALS = booleanPreferencesKey("hindi_numerals")

    fun darkModeFlow(context: Context): Flow<Boolean> =
        context.dataStore.data
            .catch { exception ->
                // Handle corruption by emitting empty preferences
                if (exception is IOException || exception is CorruptionException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { it[DARK_MODE] ?: false }

    fun requireLockFlow(context: Context): Flow<Boolean> =
        context.dataStore.data
            .catch { exception ->
                if (exception is IOException || exception is CorruptionException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { it[REQUIRE_LOCK] ?: false }

    fun hindiNumeralsFlow(context: Context): Flow<Boolean> =
        context.dataStore.data
            .catch { exception ->
                if (exception is IOException || exception is CorruptionException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { it[HINDI_NUMERALS] ?: false }

    suspend fun setDarkMode(context: Context, enabled: Boolean) {
        try {
            context.dataStore.edit { prefs ->
                prefs[DARK_MODE] = enabled
            }
        } catch (e: IOException) {
            // Handle write errors gracefully - preferences will use default values
            android.util.Log.w("SettingsStore", "Failed to save dark mode preference", e)
        } catch (e: RuntimeException) {
            // Handle protobuf serialization errors
            if (e.message?.contains("There is no way to get here") == true) {
                android.util.Log.e("SettingsStore", "Protobuf serialization error, clearing DataStore", e)
                clearAll(context)
            } else {
                android.util.Log.w("SettingsStore", "Runtime error saving dark mode preference", e)
            }
        }
    }

    suspend fun setRequireLock(context: Context, enabled: Boolean) {
        try {
            context.dataStore.edit { prefs ->
                prefs[REQUIRE_LOCK] = enabled
            }
        } catch (e: IOException) {
            android.util.Log.w("SettingsStore", "Failed to save lock requirement preference", e)
        } catch (e: RuntimeException) {
            if (e.message?.contains("There is no way to get here") == true) {
                android.util.Log.e("SettingsStore", "Protobuf serialization error, clearing DataStore", e)
                clearAll(context)
            } else {
                android.util.Log.w("SettingsStore", "Runtime error saving lock requirement preference", e)
            }
        }
    }

    suspend fun setHindiNumerals(context: Context, enabled: Boolean) {
        try {
            context.dataStore.edit { prefs ->
                prefs[HINDI_NUMERALS] = enabled
            }
        } catch (e: IOException) {
            android.util.Log.w("SettingsStore", "Failed to save Hindi numerals preference", e)
        } catch (e: RuntimeException) {
            if (e.message?.contains("There is no way to get here") == true) {
                android.util.Log.e("SettingsStore", "Protobuf serialization error, clearing DataStore", e)
                clearAll(context)
            } else {
                android.util.Log.w("SettingsStore", "Runtime error saving Hindi numerals preference", e)
            }
        }
    }
    
    /**
     * Clear all DataStore preferences - useful for recovery from corruption
     */
    suspend fun clearAll(context: Context) {
        try {
            context.dataStore.edit { prefs ->
                prefs.clear()
            }
            android.util.Log.i("SettingsStore", "All preferences cleared successfully")
        } catch (e: IOException) {
            android.util.Log.w("SettingsStore", "Failed to clear preferences", e)
            // If we can't clear via DataStore, try to delete the file directly
            try {
                val prefsFile = context.filesDir.resolve("datastore/settings.preferences_pb")
                if (prefsFile.exists() && prefsFile.delete()) {
                    android.util.Log.i("SettingsStore", "Corrupted preferences file deleted")
                }
            } catch (fileException: Exception) {
                android.util.Log.w("SettingsStore", "Failed to delete corrupted preferences file", fileException)
            }
        }
    }
}
