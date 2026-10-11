package xyz.pepe.glacierclient.calculatorapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormatTest {

    @Test
    fun reformatHonoursDecimalPlaces() {
        assertEquals("0.33", NumberFormat.reformat("0.3333333333", NumberStyle(decimals = 2, grouping = true)))
        assertEquals("0.3333333333", NumberFormat.reformat("0.3333333333", NumberStyle(decimals = null, grouping = true)))
    }

    @Test
    fun reformatHonoursGrouping() {
        assertEquals("1,234,567", NumberFormat.reformat("1234567", NumberStyle(null, true)))
        assertEquals("1234567", NumberFormat.reformat("1234567", NumberStyle(null, false)))
    }

    @Test
    fun reformatLeavesNonNumbersAlone() {
        assertEquals("abc", NumberFormat.reformat("abc", NumberStyle()))
        assertEquals("", NumberFormat.reformat("", NumberStyle()))
    }
}
