package xyz.pepe.glacierclient.calculatorapp.ui.mvi

sealed interface CalculatorIntent {
    data class InputToken(val token: String) : CalculatorIntent
    object Backspace : CalculatorIntent
    object ClearAll : CalculatorIntent
    object Evaluate : CalculatorIntent
    object ToggleMode : CalculatorIntent
    data class UseHistoryEntry(val entry: HistoryEntry) : CalculatorIntent
    object ClearHistory : CalculatorIntent
    data class ToggleHistory(val open: Boolean) : CalculatorIntent
    data class ToggleSettings(val open: Boolean) : CalculatorIntent
    data class SetHaptics(val enabled: Boolean) : CalculatorIntent
    data class SetUseRadians(val enabled: Boolean) : CalculatorIntent
    data class SetKeepHistory(val enabled: Boolean) : CalculatorIntent
}
