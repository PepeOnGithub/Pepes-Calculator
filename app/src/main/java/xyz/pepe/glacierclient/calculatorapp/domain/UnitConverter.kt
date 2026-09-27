package xyz.pepe.glacierclient.calculatorapp.domain

/** A convertible unit: [factor] converts 1 of this unit into the category's base unit. */
data class ConvertibleUnit(val symbol: String, val label: String, val factor: Double)

enum class UnitCategory(val label: String, val units: List<ConvertibleUnit>) {
    LENGTH("Length", listOf(
        ConvertibleUnit("m", "Meters", 1.0),
        ConvertibleUnit("km", "Kilometers", 1000.0),
        ConvertibleUnit("cm", "Centimeters", 0.01),
        ConvertibleUnit("mm", "Millimeters", 0.001),
        ConvertibleUnit("mi", "Miles", 1609.344),
        ConvertibleUnit("yd", "Yards", 0.9144),
        ConvertibleUnit("ft", "Feet", 0.3048),
        ConvertibleUnit("in", "Inches", 0.0254)
    )),
    WEIGHT("Weight", listOf(
        ConvertibleUnit("kg", "Kilograms", 1.0),
        ConvertibleUnit("g", "Grams", 0.001),
        ConvertibleUnit("mg", "Milligrams", 0.000001),
        ConvertibleUnit("lb", "Pounds", 0.45359237),
        ConvertibleUnit("oz", "Ounces", 0.028349523125),
        ConvertibleUnit("st", "Stone", 6.35029318)
    )),
    AREA("Area", listOf(
        ConvertibleUnit("m2", "Sq. meters", 1.0),
        ConvertibleUnit("km2", "Sq. kilometers", 1_000_000.0),
        ConvertibleUnit("ha", "Hectares", 10_000.0),
        ConvertibleUnit("ac", "Acres", 4046.8564224),
        ConvertibleUnit("ft2", "Sq. feet", 0.09290304)
    )),
    SPEED("Speed", listOf(
        ConvertibleUnit("m/s", "Meters/sec", 1.0),
        ConvertibleUnit("km/h", "Km/hour", 0.277778),
        ConvertibleUnit("mph", "Miles/hour", 0.44704),
        ConvertibleUnit("kn", "Knots", 0.514444)
    )),
    PRESSURE("Pressure", listOf(
        ConvertibleUnit("Pa", "Pascals", 1.0),
        ConvertibleUnit("hPa", "Hectopascals", 100.0),
        ConvertibleUnit("bar", "Bar", 100_000.0),
        ConvertibleUnit("atm", "Atmospheres", 101_325.0),
        ConvertibleUnit("psi", "PSI", 6894.757293168)
    )),
    ENERGY("Energy", listOf(
        ConvertibleUnit("J", "Joules", 1.0),
        ConvertibleUnit("kJ", "Kilojoules", 1000.0),
        ConvertibleUnit("cal", "Calories", 4.184),
        ConvertibleUnit("kcal", "Kilocalories", 4184.0),
        ConvertibleUnit("Wh", "Watt-hours", 3600.0)
    )),
    // Fixed snapshot rates (approximate, not live) — this app has no network dependency for
    // currency data. Base unit is USD.
    CURRENCY("Currency (approximate)", listOf(
        ConvertibleUnit("USD", "US Dollar", 1.0),
        ConvertibleUnit("EUR", "Euro", 0.92),
        ConvertibleUnit("GBP", "British Pound", 0.79),
        ConvertibleUnit("JPY", "Japanese Yen", 149.5),
        ConvertibleUnit("INR", "Indian Rupee", 83.3),
        ConvertibleUnit("CAD", "Canadian Dollar", 1.36),
        ConvertibleUnit("AUD", "Australian Dollar", 1.52),
        ConvertibleUnit("CNY", "Chinese Yuan", 7.24)
    ))
}

object UnitConverter {
    fun convert(value: Double, from: ConvertibleUnit, to: ConvertibleUnit): Double {
        return value * from.factor / to.factor
    }
}
