package xyz.pepe.glacierclient.calculatorapp.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class CalculatorSettings(
    val hapticFeedbackEnabled: Boolean = true,
    val useRadians: Boolean = false,
    val keepHistory: Boolean = true
)

private val HAPTICS = booleanPreferencesKey("haptics_enabled")
private val RADIANS = booleanPreferencesKey("use_radians")
private val KEEP_HISTORY = booleanPreferencesKey("keep_history")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<CalculatorSettings> = dataStore.data.map { prefs ->
        CalculatorSettings(
            hapticFeedbackEnabled = prefs[HAPTICS] ?: true,
            useRadians = prefs[RADIANS] ?: false,
            keepHistory = prefs[KEEP_HISTORY] ?: true
        )
    }

    suspend fun setHaptics(enabled: Boolean) = dataStore.edit { it[HAPTICS] = enabled }
    suspend fun setUseRadians(enabled: Boolean) = dataStore.edit { it[RADIANS] = enabled }
    suspend fun setKeepHistory(enabled: Boolean) = dataStore.edit { it[KEEP_HISTORY] = enabled }
}
