package xyz.pepe.glacierclient.calculatorapp.data

import java.net.HttpURLConnection
import java.net.URL
import java.util.Currency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import xyz.pepe.glacierclient.calculatorapp.domain.ConvertibleUnit
import xyz.pepe.glacierclient.calculatorapp.domain.model.CurrencyRates

/**
 * Daily reference rates from the European Central Bank, served by Frankfurter (free, no key, no
 * tracking). Only used by the converter's Currency category and Math Notes' "USD to EUR".
 */
class CurrencyRatesSource {

  suspend fun fetch(): CurrencyRates? = withContext(Dispatchers.IO) {
    for (url in ENDPOINTS) {
      runCatching {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
          connectTimeout = 8000
          readTimeout = 8000
          setRequestProperty("Accept", "application/json")
        }
        try {
          if (c.responseCode != 200) return@runCatching null
          val o = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
          val r = o.getJSONObject("rates")
          val map = r.keys().asSequence().associateWith { r.getDouble(it) } + ("USD" to 1.0)
          CurrencyRates(map, o.optString("date"), System.currentTimeMillis(), live = true)
        } finally {
          c.disconnect()
        }
      }.getOrNull()?.let { return@withContext it }
    }
    null
  }

  companion object {
    private val ENDPOINTS = listOf(
      "https://api.frankfurter.dev/v1/latest?base=USD",
      "https://api.frankfurter.app/latest?from=USD",
    )

    /** Shipped snapshot so currency works offline before the first fetch. Approximate. */
    val FALLBACK = CurrencyRates(
      mapOf(
        "USD" to 1.0, "EUR" to 0.92, "GBP" to 0.79, "JPY" to 149.5, "INR" to 83.3, "CAD" to 1.36,
        "AUD" to 1.52, "CNY" to 7.24, "CHF" to 0.88, "MXN" to 17.1, "BRL" to 4.95, "KRW" to 1330.0,
        "SEK" to 10.6, "NOK" to 10.7, "DKK" to 6.87, "PLN" to 4.0, "NZD" to 1.65, "SGD" to 1.35,
        "HKD" to 7.82, "ZAR" to 18.6, "TRY" to 32.0, "ILS" to 3.7, "THB" to 35.8, "IDR" to 15600.0,
      ),
      date = "", fetchedAt = 0L, live = false,
    )

    private val POPULAR = listOf("USD", "EUR", "GBP", "JPY", "INR", "CAD", "AUD", "CNY", "CHF")

    /** Rates as converter units: popular currencies first, then alphabetical. */
    fun units(rates: CurrencyRates): List<ConvertibleUnit> =
      rates.perUsd.entries
        .sortedWith(compareBy({ POPULAR.indexOf(it.key).let { i -> if (i < 0) Int.MAX_VALUE else i } }, { it.key }))
        .map { (code, perUsd) ->
          val name = runCatching { Currency.getInstance(code).displayName }.getOrDefault(code)
          ConvertibleUnit(code, name, 1.0 / perUsd, aliases = listOfNotNull(symbolFor(code)))
        }

    private fun symbolFor(code: String) = when (code) {
      "USD" -> "$"; "EUR" -> "€"; "GBP" -> "£"; "JPY" -> "¥"; "INR" -> "₹"; "KRW" -> "₩"; else -> null
    }
  }
}
