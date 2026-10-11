package xyz.pepe.glacierclient.calculatorapp.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetCalcTest {
  // A tiny stand-in for the real engine: handles "a+b" and "a×b" with integers, anything else is an error.
  private val eval: (String) -> String? = { e ->
    Regex("^(\\d+)([+×])(\\d+)$").matchEntire(e)?.let { m ->
      val a = m.groupValues[1].toLong(); val b = m.groupValues[3].toLong()
      (if (m.groupValues[2] == "+") a + b else a * b).toString()
    }
  }

  private fun type(vararg keys: String): WidgetCalcState = keys.fold(WidgetCalcState()) { s, k -> WidgetCalc.press(s, k, eval) }

  @Test fun typingBuildsTheExpression() = assertEquals("12+3", type("1", "2", "+", "3").expr)

  @Test fun equalsShowsTheResult() {
    val s = type("2", "+", "3", "=")
    assertEquals("5", s.shown)
    assertEquals(true, s.done)
    assertEquals("2+3", s.expr)
  }

  @Test fun operatorAfterEqualsContinuesFromTheResult() {
    assertEquals("5×", type("2", "+", "3", "=", "×").expr)
  }

  @Test fun digitAfterEqualsStartsOver() = assertEquals("7", type("2", "+", "3", "=", "7").expr)

  @Test fun consecutiveOperatorsReplaceEachOther() = assertEquals("2×", type("2", "+", "×").expr)

  @Test fun leadingOperatorIsIgnoredExceptMinus() {
    assertEquals("", type("+").expr)
    assertEquals("−", type("−").expr)
  }

  @Test fun backspaceAndClear() {
    assertEquals("1", type("1", "2", "⌫").expr)
    assertEquals("", type("1", "2", "C").expr)
    assertEquals("", type("2", "+", "3", "=", "⌫").expr)
  }

  @Test fun errorKeepsTheExpression() {
    val s = type("2", "+", "=")
    assertEquals("Error", s.shown)
    assertEquals("2+", s.expr)
    assertEquals(false, s.done)
  }

  @Test fun lengthIsCapped() {
    val s = type(*Array(80) { "1" })
    assertEquals(WidgetCalc.MAX_LEN, s.expr.length)
  }

  @Test fun previewOnlyWhileTypingAnExpression() {
    assertNull(WidgetCalc.preview(type("1", "2"), eval))
    assertEquals("= 5", WidgetCalc.preview(type("2", "+", "3"), eval))
    assertNull(WidgetCalc.preview(type("2", "+", "3", "="), eval))
  }

  @Test fun toEngineMapsSymbols() = assertEquals("6/3*2-1", WidgetCalc.toEngine("6÷3×2−1"))
}
