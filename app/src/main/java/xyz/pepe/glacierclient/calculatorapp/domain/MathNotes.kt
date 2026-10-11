package xyz.pepe.glacierclient.calculatorapp.domain

/** A variable defined on a line ("price = 12"). [literal] is true when the value is a plain
 *  number, which is what makes it draggable with a slider — like scrubbing a value on iPad. */
data class NoteVariable(val name: String, val value: Double, val line: Int, val literal: Boolean)

/** A "y = …" (or "f(x) = …") line, plotted in the note's graph. */
data class NoteGraph(val line: Int, val label: String, val expression: String)

data class NoteAnalysis(
  /** Per logical line: the text to show after "=", or null. */
  val results: List<String?>,
  val variables: List<NoteVariable>,
  val graphs: List<NoteGraph>,
  /** Final variable values, for plotting graphs that use them. */
  val environment: Map<String, Double>,
)

/**
 * Solves a Math Notes page line by line, top to bottom, the way the iPad's Math Notes does:
 *  - "12 × 4 =" shows its answer right after the "=";
 *  - "price = 12" defines a variable that later lines can use;
 *  - "Dinner: 45 + 12 =" ignores the words in front;
 *  - "5 km in mi =" and "20 USD to EUR =" convert units and currencies;
 *  - "sum =" / "total =" add up the numbers above it (back to the previous blank line);
 *  - "y = 2x + 1" is graphed instead of solved.
 */
object MathNotes {

  private val ASSIGN = Regex("""^([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.+)$""")
  private val GRAPH = Regex("""^(y|[A-Za-z]\w*\(x\))\s*=\s*(.+)$""")
  private val CONNECTOR = Regex("""\s+(?:in|to|as|into|→|->)\s+""", RegexOption.IGNORE_CASE)
  private val SUM_WORDS = setOf("sum", "total")

  fun analyze(
    text: String,
    useRadians: Boolean,
    style: NumberStyle,
    currency: List<ConvertibleUnit> = emptyList(),
    solveWithoutEquals: Boolean = false,
  ): NoteAnalysis {
    val lines = text.split('\n')
    val env = LinkedHashMap<String, Double>()
    val vars = ArrayList<NoteVariable>()
    val graphs = ArrayList<NoteGraph>()
    val results = ArrayList<String?>(lines.size)
    var blockSum = 0.0

    lines.forEachIndexed { index, raw ->
      val line = raw.trim()
      if (line.isEmpty()) { blockSum = 0.0; results.add(null); return@forEachIndexed }
      val wantsResult = line.endsWith("=")
      val body = line.removeSuffix("=").trim()
      val scope: Map<String, Double> = env + SUM_WORDS.filter { it !in env }.associateWith { blockSum }

      // Graphs: y = f(x), as long as x isn't a variable someone defined.
      GRAPH.matchEntire(body)?.let { m ->
        val rhs = m.groupValues[2]
        if ("x" !in env && Regex("""(?<![A-Za-z])x(?![A-Za-z])""").containsMatchIn(CalculatorEngine.normalize(rhs).replace(Regex("""(\d)x"""), "$1 x"))) {
          graphs.add(NoteGraph(index, m.groupValues[1], rhs))
          results.add(null)
          return@forEachIndexed
        }
      }

      // Assignments.
      ASSIGN.matchEntire(body)?.let { m ->
        val name = m.groupValues[1]
        if (name !in CalculatorEngine.FUNCTIONS && name != "e" && name != "pi" && name.lowercase() !in SUM_WORDS) {
          val rhs = m.groupValues[2]
          val value = solveExpression(rhs, useRadians, scope, currency)
          if (value != null) {
            env[name] = value.first
            val literal = rhs.trim().replace(",", "").toDoubleOrNull() != null
            vars.removeAll { it.name == name }
            vars.add(NoteVariable(name, value.first, index, literal))
            val show = (wantsResult || (solveWithoutEquals && !literal))
            results.add(if (show) format(value, style) else null)
            return@forEachIndexed
          }
        }
      }

      if (body.lowercase() in SUM_WORDS && wantsResult) {
        results.add(NumberFormat.format(blockSum, style))
        blockSum = 0.0
        return@forEachIndexed
      }

      val plain = body.replace(",", "").toDoubleOrNull()
      if (plain != null) {
        blockSum += plain
        results.add(null)
        return@forEachIndexed
      }

      if (!wantsResult && !solveWithoutEquals) { results.add(null); return@forEachIndexed }
      val solved = solveWithLabel(body, useRadians, scope, currency)
      if (solved != null) {
        blockSum += solved.first
        results.add(format(solved, style))
      } else {
        results.add(if (wantsResult) "?" else null)
      }
    }
    return NoteAnalysis(results, vars, graphs, env)
  }

  private fun format(v: Pair<Double, String?>, style: NumberStyle) =
    NumberFormat.format(v.first, style) + (v.second?.let { " $it" } ?: "")

  /** Tries the whole line, then drops leading words ("Dinner for 4: 45 + 12") until it parses. */
  private fun solveWithLabel(body: String, useRadians: Boolean, scope: Map<String, Double>, currency: List<ConvertibleUnit>): Pair<Double, String?>? {
    solveExpression(body, useRadians, scope, currency)?.let { return it }
    val afterColon = body.substringAfterLast(':', "").trim()
    if (afterColon.isNotEmpty()) solveExpression(afterColon, useRadians, scope, currency)?.let { return it }
    val words = body.split(Regex("\\s+"))
    for (drop in 1 until words.size) {
      solveExpression(words.drop(drop).joinToString(" "), useRadians, scope, currency)?.let { return it }
    }
    return null
  }

  /** A value plus, for unit conversions, the unit it's in. */
  fun solveExpression(expr: String, useRadians: Boolean, scope: Map<String, Double>, currency: List<ConvertibleUnit>): Pair<Double, String?>? {
    convert(expr, useRadians, scope, currency)?.let { return it }
    return CalculatorEngine.evaluateOrNull(expr, useRadians, scope)?.let { it to null }
  }

  /** "5 km in mi", "(3+2) USD to EUR", "100 fl oz in L": amount, unit (one or two words), connector, unit. */
  private fun convert(expr: String, useRadians: Boolean, scope: Map<String, Double>, currency: List<ConvertibleUnit>): Pair<Double, String?>? {
    val m = CONNECTOR.findAll(expr).lastOrNull() ?: return null
    val left = expr.substring(0, m.range.first).trim()
    val to = UnitConverter.find(expr.substring(m.range.last + 1), currency) ?: return null
    val words = left.split(Regex("\\s+"))
    for (unitWords in 2 downTo 1) {
      if (words.size <= unitWords) {
        // "5km": the number and unit are glued together.
        if (unitWords == 1) {
          val split = Regex("""^([\d.,]+)\s*(\D.*)$""").matchEntire(left) ?: continue
          val from = UnitConverter.find(split.groupValues[2], currency) ?: continue
          if (from.first != to.first) continue
          val amount = split.groupValues[1].replace(",", "").toDoubleOrNull() ?: continue
          return UnitConverter.convert(amount, from.second, to.second) to to.second.symbol
        }
        continue
      }
      val from = UnitConverter.find(words.takeLast(unitWords).joinToString(" "), currency) ?: continue
      if (from.first != to.first) continue
      val amount = CalculatorEngine.evaluateOrNull(words.dropLast(unitWords).joinToString(" "), useRadians, scope) ?: continue
      return UnitConverter.convert(amount, from.second, to.second) to to.second.symbol
    }
    return null
  }

  /** Rewrites the literal on a variable's line — used by the slider. */
  fun setVariable(text: String, variable: NoteVariable, value: Double, style: NumberStyle): String {
    val lines = text.split('\n').toMutableList()
    if (variable.line !in lines.indices) return text
    val line = lines[variable.line]
    val eq = line.indexOf('=')
    if (eq < 0) return text
    val trailingEquals = line.trimEnd().let { it.endsWith("=") && it.indexOf('=') != it.lastIndexOf('=') }
    lines[variable.line] = line.substring(0, eq + 1) + " " + NumberFormat.format(value, style.copy(grouping = false)) + if (trailingEquals) " =" else ""
    return lines.joinToString("\n")
  }
}
