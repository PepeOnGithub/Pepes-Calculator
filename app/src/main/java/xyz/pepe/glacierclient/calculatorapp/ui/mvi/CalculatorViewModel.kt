package xyz.pepe.glacierclient.calculatorapp.ui.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.pepe.glacierclient.calculatorapp.data.CurrencyRatesSource
import xyz.pepe.glacierclient.calculatorapp.data.SettingsRepository
import xyz.pepe.glacierclient.calculatorapp.domain.CalculatorEngine
import xyz.pepe.glacierclient.calculatorapp.domain.NumberFormat
import xyz.pepe.glacierclient.calculatorapp.domain.NumberStyle
import xyz.pepe.glacierclient.calculatorapp.domain.model.AngleUnit
import xyz.pepe.glacierclient.calculatorapp.domain.model.AppSettings
import xyz.pepe.glacierclient.calculatorapp.domain.model.HistoryEntry
import xyz.pepe.glacierclient.calculatorapp.domain.model.MathNote
import xyz.pepe.glacierclient.calculatorapp.util.InkRecognizerManager

private const val HISTORY_LIMIT = 100
private const val RATES_MAX_AGE_MS = 6 * 60 * 60 * 1000L

class CalculatorViewModel(
  private val repo: SettingsRepository,
  private val ratesSource: CurrencyRatesSource,
  private val ink: InkRecognizerManager,
) : ViewModel() {

  private val _uiState = MutableStateFlow(CalculatorUiState())
  val uiState: StateFlow<CalculatorUiState> = _uiState

  private var saveNotesJob: Job? = null

  init {
    viewModelScope.launch { repo.settings.collect { s -> _uiState.update { it.copy(settings = s, settingsLoaded = true) } } }
    viewModelScope.launch { repo.history.collect { h -> _uiState.update { it.copy(history = h) } } }
    viewModelScope.launch { repo.memory.collect { m -> _uiState.update { it.copy(memory = m) } } }
    viewModelScope.launch { repo.favoriteCurrencies.collect { f -> _uiState.update { it.copy(favoriteCurrencies = f) } } }
    viewModelScope.launch { repo.rates.collect { r -> if (r != null) _uiState.update { it.copy(rates = r) } } }
    viewModelScope.launch {
      val notes = repo.notes.first()
      _uiState.update { it.copy(notes = notes) }
      val settings = repo.settings.first()
      val cached = repo.rates.first()
      if (settings.autoUpdateRates && (cached == null || System.currentTimeMillis() - cached.fetchedAt > RATES_MAX_AGE_MS)) refreshRates()
    }
  }

  fun onIntent(intent: CalculatorIntent) {
    when (intent) {
      is CalculatorIntent.Input -> input(intent.token)
      CalculatorIntent.Backspace -> backspace()
      CalculatorIntent.ClearAll -> _uiState.update { it.copy(expression = "", previousExpression = null, justEvaluated = false, error = null) }
      CalculatorIntent.Evaluate -> evaluate()
      CalculatorIntent.ToggleSign -> edit { toggleSign(it) }
      CalculatorIntent.ToggleMode -> _uiState.update {
        it.copy(mode = if (it.mode == CalculatorMode.BASIC) CalculatorMode.SCIENTIFIC else CalculatorMode.BASIC)
      }
      CalculatorIntent.ToggleSecond -> _uiState.update { it.copy(second = !it.second) }
      CalculatorIntent.ToggleAngle -> settings { it.copy(angleUnit = if (it.angleUnit == AngleUnit.DEG) AngleUnit.RAD else AngleUnit.DEG) }
      is CalculatorIntent.Paste -> {
        val clean = CalculatorEngine.normalize(intent.text).filter { it.isLetterOrDigit() || it in ".+-*/^()%!√π " }.trim()
        if (clean.isNotEmpty()) input(clean)
      }

      CalculatorIntent.MemoryAdd -> memoryStep(+1.0)
      CalculatorIntent.MemorySubtract -> memoryStep(-1.0)
      CalculatorIntent.MemoryClear -> viewModelScope.launch { repo.setMemory(null) }
      CalculatorIntent.MemoryRecall -> _uiState.value.memory?.let { m ->
        val raw = NumberFormat.format(m, NumberStyle(decimals = null, grouping = false))
        input(if (m < 0) "($raw)" else raw)
      }

      // Continue from a past result, as if "=" had just been pressed on it.
      is CalculatorIntent.UseHistory -> _uiState.update {
        it.copy(expression = intent.entry.result.replace(",", ""), previousExpression = intent.entry.expression, justEvaluated = true, error = null)
      }
      is CalculatorIntent.DeleteHistory -> viewModelScope.launch { repo.setHistory(_uiState.value.history - intent.entry) }
      is CalculatorIntent.RestoreHistory -> viewModelScope.launch {
        val h = _uiState.value.history
        if (intent.entry !in h) repo.setHistory((h + intent.entry).sortedByDescending { it.at })
      }
      CalculatorIntent.ClearHistory -> viewModelScope.launch { repo.setHistory(emptyList()) }

      is CalculatorIntent.TogglePin -> viewModelScope.launch {
        repo.setHistory(_uiState.value.history.map { if (it == intent.entry) it.copy(pinned = !it.pinned) else it })
      }
      is CalculatorIntent.ToggleFavoriteCurrency -> viewModelScope.launch {
        val f = _uiState.value.favoriteCurrencies
        repo.setFavoriteCurrencies(if (intent.code in f) f - intent.code else f + intent.code)
      }

      is CalculatorIntent.UpdateSettings -> settings(intent.transform)

      is CalculatorIntent.CreateNote -> notes { listOf(MathNote(intent.id, intent.body, now())) + it }
      is CalculatorIntent.UpdateNote -> notes { list ->
        list.map { if (it.id == intent.id && it.body != intent.body) it.copy(body = intent.body, updatedAt = now()) else it }
          .sortedByDescending { it.updatedAt }
      }
      is CalculatorIntent.DeleteNote -> notes { list -> list.filterNot { it.id == intent.id } }
      is CalculatorIntent.RestoreNote -> notes { (it + intent.note).sortedByDescending { n -> n.updatedAt } }
      CalculatorIntent.PrepareInkModel -> prepareInkModel()
      is CalculatorIntent.RecognizeInk -> recognizeInk(intent)

      CalculatorIntent.RefreshRates -> refreshRates()
    }
  }

  // Keypad ------------------------------------------------------------------------------------

  private fun edit(transform: (String) -> String) = _uiState.update {
    it.copy(expression = transform(it.expression), justEvaluated = false, previousExpression = null, error = null)
  }

  private fun input(token: String) {
    val state = _uiState.value
    // After "=", a number starts a new calculation; an operator continues from the answer.
    val startsFresh = state.justEvaluated && !isBinaryOperator(token.first()) && token != "%" && token != "!" && !token.startsWith("^") &&
      token != "nCr" && token != "nPr" && token != "mod"
    val base = if (startsFresh || state.error != null && state.justEvaluated) "" else state.expression
    edit { appendToken(base, token) }
  }

  private fun appendToken(expr: String, token: String): String {
    val last = expr.lastOrNull()
    val first = token.first()
    return when {
      // Replace a dangling operator ("5+" then "×" → "5×"), but keep "×−" for negatives.
      last != null && isBinaryOperator(last) && isBinaryOperator(first) && token.length == 1 ->
        if (first == '-' && last in "*/^") expr + token else expr.dropLast(1) + token
      expr.isEmpty() && isBinaryOperator(first) && first != '-' && token.length == 1 ->
        (_uiState.value.lastAnswer?.let { "Ans" } ?: "0") + token
      // One decimal point per number.
      token == "." -> {
        val number = expr.takeLastWhile { it.isDigit() || it == '.' }
        when {
          '.' in number -> expr
          number.isEmpty() -> expr + "0."
          else -> expr + "."
        }
      }
      else -> expr + token
    }
  }

  private fun isBinaryOperator(c: Char) = c in "+-*/"

  private fun backspace() {
    val state = _uiState.value
    if (state.justEvaluated) { onIntent(CalculatorIntent.ClearAll); return }
    edit { expr ->
      val fn = Regex("""(?:a?(?:sin|cos|tan)h?|sqrt|cbrt|ln|log2|log|exp|abs|Ans|nCr|nPr|mod)\(?$""").find(expr)
      if (fn != null) expr.substring(0, fn.range.first) else expr.dropLast(1)
    }
  }

  private fun toggleSign(expr: String): String {
    Regex("""^-([\d.]+)$""").matchEntire(expr)?.let { return it.groupValues[1] }
    Regex("""^(.*)\(-([\d.]+)\)$""").matchEntire(expr)?.let { return it.groupValues[1] + it.groupValues[2] }
    Regex("""^(.*?)([\d.]+|Ans|π|e)$""").matchEntire(expr)?.let { m ->
      val (head, number) = m.destructured
      return if (head.isEmpty()) "-$number" else "$head(-$number)"
    }
    return if (expr.isEmpty()) "-" else "$expr(-"
  }

  private fun evaluate() {
    val state = _uiState.value
    if (state.expression.isBlank() || state.justEvaluated) return
    try {
      val value = state.evaluate()
      val raw = NumberFormat.format(value, NumberStyle(decimals = null, grouping = false))
      val pinned = state.history.any { it.expression == state.expression && it.pinned }
      val entry = HistoryEntry(state.expression, NumberFormat.format(value, state.settings.numberStyle), pinned = pinned)
      _uiState.update {
        it.copy(expression = raw, previousExpression = state.expression, justEvaluated = true, error = null, lastAnswer = value)
      }
      if (state.settings.keepHistory) viewModelScope.launch {
        val others = state.history.filterNot { it.expression == entry.expression }
        // Pinned calculations are never pushed out by the limit.
        val kept = (listOf(entry) + others).filter { it.pinned } + (listOf(entry) + others).filterNot { it.pinned }.take(HISTORY_LIMIT)
        repo.setHistory(kept.sortedByDescending { it.at })
      }
    } catch (e: CalculatorEngine.EvaluationException) {
      _uiState.update { it.copy(error = e.message ?: "Error") }
    } catch (_: ArithmeticException) {
      _uiState.update { it.copy(error = "Math error") }
    }
  }

  // Settings / notes / rates ------------------------------------------------------------------

  /** M+ / M−: adds or subtracts what is on the display (or the last answer) to the memory. */
  private fun memoryStep(sign: Double) {
    val state = _uiState.value
    val v = state.currentValue ?: state.lastAnswer ?: return
    viewModelScope.launch { repo.setMemory((state.memory ?: 0.0) + sign * v) }
  }

  private fun settings(transform: (AppSettings) -> AppSettings) {
    viewModelScope.launch {
      repo.update(transform)
      if (!transform(_uiState.value.settings).keepHistory) repo.setHistory(emptyList())
    }
  }

  private fun notes(transform: (List<MathNote>) -> List<MathNote>) {
    _uiState.update { it.copy(notes = transform(it.notes)) }
    // Typing updates the state at once; the disk write waits for a pause.
    saveNotesJob?.cancel()
    saveNotesJob = viewModelScope.launch {
      delay(400)
      repo.setNotes(_uiState.value.notes.filter { it.body.isNotBlank() })
    }
  }

  private fun refreshRates() {
    if (_uiState.value.ratesLoading) return
    _uiState.update { it.copy(ratesLoading = true, ratesError = false) }
    viewModelScope.launch {
      val fetched = ratesSource.fetch()
      if (fetched != null) repo.setRates(fetched)
      _uiState.update { it.copy(ratesLoading = false, ratesError = fetched == null) }
    }
  }

  private fun prepareInkModel() {
    if (_uiState.value.inkModelStatus == InkModelStatus.READY || _uiState.value.inkModelStatus == InkModelStatus.DOWNLOADING) return
    _uiState.update { it.copy(inkModelStatus = InkModelStatus.DOWNLOADING) }
    viewModelScope.launch {
      val ok = ink.ensureModelDownloaded()
      _uiState.update { it.copy(inkModelStatus = if (ok) InkModelStatus.READY else InkModelStatus.FAILED) }
    }
  }

  /** Real ML Kit recognition of the drawn strokes; the result goes back to the editor's cursor. */
  private fun recognizeInk(intent: CalculatorIntent.RecognizeInk) {
    _uiState.update { it.copy(isRecognizingInk = true) }
    viewModelScope.launch {
      val text = ink.recognize(intent.strokes)
      _uiState.update {
        it.copy(isRecognizingInk = false, inkModelStatus = if (text != null) InkModelStatus.READY else it.inkModelStatus)
      }
      intent.onResult(text)
    }
  }

  private fun now() = System.currentTimeMillis()

  class Factory(
    private val repo: SettingsRepository,
    private val ratesSource: CurrencyRatesSource,
    private val ink: InkRecognizerManager,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = CalculatorViewModel(repo, ratesSource, ink) as T
  }
}
