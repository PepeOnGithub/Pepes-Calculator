package xyz.pepe.glacierclient.calculatorapp.ui.mvi

import xyz.pepe.glacierclient.calculatorapp.data.CalculatorSettings

data class HistoryEntry(val expression: String, val result: String)

enum class CalculatorMode { BASIC, SCIENTIFIC }

/** Category rows on the Settings main page, each opening its own sub-page — matching the
 *  Pixel Settings pattern shared with Notes/Clock/Weather. */
enum class SettingsSection { APPEARANCE, CALCULATION, DATA }

enum class InkModelStatus { IDLE, DOWNLOADING, READY, FAILED }

data class CalculatorUiState(
    val expression: String = "",
    val display: String = "0",
    val isError: Boolean = false,
    val mode: CalculatorMode = CalculatorMode.BASIC,
    val history: List<HistoryEntry> = emptyList(),
    val isHistoryOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val settingsSection: SettingsSection? = null,
    val settings: CalculatorSettings = CalculatorSettings(),
    // Math notes lines — held here (not local Composable state) so a Settings action can export
    // the same content the Notes tab is showing.
    val notesLines: List<String> = listOf(""),
    // Handwriting input for Math Notes (real ML Kit Digital Ink Recognition, not simulated).
    val inkModelStatus: InkModelStatus = InkModelStatus.IDLE,
    val isRecognizingInk: Boolean = false,
    val inkTargetLine: Int? = null
)
