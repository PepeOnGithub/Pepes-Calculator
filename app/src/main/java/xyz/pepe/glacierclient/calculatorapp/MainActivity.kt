package xyz.pepe.glacierclient.calculatorapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorViewModel
import xyz.pepe.glacierclient.calculatorapp.ui.screens.CalculatorScreen
import xyz.pepe.glacierclient.calculatorapp.ui.theme.PepesCalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CalculatorApplication).container
        setContent {
            val viewModel: CalculatorViewModel = viewModel(
                factory = CalculatorViewModel.Factory(container.settingsRepository)
            )
            val uiState by viewModel.uiState.collectAsState()
            PepesCalculatorTheme(wallpaperColorsEnabled = uiState.settings.wallpaperColorsEnabled) {
                CalculatorScreen(viewModel = viewModel)
            }
        }
    }
}
