package xyz.pepe.glacierclient.calculatorapp.domain

import kotlin.math.abs
import kotlin.math.floor

/** Turns decimal results into fractions ("0.75" -> "3/4", "1.25" -> "1 1/4"). */
object Fractions {
  data class Fraction(val numerator: Long, val denominator: Long)

  /** Best fraction for [value] with a denominator up to [maxDenominator], or null if none is exact enough. */
  fun of(value: Double, maxDenominator: Long = 1000, tolerance: Double = 1e-9): Fraction? {
    if (value.isNaN() || value.isInfinite() || abs(value) > 1e9) return null
    val negative = value < 0
    val x = abs(value)
    // Continued-fraction expansion.
    var h1 = 1L; var h0 = 0L
    var k1 = 0L; var k0 = 1L
    var r = x
    repeat(40) {
      val a = floor(r).toLong()
      val h2 = a * h1 + h0
      val k2 = a * k1 + k0
      if (k2 > maxDenominator) return@repeat
      h0 = h1; h1 = h2; k0 = k1; k1 = k2
      val frac = r - a
      if (abs(x - h1.toDouble() / k1) <= tolerance * maxOf(1.0, x) || frac < 1e-12) {
        return Fraction(if (negative) -h1 else h1, k1)
      }
      r = 1.0 / frac
    }
    return if (k1 != 0L && abs(x - h1.toDouble() / k1) <= tolerance * maxOf(1.0, x)) Fraction(if (negative) -h1 else h1, k1) else null
  }

  /** Display text for non-integer values only (null for whole numbers or when no neat fraction exists). */
  fun format(value: Double): String? {
    val f = of(value) ?: return null
    if (f.denominator == 1L) return null
    val whole = abs(f.numerator) / f.denominator
    val rem = abs(f.numerator) % f.denominator
    val sign = if (f.numerator < 0) "-" else ""
    return if (whole == 0L) "$sign$rem/${f.denominator}" else "$sign$whole $rem/${f.denominator}"
  }
}
