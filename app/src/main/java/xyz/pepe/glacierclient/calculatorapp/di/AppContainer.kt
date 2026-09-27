package xyz.pepe.glacierclient.calculatorapp.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import xyz.pepe.glacierclient.calculatorapp.data.SettingsRepository
import xyz.pepe.glacierclient.calculatorapp.util.InkRecognizerManager

private val Context.dataStore by preferencesDataStore(name = "calculator_settings")

/** Manual DI container, matching the pattern shared with Pepe's Notes / Clock / Weather. */
class AppContainer(context: Context) {
    val settingsRepository: SettingsRepository = SettingsRepository(context.dataStore)
    val inkRecognizerManager: InkRecognizerManager = InkRecognizerManager()
}
