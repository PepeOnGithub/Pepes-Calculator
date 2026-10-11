package xyz.pepe.glacierclient.calculatorapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorEngineTest {

    @Test
    fun testBasicArithmetic() {
        assertEquals(7.0, CalculatorEngine.evaluate("3 + 4", useRadians = true), 1e-6)
        assertEquals(14.0, CalculatorEngine.evaluate("2 + 3 * 4", useRadians = true), 1e-6)
        assertEquals(20.0, CalculatorEngine.evaluate("(2 + 3) * 4", useRadians = true), 1e-6)
        assertEquals(2.5, CalculatorEngine.evaluate("5 / 2", useRadians = true), 1e-6)
    }

    @Test
    fun testScientificFunctions() {
        assertEquals(0.0, CalculatorEngine.evaluate("sin(0)", useRadians = true), 1e-6)
        assertEquals(1.0, CalculatorEngine.evaluate("cos(0)", useRadians = true), 1e-6)
        assertEquals(3.0, CalculatorEngine.evaluate("sqrt(9)", useRadians = true), 1e-6)
        assertEquals(8.0, CalculatorEngine.evaluate("2 ^ 3", useRadians = true), 1e-6)
    }

    @Test
    fun testVariables() {
        val res = CalculatorEngine.evaluate("2 * x + 1", useRadians = true, variables = mapOf("x" to 5.0))
        assertEquals(11.0, res, 1e-6)
    }

    @Test
    fun testUnitConverter() {
        val km = ConvertibleUnit("km", "Kilometer", 1000.0)
        val m = ConvertibleUnit("m", "Meter", 1.0)
        assertEquals(5000.0, UnitConverter.convert(5.0, km, m), 1e-6)
        assertEquals(0.005, UnitConverter.convert(5.0, m, km), 1e-6)
    }

    @Test
    fun testWhatPeopleType() {
        assertEquals(2 * Math.PI, CalculatorEngine.evaluate("2π", useRadians = true), 1e-9)
        assertEquals(15.0, CalculatorEngine.evaluate("3(4+1)", useRadians = true), 1e-9)
        assertEquals(12.0, CalculatorEngine.evaluate("12 × 4 ÷ 4", useRadians = true), 1e-9)
        assertEquals(3.0, CalculatorEngine.evaluate("√9", useRadians = true), 1e-9)
        assertEquals(1.0, CalculatorEngine.evaluate("sin(90", useRadians = false), 1e-9) // unclosed paren
        assertEquals(0.0, CalculatorEngine.evaluate("sin(180)", useRadians = false), 0.0)
        assertEquals(1500.0, CalculatorEngine.evaluate("1.5E3", useRadians = true), 1e-9)
        assertEquals(1234.0, CalculatorEngine.evaluate("1,234", useRadians = true), 1e-9)
    }

    @Test
    fun testPercentLikeIos() {
        assertEquals(220.0, CalculatorEngine.evaluate("200 + 10%", useRadians = true), 1e-9)
        assertEquals(180.0, CalculatorEngine.evaluate("200 - 10%", useRadians = true), 1e-9)
        assertEquals(0.5, CalculatorEngine.evaluate("50%", useRadians = true), 1e-9)
        assertEquals(11.52, CalculatorEngine.evaluate("18% × 64", useRadians = true), 1e-9)
    }

    @Test
    fun testTemperature() {
        val c = UnitCategory.TEMPERATURE.units
        assertEquals(212.0, UnitConverter.convert(100.0, c[0], c[1]), 1e-9)
        assertEquals(273.15, UnitConverter.convert(0.0, c[0], c[2]), 1e-9)
        assertEquals(-40.0, UnitConverter.convert(-40.0, c[1], c[0]), 1e-9)
    }

    @Test
    fun testNumberFormat() {
        assertEquals("1,234,567.5", NumberFormat.format(1234567.5))
        assertEquals("0.3", NumberFormat.format(0.1 + 0.2))
        assertEquals("3.14", NumberFormat.format(Math.PI, NumberStyle(decimals = 2)))
        assertEquals("1E20", NumberFormat.format(1e20))
        assertEquals("12×3,456.789", NumberFormat.group("12×3456.789"))
    }

    @Test
    fun testMathNotes() {
        val text = """
            price = 12
            qty = 3
            price × qty =
            Tip: 18% × 64 =
            240 km in mi =
            4
            6
            sum =

            y = 2x + price
        """.trimIndent()
        val a = MathNotes.analyze(text, useRadians = false, style = NumberStyle())
        assertEquals(null, a.results[0])
        assertEquals("36", a.results[2])
        assertEquals("11.52", a.results[3])
        assertEquals("149.129086137 mi", a.results[4])
        assertEquals("206.649086137", a.results[7]) // everything solved above it in the block
        assertEquals("10", MathNotes.analyze("Groceries\n4\n6\nsum =", false, NumberStyle()).results[3])
        assertEquals(listOf("price", "qty"), a.variables.map { it.name })
        assertEquals(1, a.graphs.size)
        assertEquals("price = 20", MathNotes.setVariable("price = 12", a.variables[0].copy(line = 0), 20.0, NumberStyle()))
    }
}
