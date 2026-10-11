package xyz.pepe.glacierclient.calculatorapp.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/**
 * A small recursive-descent expression evaluator for the keypad, Math Notes and the converter's
 * input field.
 *
 * Beyond + - × ÷ ^ and parentheses it understands what people actually type or write by hand:
 * implicit multiplication ("2π", "3(4+1)", "2x"), √ and π, factorial, iOS-style percent
 * ("200 + 10%" is 220, "50%" alone is 0.5), named variables (Math Notes' "price = 12") and
 * unclosed trailing parentheses (so a live preview works mid-typing).
 */
object CalculatorEngine {

  class EvaluationException(message: String) : Exception(message)

  val FUNCTIONS = setOf(
    "sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh",
    "sqrt", "cbrt", "ln", "log", "log2", "exp", "abs", "floor", "ceil", "round",
  )
  private val INFIX_WORDS = listOf("nCr", "nPr", "mod")
  private val CONSTANTS = mapOf("pi" to PI, "π" to PI, "e" to Math.E, "tau" to 2 * PI)

  fun evaluate(expression: String, useRadians: Boolean, variables: Map<String, Double> = emptyMap()): Double {
    val normalized = normalize(expression)
    if (normalized.isBlank()) throw EvaluationException("Empty expression")
    val parser = Parser(normalized, useRadians, variables)
    val result = parser.parseExpression()
    parser.skipSpaces()
    if (!parser.isAtEnd()) throw EvaluationException("Unexpected '${normalized[parser.pos]}'")
    if (result.isNaN() || result.isInfinite()) throw EvaluationException("Math error")
    return result
  }

  fun evaluateOrNull(expression: String, useRadians: Boolean, variables: Map<String, Double> = emptyMap()): Double? =
    try { evaluate(expression, useRadians, variables) } catch (_: EvaluationException) { null } catch (_: ArithmeticException) { null }

  /** Maps display glyphs and handwriting look-alikes onto the parser's ASCII operators. */
  fun normalize(expression: String): String = expression
    .replace('×', '*').replace('·', '*').replace('∙', '*')
    .replace('÷', '/').replace('−', '-').replace('–', '-').replace('—', '-')
    .replace("²", "^2").replace("³", "^3")
    .replace(",", "")
    .trim()

  private class Parser(
    private val input: String,
    private val useRadians: Boolean,
    private val variables: Map<String, Double>,
  ) {
    var pos = 0
    /** True when the factor just parsed was a bare "n%" — lets "a + b%" mean a + a·b/100. */
    private var lastWasPercent = false

    fun isAtEnd() = pos >= input.length
    fun skipSpaces() { while (!isAtEnd() && input[pos].isWhitespace()) pos++ }
    private fun peek(): Char { skipSpaces(); return if (isAtEnd()) '\u0000' else input[pos] }
    private fun consume(): Char { skipSpaces(); return input[pos++] }

    // expression := term (('+' | '-') term)*
    fun parseExpression(): Double {
      var value = parseTerm()
      while (true) {
        val op = peek()
        if (op != '+' && op != '-') return value
        consume()
        val rhs = parseTerm()
        val amount = if (lastWasPercent) value * rhs else rhs
        value = if (op == '+') value + amount else value - amount
      }
    }

    // term := factor (('*' | '/' | implicit) factor)*
    private fun parseTerm(): Double {
      var value = parseFactor()
      var single = true
      while (true) {
        val c = peek()
        val word = infixWord()
        when {
          word != null -> { pos += word.length; value = applyInfix(word, value, parseFactor()) }
          c == '*' -> { consume(); value *= parseFactor() }
          c == '/' -> {
            consume()
            val divisor = parseFactor()
            if (divisor == 0.0) throw EvaluationException("Can't divide by zero")
            value /= divisor
          }
          startsAtom(c) -> value *= parseFactor() // implicit: 2π, 3(4), 2x, (1)(2)
          else -> { if (!single) lastWasPercent = false; return value }
        }
        single = false
      }
    }

    /** "nCr", "nPr" or "mod" at the cursor (when not just the start of a longer name), else null. */
    private fun infixWord(): String? {
      for (w in INFIX_WORDS) {
        if (input.startsWith(w, pos)) {
          val next = input.getOrNull(pos + w.length)
          if (next == null || !next.isLetter()) return w
        }
      }
      return null
    }

    private fun applyInfix(word: String, a: Double, b: Double): Double = when (word) {
      "mod" -> if (b == 0.0) throw EvaluationException("Can't divide by zero") else a - b * Math.floor(a / b)
      else -> {
        if (a < 0 || b < 0 || a != Math.floor(a) || b != Math.floor(b) || b > a || a > 170) throw EvaluationException("Invalid input")
        val n = a.toInt(); val k = b.toInt()
        var r = 1.0
        if (word == "nCr") { for (i in 1..k) r = r * (n - k + i) / i; Math.rint(r) }
        else { for (i in 0 until k) r *= (n - i); r }
      }
    }

    private fun startsAtom(c: Char) = c == '(' || c == '√' || c == 'π' || c.isLetter() || c.isDigit() || c == '.'

    // factor := unary ('^' factor)?  — right-associative
    private fun parseFactor(): Double {
      val base = parseUnary()
      if (peek() == '^') {
        consume()
        lastWasPercent = false
        return base.pow(parseFactor())
      }
      return base
    }

    // unary := ('-' | '+' | '√')* postfix
    private fun parseUnary(): Double = when (peek()) {
      '-' -> { consume(); -parseUnary() }
      '+' -> { consume(); parseUnary() }
      '√' -> {
        consume()
        val v = parseUnary()
        if (v < 0) throw EvaluationException("Invalid input")
        sqrt(v)
      }
      else -> parsePostfix()
    }

    // postfix := atom ('!' | '%')*
    private fun parsePostfix(): Double {
      var value = parseAtom()
      lastWasPercent = false
      while (true) {
        when (peek()) {
          '!' -> { consume(); value = factorial(value); lastWasPercent = false }
          '%' -> { consume(); value /= 100.0; lastWasPercent = true }
          else -> return value
        }
      }
    }

    private fun closeParen() {
      // A missing ")" at the very end is forgiven, so "sin(30" still previews.
      if (peek() == ')') consume() else if (!isAtEnd()) throw EvaluationException("Missing )")
    }

    private fun parseAtom(): Double {
      skipSpaces()
      if (isAtEnd()) throw EvaluationException("Incomplete expression")
      val c = input[pos]
      return when {
        c == '(' -> { consume(); val v = parseExpression(); closeParen(); v }
        c.isDigit() || c == '.' -> parseNumber()
        c == 'π' -> { pos++; PI }
        c.isLetter() -> parseIdentifier()
        else -> throw EvaluationException("Unexpected '$c'")
      }
    }

    private fun parseNumber(): Double {
      val start = pos
      while (!isAtEnd() && (input[pos].isDigit() || input[pos] == '.')) pos++
      // Scientific notation: 1.5E3 / 2e-4 (only when followed by a digit, so "2e" stays 2·e).
      if (!isAtEnd() && (input[pos] == 'E' || input[pos] == 'e')) {
        val save = pos
        pos++
        if (!isAtEnd() && (input[pos] == '-' || input[pos] == '+')) pos++
        if (!isAtEnd() && input[pos].isDigit()) { while (!isAtEnd() && input[pos].isDigit()) pos++ } else pos = save
      }
      return input.substring(start, pos).toDoubleOrNull() ?: throw EvaluationException("Invalid number")
    }

    private fun parseIdentifier(): Double {
      val start = pos
      while (!isAtEnd() && (input[pos].isLetterOrDigit() || input[pos] == '_')) pos++
      val name = input.substring(start, pos)
      variables[name]?.let { return it }
      CONSTANTS[name]?.let { return it }
      if (name in FUNCTIONS) return callFunction(name)
      // "2xy" or "sinx": try the longest known prefix, then parse the rest as another atom.
      for (end in name.length - 1 downTo 1) {
        val prefix = name.substring(0, end)
        val known = variables[prefix] ?: CONSTANTS[prefix]
        if (known != null) { pos = start + end; return known }
        if (prefix in FUNCTIONS) { pos = start + end; return callFunction(prefix) }
      }
      throw EvaluationException("Unknown name '$name'")
    }

    private fun callFunction(name: String): Double {
      val arg = if (peek() == '(') { consume(); val v = parseExpression(); closeParen(); v } else parseFactor()
      return applyFunction(name, arg)
    }

    private fun toRadians(x: Double) = if (useRadians) x else Math.toRadians(x)
    private fun fromRadians(x: Double) = if (useRadians) x else Math.toDegrees(x)

    private fun applyFunction(name: String, arg: Double): Double = when (name) {
      "sin" -> snap(sin(toRadians(arg)))
      "cos" -> snap(cos(toRadians(arg)))
      "tan" -> {
        val c = snap(cos(toRadians(arg)))
        if (c == 0.0) throw EvaluationException("Undefined") else snap(sin(toRadians(arg))) / c
      }
      "asin" -> if (arg !in -1.0..1.0) throw EvaluationException("Invalid input") else fromRadians(Math.asin(arg))
      "acos" -> if (arg !in -1.0..1.0) throw EvaluationException("Invalid input") else fromRadians(Math.acos(arg))
      "atan" -> fromRadians(Math.atan(arg))
      "sinh" -> sinh(arg)
      "cosh" -> cosh(arg)
      "tanh" -> tanh(arg)
      "sqrt" -> if (arg < 0) throw EvaluationException("Invalid input") else sqrt(arg)
      "cbrt" -> Math.cbrt(arg)
      "ln" -> if (arg <= 0) throw EvaluationException("Invalid input") else ln(arg)
      "log" -> if (arg <= 0) throw EvaluationException("Invalid input") else log10(arg)
      "log2" -> if (arg <= 0) throw EvaluationException("Invalid input") else log2(arg)
      "exp" -> exp(arg)
      "abs" -> abs(arg)
      "floor" -> Math.floor(arg)
      "ceil" -> Math.ceil(arg)
      "round" -> Math.round(arg).toDouble()
      else -> throw EvaluationException("Unknown function '$name'")
    }

    /** sin(180°) is 1.2e-16 in floating point; a calculator should say 0. */
    private fun snap(x: Double) = if (abs(x) < 1e-12) 0.0 else x

    private fun factorial(value: Double): Double {
      if (value < 0 || value != Math.floor(value) || value > 170) throw EvaluationException("Invalid factorial input")
      var result = 1.0
      var n = value.toInt()
      while (n > 1) { result *= n; n-- }
      return result
    }
  }
}
