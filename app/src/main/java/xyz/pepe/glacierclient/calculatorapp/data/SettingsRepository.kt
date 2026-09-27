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
    val keepHistory: Boolean = true,
    // Off by default: a fixed brand-green dark palette, matching Notes/Clock/Weather's default
    // look. On, it pulls Material You colors from the wallpaper instead (same toggle name/idea
    // as those apps' "wallpaper colors" setting).
    val wallpaperColorsEnabled: Boolean = false,
    val exportTargetPackage: String = NOTES_APP_PACKAGE
)

/** Pepe's Notes' application id — the default share target for "Export to Notes". */
const val NOTES_APP_PACKAGE = "xyz.pepe.glacierclient.notesapp"

private val HAPTICS = booleanPreferencesKey("haptics_enabled")
private val RADIANS = booleanPreferencesKey("use_radians")
private val KEEP_HISTORY = booleanPreferencesKey("keep_history")
private val WALLPAPER_COLORS = booleanPreferencesKey("wallpaper_colors_enabled")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<CalculatorSettings> = dataStore.data.map { prefs ->
        CalculatorSettings(
            hapticFeedbackEnabled = prefs[HAPTICS] ?: true,
            useRadians = prefs[RADIANS] ?: false,
            keepHistory = prefs[KEEP_HISTORY] ?: true,
            wallpaperColorsEnabled = prefs[WALLPAPER_COLORS] ?: false
        )
    }

    suspend fun setHaptics(enabled: Boolean) = dataStore.edit { it[HAPTICS] = enabled }
    suspend fun setUseRadians(enabled: Boolean) = dataStore.edit { it[RADIANS] = enabled }
    suspend fun setKeepHistory(enabled: Boolean) = dataStore.edit { it[KEEP_HISTORY] = enabled }
    suspend fun setWallpaperColors(enabled: Boolean) = dataStore.edit { it[WALLPAPER_COLORS] = enabled }
}
