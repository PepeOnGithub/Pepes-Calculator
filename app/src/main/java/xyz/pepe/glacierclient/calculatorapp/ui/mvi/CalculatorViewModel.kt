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
import xyz.pepe.glacierclient.calculatorapp.util.InkRecognizerManager
import java.math.BigDecimal
import java.math.RoundingMode

class CalculatorViewModel(
    private val settingsRepository: SettingsRepository,
    private val inkRecognizerManager: InkRecognizerManager
) : ViewModel() {

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
            is CalculatorIntent.ToggleSettings -> _uiState.update {
                it.copy(isSettingsOpen = intent.open, settingsSection = if (intent.open) it.settingsSection else null)
            }
            is CalculatorIntent.ToggleSettingsSection -> _uiState.update { it.copy(settingsSection = intent.section) }
            is CalculatorIntent.SetHaptics -> viewModelScope.launch { settingsRepository.setHaptics(intent.enabled) }
            is CalculatorIntent.SetUseRadians -> viewModelScope.launch { settingsRepository.setUseRadians(intent.enabled) }
            is CalculatorIntent.SetKeepHistory -> viewModelScope.launch { settingsRepository.setKeepHistory(intent.enabled) }
            is CalculatorIntent.SetWallpaperColors -> viewModelScope.launch { settingsRepository.setWallpaperColors(intent.enabled) }
            is CalculatorIntent.SetNotesLine -> _uiState.update { state ->
                val lines = state.notesLines.toMutableList()
                if (intent.index in lines.indices) lines[intent.index] = intent.value
                state.copy(notesLines = lines)
            }
            is CalculatorIntent.AddNotesLine -> _uiState.update { it.copy(notesLines = it.notesLines + intent.text) }
            CalculatorIntent.PrepareInkModel -> prepareInkModel()
            is CalculatorIntent.RecognizeInk -> recognizeInk(intent.targetLine, intent.strokes)
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

    private fun prepareInkModel() {
        if (_uiState.value.inkModelStatus == InkModelStatus.READY) return
        _uiState.update { it.copy(inkModelStatus = InkModelStatus.DOWNLOADING) }
        viewModelScope.launch {
            val ok = inkRecognizerManager.ensureModelDownloaded()
            _uiState.update { it.copy(inkModelStatus = if (ok) InkModelStatus.READY else InkModelStatus.FAILED) }
        }
    }

    /** Recognizes handwritten [strokes] via ML Kit and appends the result onto the target
     *  Math Notes line — a real recognition call, not a canned/simulated result. */
    private fun recognizeInk(targetLine: Int, strokes: List<List<Pair<Float, Float>>>) {
        _uiState.update { it.copy(isRecognizingInk = true, inkTargetLine = targetLine) }
        viewModelScope.launch {
            val recognized = inkRecognizerManager.recognize(strokes)
            _uiState.update { state ->
                val lines = state.notesLines.toMutableList()
                if (recognized != null && targetLine in lines.indices) {
                    lines[targetLine] = (lines[targetLine] + recognized).trim()
                }
                state.copy(
                    notesLines = lines,
                    isRecognizingInk = false,
                    inkTargetLine = null,
                    inkModelStatus = if (recognized != null) InkModelStatus.READY else state.inkModelStatus
                )
            }
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val inkRecognizerManager: InkRecognizerManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalculatorViewModel(settingsRepository, inkRecognizerManager) as T
        }
    }
}
