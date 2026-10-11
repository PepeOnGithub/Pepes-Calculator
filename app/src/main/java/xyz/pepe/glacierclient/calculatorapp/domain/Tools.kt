package xyz.pepe.glacierclient.calculatorapp.domain

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt

/** Pure logic behind the Tools tab (money, percentages, dates, health, numbers). No Android here. */
object Tools {

  // Percentages ---------------------------------------------------------------------------------

  /** [percent]% of [value]. */
  fun percentOf(percent: Double, value: Double) = value * percent / 100.0

  /** [part] is what percent of [whole]; null when [whole] is 0. */
  fun whatPercent(part: Double, whole: Double): Double? = if (whole == 0.0) null else part / whole * 100.0

  /** Percentage change from [from] to [to]; null when [from] is 0. */
  fun percentChange(from: Double, to: Double): Double? = if (from == 0.0) null else (to - from) / abs(from) * 100.0

  data class Discount(val saved: Double, val finalPrice: Double)

  fun discount(price: Double, percent: Double): Discount {
    val saved = price * percent / 100.0
    return Discount(saved, price - saved)
  }

  // Tip and split -------------------------------------------------------------------------------

  data class TipSplit(val tip: Double, val total: Double, val perPerson: Double, val tipPerPerson: Double)

  fun tipSplit(bill: Double, tipPercent: Double, people: Int): TipSplit {
    val n = people.coerceAtLeast(1)
    val tip = bill * tipPercent / 100.0
    val total = bill + tip
    return TipSplit(tip, total, total / n, tip / n)
  }

  // Tax (GST / VAT) -----------------------------------------------------------------------------

  data class Tax(val net: Double, val tax: Double, val gross: Double)

  /** [inclusive] = [amount] already contains the tax (extract it); otherwise the tax is added on top. */
  fun tax(amount: Double, ratePercent: Double, inclusive: Boolean): Tax =
    if (inclusive) {
      val net = amount / (1 + ratePercent / 100.0)
      Tax(net, amount - net, amount)
    } else {
      val tax = amount * ratePercent / 100.0
      Tax(amount, tax, amount + tax)
    }

  // Loans and savings ---------------------------------------------------------------------------

  data class Loan(val monthly: Double, val total: Double, val interest: Double)

  /** Fixed monthly instalment (EMI) for [principal] at [annualPercent] a year over [months]. */
  fun loan(principal: Double, annualPercent: Double, months: Int): Loan? {
    if (months <= 0 || principal < 0) return null
    val r = annualPercent / 12.0 / 100.0
    val emi = if (r == 0.0) principal / months else {
      val g = (1 + r).pow(months)
      principal * r * g / (g - 1)
    }
    val total = emi * months
    return Loan(emi, total, total - principal)
  }

  data class Compound(val finalAmount: Double, val interest: Double)

  /** Compound interest: [timesPerYear] compounding periods a year for [years] years. */
  fun compound(principal: Double, annualPercent: Double, years: Double, timesPerYear: Int): Compound? {
    if (timesPerYear <= 0 || years < 0) return null
    val amount = principal * (1 + annualPercent / 100.0 / timesPerYear).pow(timesPerYear * years)
    return Compound(amount, amount - principal)
  }

  // Health --------------------------------------------------------------------------------------

  fun bmi(weightKg: Double, heightCm: Double): Double? {
    if (weightKg <= 0 || heightCm <= 0) return null
    val m = heightCm / 100.0
    return weightKg / (m * m)
  }

  fun bmiCategory(bmi: Double) = when {
    bmi < 18.5 -> "Underweight"
    bmi < 25.0 -> "Healthy weight"
    bmi < 30.0 -> "Overweight"
    else -> "Obese"
  }

  // Dates ---------------------------------------------------------------------------------------

  /** Days since 1970-01-01 for a proleptic Gregorian date (month 1..12). */
  fun epochDay(year: Int, month: Int, day: Int): Long {
    val y = if (month <= 2) year - 1L else year.toLong()
    val era = floorDiv(y, 400)
    val yoe = y - era * 400
    val mp = (month + 9) % 12
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
  }

  data class Ymd(val year: Int, val month: Int, val day: Int) {
    override fun toString() = "%04d-%02d-%02d".format(year, month, day)
  }

  fun fromEpochDay(epoch: Long): Ymd {
    val z = epoch + 719468
    val era = floorDiv(z, 146097)
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
    return Ymd((if (m <= 2) y + 1 else y).toInt(), m, d)
  }

  fun isValidDate(year: Int, month: Int, day: Int): Boolean {
    if (month !in 1..12 || day < 1 || year < 1) return false
    return day <= daysInMonth(year, month)
  }

  fun daysInMonth(year: Int, month: Int): Int = when (month) {
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
  }

  /** Signed number of days from the first date to the second. */
  fun daysBetween(from: Ymd, to: Ymd) = epochDay(to.year, to.month, to.day) - epochDay(from.year, from.month, from.day)

  fun addDays(date: Ymd, days: Long): Ymd = fromEpochDay(epochDay(date.year, date.month, date.day) + days)

  fun weekdayName(date: Ymd): String {
    val names = listOf("Thursday", "Friday", "Saturday", "Sunday", "Monday", "Tuesday", "Wednesday") // 1970-01-01 was a Thursday
    return names[floorMod(epochDay(date.year, date.month, date.day), 7).toInt()]
  }

  data class Age(val years: Int, val months: Int, val days: Int, val totalDays: Long, val daysToBirthday: Long)

  /** Age on [today] for someone born on [birth]; null if born in the future. */
  fun age(birth: Ymd, today: Ymd): Age? {
    val total = daysBetween(birth, today)
    if (total < 0) return null
    var years = today.year - birth.year
    var months = today.month - birth.month
    var days = today.day - birth.day
    if (days < 0) {
      months -= 1
      val prevMonth = if (today.month == 1) 12 else today.month - 1
      val prevYear = if (today.month == 1) today.year - 1 else today.year
      days += daysInMonth(prevYear, prevMonth)
    }
    if (months < 0) { years -= 1; months += 12 }
    var next = nextBirthday(birth, today.year)
    if (daysBetween(today, next) < 0) next = nextBirthday(birth, today.year + 1)
    return Age(years, months, days, total, daysBetween(today, next))
  }

  private fun nextBirthday(birth: Ymd, year: Int): Ymd {
    // A 29 February birthday is celebrated on 28 February in common years.
    val day = if (birth.month == 2 && birth.day == 29 && !isValidDate(year, 2, 29)) 28 else birth.day
    return Ymd(year, birth.month, day)
  }

  // Numbers -------------------------------------------------------------------------------------

  fun gcd(a: Long, b: Long): Long {
    var x = abs(a); var y = abs(b)
    while (y != 0L) { val t = x % y; x = y; y = t }
    return x
  }

  /** Least common multiple; null on overflow. */
  fun lcm(a: Long, b: Long): Long? {
    if (a == 0L || b == 0L) return 0L
    val g = gcd(a, b)
    return try { Math.multiplyExact(abs(a / g), abs(b)) } catch (_: ArithmeticException) { null }
  }

  fun gcdAll(values: List<Long>): Long? = values.takeIf { it.isNotEmpty() }?.reduce(::gcd)

  fun lcmAll(values: List<Long>): Long? {
    if (values.isEmpty()) return null
    var acc = values.first()
    for (v in values.drop(1)) acc = lcm(acc, v) ?: return null
    return acc
  }

  /** Prime factors in ascending order, e.g. 360 -> [2,2,2,3,3,5]. Empty for n < 2. */
  fun primeFactors(n: Long): List<Long> {
    var m = n
    val out = ArrayList<Long>()
    if (m < 2) return out
    var p = 2L
    while (p * p <= m) {
      while (m % p == 0L) { out += p; m /= p }
      p += if (p == 2L) 1 else 2
    }
    if (m > 1) out += m
    return out
  }

  /** "2³ × 3² × 5" style text. */
  fun factorsText(factors: List<Long>): String {
    if (factors.isEmpty()) return ""
    val sup = mapOf('0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴', '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹')
    return factors.groupingBy { it }.eachCount().entries.joinToString(" × ") { (p, c) ->
      if (c == 1) "$p" else "$p" + c.toString().map { sup[it] ?: it }.joinToString("")
    }
  }

  fun isPrime(n: Long) = n >= 2 && primeFactors(n).size == 1

  // Statistics ----------------------------------------------------------------------------------

  data class Stats(
    val count: Int, val sum: Double, val mean: Double, val median: Double, val modes: List<Double>,
    val min: Double, val max: Double, val range: Double, val populationStdDev: Double, val sampleStdDev: Double?,
  )

  fun stats(values: List<Double>): Stats? {
    if (values.isEmpty()) return null
    val sorted = values.sorted()
    val n = sorted.size
    val sum = sorted.sum()
    val mean = sum / n
    val median = if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2
    val counts = sorted.groupingBy { it }.eachCount()
    val top = counts.values.max()
    val modes = if (top == 1) emptyList() else counts.filterValues { it == top }.keys.sorted()
    val ss = sorted.sumOf { (it - mean) * (it - mean) }
    return Stats(n, sum, mean, median, modes, sorted.first(), sorted.last(), sorted.last() - sorted.first(),
      sqrt(ss / n), if (n > 1) sqrt(ss / (n - 1)) else null)
  }

  /** Splits "1, 2 3;4\n5" into numbers; null if any piece isn't a number. */
  fun parseNumbers(text: String): List<Double>? {
    val parts = text.split(Regex("[\\s,;]+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return emptyList()
    val out = ArrayList<Double>()
    for (p in parts) out += p.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
    return out
  }

  // Number bases and bitwise ----------------------------------------------------------------------

  fun toBase(value: Long, base: Int): String = java.lang.Long.toString(value, base).uppercase()

  /** Parses [text] in [base]; underscores and spaces are ignored. Null if invalid. */
  fun parseBase(text: String, base: Int): Long? =
    text.filterNot { it == '_' || it == ' ' }.takeIf { it.isNotEmpty() }?.toLongOrNull(base)

  enum class BitOp(val label: String) { AND("AND"), OR("OR"), XOR("XOR"), SHL("<<"), SHR(">>") }

  fun bitwise(op: BitOp, a: Long, b: Long): Long = when (op) {
    BitOp.AND -> a and b
    BitOp.OR -> a or b
    BitOp.XOR -> a xor b
    BitOp.SHL -> a shl b.toInt().coerceIn(0, 63)
    BitOp.SHR -> a shr b.toInt().coerceIn(0, 63)
  }

  // Random ----------------------------------------------------------------------------------------

  /** Rolls [count] dice with [sides] sides using [random]. */
  fun rollDice(count: Int, sides: Int, random: java.util.Random): List<Int> =
    List(count.coerceIn(1, 20)) { random.nextInt(sides.coerceAtLeast(2)) + 1 }

  /** A whole number in [min]..[max] inclusive (order-insensitive). */
  fun randomInt(min: Long, max: Long, random: java.util.Random): Long {
    val lo = minOf(min, max); val hi = maxOf(min, max)
    val span = hi - lo + 1
    return if (span <= 0) lo else lo + floor(random.nextDouble() * span).toLong().coerceAtMost(span - 1)
  }

  // Misc ------------------------------------------------------------------------------------------

  fun round2(x: Double) = (x * 100).roundToLong() / 100.0

  private fun floorDiv(a: Long, b: Long) = Math.floorDiv(a, b)
  private fun floorMod(a: Long, b: Long) = Math.floorMod(a, b)
}
