@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.calculatorapp.ui.settings

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Build as BuildIcon
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import xyz.pepe.glacierclient.calculatorapp.BuildConfig
import xyz.pepe.glacierclient.calculatorapp.domain.model.AngleUnit
import xyz.pepe.glacierclient.calculatorapp.domain.model.AppSettings
import xyz.pepe.glacierclient.calculatorapp.domain.model.NavLabels
import xyz.pepe.glacierclient.calculatorapp.domain.model.NavStyle
import xyz.pepe.glacierclient.calculatorapp.ui.components.BadgeShape
import xyz.pepe.glacierclient.calculatorapp.ui.components.CheckSwitch
import xyz.pepe.glacierclient.calculatorapp.ui.components.ConnectedChoiceRow
import xyz.pepe.glacierclient.calculatorapp.ui.components.Haptics
import xyz.pepe.glacierclient.calculatorapp.ui.components.PageScaffold
import xyz.pepe.glacierclient.calculatorapp.ui.components.SegmentedGroup
import xyz.pepe.glacierclient.calculatorapp.ui.components.ShapeBadge
import xyz.pepe.glacierclient.calculatorapp.ui.components.rowIconColors
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorIntent
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorUiState
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.InkModelStatus
import xyz.pepe.glacierclient.calculatorapp.ui.notes.allNotesAsText
import xyz.pepe.glacierclient.calculatorapp.ui.notes.exportToNotes

/**
 * Settings as its own full-screen flow: a main page of category rows (A-Z) with a search field on
 * top, each category opening its own sub-page — the same structure and components as Pepe's Clock /
 * Notes / Weather. Rows on every page are sorted A-Z by title. When you add or change a setting,
 * also update [SettingsIndex] so search can find it.
 */
@Composable
fun SettingsHost(state: CalculatorUiState, onIntent: (CalculatorIntent) -> Unit, haptics: Haptics, onEditTools: () -> Unit, onBack: () -> Unit) {
  var page by rememberSaveable { mutableStateOf(SettingsPage.MAIN) }
  var query by rememberSaveable { mutableStateOf("") }
  BackHandler(enabled = page != SettingsPage.MAIN) { page = if (page == SettingsPage.LICENSES) SettingsPage.ABOUT else SettingsPage.MAIN }
  val toMain = { page = SettingsPage.MAIN }
  val update: ((AppSettings) -> AppSettings) -> Unit = { onIntent(CalculatorIntent.UpdateSettings(it)) }

  when (page) {
    SettingsPage.MAIN -> MainPage(state, query, { query = it }, onBack) { page = it }
    SettingsPage.CALCULATION -> CalculationPage(state.settings, update, haptics, toMain)
    SettingsPage.MATH_NOTES -> NotesPage(state, onIntent, update, haptics, toMain)
    SettingsPage.CONVERTER -> ConverterPage(state, onIntent, update, haptics, toMain)
    SettingsPage.APPEARANCE -> AppearancePage(state.settings, update, haptics, toMain)
    SettingsPage.TOOLS -> ToolsPage(state.settings, update, haptics, onEditTools, toMain)
    SettingsPage.DATA -> DataPage(state, onIntent, update, haptics, toMain)
    SettingsPage.ABOUT -> AboutPage(onBack = toMain, onLicenses = { page = SettingsPage.LICENSES })
    SettingsPage.LICENSES -> LicensesPage { page = SettingsPage.ABOUT }
  }
}

@Composable
private fun CategoryRow(index: Int, icon: ImageVector, title: String, summary: String, shapes: androidx.compose.material3.ListItemShapes, onClick: () -> Unit) {
  SegmentedListItem(
    onClick = onClick, shapes = shapes,
    leadingContent = { ShapeBadge(icon, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(index).first, content = rowIconColors(index).second) },
    content = { Text(title) },
    supportingContent = { Text(summary) },
  )
}

/** One switch row in its own rounded group. */
@Composable
private fun ToggleRow(icon: ImageVector, colorIndex: Int, title: String, summary: String, checked: Boolean, onChange: (Boolean) -> Unit, haptics: Haptics) {
  SegmentedGroup(1) { _, shapes ->
    SegmentedListItem(
      onClick = { onChange(!checked) }, shapes = shapes,
      leadingContent = { ShapeBadge(icon, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(colorIndex).first, content = rowIconColors(colorIndex).second) },
      content = { Text(title) },
      supportingContent = { Text(summary) },
      trailingContent = { CheckSwitch(checked, onChange, haptics) },
    )
  }
}

@Composable
private fun MainPage(state: CalculatorUiState, query: String, onQuery: (String) -> Unit, onBack: () -> Unit, onOpen: (SettingsPage) -> Unit) {
  PageScaffold(title = "Settings", subtitle = "Calculation, notes, converter and look", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      item {
        OutlinedTextField(
          value = query, onValueChange = onQuery, singleLine = true,
          placeholder = { Text("Search settings") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
          trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Rounded.Clear, contentDescription = "Clear search") } },
          shape = RoundedCornerShape(28.dp),
          modifier = Modifier.fillMaxWidth(),
        )
      }
      if (query.isNotBlank()) {
        val results = SettingsIndex.search(query)
        if (results.isEmpty()) item {
          Column(Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShapeBadge(Icons.Rounded.Search, shape = BadgeShape.COOKIE, size = 72.dp)
            Text("No settings match", style = MaterialTheme.typography.titleMedium)
            Text("Try another word, like “sound”, “tip” or “history”.", color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        } else item {
          SegmentedGroup(results.size) { i, shapes ->
            val e = results[i]
            SegmentedListItem(
              onClick = { onOpen(e.page) }, shapes = shapes,
              overlineContent = { Text(e.page.label) },
              content = { Text(e.title) },
              supportingContent = { Text(e.description) },
            )
          }
        }
      } else item {
        // A-Z by title.
        SegmentedGroup(7) { i, shapes ->
          when (i) {
            0 -> CategoryRow(3, Icons.Rounded.Info, "About", "Pepe's Calculator " + BuildConfig.VERSION_NAME, shapes) { onOpen(SettingsPage.ABOUT) }
            1 -> CategoryRow(0, Icons.Rounded.PhoneAndroid, "Appearance", "Navigation, colors, sounds and haptics", shapes) { onOpen(SettingsPage.APPEARANCE) }
            2 -> CategoryRow(1, Icons.Rounded.Calculate, "Calculation", "Angle unit, decimal places, fractions, memory", shapes) { onOpen(SettingsPage.CALCULATION) }
            3 -> CategoryRow(2, Icons.Rounded.CurrencyExchange, "Converter", "Exchange rates", shapes) { onOpen(SettingsPage.CONVERTER) }
            4 -> CategoryRow(3, Icons.Rounded.Storage, "Data", "History and exporting math notes", shapes) { onOpen(SettingsPage.DATA) }
            5 -> CategoryRow(0, Icons.Rounded.EditNote, "Math Notes", "Solving and handwriting", shapes) { onOpen(SettingsPage.MATH_NOTES) }
            else -> CategoryRow(1, Icons.Rounded.BuildIcon, "Tools", "Default tax rate, tip and layout", shapes) { onOpen(SettingsPage.TOOLS) }
          }
        }
      }
    }
  }
}

@Composable
private fun CalculationPage(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit, haptics: Haptics, onBack: () -> Unit) {
  PageScaffold(title = "Calculation", subtitle = "Angle unit, decimal places, fractions, memory", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      item {
        Labeled("Angle unit", "How sin, cos, tan and their inverses read angles; also on the keypad's Deg/Rad key") {
          ConnectedChoiceRow(AngleUnit.entries, s.angleUnit, { if (it == AngleUnit.DEG) "Degrees" else "Radians" }, { a -> update { it.copy(angleUnit = a) } }, haptics)
        }
      }
      item {
        Labeled("Decimal places", "Rounding of answers in the calculator, converter and Math Notes; Auto keeps up to 10") {
          ConnectedChoiceRow(listOf(null, 2, 4, 6, 10), s.decimals, { it?.toString() ?: "Auto" }, { d -> update { it.copy(decimals = d) } }, haptics)
        }
      }
      item {
        ToggleRow(Icons.Rounded.Functions, 0, "Digit grouping", if (s.grouping) "1,234,567.89" else "1234567.89", s.grouping, { g -> update { it.copy(grouping = g) } }, haptics)
      }
      item {
        ToggleRow(Icons.Rounded.Percent, 1, "Fraction results", "Also show decimal answers as a fraction, like 0.75 = 3/4", s.showFractions, { v -> update { it.copy(showFractions = v) } }, haptics)
      }
      item {
        ToggleRow(Icons.Rounded.Memory, 2, "Memory keys", "Show the MC, MR, M+ and M− memory keys above the keypad", s.memoryKeys, { v -> update { it.copy(memoryKeys = v) } }, haptics)
      }
    }
  }
}

@Composable
private fun NotesPage(state: CalculatorUiState, onIntent: (CalculatorIntent) -> Unit, update: ((AppSettings) -> AppSettings) -> Unit, haptics: Haptics, onBack: () -> Unit) {
  val s = state.settings
  PageScaffold(title = "Math Notes", subtitle = "Solving and handwriting", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      item {
        SegmentedGroup(2) { i, shapes ->
          if (i == 0) SegmentedListItem(
            onClick = { if (state.inkModelStatus != InkModelStatus.READY) onIntent(CalculatorIntent.PrepareInkModel) }, shapes = shapes,
            leadingContent = { ShapeBadge(Icons.Rounded.Draw, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(1).first, content = rowIconColors(1).second) },
            content = { Text("Handwriting model") },
            supportingContent = {
              Text(when (state.inkModelStatus) {
                InkModelStatus.READY -> "Downloaded — handwriting works offline"
                InkModelStatus.DOWNLOADING -> "Downloading…"
                InkModelStatus.FAILED -> "Download failed. Tap to retry."
                InkModelStatus.IDLE -> "Tap to download now (about 20 MB, once) so handwriting is ready offline"
              })
            },
            trailingContent = { if (state.inkModelStatus == InkModelStatus.DOWNLOADING) LoadingIndicator(Modifier.size(36.dp)) },
          ) else SegmentedListItem(
            onClick = { update { it.copy(solveWithoutEquals = !s.solveWithoutEquals) } }, shapes = shapes,
            leadingContent = { ShapeBadge(Icons.Rounded.Functions, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(0).first, content = rowIconColors(0).second) },
            content = { Text("Solve without “=”") },
            supportingContent = { Text("Show an answer for every line with math, not only lines ending in “=”") },
            trailingContent = { CheckSwitch(s.solveWithoutEquals, { v -> update { it.copy(solveWithoutEquals = v) } }, haptics) },
          )
        }
      }
      item {
        Text(
          "Tips: name a value (rent = 1200) and use it below · “sum =” adds the numbers above it · “5 km in mi =” converts · " +
            "“y = x²” draws a graph · drag a variable's slider to see graphs and answers change.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      }
    }
  }
}

@Composable
private fun ConverterPage(state: CalculatorUiState, onIntent: (CalculatorIntent) -> Unit, update: ((AppSettings) -> AppSettings) -> Unit, haptics: Haptics, onBack: () -> Unit) {
  val s = state.settings
  PageScaffold(title = "Converter", subtitle = "Exchange rates", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      item {
        SegmentedGroup(2) { i, shapes ->
          if (i == 0) SegmentedListItem(
            onClick = { update { it.copy(autoUpdateRates = !s.autoUpdateRates) } }, shapes = shapes,
            leadingContent = { ShapeBadge(Icons.Rounded.CurrencyExchange, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(0).first, content = rowIconColors(0).second) },
            content = { Text("Update exchange rates automatically") },
            supportingContent = { Text("Daily European Central Bank rates, fetched when the app opens, at most every 6 hours") },
            trailingContent = { CheckSwitch(s.autoUpdateRates, { v -> update { it.copy(autoUpdateRates = v) } }, haptics) },
          ) else SegmentedListItem(
            onClick = { haptics.tick(); onIntent(CalculatorIntent.RefreshRates) }, shapes = shapes,
            leadingContent = { ShapeBadge(Icons.Rounded.Refresh, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(1).first, content = rowIconColors(1).second) },
            content = { Text("Update rates now") },
            supportingContent = {
              val r = state.rates
              Text(when {
                state.ratesLoading -> "Updating…"
                state.ratesError -> "Couldn't reach the rates service"
                !r.live -> "Using built-in approximate rates"
                else -> "Last checked " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(r.fetchedAt))
              })
            },
            trailingContent = { if (state.ratesLoading) LoadingIndicator(Modifier.size(36.dp)) },
          )
        }
      }
    }
  }
}

@Composable
private fun AppearancePage(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit, haptics: Haptics, onBack: () -> Unit) {
  PageScaffold(title = "Appearance", subtitle = "Navigation, colors, sounds and haptics", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      item {
        ToggleRow(Icons.Rounded.BlurOn, 2, "Frosted glass", "Translucent floating nav bar; off uses a solid card", s.glassEffects, { v -> update { it.copy(glassEffects = v) } }, haptics)
      }
      item {
        ToggleRow(Icons.Rounded.Vibration, 1, "Haptic feedback", "A light tick on keys, tabs and switches", s.haptics, { v -> update { it.copy(haptics = v) } }, haptics)
      }
      item {
        ToggleRow(Icons.Rounded.VolumeUp, 3, "Key sounds", "Play a click sound on every key press", s.keySounds, { v -> update { it.copy(keySounds = v) } }, haptics)
      }
      item {
        ToggleRow(Icons.Rounded.ZoomIn, 0, "Large display", "Bigger digits in the calculator display", s.largeDisplay, { v -> update { it.copy(largeDisplay = v) } }, haptics)
      }
      item {
        Labeled("Navigation bar", "A floating pill or the classic bar (phones; wide screens use a side rail)") {
          ConnectedChoiceRow(NavStyle.entries, s.navStyle, { if (it == NavStyle.FLOATING) "Floating" else "Classic" }, { n -> update { it.copy(navStyle = n) } }, haptics)
        }
      }
      if (s.navStyle == NavStyle.FLOATING) item {
        Labeled("Navigation labels", "Which tabs of the floating bar show their name") {
          ConnectedChoiceRow(
            NavLabels.entries, s.navLabels,
            { when (it) { NavLabels.SELECTED -> "Selected"; NavLabels.ALL -> "Always"; NavLabels.NONE -> "Never" } },
            { l -> update { it.copy(navLabels = l) } }, haptics,
          )
        }
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) item {
        ToggleRow(Icons.Rounded.Palette, 0, "Wallpaper colors", "Use your wallpaper's colors for the app theme", s.wallpaperColors, { v -> update { it.copy(wallpaperColors = v) } }, haptics)
      }
    }
  }
}

@Composable
private fun ToolsPage(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit, haptics: Haptics, onEditTools: () -> Unit, onBack: () -> Unit) {
  PageScaffold(title = "Tools", subtitle = "Default tax rate, tip and layout", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      item {
        Labeled("Default tax rate", "Starting rate for the Tax (GST / VAT) tool") {
          ConnectedChoiceRow(listOf(5, 10, 12, 18, 20), s.taxPercent, { "$it%" }, { r -> update { it.copy(taxPercent = r) } }, haptics)
        }
      }
      item {
        Labeled("Default tip", "Starting tip percentage for the Tip & split bill tool") {
          ConnectedChoiceRow(listOf(0, 10, 15, 18, 20), s.tipPercent, { "$it%" }, { r -> update { it.copy(tipPercent = r) } }, haptics)
        }
      }
      item {
        SegmentedGroup(2) { i, shapes ->
          when (i) {
            0 -> SegmentedListItem(
              onClick = { haptics.tick(); onEditTools() }, shapes = shapes,
              leadingContent = { ShapeBadge(Icons.Rounded.Edit, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(0).first, content = rowIconColors(0).second) },
              content = { Text("Edit tools layout") },
              supportingContent = { Text("Resize, reorder, hide or pin the tools on the Tools tab") },
            )
            else -> SegmentedListItem(
              onClick = { haptics.confirm(); update { it.copy(toolsLayout = "") } }, shapes = shapes,
              leadingContent = { ShapeBadge(Icons.Rounded.RestartAlt, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(3).first, content = rowIconColors(3).second) },
              content = { Text("Reset tools layout") },
              supportingContent = { Text("Put the Tools tab back to its default order and sizes") },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DataPage(state: CalculatorUiState, onIntent: (CalculatorIntent) -> Unit, update: ((AppSettings) -> AppSettings) -> Unit, haptics: Haptics, onBack: () -> Unit) {
  val context = LocalContext.current
  val s = state.settings
  PageScaffold(title = "Data", subtitle = "History and exporting math notes", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      item {
        SegmentedGroup(3) { i, shapes ->
          when (i) {
            0 -> SegmentedListItem(
              onClick = { haptics.confirm(); onIntent(CalculatorIntent.ClearHistory) }, shapes = shapes,
              leadingContent = { ShapeBadge(Icons.Rounded.DeleteSweep, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(3).first, content = rowIconColors(3).second) },
              content = { Text("Clear history") },
              supportingContent = { Text(if (state.history.isEmpty()) "Nothing saved" else "${state.history.size} calculations saved") },
            )
            1 -> SegmentedListItem(
              onClick = { if (state.notes.isNotEmpty()) exportToNotes(context, allNotesAsText(state)) }, shapes = shapes,
              leadingContent = { ShapeBadge(Icons.Rounded.Share, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(1).first, content = rowIconColors(1).second) },
              content = { Text("Export math notes to Pepe's Notes") },
              supportingContent = { Text(if (state.notes.isEmpty()) "No math notes yet" else "Sends all ${state.notes.size} notes, with answers, as one note") },
            )
            else -> SegmentedListItem(
              onClick = { update { it.copy(keepHistory = !s.keepHistory) } }, shapes = shapes,
              leadingContent = { ShapeBadge(Icons.Rounded.History, shape = BadgeShape.CIRCLE, size = 40.dp, container = rowIconColors(0).first, content = rowIconColors(0).second) },
              content = { Text("Keep calculation history") },
              supportingContent = { Text("Turning this off also clears it") },
              trailingContent = { CheckSwitch(s.keepHistory, { v -> update { it.copy(keepHistory = v) } }, haptics) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AboutPage(onBack: () -> Unit, onLicenses: () -> Unit) {
  var showShortcuts by rememberSaveable { mutableStateOf(false) }
  if (showShortcuts) xyz.pepe.glacierclient.calculatorapp.ui.components.ShortcutsSheet { showShortcuts = false }
  PageScaffold(title = "About", subtitle = "Version, privacy and licenses", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      item {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
          ShapeBadge(Icons.Rounded.Calculate, shape = BadgeShape.COOKIE, size = 112.dp, container = MaterialTheme.colorScheme.primary, content = MaterialTheme.colorScheme.onPrimary)
          Text("Pepe's Calculator", style = MaterialTheme.typography.headlineMediumEmphasized)
          Text("Calculator, converter, tools and Math Notes", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
      }
      item {
        SegmentedGroup(3) { i, shapes ->
          if (i == 0) SegmentedListItem(onClick = onLicenses, shapes = shapes, content = { Text("Open-source licenses") })
          else if (i == 1) SegmentedListItem(onClick = { showShortcuts = true }, shapes = shapes, content = { Text("Keyboard shortcuts") }, supportingContent = { Text("For a physical keyboard or mouse") })
          else SegmentedListItem(onClick = {}, shapes = shapes, content = { Text("Version") }, supportingContent = { Text(BuildConfig.VERSION_NAME) })
        }
      }
      item {
        Text(
          "No accounts, ads or tracking. Your history and notes stay on this device. The app goes online only to fetch " +
            "exchange rates (European Central Bank, via Frankfurter) and, once, to download Google's handwriting model.",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      item { xyz.pepe.glacierclient.ecosystem.PepeAppsSection() }
    }
  }
}

private val licenses = listOf(
  "AndroidX (Compose, Material 3, Lifecycle, DataStore, Activity, Core)" to "Apache License 2.0",
  "Kotlin and kotlinx.coroutines" to "Apache License 2.0",
  "ML Kit Digital Ink Recognition (Google)" to "ML Kit Terms of Service",
  "Exchange rates: European Central Bank via Frankfurter" to "Free to use",
  "Calculator glyph in the app icon: Font Awesome Free (Fonticons, Inc.)" to "CC BY 4.0",
).sortedBy { it.first.lowercase() }

@Composable
private fun LicensesPage(onBack: () -> Unit) {
  PageScaffold(title = "Open-source licenses", subtitle = "Libraries used by this app", onBack = onBack) { padding ->
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
      item {
        SegmentedGroup(licenses.size) { i, shapes ->
          SegmentedListItem(onClick = {}, shapes = shapes, content = { Text(licenses[i].first) }, supportingContent = { Text(licenses[i].second) })
        }
      }
    }
  }
}

@Composable
private fun Labeled(title: String, description: String, content: @Composable () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp))
    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
    content()
  }
}
