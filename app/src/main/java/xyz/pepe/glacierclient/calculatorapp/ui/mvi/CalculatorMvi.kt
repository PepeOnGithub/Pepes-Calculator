package xyz.pepe.glacierclient.calculatorapp.ui.mvi

import xyz.pepe.glacierclient.calculatorapp.data.CurrencyRatesSource
import xyz.pepe.glacierclient.calculatorapp.domain.CalculatorEngine
import xyz.pepe.glacierclient.calculatorapp.domain.Fractions
import xyz.pepe.glacierclient.calculatorapp.domain.NumberFormat
import xyz.pepe.glacierclient.calculatorapp.domain.model.AppSettings
import xyz.pepe.glacierclient.calculatorapp.domain.model.CurrencyRates
import xyz.pepe.glacierclient.calculatorapp.domain.model.HistoryEntry
import xyz.pepe.glacierclient.calculatorapp.domain.model.MathNote

enum class CalculatorMode { BASIC, SCIENTIFIC }

enum class InkModelStatus { IDLE, DOWNLOADING, READY, FAILED }

data class CalculatorUiState(
  /** Raw ASCII expression ("12*3-4"); the display maps it to ×, ÷, − and groups digits. */
  val expression: String = "",
  /** After "=", the expression that produced the current result, shown small above it. */
  val previousExpression: String? = null,
  val justEvaluated: Boolean = false,
  val error: String? = null,
  val mode: CalculatorMode = CalculatorMode.BASIC,
  /** Scientific "2nd": sin → sin⁻¹, ln → eˣ, and so on. */
  val second: Boolean = false,
  val lastAnswer: Double? = null,
  val history: List<HistoryEntry> = emptyList(),
  val settings: AppSettings = AppSettings(),
  val settingsLoaded: Boolean = false,
  val notes: List<MathNote> = emptyList(),
  val rates: CurrencyRates = CurrencyRatesSource.FALLBACK,
  val ratesLoading: Boolean = false,
  val ratesError: Boolean = false,
  val inkModelStatus: InkModelStatus = InkModelStatus.IDLE,
  val isRecognizingInk: Boolean = false,
  /** The memory register (M+ / M− / MR / MC); null when empty. */
  val memory: Double? = null,
  /** Currency codes the user starred; shown first in the converter. */
  val favoriteCurrencies: Set<String> = emptySet(),
) {
  private val variables get() = lastAnswer?.let { mapOf("Ans" to it) } ?: emptyMap()

  /** The live answer shown under the expression while typing (like Pixel/iOS), or null. */
  val preview: String?
    get() {
      if (justEvaluated || expression.isBlank()) return null
      if (expression.none { it in "+-*/^%!√(" } && !expression.any { it.isLetter() }) return null
      return CalculatorEngine.evaluateOrNull(expression, settings.useRadians, variables)
        ?.let { NumberFormat.format(it, settings.numberStyle) }
    }

  /** The number currently on the display (finished result, or the live preview value), or null. */
  val currentValue: Double?
    get() = when {
      error != null || expression.isBlank() -> null
      justEvaluated -> expression.toDoubleOrNull()
      else -> CalculatorEngine.evaluateOrNull(expression, settings.useRadians, variables)
    }

  /** "3/4" for a decimal answer, when "Fraction results" is on. */
  val fraction: String?
    get() = if (settings.showFractions) currentValue?.let { Fractions.format(it) } else null

  fun evaluate(): Double = CalculatorEngine.evaluate(expression, settings.useRadians, variables)
}

sealed interface CalculatorIntent {
  // Keypad
  data class Input(val token: String) : CalculatorIntent
  data object Backspace : CalculatorIntent
  data object ClearAll : CalculatorIntent
  data object Evaluate : CalculatorIntent
  data object ToggleSign : CalculatorIntent
  data object ToggleMode : CalculatorIntent
  data object ToggleSecond : CalculatorIntent
  data object ToggleAngle : CalculatorIntent
  data class Paste(val text: String) : CalculatorIntent

  // Memory register
  data object MemoryAdd : CalculatorIntent
  data object MemorySubtract : CalculatorIntent
  data object MemoryRecall : CalculatorIntent
  data object MemoryClear : CalculatorIntent

  // History
  data class UseHistory(val entry: HistoryEntry) : CalculatorIntent
  data class DeleteHistory(val entry: HistoryEntry) : CalculatorIntent
  data class RestoreHistory(val entry: HistoryEntry) : CalculatorIntent
  data object ClearHistory : CalculatorIntent
  data class TogglePin(val entry: HistoryEntry) : CalculatorIntent

  // Settings
  data class UpdateSettings(val transform: (AppSettings) -> AppSettings) : CalculatorIntent

  // Math Notes
  data class CreateNote(val id: Long, val body: String = "") : CalculatorIntent
  data class UpdateNote(val id: Long, val body: String) : CalculatorIntent
  data class DeleteNote(val id: Long) : CalculatorIntent
  data class RestoreNote(val note: MathNote) : CalculatorIntent
  data object PrepareInkModel : CalculatorIntent
  data class RecognizeInk(val strokes: List<List<InkPoint>>, val onResult: (String?) -> Unit) : CalculatorIntent

  // Converter
  data object RefreshRates : CalculatorIntent
  data class ToggleFavoriteCurrency(val code: String) : CalculatorIntent
}

/** One sampled touch point of a handwriting stroke (t in ms, for ML Kit's stroke timing). */
data class InkPoint(val x: Float, val y: Float, val t: Long)
