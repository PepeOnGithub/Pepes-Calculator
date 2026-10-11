package xyz.pepe.glacierclient.calculatorapp.widget

/** What the home-screen calculator shows: the typed [expr], and after "=" the [shown] result (with [done] = true). */
data class WidgetCalcState(val expr: String = "", val shown: String = "", val done: Boolean = false)

/**
 * Key handling for the home-screen calculator, kept free of Android so it can be unit-tested. [eval] turns an
 * expression (using + − × ÷ % ^ and brackets) into a result string, or null when it can't be evaluated.
 */
object WidgetCalc {
  const val MAX_LEN = 60
  private val DIGITS = "0123456789."
  private val OPERATORS = "+−×÷%^"

  val KEY_ROWS: List<List<String>> = listOf(
    listOf("C", "(", ")", "÷"),
    listOf("7", "8", "9", "×"),
    listOf("4", "5", "6", "−"),
    listOf("1", "2", "3", "+"),
    listOf("0", ".", "⌫", "="),
  )

  fun press(state: WidgetCalcState, key: String, eval: (String) -> String?): WidgetCalcState {
    if (key.isEmpty()) return state
    val c = key[0]
    return when {
      key == "C" -> WidgetCalcState()
      key == "⌫" -> if (state.done) WidgetCalcState() else state.copy(expr = state.expr.dropLast(1))
      key == "=" -> equals(state, eval)
      c in DIGITS -> if (state.done) WidgetCalcState(expr = key) else append(state, key)
      c in OPERATORS -> operator(state, key)
      c == '(' -> if (state.done) WidgetCalcState(expr = key) else append(state, key)
      c == ')' -> append(state.copy(done = false), key)
      else -> state
    }
  }

  private fun append(state: WidgetCalcState, text: String): WidgetCalcState =
    if (state.expr.length + text.length > MAX_LEN) state else state.copy(expr = state.expr + text, shown = "", done = false)

  private fun operator(state: WidgetCalcState, op: String): WidgetCalcState {
    // After "=" the result becomes the left-hand side of the next operation.
    val base = if (state.done && state.shown != "Error") WidgetCalcState(expr = state.shown) else state
    if (base.expr.isEmpty()) return if (op == "−") append(base, op) else base
    val last = base.expr.last()
    return if (last in OPERATORS && op != "(") {
      base.copy(expr = base.expr.dropLast(1) + op, done = false, shown = "")
    } else {
      append(base, op)
    }
  }

  private fun equals(state: WidgetCalcState, eval: (String) -> String?): WidgetCalcState {
    if (state.expr.isBlank()) return state
    val result = eval(state.expr) ?: return state.copy(shown = "Error", done = false)
    return WidgetCalcState(expr = state.expr, shown = result, done = true)
  }

  /** Small preview line shown while typing: "= 42" once the expression already evaluates and has an operator. */
  fun preview(state: WidgetCalcState, eval: (String) -> String?): String? {
    if (state.done || state.expr.none { it in OPERATORS }) return null
    return eval(state.expr)?.let { "= $it" }
  }

  /** Display text for the engine: our symbols mapped to the ASCII operators the engine understands. */
  fun toEngine(expr: String): String = expr.replace('×', '*').replace('÷', '/').replace('−', '-')
}
