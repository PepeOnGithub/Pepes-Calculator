package xyz.pepe.glacierclient.calculatorapp.ui.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.pepe.glacierclient.calculatorapp.data.SettingsRepository
import xyz.pepe.glacierclient.calculatorapp.domain.CalculatorEngine
import java.math.BigDecimal
import java.math.RoundingMode

class CalculatorViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun onIntent(intent: CalculatorIntent) {
        when (intent) {
            is CalculatorIntent.InputToken -> inputToken(intent.token)
            CalculatorIntent.Backspace -> backspace()
            CalculatorIntent.ClearAll -> _uiState.update { it.copy(expression = "", display = "0", isError = false) }
            CalculatorIntent.Evaluate -> evaluate()
            CalculatorIntent.ToggleMode -> _uiState.update {
                it.copy(mode = if (it.mode == CalculatorMode.BASIC) CalculatorMode.SCIENTIFIC else CalculatorMode.BASIC)
            }
            is CalculatorIntent.UseHistoryEntry -> _uiState.update {
                it.copy(expression = intent.entry.expression, display = intent.entry.expression, isHistoryOpen = false, isError = false)
            }
            CalculatorIntent.ClearHistory -> _uiState.update { it.copy(history = emptyList()) }
            is CalculatorIntent.DeleteHistoryEntry -> _uiState.update { it.copy(history = it.history - intent.entry) }
            is CalculatorIntent.ToggleHistory -> _uiState.update { it.copy(isHistoryOpen = intent.open) }
            is CalculatorIntent.ToggleSettings -> _uiState.update { it.copy(isSettingsOpen = intent.open) }
            is CalculatorIntent.SetHaptics -> viewModelScope.launch { settingsRepository.setHaptics(intent.enabled) }
            is CalculatorIntent.SetUseRadians -> viewModelScope.launch { settingsRepository.setUseRadians(intent.enabled) }
            is CalculatorIntent.SetKeepHistory -> viewModelScope.launch { settingsRepository.setKeepHistory(intent.enabled) }
        }
    }

    private fun inputToken(token: String) {
        _uiState.update { state ->
            val expr = if (state.isError) "" else state.expression
            val next = expr + token
            state.copy(expression = next, display = next.ifEmpty { "0" }, isError = false)
        }
    }

    private fun backspace() {
        _uiState.update { state ->
            if (state.isError) return@update state.copy(expression = "", display = "0", isError = false)
            val next = state.expression.dropLast(1)
            state.copy(expression = next, display = next.ifEmpty { "0" })
        }
    }

    private fun evaluate() {
        val state = _uiState.value
        if (state.expression.isBlank()) return
        try {
            val raw = CalculatorEngine.evaluate(state.expression, state.settings.useRadians)
            val formatted = formatResult(raw)
            val entry = HistoryEntry(expression = state.expression, result = formatted)
            _uiState.update {
                it.copy(
                    expression = formatted,
                    display = formatted,
                    isError = false,
                    history = if (it.settings.keepHistory) (listOf(entry) + it.history).take(50) else it.history
                )
            }
        } catch (e: CalculatorEngine.EvaluationException) {
            _uiState.update { it.copy(display = "Error", isError = true) }
        } catch (e: ArithmeticException) {
            _uiState.update { it.copy(display = "Error", isError = true) }
        }
    }

    private fun formatResult(value: Double): String {
        val decimal = BigDecimal(value).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros()
        val plain = decimal.toPlainString()
        return if (plain.length > 15) {
            String.format("%.6e", value)
        } else plain
    }

    class Factory(private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalculatorViewModel(settingsRepository) as T
        }
    }
}
