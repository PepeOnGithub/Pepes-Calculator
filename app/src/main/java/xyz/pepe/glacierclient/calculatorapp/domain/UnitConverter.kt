package xyz.pepe.glacierclient.calculatorapp.domain

/**
 * A convertible unit. In base units: base = value × [factor] + [offset]. The offset is only
 * non-zero for temperature, where scales don't share a zero.
 */
data class ConvertibleUnit(
  val symbol: String,
  val label: String,
  val factor: Double,
  val offset: Double = 0.0,
  /** Extra spellings accepted in Math Notes ("5 km in miles"). */
  val aliases: List<String> = emptyList(),
) {
  fun toBase(value: Double) = value * factor + offset
  fun fromBase(base: Double) = (base - offset) / factor
}

enum class UnitCategory(val label: String, val units: List<ConvertibleUnit>) {
  LENGTH("Length", listOf(
    ConvertibleUnit("m", "Meters", 1.0, aliases = listOf("meter", "meters", "metre", "metres")),
    ConvertibleUnit("km", "Kilometers", 1000.0, aliases = listOf("kilometer", "kilometers", "kilometre", "kilometres")),
    ConvertibleUnit("cm", "Centimeters", 0.01, aliases = listOf("centimeter", "centimeters")),
    ConvertibleUnit("mm", "Millimeters", 0.001, aliases = listOf("millimeter", "millimeters")),
    ConvertibleUnit("µm", "Micrometers", 1e-6, aliases = listOf("um", "micron", "microns")),
    ConvertibleUnit("nm", "Nanometers", 1e-9, aliases = listOf("nanometer", "nanometers")),
    ConvertibleUnit("mi", "Miles", 1609.344, aliases = listOf("mile", "miles")),
    ConvertibleUnit("yd", "Yards", 0.9144, aliases = listOf("yard", "yards")),
    ConvertibleUnit("ft", "Feet", 0.3048, aliases = listOf("foot", "feet")),
    ConvertibleUnit("in", "Inches", 0.0254, aliases = listOf("inch", "inches")),
    ConvertibleUnit("nmi", "Nautical miles", 1852.0, aliases = listOf("nautical mile", "nautical miles")),
    ConvertibleUnit("ly", "Light-years", 9.4607304725808e15, aliases = listOf("lightyear", "lightyears")),
  )),
  AREA("Area", listOf(
    ConvertibleUnit("m²", "Square meters", 1.0, aliases = listOf("m2", "sqm")),
    ConvertibleUnit("km²", "Square kilometers", 1e6, aliases = listOf("km2")),
    ConvertibleUnit("cm²", "Square centimeters", 1e-4, aliases = listOf("cm2")),
    ConvertibleUnit("ha", "Hectares", 1e4, aliases = listOf("hectare", "hectares")),
    ConvertibleUnit("ac", "Acres", 4046.8564224, aliases = listOf("acre", "acres")),
    ConvertibleUnit("mi²", "Square miles", 2589988.110336, aliases = listOf("mi2", "sqmi")),
    ConvertibleUnit("yd²", "Square yards", 0.83612736, aliases = listOf("yd2")),
    ConvertibleUnit("ft²", "Square feet", 0.09290304, aliases = listOf("ft2", "sqft")),
    ConvertibleUnit("in²", "Square inches", 0.00064516, aliases = listOf("in2")),
  )),
  VOLUME("Volume", listOf(
    ConvertibleUnit("L", "Liters", 1.0, aliases = listOf("l", "liter", "liters", "litre", "litres")),
    ConvertibleUnit("mL", "Milliliters", 0.001, aliases = listOf("ml", "milliliter", "milliliters")),
    ConvertibleUnit("m³", "Cubic meters", 1000.0, aliases = listOf("m3")),
    ConvertibleUnit("cm³", "Cubic centimeters", 0.001, aliases = listOf("cm3", "cc")),
    ConvertibleUnit("gal", "Gallons (US)", 3.785411784, aliases = listOf("gallon", "gallons")),
    ConvertibleUnit("qt", "Quarts (US)", 0.946352946, aliases = listOf("quart", "quarts")),
    ConvertibleUnit("pt", "Pints (US)", 0.473176473, aliases = listOf("pint", "pints")),
    ConvertibleUnit("cup", "Cups (US)", 0.2365882365, aliases = listOf("cups")),
    ConvertibleUnit("fl oz", "Fluid ounces (US)", 0.0295735295625, aliases = listOf("floz")),
    ConvertibleUnit("tbsp", "Tablespoons (US)", 0.01478676478125, aliases = listOf("tablespoon", "tablespoons")),
    ConvertibleUnit("tsp", "Teaspoons (US)", 0.00492892159375, aliases = listOf("teaspoon", "teaspoons")),
    ConvertibleUnit("imp gal", "Gallons (UK)", 4.54609, aliases = listOf("impgal")),
    ConvertibleUnit("ft³", "Cubic feet", 28.316846592, aliases = listOf("ft3")),
  )),
  WEIGHT("Weight", listOf(
    ConvertibleUnit("kg", "Kilograms", 1.0, aliases = listOf("kilogram", "kilograms", "kilo", "kilos")),
    ConvertibleUnit("g", "Grams", 0.001, aliases = listOf("gram", "grams")),
    ConvertibleUnit("mg", "Milligrams", 1e-6, aliases = listOf("milligram", "milligrams")),
    ConvertibleUnit("t", "Metric tons", 1000.0, aliases = listOf("tonne", "tonnes")),
    ConvertibleUnit("lb", "Pounds", 0.45359237, aliases = listOf("lbs", "pound", "pounds")),
    ConvertibleUnit("oz", "Ounces", 0.028349523125, aliases = listOf("ounce", "ounces")),
    ConvertibleUnit("st", "Stone", 6.35029318, aliases = listOf("stone", "stones")),
    ConvertibleUnit("ct", "Carats", 0.0002, aliases = listOf("carat", "carats")),
  )),
  TEMPERATURE("Temperature", listOf(
    ConvertibleUnit("°C", "Celsius", 1.0, aliases = listOf("C", "celsius", "degC")),
    ConvertibleUnit("°F", "Fahrenheit", 5.0 / 9.0, -32.0 * 5.0 / 9.0, aliases = listOf("F", "fahrenheit", "degF")),
    ConvertibleUnit("K", "Kelvin", 1.0, -273.15, aliases = listOf("kelvin")),
  )),
  SPEED("Speed", listOf(
    ConvertibleUnit("km/h", "Kilometers per hour", 1 / 3.6, aliases = listOf("kmh", "kph")),
    ConvertibleUnit("mph", "Miles per hour", 0.44704),
    ConvertibleUnit("m/s", "Meters per second", 1.0, aliases = listOf("mps")),
    ConvertibleUnit("ft/s", "Feet per second", 0.3048, aliases = listOf("fps")),
    ConvertibleUnit("kn", "Knots", 1852.0 / 3600.0, aliases = listOf("knot", "knots")),
    ConvertibleUnit("Mach", "Mach (sea level)", 340.29, aliases = listOf("mach")),
  )),
  TIME("Time", listOf(
    ConvertibleUnit("s", "Seconds", 1.0, aliases = listOf("sec", "second", "seconds")),
    ConvertibleUnit("ms", "Milliseconds", 0.001, aliases = listOf("millisecond", "milliseconds")),
    ConvertibleUnit("min", "Minutes", 60.0, aliases = listOf("minute", "minutes")),
    ConvertibleUnit("h", "Hours", 3600.0, aliases = listOf("hr", "hrs", "hour", "hours")),
    ConvertibleUnit("d", "Days", 86400.0, aliases = listOf("day", "days")),
    ConvertibleUnit("wk", "Weeks", 604800.0, aliases = listOf("week", "weeks")),
    ConvertibleUnit("mo", "Months (avg.)", 2629746.0, aliases = listOf("month", "months")),
    ConvertibleUnit("yr", "Years (avg.)", 31556952.0, aliases = listOf("year", "years")),
  )),
  DATA("Data", listOf(
    ConvertibleUnit("B", "Bytes", 1.0, aliases = listOf("byte", "bytes")),
    ConvertibleUnit("bit", "Bits", 0.125, aliases = listOf("bits")),
    ConvertibleUnit("KB", "Kilobytes", 1e3, aliases = listOf("kB")),
    ConvertibleUnit("MB", "Megabytes", 1e6),
    ConvertibleUnit("GB", "Gigabytes", 1e9),
    ConvertibleUnit("TB", "Terabytes", 1e12),
    ConvertibleUnit("KiB", "Kibibytes", 1024.0),
    ConvertibleUnit("MiB", "Mebibytes", 1048576.0),
    ConvertibleUnit("GiB", "Gibibytes", 1073741824.0),
    ConvertibleUnit("TiB", "Tebibytes", 1099511627776.0),
  )),
  PRESSURE("Pressure", listOf(
    ConvertibleUnit("Pa", "Pascals", 1.0),
    ConvertibleUnit("hPa", "Hectopascals", 100.0),
    ConvertibleUnit("kPa", "Kilopascals", 1000.0),
    ConvertibleUnit("bar", "Bar", 1e5),
    ConvertibleUnit("atm", "Atmospheres", 101325.0),
    ConvertibleUnit("psi", "PSI", 6894.757293168),
    ConvertibleUnit("mmHg", "Millimeters of mercury", 133.322387415),
    ConvertibleUnit("inHg", "Inches of mercury", 3386.389),
  )),
  ENERGY("Energy", listOf(
    ConvertibleUnit("J", "Joules", 1.0, aliases = listOf("joule", "joules")),
    ConvertibleUnit("kJ", "Kilojoules", 1000.0),
    ConvertibleUnit("cal", "Calories", 4.184, aliases = listOf("calorie", "calories")),
    ConvertibleUnit("kcal", "Kilocalories", 4184.0, aliases = listOf("Cal")),
    ConvertibleUnit("Wh", "Watt-hours", 3600.0),
    ConvertibleUnit("kWh", "Kilowatt-hours", 3.6e6),
    ConvertibleUnit("BTU", "British thermal units", 1055.05585262),
    ConvertibleUnit("eV", "Electronvolts", 1.602176634e-19),
  )),
  POWER("Power", listOf(
    ConvertibleUnit("W", "Watts", 1.0, aliases = listOf("watt", "watts")),
    ConvertibleUnit("kW", "Kilowatts", 1000.0),
    ConvertibleUnit("MW", "Megawatts", 1e6),
    ConvertibleUnit("hp", "Horsepower", 745.69987158227022),
    ConvertibleUnit("PS", "Metric horsepower", 735.49875),
  )),
  ANGLE("Angle", listOf(
    ConvertibleUnit("°", "Degrees", 1.0, aliases = listOf("deg", "degree", "degrees")),
    ConvertibleUnit("rad", "Radians", 180.0 / Math.PI, aliases = listOf("radian", "radians")),
    ConvertibleUnit("grad", "Gradians", 0.9, aliases = listOf("gon")),
    ConvertibleUnit("′", "Arcminutes", 1.0 / 60.0, aliases = listOf("arcmin")),
    ConvertibleUnit("″", "Arcseconds", 1.0 / 3600.0, aliases = listOf("arcsec")),
    ConvertibleUnit("turn", "Turns", 360.0, aliases = listOf("turns", "rev")),
  )),
  FORCE("Force", listOf(
    ConvertibleUnit("N", "Newtons", 1.0, aliases = listOf("newton", "newtons")),
    ConvertibleUnit("kN", "Kilonewtons", 1000.0),
    ConvertibleUnit("lbf", "Pound-force", 4.4482216152605),
    ConvertibleUnit("kgf", "Kilogram-force", 9.80665),
    ConvertibleUnit("dyn", "Dynes", 1e-5, aliases = listOf("dyne", "dynes")),
  )),
  FREQUENCY("Frequency", listOf(
    ConvertibleUnit("Hz", "Hertz", 1.0, aliases = listOf("hertz")),
    ConvertibleUnit("kHz", "Kilohertz", 1e3),
    ConvertibleUnit("MHz", "Megahertz", 1e6),
    ConvertibleUnit("GHz", "Gigahertz", 1e9),
    ConvertibleUnit("rpm", "Revolutions per minute", 1.0 / 60.0),
    ConvertibleUnit("bpm", "Beats per minute", 1.0 / 60.0),
  )),
  // Units are filled from live rates at runtime (see CurrencyRates); base unit is USD.
  CURRENCY("Currency", emptyList()),
}

object UnitConverter {
  fun convert(value: Double, from: ConvertibleUnit, to: ConvertibleUnit): Double = to.fromBase(from.toBase(value))

  /**
   * Finds a unit by what someone typed — exact symbol first, then aliases, then case-insensitive —
   * searching [currency] too, so Math Notes can read "20 USD in EUR".
   */
  fun find(name: String, currency: List<ConvertibleUnit>): Pair<UnitCategory, ConvertibleUnit>? {
    val n = name.trim()
    if (n.isEmpty()) return null
    val all = UnitCategory.entries.filter { it != UnitCategory.CURRENCY }.flatMap { c -> c.units.map { c to it } } +
      currency.map { UnitCategory.CURRENCY to it }
    return all.firstOrNull { it.second.symbol == n }
      ?: all.firstOrNull { n in it.second.aliases }
      ?: all.firstOrNull { it.second.symbol.equals(n, true) || it.second.aliases.any { a -> a.equals(n, true) } }
  }
}
