package xyz.pepe.glacierclient.calculatorapp.ui.mvi

sealed interface CalculatorIntent {
    data class InputToken(val token: String) : CalculatorIntent
    object Backspace : CalculatorIntent
    object ClearAll : CalculatorIntent
    object Evaluate : CalculatorIntent
    object ToggleMode : CalculatorIntent
    data class UseHistoryEntry(val entry: HistoryEntry) : CalculatorIntent
    object ClearHistory : CalculatorIntent
    data class DeleteHistoryEntry(val entry: HistoryEntry) : CalculatorIntent
    data class ToggleHistory(val open: Boolean) : CalculatorIntent
    data class ToggleSettings(val open: Boolean) : CalculatorIntent
    data class SetHaptics(val enabled: Boolean) : CalculatorIntent
    data class SetUseRadians(val enabled: Boolean) : CalculatorIntent
    data class SetKeepHistory(val enabled: Boolean) : CalculatorIntent
    data class SetWallpaperColors(val enabled: Boolean) : CalculatorIntent
    data class ToggleSettingsSection(val section: SettingsSection?) : CalculatorIntent
    data class SetNotesLine(val index: Int, val value: String) : CalculatorIntent
    data class AddNotesLine(val text: String = "") : CalculatorIntent

    // Handwriting input for Math Notes — real ML Kit Digital Ink Recognition.
    data class RecognizeInk(val targetLine: Int, val strokes: List<List<Pair<Float, Float>>>) : CalculatorIntent
    object PrepareInkModel : CalculatorIntent
}
