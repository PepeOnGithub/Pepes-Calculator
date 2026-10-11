package xyz.pepe.glacierclient.calculatorapp.domain

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/** How results are printed — shared by the keypad, Math Notes and the converter. */
data class NumberStyle(val decimals: Int? = null, val grouping: Boolean = true)

object NumberFormat {
  private const val AUTO_SCALE = 10

  fun format(value: Double, style: NumberStyle = NumberStyle()): String {
    if (value.isNaN() || value.isInfinite()) return "Error"
    val a = abs(value)
    if (a != 0.0 && (a >= 1e15 || (style.decimals == null && a < 1e-9))) return scientific(value)
    val scale = style.decimals ?: AUTO_SCALE
    var d = BigDecimal(value).setScale(scale, RoundingMode.HALF_UP)
    if (style.decimals == null) d = d.stripTrailingZeros()
    if (d.signum() == 0) d = BigDecimal.ZERO.setScale(style.decimals ?: 0)
    val plain = d.toPlainString()
    return if (style.grouping) group(plain) else plain
  }

  /** Re-prints an evaluated result (raw, full precision) in [style]; non-numeric text is returned as is. */
  fun reformat(raw: String, style: NumberStyle): String {
    val v = raw.toDoubleOrNull() ?: return raw
    return if (v.isNaN() || v.isInfinite()) raw else format(v, style)
  }

  private fun scientific(value: Double): String {
    val s = String.format(java.util.Locale.US, "%.8E", value)
    val (mant, exp) = s.split('E')
    val m = mant.trimEnd('0').trimEnd('.')
    return "${m}E${exp.toInt()}"
  }

  /** Inserts thousands separators into every run of digits before a decimal point. */
  fun group(text: String): String {
    val out = StringBuilder()
    var i = 0
    while (i < text.length) {
      val c = text[i]
      if (c.isDigit()) {
        val start = i
        while (i < text.length && text[i].isDigit()) i++
        val run = text.substring(start, i)
        val afterPoint = start > 0 && text[start - 1] == '.'
        val exponent = start > 0 && text[start - 1].let { it == 'E' || ((it == '-' || it == '+') && start > 1 && text[start - 2] == 'E') }
        out.append(if (afterPoint || exponent || run.length <= 3) run else groupRun(run))
      } else {
        out.append(c); i++
      }
    }
    return out.toString()
  }

  private fun groupRun(run: String): String {
    val first = run.length % 3
    val sb = StringBuilder(run.substring(0, first))
    var i = first
    while (i < run.length) {
      if (sb.isNotEmpty()) sb.append(',')
      sb.append(run, i, i + 3)
      i += 3
    }
    return sb.toString()
  }
}
