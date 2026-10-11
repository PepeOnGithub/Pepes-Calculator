@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.calculatorapp.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SquareFoot
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.util.Date
import xyz.pepe.glacierclient.calculatorapp.data.CurrencyRatesSource
import xyz.pepe.glacierclient.calculatorapp.domain.CalculatorEngine
import xyz.pepe.glacierclient.calculatorapp.domain.ConvertibleUnit
import xyz.pepe.glacierclient.calculatorapp.domain.NumberFormat
import xyz.pepe.glacierclient.calculatorapp.domain.UnitCategory
import xyz.pepe.glacierclient.calculatorapp.domain.UnitConverter
import xyz.pepe.glacierclient.calculatorapp.ui.components.GlassCard
import xyz.pepe.glacierclient.calculatorapp.ui.components.Haptics
import xyz.pepe.glacierclient.calculatorapp.ui.components.SectionHeader
import xyz.pepe.glacierclient.calculatorapp.ui.components.PageScaffold
import xyz.pepe.glacierclient.calculatorapp.ui.components.SectionTitle
import xyz.pepe.glacierclient.calculatorapp.ui.components.SegmentedGroup
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorIntent
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorUiState
import xyz.pepe.glacierclient.calculatorapp.ui.theme.tabularStyle

private val UnitCategory.icon: ImageVector
  get() = when (this) {
    UnitCategory.LENGTH -> Icons.Rounded.Straighten
    UnitCategory.AREA -> Icons.Rounded.SquareFoot
    UnitCategory.VOLUME -> Icons.Rounded.LocalDrink
    UnitCategory.WEIGHT -> Icons.Rounded.FitnessCenter
    UnitCategory.TEMPERATURE -> Icons.Rounded.Thermostat
    UnitCategory.SPEED -> Icons.Rounded.Speed
    UnitCategory.TIME -> Icons.Rounded.AccessTime
    UnitCategory.DATA -> Icons.Rounded.DataUsage
    UnitCategory.PRESSURE -> Icons.Rounded.Compress
    UnitCategory.ENERGY -> Icons.Rounded.Bolt
    UnitCategory.POWER -> Icons.Rounded.ElectricBolt
    UnitCategory.ANGLE -> Icons.Rounded.Architecture
    UnitCategory.FORCE -> Icons.Rounded.Scale
    UnitCategory.FREQUENCY -> Icons.Rounded.Waves
    UnitCategory.CURRENCY -> Icons.Rounded.CurrencyExchange
  }

/** Default "from → to" pair per category — the conversions people reach for first. */
private val DEFAULT_PAIRS = mapOf(
  UnitCategory.LENGTH to ("km" to "mi"), UnitCategory.AREA to ("m²" to "ft²"), UnitCategory.VOLUME to ("L" to "gal"),
  UnitCategory.WEIGHT to ("kg" to "lb"), UnitCategory.TEMPERATURE to ("°C" to "°F"), UnitCategory.SPEED to ("km/h" to "mph"),
  UnitCategory.TIME to ("h" to "min"), UnitCategory.DATA to ("GB" to "MB"), UnitCategory.PRESSURE to ("bar" to "psi"),
  UnitCategory.ENERGY to ("kcal" to "kJ"), UnitCategory.POWER to ("kW" to "hp"), UnitCategory.ANGLE to ("°" to "rad"),
  UnitCategory.FORCE to ("N" to "lbf"), UnitCategory.FREQUENCY to ("Hz" to "kHz"),
  UnitCategory.CURRENCY to ("USD" to "EUR"),
)

/**
 * Unit & currency converter. The amount field takes math too ("12*2.5"), the result updates as
 * you type, and every other unit in the category is listed below so one input answers them all.
 * Currency uses daily ECB rates (cached; shipped rates as the offline fallback).
 */
@Composable
fun ConverterScreen(state: CalculatorUiState, onIntent: (CalculatorIntent) -> Unit, haptics: Haptics, openSettings: () -> Unit) {
  val clipboard = LocalClipboardManager.current
  var category by rememberSaveable { mutableStateOf(UnitCategory.LENGTH) }
  var fromSymbol by rememberSaveable { mutableStateOf("km") }
  var toSymbol by rememberSaveable { mutableStateOf("mi") }
  var input by rememberSaveable { mutableStateOf("1") }
  var picking by rememberSaveable { mutableStateOf<Boolean?>(null) } // true = picking "from", false = "to"
  var swapTurns by remember { mutableFloatStateOf(0f) }
  val swapRotation by animateFloatAsState(swapTurns * 180f, label = "swap")

  val currencyUnits = remember(state.rates, state.favoriteCurrencies) {
    // Starred currencies first (in their usual order), then the rest.
    CurrencyRatesSource.units(state.rates).let { all -> all.filter { it.symbol in state.favoriteCurrencies } + all.filterNot { it.symbol in state.favoriteCurrencies } }
  }
  val units = if (category == UnitCategory.CURRENCY) currencyUnits else category.units
  val from = units.firstOrNull { it.symbol == fromSymbol } ?: units.first()
  val to = units.firstOrNull { it.symbol == toSymbol } ?: units.getOrElse(1) { units.first() }
  val amount = CalculatorEngine.evaluateOrNull(input.ifBlank { "0" }, state.settings.useRadians)
  val style = state.settings.numberStyle
  val result = amount?.let { UnitConverter.convert(it, from, to) }
  val resultText = result?.let { NumberFormat.format(it, style) } ?: "—"

  fun selectCategory(c: UnitCategory) {
    haptics.tick()
    category = c
    DEFAULT_PAIRS[c]?.let { (f, t) -> fromSymbol = f; toSymbol = t }
  }

  PageScaffold(
    title = "Converter",
    actions = { IconButton(onClick = openSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") } },
  ) { padding ->
    LazyColumn(
      Modifier.fillMaxSize().padding(padding),
      contentPadding = PaddingValues(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(UnitCategory.entries) { c ->
            FilterChip(
              selected = c == category,
              onClick = { selectCategory(c) },
              label = { Text(c.label) },
              leadingIcon = { Icon(c.icon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
            )
          }
        }
      }

      item {
        Column(Modifier.padding(horizontal = 16.dp)) {
          UnitCard(label = "From", unit = from, onPickUnit = { picking = true }) {
            BasicTextField(
              value = input,
              onValueChange = { input = it.replace('\n', ' ') },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              textStyle = tabularStyle(MaterialTheme.typography.displaySmall).copy(color = MaterialTheme.colorScheme.onSurface),
              cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
              modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = "Amount to convert" },
            )
            if (amount == null && input.isNotBlank()) {
              Text("Not a number", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }
          }
          Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FilledTonalIconButton(
              onClick = {
                haptics.tick()
                swapTurns += 1f
                val f = fromSymbol
                fromSymbol = to.symbol; toSymbol = f
                result?.let { input = NumberFormat.format(it, style.copy(grouping = false)) }
              },
              modifier = Modifier.padding(vertical = 4.dp).size(48.dp),
            ) { Icon(Icons.Rounded.SwapVert, contentDescription = "Swap units", modifier = Modifier.rotate(swapRotation)) }
          }
          UnitCard(label = "To", unit = to, onPickUnit = { picking = false }, highlighted = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                resultText,
                style = tabularStyle(MaterialTheme.typography.displaySmall),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              IconButton(onClick = {
                haptics.confirm()
                result?.let { clipboard.setText(AnnotatedString(NumberFormat.format(it, style.copy(grouping = false)))) }
              }) { Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy result") }
            }
          }
        }
      }

      if (category == UnitCategory.CURRENCY) item { RatesInfo(state, onIntent) }

      if (amount != null) {
        item { SectionTitle("${NumberFormat.format(amount, style)} ${from.symbol} in every unit", icon = Icons.Rounded.SwapVert) }
        item {
          val others = units.filter { it != from }
          SegmentedGroup(others.size, Modifier.padding(horizontal = 16.dp)) { i, shapes ->
            val u = others[i]
            SegmentedListItem(
              onClick = { haptics.tick(); toSymbol = u.symbol },
              shapes = shapes,
              content = { Text("${NumberFormat.format(UnitConverter.convert(amount, from, u), style)} ${u.symbol}", style = tabularStyle(MaterialTheme.typography.titleMedium)) },
              supportingContent = { Text(u.label) },
            )
          }
        }
      }
    }
  }

  picking?.let { pickingFrom ->
    UnitPickerSheet(
      units = units,
      selected = if (pickingFrom) from else to,
      favorites = if (category == UnitCategory.CURRENCY) state.favoriteCurrencies else null,
      onToggleFavorite = { onIntent(CalculatorIntent.ToggleFavoriteCurrency(it.symbol)) },
      onDismiss = { picking = null },
      onSelect = { u ->
        haptics.tick()
        if (pickingFrom) fromSymbol = u.symbol else toSymbol = u.symbol
        picking = null
      },
    )
  }
}

@Composable
private fun UnitCard(
  label: String,
  unit: ConvertibleUnit,
  onPickUnit: () -> Unit,
  highlighted: Boolean = false,
  content: @Composable () -> Unit,
) {
  GlassCard(Modifier.fillMaxWidth(), contentPadding = 20.dp) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        SectionHeader(label, if (highlighted) Icons.Rounded.SwapVert else Icons.Rounded.Straighten, Modifier.weight(1f))
        Surface(onClick = onPickUnit, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.heightIn(min = 48.dp)) {
          Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp).heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(unit.symbol, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(
              "  " + unit.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
              maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(end = 2.dp).weight(1f, fill = false),
            )
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Change unit", tint = MaterialTheme.colorScheme.onSecondaryContainer)
          }
        }
      }
      content()
    }
  }
}

@Composable
private fun RatesInfo(state: CalculatorUiState, onIntent: (CalculatorIntent) -> Unit) {
  val rates = state.rates
  val text = when {
    state.ratesLoading -> "Updating exchange rates…"
    !rates.live -> "Offline rates — approximate. Connect to update."
    else -> {
      val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(rates.fetchedAt))
      "European Central Bank rates" + (if (rates.date.isNotBlank()) " for ${rates.date}" else "") + " · checked $date" +
        if (state.ratesError) " · couldn't refresh" else ""
    }
  }
  Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
    if (state.ratesLoading) LoadingIndicator(Modifier.size(40.dp))
    else IconButton(onClick = { onIntent(CalculatorIntent.RefreshRates) }) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh rates") }
  }
}

@Composable
private fun UnitPickerSheet(
  units: List<ConvertibleUnit>, selected: ConvertibleUnit, favorites: Set<String>?, onToggleFavorite: (ConvertibleUnit) -> Unit,
  onDismiss: () -> Unit, onSelect: (ConvertibleUnit) -> Unit,
) {
  var query by rememberSaveable { mutableStateOf("") }
  val filtered = units.filter {
    query.isBlank() || it.symbol.contains(query, true) || it.label.contains(query, true) || it.aliases.any { a -> a.contains(query, true) }
  }
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = units.size > 12)) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("Choose a unit", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 8.dp))
      if (units.size > 8) OutlinedTextField(
        value = query, onValueChange = { query = it }, singleLine = true,
        label = { Text("Search") }, modifier = Modifier.fillMaxWidth(),
      )
      LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
          SegmentedGroup(filtered.size) { i, shapes ->
            val u = filtered[i]
            SegmentedListItem(
              onClick = { onSelect(u) },
              shapes = shapes,
              leadingContent = { Text(u.symbol, style = MaterialTheme.typography.titleMedium, fontSize = 18.sp) },
              content = { Text(u.label) },
              trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  if (u == selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                  if (favorites != null) IconButton(onClick = { onToggleFavorite(u) }) {
                    val on = u.symbol in favorites
                    Icon(if (on) Icons.Rounded.Star else Icons.Rounded.StarBorder, contentDescription = if (on) "Remove favourite" else "Add favourite")
                  }
                }
              },
            )
          }
        }
      }
    }
  }
}
