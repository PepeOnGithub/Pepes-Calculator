package xyz.pepe.glacierclient.calculatorapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolsTest {
  private val eps = 1e-6

  @Test fun percentages() {
    assertEquals(30.0, Tools.percentOf(15.0, 200.0), eps)
    assertEquals(25.0, Tools.whatPercent(30.0, 120.0)!!, eps)
    assertNull(Tools.whatPercent(1.0, 0.0))
    assertEquals(25.0, Tools.percentChange(80.0, 100.0)!!, eps)
    val d = Tools.discount(200.0, 25.0)
    assertEquals(50.0, d.saved, eps)
    assertEquals(150.0, d.finalPrice, eps)
  }

  @Test fun tipAndTax() {
    val t = Tools.tipSplit(100.0, 20.0, 4)
    assertEquals(120.0, t.total, eps)
    assertEquals(30.0, t.perPerson, eps)
    val inc = Tools.tax(118.0, 18.0, inclusive = true)
    assertEquals(100.0, inc.net, eps)
    assertEquals(18.0, inc.tax, eps)
    val exc = Tools.tax(100.0, 18.0, inclusive = false)
    assertEquals(118.0, exc.gross, eps)
  }

  @Test fun loanAndCompound() {
    val l = Tools.loan(100000.0, 12.0, 12)!!
    assertEquals(8884.88, l.monthly, 0.01)
    assertEquals(3.0, Tools.loan(36.0, 0.0, 12)!!.monthly, eps)
    assertNull(Tools.loan(1000.0, 5.0, 0))
    val c = Tools.compound(1000.0, 10.0, 2.0, 1)!!
    assertEquals(1210.0, c.finalAmount, eps)
  }

  @Test fun bmi() {
    val b = Tools.bmi(70.0, 175.0)!!
    assertEquals(22.86, b, 0.01)
    assertEquals("Healthy weight", Tools.bmiCategory(b))
    assertEquals("Obese", Tools.bmiCategory(31.0))
  }

  @Test fun dates() {
    assertEquals(0L, Tools.epochDay(1970, 1, 1))
    assertEquals(19723L, Tools.epochDay(2024, 1, 1))
    assertEquals(Tools.Ymd(2024, 2, 29), Tools.fromEpochDay(Tools.epochDay(2024, 2, 29)))
    assertEquals(366L, Tools.daysBetween(Tools.Ymd(2024, 1, 1), Tools.Ymd(2025, 1, 1)))
    assertEquals(Tools.Ymd(2024, 3, 1), Tools.addDays(Tools.Ymd(2024, 2, 29), 1))
    assertEquals("Thursday", Tools.weekdayName(Tools.Ymd(1970, 1, 1)))
    assertEquals("Monday", Tools.weekdayName(Tools.Ymd(2024, 1, 1)))
    assertTrue(Tools.isValidDate(2024, 2, 29))
    assertTrue(!Tools.isValidDate(2023, 2, 29))
  }

  @Test fun age() {
    val a = Tools.age(Tools.Ymd(1990, 5, 15), Tools.Ymd(2024, 5, 10))!!
    assertEquals(33, a.years); assertEquals(11, a.months); assertEquals(25, a.days)
    assertEquals(5L, a.daysToBirthday)
    assertNull(Tools.age(Tools.Ymd(2030, 1, 1), Tools.Ymd(2024, 1, 1)))
    assertEquals(0L, Tools.age(Tools.Ymd(2000, 3, 3), Tools.Ymd(2024, 3, 3))!!.daysToBirthday)
  }

  @Test fun numberTheory() {
    assertEquals(6L, Tools.gcd(12, 18))
    assertEquals(36L, Tools.lcm(12, 18))
    assertEquals(listOf(2L, 2L, 2L, 3L, 3L, 5L), Tools.primeFactors(360))
    assertEquals("2³ × 3² × 5", Tools.factorsText(Tools.primeFactors(360)))
    assertTrue(Tools.isPrime(97)); assertTrue(!Tools.isPrime(91))
    assertEquals(4L, Tools.gcdAll(listOf(8, 12, 20)))
    assertEquals(60L, Tools.lcmAll(listOf(4, 6, 10)))
  }

  @Test fun statistics() {
    val s = Tools.stats(listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0))!!
    assertEquals(5.0, s.mean, eps)
    assertEquals(4.5, s.median, eps)
    assertEquals(listOf(4.0), s.modes)
    assertEquals(2.0, s.populationStdDev, eps)
    assertNull(Tools.stats(emptyList()))
    assertEquals(listOf(1.0, 2.5, 3.0), Tools.parseNumbers("1, 2.5\n3"))
    assertNull(Tools.parseNumbers("1, x"))
  }

  @Test fun basesAndBits() {
    assertEquals("FF", Tools.toBase(255, 16))
    assertEquals(255L, Tools.parseBase("1111 1111", 2))
    assertNull(Tools.parseBase("12", 2))
    assertEquals(8L, Tools.bitwise(Tools.BitOp.AND, 12, 10))
    assertEquals(14L, Tools.bitwise(Tools.BitOp.OR, 12, 10))
    assertEquals(6L, Tools.bitwise(Tools.BitOp.XOR, 12, 10))
    assertEquals(40L, Tools.bitwise(Tools.BitOp.SHL, 5, 3))
  }

  @Test fun randomStaysInRange() {
    val r = java.util.Random(1)
    repeat(200) { assertTrue(Tools.randomInt(3, 7, r) in 3L..7L) }
    assertTrue(Tools.rollDice(3, 6, r).all { it in 1..6 })
    assertNotNull(Tools.rollDice(1, 20, r).firstOrNull())
  }
}
