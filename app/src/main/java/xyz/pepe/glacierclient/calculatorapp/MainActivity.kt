package xyz.pepe.glacierclient.calculatorapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import xyz.pepe.glacierclient.calculatorapp.ui.components.LocalGlassEnabled
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorIntent
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorViewModel
import xyz.pepe.glacierclient.calculatorapp.ui.nav.AppRoot
import xyz.pepe.glacierclient.calculatorapp.ui.theme.PepesCalculatorTheme
import xyz.pepe.glacierclient.ecosystem.PepeEcosystem

class MainActivity : ComponentActivity() {
  // Calculator's ecosystem agent: `pepe://calculator/calc?expr=…` (already validated to math
  // characters by PepeEcosystem.parse) replaces the display with that expression.
  private val pendingExpression = mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    if (savedInstanceState == null) route(intent)
    val container = (application as CalculatorApplication).container
    setContent {
      val vm: CalculatorViewModel = viewModel(
        factory = CalculatorViewModel.Factory(container.settingsRepository, container.currencyRatesSource, container.inkRecognizerManager),
      )
      val state by vm.uiState.collectAsStateWithLifecycle()
      val expression = pendingExpression.value
      LaunchedEffect(expression) {
        if (expression == null) return@LaunchedEffect
        vm.onIntent(CalculatorIntent.ClearAll)
        vm.onIntent(CalculatorIntent.Paste(expression))
        pendingExpression.value = null
      }
      PepesCalculatorTheme(wallpaperColors = state.settings.wallpaperColors) {
        CompositionLocalProvider(LocalGlassEnabled provides state.settings.glassEffects) { AppRoot(vm) }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    route(intent)
  }

  private fun route(intent: Intent?) {
    (PepeEcosystem.actionFrom(intent) as? PepeEcosystem.Calculate)?.let { pendingExpression.value = it.expression }
  }
}
