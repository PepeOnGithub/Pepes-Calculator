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
}
