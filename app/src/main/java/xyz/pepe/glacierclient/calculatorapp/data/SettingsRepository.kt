package xyz.pepe.glacierclient.calculatorapp.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import xyz.pepe.glacierclient.calculatorapp.domain.model.AngleUnit
import xyz.pepe.glacierclient.calculatorapp.domain.model.AppSettings
import xyz.pepe.glacierclient.calculatorapp.domain.model.CurrencyRates
import xyz.pepe.glacierclient.calculatorapp.domain.model.HistoryEntry
import xyz.pepe.glacierclient.calculatorapp.domain.model.MathNote
import xyz.pepe.glacierclient.calculatorapp.domain.model.NavLabels
import xyz.pepe.glacierclient.calculatorapp.domain.model.NavStyle

/** Pepe's Notes' application id — the default share target for "Export to Notes". */
const val NOTES_APP_PACKAGE = "xyz.pepe.glacierclient.notesapp"

private val HAPTICS = booleanPreferencesKey("haptics_enabled")
private val RADIANS = booleanPreferencesKey("use_radians")
private val KEEP_HISTORY = booleanPreferencesKey("keep_history")
private val WALLPAPER_COLORS = booleanPreferencesKey("wallpaper_colors_enabled")
private val DECIMALS = intPreferencesKey("decimals") // -1 = auto
private val GROUPING = booleanPreferencesKey("grouping")
private val SOLVE_ALL = booleanPreferencesKey("solve_without_equals")
private val AUTO_RATES = booleanPreferencesKey("auto_update_rates")
private val NAV_STYLE = stringPreferencesKey("nav_style")
private val NAV_LABELS = stringPreferencesKey("nav_labels")
private val GLASS = booleanPreferencesKey("glass_effects")
private val SHOW_FRACTIONS = booleanPreferencesKey("show_fractions")
private val MEMORY_KEYS = booleanPreferencesKey("memory_keys")
private val KEY_SOUNDS = booleanPreferencesKey("key_sounds")
private val LARGE_DISPLAY = booleanPreferencesKey("large_display")
private val TIP_PERCENT = intPreferencesKey("tip_percent")
private val TAX_PERCENT = intPreferencesKey("tax_percent")
private val TOOLS_LAYOUT = stringPreferencesKey("tools_layout")
private val MEMORY = stringPreferencesKey("memory_value")
private val FAV_CURRENCIES = stringPreferencesKey("favorite_currencies")
private val HISTORY =stringPreferencesKey("history_json")
private val NOTES = stringPreferencesKey("math_notes_json")
private val RATES = stringPreferencesKey("currency_rates_json")
private val RATES_DATE = stringPreferencesKey("currency_rates_date")
private val RATES_AT = longPreferencesKey("currency_rates_at")

/** Everything the app keeps: settings, calculation history, Math Notes and cached exchange rates. */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

  val settings: Flow<AppSettings> = dataStore.data.map { currentFrom(it) }.distinctUntilChanged()

  suspend fun update(transform: (AppSettings) -> AppSettings) {
    dataStore.edit { p ->
      val s = transform(currentFrom(p))
      p[RADIANS] = s.angleUnit == AngleUnit.RAD
      p[DECIMALS] = s.decimals ?: -1
      p[GROUPING] = s.grouping
      p[SOLVE_ALL] = s.solveWithoutEquals
      p[AUTO_RATES] = s.autoUpdateRates
      p[WALLPAPER_COLORS] = s.wallpaperColors
      p[HAPTICS] = s.haptics
      p[NAV_STYLE] = s.navStyle.name
      p[NAV_LABELS] = s.navLabels.name
      p[GLASS] = s.glassEffects
      p[KEEP_HISTORY] = s.keepHistory
      p[SHOW_FRACTIONS] = s.showFractions
      p[MEMORY_KEYS] = s.memoryKeys
      p[KEY_SOUNDS] = s.keySounds
      p[LARGE_DISPLAY] = s.largeDisplay
      p[TIP_PERCENT] = s.tipPercent
      p[TAX_PERCENT] = s.taxPercent
      p[TOOLS_LAYOUT] = s.toolsLayout
    }
  }

  private fun currentFrom(p: Preferences) = AppSettings(
    angleUnit = if (p[RADIANS] == true) AngleUnit.RAD else AngleUnit.DEG,
    decimals = (p[DECIMALS] ?: -1).takeIf { it >= 0 },
    grouping = p[GROUPING] ?: true,
    solveWithoutEquals = p[SOLVE_ALL] ?: false,
    autoUpdateRates = p[AUTO_RATES] ?: true,
    wallpaperColors = p[WALLPAPER_COLORS] ?: false,
    haptics = p[HAPTICS] ?: true,
    navStyle = p[NAV_STYLE]?.let { runCatching { NavStyle.valueOf(it) }.getOrNull() } ?: NavStyle.CLASSIC,
    navLabels = p[NAV_LABELS]?.let { runCatching { NavLabels.valueOf(it) }.getOrNull() } ?: NavLabels.SELECTED,
    glassEffects = p[GLASS] ?: true,
    keepHistory = p[KEEP_HISTORY] ?: true,
    showFractions = p[SHOW_FRACTIONS] ?: false,
    memoryKeys = p[MEMORY_KEYS] ?: true,
    keySounds = p[KEY_SOUNDS] ?: false,
    largeDisplay = p[LARGE_DISPLAY] ?: false,
    tipPercent = p[TIP_PERCENT] ?: 15,
    taxPercent = p[TAX_PERCENT] ?: 10,
    toolsLayout = p[TOOLS_LAYOUT] ?: "",
  )

  // History -----------------------------------------------------------------------------------

  val history: Flow<List<HistoryEntry>> = dataStore.data.map { p ->
    runCatching {
      val a = JSONArray(p[HISTORY] ?: "[]")
      List(a.length()) { i -> a.getJSONObject(i).let { HistoryEntry(it.getString("e"), it.getString("r"), it.optLong("t"), it.optBoolean("p", false)) } }
    }.getOrDefault(emptyList())
  }.distinctUntilChanged()

  suspend fun setHistory(entries: List<HistoryEntry>) = dataStore.edit { p ->
    p[HISTORY] = JSONArray().apply {
      entries.forEach { put(JSONObject().put("e", it.expression).put("r", it.result).put("t", it.at).put("p", it.pinned)) }
    }.toString()
  }

  // Memory register and favourite currencies --------------------------------------------------

  val memory: Flow<Double?> = dataStore.data.map { p -> p[MEMORY]?.toDoubleOrNull() }.distinctUntilChanged()

  suspend fun setMemory(value: Double?) = dataStore.edit { p ->
    if (value == null) p.remove(MEMORY) else p[MEMORY] = value.toString()
  }

  val favoriteCurrencies: Flow<Set<String>> = dataStore.data.map { p ->
    (p[FAV_CURRENCIES] ?: "").split(',').filter { it.isNotBlank() }.toSet()
  }.distinctUntilChanged()

  suspend fun setFavoriteCurrencies(codes: Set<String>) = dataStore.edit { p -> p[FAV_CURRENCIES] = codes.joinToString(",") }

  // Math Notes --------------------------------------------------------------------------------

  val notes: Flow<List<MathNote>> = dataStore.data.map { p ->
    val json = p[NOTES] ?: return@map emptyList()
    runCatching {
      val a = JSONArray(json)
      List(a.length()) { i -> a.getJSONObject(i).let { MathNote(it.getLong("id"), it.getString("body"), it.getLong("at")) } }
    }.getOrDefault(emptyList())
  }.distinctUntilChanged()

  suspend fun setNotes(notes: List<MathNote>) = dataStore.edit { p ->
    p[NOTES] = JSONArray().apply {
      notes.forEach { put(JSONObject().put("id", it.id).put("body", it.body).put("at", it.updatedAt)) }
    }.toString()
  }

  // Currency ----------------------------------------------------------------------------------

  val rates: Flow<CurrencyRates?> = dataStore.data.map { p ->
    val json = p[RATES] ?: return@map null
    runCatching {
      val o = JSONObject(json)
      CurrencyRates(o.keys().asSequence().associateWith { o.getDouble(it) }, p[RATES_DATE] ?: "", p[RATES_AT] ?: 0L, live = true)
    }.getOrNull()
  }.distinctUntilChanged()

  suspend fun setRates(r: CurrencyRates) = dataStore.edit { p ->
    p[RATES] = JSONObject().apply { r.perUsd.forEach { (k, v) -> put(k, v) } }.toString()
    p[RATES_DATE] = r.date
    p[RATES_AT] = r.fetchedAt
  }
}
