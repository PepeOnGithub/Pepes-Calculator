package xyz.pepe.glacierclient.calculatorapp.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * A small recursive-descent expression evaluator, powerful enough for a basic-plus-scientific
 * calculator: + - * / ^, unary minus, parentheses, %, factorial (!), and the common scientific
 * functions (sin/cos/tan and their inverses, ln/log, sqrt, and the constants pi/e).
 *
 * This deliberately avoids a heavyweight parser library — the grammar is small and the whole
 * thing is easy to reason about and unit-test.
 */
object CalculatorEngine {

    class EvaluationException(message: String) : Exception(message)

    /** Evaluates [expression] and returns the numeric result, or throws [EvaluationException].
     *  [variables] lets callers (e.g. the Notes graphing screen) bind names like "x" to a value. */
    fun evaluate(expression: String, useRadians: Boolean, variables: Map<String, Double> = emptyMap()): Double {
        val normalized = expression
            .replace('×', '*')
            .replace('÷', '/')
            .replace('−', '-')
            .trim()
        if (normalized.isEmpty()) throw EvaluationException("Empty expression")
        val parser = Parser(normalized, useRadians, variables)
        val result = parser.parseExpression()
        parser.skipSpaces()
        if (!parser.isAtEnd()) throw EvaluationException("Unexpected character at ${parser.pos}")
        if (result.isNaN() || result.isInfinite()) throw EvaluationException("Math error")
        return result
    }

    private class Parser(
        private val input: String,
        private val useRadians: Boolean,
        private val variables: Map<String, Double>
    ) {
        var pos = 0

        fun isAtEnd() = pos >= input.length
        fun skipSpaces() { while (!isAtEnd() && input[pos] == ' ') pos++ }
        private fun peek(): Char { skipSpaces(); return if (isAtEnd()) '\u0000' else input[pos] }
        private fun consume(): Char { skipSpaces(); return input[pos++] }

        // expression := term (('+' | '-') term)*
        fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                when (peek()) {
                    '+' -> { consume(); value += parseTerm() }
                    '-' -> { consume(); value -= parseTerm() }
                    else -> return value
                }
            }
        }

        // term := factor (('*' | '/' | '%') factor)*
        private fun parseTerm(): Double {
            var value = parseFactor()
            while (true) {
                when (peek()) {
                    '*' -> { consume(); value *= parseFactor() }
                    '/' -> {
                        consume()
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw EvaluationException("Division by zero")
                        value /= divisor
                    }
                    '%' -> { consume(); value %= parseFactor() }
                    else -> return value
                }
            }
        }

        // factor := power ('^' factor)?  -- right-associative
        private fun parseFactor(): Double {
            val base = parseUnary()
            if (peek() == '^') {
                consume()
                val exponent = parseFactor()
                return base.pow(exponent)
            }
            return base
        }

        // unary := ('-' | '+')* postfix
        private fun parseUnary(): Double {
            return when (peek()) {
                '-' -> { consume(); -parseUnary() }
                '+' -> { consume(); parseUnary() }
                else -> parsePostfix()
            }
        }

        // postfix := atom ('!')* ('%')*   -- factorial / trailing percent
        private fun parsePostfix(): Double {
            var value = parseAtom()
            while (true) {
                when (peek()) {
                    '!' -> { consume(); value = factorial(value) }
                    else -> return value
                }
            }
        }

        // atom := number | constant | function '(' expression ')' | '(' expression ')'
        private fun parseAtom(): Double {
            skipSpaces()
            if (isAtEnd()) throw EvaluationException("Unexpected end of expression")
            val c = input[pos]
            return when {
                c == '(' -> {
                    consume()
                    val value = parseExpression()
                    if (peek() != ')') throw EvaluationException("Missing closing parenthesis")
                    consume()
                    value
                }
                c.isDigit() || c == '.' -> parseNumber()
                c.isLetter() -> parseIdentifier()
                else -> throw EvaluationException("Unexpected character '$c'")
            }
        }

        private fun parseNumber(): Double {
            val start = pos
            while (!isAtEnd() && (input[pos].isDigit() || input[pos] == '.')) pos++
            return input.substring(start, pos).toDoubleOrNull()
                ?: throw EvaluationException("Invalid number")
        }

        private fun parseIdentifier(): Double {
            val start = pos
            while (!isAtEnd() && input[pos].isLetter()) pos++
            val name = input.substring(start, pos)
            return when {
                name == "pi" -> PI
                name == "e" -> Math.E
                peek() != '(' -> variables[name]
                    ?: throw EvaluationException("Unknown identifier '$name'")
                else -> {
                    consume()
                    val arg = parseExpression()
                    if (peek() != ')') throw EvaluationException("Missing closing parenthesis")
                    consume()
                    applyFunction(name, arg)
                }
            }
        }

        private fun toRadians(x: Double) = if (useRadians) x else Math.toRadians(x)
        private fun fromRadians(x: Double) = if (useRadians) x else Math.toDegrees(x)

        private fun applyFunction(name: String, arg: Double): Double = when (name) {
            "sin" -> sin(toRadians(arg))
            "cos" -> cos(toRadians(arg))
            "tan" -> tan(toRadians(arg))
            "asin" -> fromRadians(Math.asin(arg))
            "acos" -> fromRadians(Math.acos(arg))
            "atan" -> fromRadians(Math.atan(arg))
            "sqrt" -> if (arg < 0) throw EvaluationException("Invalid input") else sqrt(arg)
            "cbrt" -> Math.cbrt(arg)
            "ln" -> if (arg <= 0) throw EvaluationException("Invalid input") else ln(arg)
            "log" -> if (arg <= 0) throw EvaluationException("Invalid input") else log10(arg)
            "exp" -> exp(arg)
            "abs" -> abs(arg)
            else -> throw EvaluationException("Unknown function '$name'")
        }

        private fun factorial(value: Double): Double {
            if (value < 0 || value != Math.floor(value) || value > 170) {
                throw EvaluationException("Invalid factorial input")
            }
            var result = 1.0
            var n = value.toInt()
            while (n > 1) { result *= n; n-- }
            return result
        }
    }
}
