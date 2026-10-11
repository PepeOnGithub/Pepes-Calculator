package xyz.pepe.glacierclient.calculatorapp.ui.settings

/** Every page of the Settings flow. [label] is the page title shown in search results. */
enum class SettingsPage(val label: String) {
  MAIN("Settings"),
  ABOUT("About"),
  APPEARANCE("Appearance"),
  CALCULATION("Calculation"),
  CONVERTER("Converter"),
  DATA("Data"),
  LICENSES("Open-source licenses"),
  MATH_NOTES("Math Notes"),
  TOOLS("Tools"),
}

/** One searchable setting: what it's called, what it does, where it lives and extra words people might type. */
data class SettingEntry(
  val title: String,
  val description: String,
  val page: SettingsPage,
  val keywords: List<String> = emptyList(),
)

/**
 * THE search index for Settings. Every setting in the app must have an entry here: whenever you add,
 * rename or move a setting (or a whole settings page), update this list too, or search won't find it.
 * Keep titles and descriptions in sync with the rows on the page, and keep the list A-Z by title.
 */
object SettingsIndex {
  val entries: List<SettingEntry> = listOf(
    SettingEntry("Angle unit", "How sin, cos, tan and their inverses read angles; also on the keypad's Deg/Rad key", SettingsPage.CALCULATION, listOf("degrees", "radians", "deg", "rad", "trigonometry")),
    SettingEntry("Clear history", "Delete every saved calculation", SettingsPage.DATA, listOf("delete", "erase", "remove", "calculations")),
    SettingEntry("Decimal places", "Rounding of answers in the calculator, converter and Math Notes; Auto keeps up to 10", SettingsPage.CALCULATION, listOf("rounding", "precision", "digits", "round")),
    SettingEntry("Default tax rate", "Starting rate for the Tax (GST / VAT) tool", SettingsPage.TOOLS, listOf("gst", "vat", "sales tax", "percent")),
    SettingEntry("Default tip", "Starting tip percentage for the Tip & split bill tool", SettingsPage.TOOLS, listOf("gratuity", "restaurant", "percent", "split")),
    SettingEntry("Digit grouping", "Thousands separators in numbers, like 1,234,567.89", SettingsPage.CALCULATION, listOf("thousands", "comma", "separator", "format")),
    SettingEntry("Edit tools layout", "Resize, reorder, hide or pin the tools on the Tools tab", SettingsPage.TOOLS, listOf("rearrange", "customize", "tiles", "grid", "pin", "star", "hide", "size", "edit mode")),
    SettingEntry("Export math notes to Pepe's Notes", "Sends all math notes, with answers, as one note", SettingsPage.DATA, listOf("share", "send", "backup", "notes app")),
    SettingEntry("Fraction results", "Also show decimal answers as a fraction, like 0.75 = 3/4", SettingsPage.CALCULATION, listOf("fraction", "rational", "mixed number", "decimal")),
    SettingEntry("Frosted glass", "Translucent floating nav bar; off uses a solid card", SettingsPage.APPEARANCE, listOf("blur", "translucent", "transparent", "navigation bar")),
    SettingEntry("Handwriting model", "Download Google's handwriting model once so handwriting works offline", SettingsPage.MATH_NOTES, listOf("ink", "pen", "draw", "download", "offline", "ml kit")),
    SettingEntry("Haptic feedback", "A light tick on keys, tabs and switches", SettingsPage.APPEARANCE, listOf("vibration", "vibrate", "touch")),
    SettingEntry("Keep calculation history", "Turning this off also clears it", SettingsPage.DATA, listOf("save", "record", "privacy", "past calculations")),
    SettingEntry("Keyboard shortcuts", "Physical keyboard keys for the calculator, history, tabs and settings", SettingsPage.ABOUT, listOf("keys", "hotkeys", "ctrl", "mouse", "bluetooth", "desktop")),
    SettingEntry("Key sounds", "Play a click sound on every key press", SettingsPage.APPEARANCE, listOf("audio", "click", "tap", "sound", "noise")),
    SettingEntry("Large display", "Bigger digits in the calculator display", SettingsPage.APPEARANCE, listOf("big", "text size", "accessibility", "zoom", "numbers")),
    SettingEntry("Memory keys", "Show the MC, MR, M+ and M− memory keys above the keypad", SettingsPage.CALCULATION, listOf("memory", "store", "recall", "register", "m+")),
    SettingEntry("Navigation bar", "A floating pill or the classic bar (phones; wide screens use a side rail)", SettingsPage.APPEARANCE, listOf("tabs", "bottom bar", "floating", "classic", "rail")),
    SettingEntry("Navigation labels", "Which tabs of the floating bar show their name", SettingsPage.APPEARANCE, listOf("tabs", "names", "text", "floating")),
    SettingEntry("Open-source licenses", "Libraries used by this app", SettingsPage.ABOUT, listOf("legal", "credits", "third party", "attribution")),
    SettingEntry("Reset tools layout", "Put the Tools tab back to its default order and sizes", SettingsPage.TOOLS, listOf("default", "restore", "arrange", "tiles", "unhide")),
    SettingEntry("Solve without “=”", "Show an answer for every line with math, not only lines ending in “=”", SettingsPage.MATH_NOTES, listOf("equals", "notes", "answers", "ipad", "inline")),
    SettingEntry("Update exchange rates automatically", "Daily European Central Bank rates, fetched when the app opens, at most every 6 hours", SettingsPage.CONVERTER, listOf("currency", "money", "ecb", "online", "network", "refresh")),
    SettingEntry("Update rates now", "Fetch the latest exchange rates right away", SettingsPage.CONVERTER, listOf("currency", "refresh", "reload", "sync", "money")),
    SettingEntry("Version", "The installed version of Pepe's Calculator", SettingsPage.ABOUT, listOf("build", "release", "app info")),
    SettingEntry("Wallpaper colors", "Use your wallpaper's colors for the app theme", SettingsPage.APPEARANCE, listOf("theme", "dynamic", "material you", "color", "colour")),
  ).sortedBy { it.title.lowercase() }

  /**
   * Case-insensitive search. The query is split into words; an entry matches when every word occurs
   * (as a word prefix or anywhere inside a word) in its title, description, page name or keywords.
   * Title matches rank first. A blank query returns nothing.
   */
  fun search(query: String, source: List<SettingEntry> = entries): List<SettingEntry> {
    val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return emptyList()
    return source.mapNotNull { e ->
      val title = e.title.lowercase()
      val haystack = (listOf(title, e.description.lowercase(), e.page.label.lowercase()) + e.keywords.map { it.lowercase() })
      var score = 0
      for (w in words) {
        score += when {
          title.startsWith(w) -> 0
          title.split(Regex("[^\\p{L}\\p{N}]+")).any { it.startsWith(w) } -> 1
          title.contains(w) -> 2
          haystack.any { h -> h.split(Regex("[^\\p{L}\\p{N}]+")).any { it.startsWith(w) } } -> 3
          haystack.any { it.contains(w) } -> 4
          else -> return@mapNotNull null
        }
      }
      score to e
    }.sortedWith(compareBy({ it.first }, { it.second.title.lowercase() })).map { it.second }
  }
}
