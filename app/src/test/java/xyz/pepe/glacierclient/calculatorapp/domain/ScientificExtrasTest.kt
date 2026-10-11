package xyz.pepe.glacierclient.calculatorapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScientificExtrasTest {
  private fun ev(e: String) = CalculatorEngine.evaluate(e, false)

  @Test fun combinationsAndPermutations() {
    assertEquals(10.0, ev("5nCr2"), 1e-9)
    assertEquals(20.0, ev("5nPr2"), 1e-9)
    assertEquals(1.0, ev("6 nCr 0"), 1e-9)
    assertEquals(20.0, ev("3 + 5nPr2 - 3"), 1e-9)
  }

  @Test fun modulo() {
    assertEquals(1.0, ev("10mod3"), 1e-9)
    assertEquals(2.0, ev("-1 mod 3"), 1e-9)
  }

  @Test fun invalidInputsFail() {
    assertNull(CalculatorEngine.evaluateOrNull("2nCr5", false))
    assertNull(CalculatorEngine.evaluateOrNull("5 mod 0", false))
  }

  @Test fun hyperbolicAndLog2() {
    assertEquals(0.0, ev("sinh(0)"), 1e-9)
    assertEquals(3.0, ev("log2(8)"), 1e-9)
  }

  @Test fun fractions() {
    assertEquals("3/4", Fractions.format(0.75))
    assertEquals("1 1/4", Fractions.format(1.25))
    assertEquals("-1/3", Fractions.format(-1.0 / 3.0))
    assertNull(Fractions.format(2.0))
    assertNull(Fractions.format(Math.PI))
  }

  @Test fun newUnits() {
    val n = UnitCategory.FORCE.units.first { it.symbol == "N" }
    val lbf = UnitCategory.FORCE.units.first { it.symbol == "lbf" }
    assertEquals(4.4482216, UnitConverter.convert(1.0, lbf, n), 1e-6)
  }
}
