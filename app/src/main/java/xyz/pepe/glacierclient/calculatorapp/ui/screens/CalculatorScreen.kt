@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)

package xyz.pepe.glacierclient.calculatorapp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.graphics.graphicsLayer
import xyz.pepe.glacierclient.calculatorapp.ui.components.handCursor
import xyz.pepe.glacierclient.calculatorapp.ui.components.onSecondaryClick
import xyz.pepe.glacierclient.calculatorapp.ui.components.onWheel
import xyz.pepe.glacierclient.calculatorapp.ui.components.rememberReducedMotion
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.Intent
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlin.math.roundToInt
import xyz.pepe.glacierclient.ecosystem.PepeEcosystem
import java.text.DateFormat
import java.util.Date
import xyz.pepe.glacierclient.calculatorapp.domain.NumberFormat
import xyz.pepe.glacierclient.calculatorapp.domain.model.AngleUnit
import xyz.pepe.glacierclient.calculatorapp.domain.model.HistoryEntry
import xyz.pepe.glacierclient.calculatorapp.ui.components.GlassBorder
import xyz.pepe.glacierclient.calculatorapp.ui.components.GlassCard
import xyz.pepe.glacierclient.calculatorapp.ui.components.Haptics
import xyz.pepe.glacierclient.calculatorapp.ui.components.LocalGlassEnabled
import xyz.pepe.glacierclient.calculatorapp.ui.components.SectionHeader
import xyz.pepe.glacierclient.calculatorapp.ui.components.ShapeBadge
import xyz.pepe.glacierclient.calculatorapp.ui.components.glass
import xyz.pepe.glacierclient.calculatorapp.ui.components.PageScaffold
import xyz.pepe.glacierclient.calculatorapp.ui.components.SegmentedGroup
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorIntent
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorMode
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorUiState
import xyz.pepe.glacierclient.calculatorapp.ui.theme.tabularStyle

@Composable
fun CalculatorScreen(
  state: CalculatorUiState, onIntent: (CalculatorIntent) -> Unit, haptics: Haptics, openSettings: () -> Unit,
  showHistory: Boolean, setShowHistory: (Boolean) -> Unit,
) {
  PageScaffold(
    title = "Calculator",
    large = false,
    actions = {
      IconToggleButton(
        checked = state.mode == CalculatorMode.SCIENTIFIC,
        onCheckedChange = { haptics.toggle(it); onIntent(CalculatorIntent.ToggleMode) },
      ) { Icon(Icons.Rounded.Science, contentDescription = "Scientific") }
      IconButton(onClick = { setShowHistory(true) }) { Icon(Icons.Rounded.History, contentDescription = "History") }
      IconButton(onClick = openSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
    },
  ) { padding ->
    BoxWithConstraints(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
      val view = LocalView.current
      val press: (Key) -> Unit = { key ->
        haptics.tick()
        if (state.settings.keySounds) view.playSoundEffect(SoundEffectConstants.CLICK)
        onIntent(key.intent(state))
      }
      val swipeDown = Modifier.pointerInput(Unit) {
        var total = 0f
        detectVerticalDragGestures(onDragStart = { total = 0f }, onDragEnd = { if (total > 140f) { haptics.tick(); setShowHistory(true) } }) { _, dy -> total += dy }
      }
      val memRow = if (state.settings.memoryKeys) listOf(memoryRow(state)) else emptyList()
      if (maxWidth > maxHeight) {
        // Landscape: scientific keys always shown, beside the basic pad (like iOS).
        Column(swipeDown, verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Display(state, onIntent, haptics, Modifier.weight(0.34f))
          if (memRow.isNotEmpty()) KeyGrid(memRow, press, Modifier.fillMaxWidth(), rowHeight = 40)
          Row(Modifier.weight(0.66f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyGrid(scientificRows(state), press, Modifier.weight(1.5f).fillMaxSize())
            KeyGrid(basicRows(state), press, Modifier.weight(1f).fillMaxSize())
          }
        }
      } else {
        Column(swipeDown, verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Display(state, onIntent, haptics, Modifier.weight(1f))
          if (memRow.isNotEmpty()) KeyGrid(memRow, press, Modifier.fillMaxWidth(), rowHeight = 48)
          AnimatedVisibility(state.mode == CalculatorMode.SCIENTIFIC) {
            KeyGrid(scientificRows(state), press, Modifier.fillMaxWidth().padding(bottom = 4.dp), rowHeight = 48)
          }
          KeyGrid(basicRows(state), press, Modifier.weight(if (state.mode == CalculatorMode.SCIENTIFIC) 1.35f else 1.6f).fillMaxWidth())
        }
      }
    }
  }
  if (showHistory) HistorySheet(state, onIntent, haptics) { setShowHistory(false) }
}

// Keys ----------------------------------------------------------------------------------------

private enum class KeyStyle { NUMBER, FUNCTION, OPERATOR, EQUALS, SCIENTIFIC, TOGGLED }

private data class Key(
  val label: String,
  val style: KeyStyle,
  val token: String? = null,
  val icon: ImageVector? = null,
  val description: String = label,
  val action: ((CalculatorUiState) -> CalculatorIntent)? = null,
) {
  fun intent(state: CalculatorUiState): CalculatorIntent = action?.invoke(state) ?: CalculatorIntent.Input(token ?: label)
}

private fun digit(d: String) = Key(d, KeyStyle.NUMBER)
private fun op(label: String, token: String, description: String) = Key(label, KeyStyle.OPERATOR, token, description = description)

private fun basicRows(state: CalculatorUiState) = listOf(
  listOf(
    Key(if (state.expression.isEmpty() || state.justEvaluated) "AC" else "C", KeyStyle.FUNCTION, description = "Clear", action = { CalculatorIntent.ClearAll }),
    Key("⌫", KeyStyle.FUNCTION, icon = Icons.AutoMirrored.Rounded.Backspace, description = "Backspace", action = { CalculatorIntent.Backspace }),
    Key("%", KeyStyle.FUNCTION, "%", description = "Percent"),
    op("÷", "/", "Divide"),
  ),
  listOf(digit("7"), digit("8"), digit("9"), op("×", "*", "Multiply")),
  listOf(digit("4"), digit("5"), digit("6"), op("−", "-", "Minus")),
  listOf(digit("1"), digit("2"), digit("3"), op("+", "+", "Plus")),
  listOf(
    Key("±", KeyStyle.NUMBER, description = "Change sign", action = { CalculatorIntent.ToggleSign }),
    digit("0"),
    Key(".", KeyStyle.NUMBER, description = "Decimal point"),
    Key("=", KeyStyle.EQUALS, description = "Equals", action = { CalculatorIntent.Evaluate }),
  ),
)

private fun memoryRow(state: CalculatorUiState) = listOf(
  Key("MC", KeyStyle.SCIENTIFIC, description = "Clear memory", action = { CalculatorIntent.MemoryClear }),
  Key("MR", if (state.memory != null) KeyStyle.TOGGLED else KeyStyle.SCIENTIFIC, description = "Recall memory", action = { CalculatorIntent.MemoryRecall }),
  Key("M+", KeyStyle.SCIENTIFIC, description = "Add to memory", action = { CalculatorIntent.MemoryAdd }),
  Key("M−", KeyStyle.SCIENTIFIC, description = "Subtract from memory", action = { CalculatorIntent.MemorySubtract }),
)

private fun scientificRows(state: CalculatorUiState): List<List<Key>> {
  val second = state.second
  fun sci(label: String, token: String, alt: Pair<String, String>? = null, description: String = label) =
    if (second && alt != null) Key(alt.first, KeyStyle.SCIENTIFIC, alt.second, description = alt.first)
    else Key(label, KeyStyle.SCIENTIFIC, token, description = description)
  return listOf(
    listOf(
      Key("2nd", if (second) KeyStyle.TOGGLED else KeyStyle.SCIENTIFIC, description = "Second functions", action = { CalculatorIntent.ToggleSecond }),
      Key(if (state.settings.angleUnit == AngleUnit.DEG) "Deg" else "Rad", KeyStyle.SCIENTIFIC, description = "Switch angle unit", action = { CalculatorIntent.ToggleAngle }),
      sci("(", "("), sci(")", ")"),
      sci("x²", "^2", "x³" to "^3", "Square"),
      sci("xʸ", "^", "ʸ√x" to "^(1/", "Power"),
    ),
    listOf(
      sci("sin", "sin(", "sin⁻¹" to "asin("), sci("cos", "cos(", "cos⁻¹" to "acos("), sci("tan", "tan(", "tan⁻¹" to "atan("),
      sci("ln", "ln(", "eˣ" to "e^("), sci("log", "log(", "10ˣ" to "10^("), sci("√", "√(", "∛" to "cbrt(", "Square root"),
    ),
    listOf(
      sci("π", "π", description = "Pi"), sci("e", "e"), sci("x!", "!", description = "Factorial"),
      sci("1/x", "^(-1)", description = "Reciprocal"), sci("|x|", "abs(", description = "Absolute value"),
      sci("Ans", "Ans", description = "Last answer"),
    ),
    listOf(
      sci("sinh", "sinh("), sci("cosh", "cosh("), sci("tanh", "tanh("),
      sci("log₂", "log2(", "2ˣ" to "2^(", "Log base 2"),
      sci("nCr", "nCr", description = "Combinations"), sci("nPr", "nPr", "mod" to "mod", "Permutations"),
    ),
  )
}

@Composable
private fun KeyGrid(rows: List<List<Key>>, onPress: (Key) -> Unit, modifier: Modifier = Modifier, rowHeight: Int? = null) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    for (row in rows) {
      val rowModifier = if (rowHeight != null) Modifier.fillMaxWidth().height(rowHeight.dp) else Modifier.fillMaxWidth().weight(1f)
      Row(rowModifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (key in row) CalcKey(key, Modifier.weight(1f).fillMaxSize()) { onPress(key) }
      }
    }
  }
}

/** A key that squishes from a pill into a rounded square while pressed — the expressive look. */
@Composable
private fun CalcKey(key: Key, modifier: Modifier, onClick: () -> Unit) {
  val cs = MaterialTheme.colorScheme
  val (container, content) = when (key.style) {
    KeyStyle.NUMBER -> cs.surfaceContainerHigh to cs.onSurface
    KeyStyle.FUNCTION -> cs.surfaceContainerHighest to cs.primary
    KeyStyle.OPERATOR -> cs.primaryContainer to cs.onPrimaryContainer
    KeyStyle.EQUALS -> cs.primary to cs.onPrimary
    KeyStyle.SCIENTIFIC -> cs.surfaceContainer to cs.onSurfaceVariant
    KeyStyle.TOGGLED -> cs.secondaryContainer to cs.onSecondaryContainer
  }
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val focused by interaction.collectIsFocusedAsState()
  val reduced = rememberReducedMotion()
  val corner by animateDpAsState(if (pressed) 16.dp else 40.dp, animationSpec = if (reduced) snap() else spring(stiffness = 700f), label = "keyCorner")
  val scale by animateFloatAsState(if (pressed && !reduced) 0.94f else 1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f), label = "keyScale")
  Box(
    modifier
      .graphicsLayer { scaleX = scale; scaleY = scale }
      .handCursor()
      .clip(RoundedCornerShape(corner))
      .background(container)
      .then(if (focused) Modifier.border(2.dp, cs.primary, RoundedCornerShape(corner)) else Modifier)
      .combinedClickable(interactionSource = interaction, indication = ripple(), onClick = onClick)
      .semantics { contentDescription = key.description }
      .testTag("calc_btn_${key.label}"),
    contentAlignment = Alignment.Center,
  ) {
    if (key.icon != null) Icon(key.icon, contentDescription = null, tint = content)
    else Text(
      key.label,
      color = content,
      fontSize = if (key.style == KeyStyle.SCIENTIFIC || key.style == KeyStyle.TOGGLED) 16.sp else 28.sp,
      fontWeight = if (key.style == KeyStyle.NUMBER) FontWeight.Normal else FontWeight.Medium,
      maxLines = 1,
    )
  }
}

// Display -------------------------------------------------------------------------------------

/** Shows an ASCII expression with calculator glyphs and (optionally) digit grouping. */
fun prettyExpression(expr: String, grouping: Boolean): String {
  val glyphs = expr
    .replace("asin(", "sin⁻¹(").replace("acos(", "cos⁻¹(").replace("atan(", "tan⁻¹(")
    .replace("cbrt(", "∛(").replace("sqrt(", "√(")
    .replace('*', '×').replace('/', '÷').replace('-', '−')
  return if (grouping) NumberFormat.group(glyphs) else glyphs
}

@Composable
private fun Display(state: CalculatorUiState, onIntent: (CalculatorIntent) -> Unit, haptics: Haptics, modifier: Modifier) {
  val clipboard = LocalClipboardManager.current
  val context = LocalContext.current
  var menu by remember { mutableStateOf(false) }
  var drag by remember { mutableFloatStateOf(0f) }
  val grouping = state.settings.grouping
  // A finished result honours "Decimal places"; what is being typed is shown exactly as entered.
  val main = when {
    state.expression.isEmpty() -> "0"
    state.justEvaluated && state.error == null -> NumberFormat.reformat(state.expression, state.settings.numberStyle).replace('-', '−')
    else -> prettyExpression(state.expression, grouping)
  }
  val mainColor = if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

  val glass = LocalGlassEnabled.current
  val displayShape = RoundedCornerShape(28.dp)
  Box(
    modifier
      .fillMaxWidth()
      .then(
        if (glass) Modifier.glass(displayShape).border(GlassBorder, displayShape)
        else Modifier.clip(displayShape).background(MaterialTheme.colorScheme.surfaceContainerLow),
      )
      .onSecondaryClick { haptics.confirm(); menu = true }
      .onWheel { onIntent(CalculatorIntent.UpdateSettings { it.copy(largeDisplay = !it.largeDisplay) }) }
      .combinedClickable(onClick = {}, onLongClick = { haptics.confirm(); menu = true })
      // Swipe sideways across the number to delete the last digit, as on iPhone.
      .pointerInput(Unit) {
        detectHorizontalDragGestures(onDragEnd = { drag = 0f }) { _, amount ->
          drag += amount
          if (kotlin.math.abs(drag) > 80f) { drag = 0f; haptics.tick(); onIntent(CalculatorIntent.Backspace) }
        }
      }
      .padding(horizontal = 18.dp, vertical = 12.dp),
  ) {
    Row(Modifier.align(Alignment.TopStart), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      if (state.mode == CalculatorMode.SCIENTIFIC || state.settings.angleUnit == AngleUnit.RAD) Indicator(if (state.settings.useRadians) "RAD" else "DEG")
      if (state.second) Indicator("2nd")
      if (state.memory != null && state.settings.memoryKeys) Indicator("M")
    }
    Column(Modifier.align(Alignment.BottomEnd).fillMaxWidth(), horizontalAlignment = Alignment.End) {
      state.previousExpression?.let {
        Text(
          prettyExpression(it, grouping) + " =",
          style = tabularStyle(MaterialTheme.typography.titleLarge),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1, overflow = TextOverflow.StartEllipsis,
        )
      }
      val reduced = rememberReducedMotion()
      AnimatedContent(
        targetState = main,
        transitionSpec = {
          if (state.justEvaluated && !reduced) (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 4 }) togetherWith fadeOut(tween(120))
          else EnterTransition.None togetherWith ExitTransition.None
        },
        contentAlignment = Alignment.BottomEnd,
        label = "displayText",
      ) { shown ->
        BasicText(
          text = shown,
          modifier = Modifier.fillMaxWidth().testTag("calculator_display"),
          style = tabularStyle(MaterialTheme.typography.displayLarge).copy(color = mainColor, textAlign = TextAlign.End, fontWeight = FontWeight.Light),
          maxLines = 2,
          overflow = TextOverflow.StartEllipsis,
          autoSize = if (state.settings.largeDisplay) TextAutoSize.StepBased(minFontSize = 44.sp, maxFontSize = 112.sp, stepSize = 2.sp)
          else TextAutoSize.StepBased(minFontSize = 30.sp, maxFontSize = 76.sp, stepSize = 2.sp),
        )
      }
      val secondary = state.error ?: listOfNotNull(state.preview, state.fraction).joinToString("  =  ").ifEmpty { null }
      Text(
        secondary ?: " ",
        style = tabularStyle(MaterialTheme.typography.headlineSmall),
        color = if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1, overflow = TextOverflow.StartEllipsis,
      )
    }
    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
      DropdownMenuItem(text = { Text("Copy") }, onClick = {
        val value = state.preview ?: main
        clipboard.setText(AnnotatedString(value.replace(",", "").replace('−', '-'))); menu = false
      })
      DropdownMenuItem(text = { Text("Paste") }, onClick = {
        clipboard.getText()?.text?.let { onIntent(CalculatorIntent.Paste(it)) }; menu = false
      })
      val plain = (state.preview ?: main).replace(",", "").replace('−', '-')
      val shareBody = (state.previousExpression?.let { prettyExpression(it, false) + " = " } ?: "") + plain
      DropdownMenuItem(text = { Text("Share result") }, onClick = { menu = false; shareText(context, shareBody) })
      DropdownMenuItem(text = { Text("Send to Pepe's Notes") }, onClick = {
        menu = false
        if (!PepeEcosystem.send(context, PepeEcosystem.CreateNote(shareBody, "Calculation"))) shareText(context, shareBody)
      })
      // Treat the answer as a number of minutes and hand it to Pepe's Clock as a countdown.
      val minutes = plain.toDoubleOrNull()?.takeIf { it > 0 && it <= 1440 }
      if (minutes != null) DropdownMenuItem(text = { Text("Set a timer for $plain min") }, onClick = {
        menu = false
        val ok = PepeEcosystem.send(context, PepeEcosystem.StartTimer((minutes * 60).roundToInt().coerceAtLeast(1), "Calculator"))
        if (!ok) Toast.makeText(context, "Pepe's Clock isn't installed", Toast.LENGTH_SHORT).show()
      })
    }
  }
}

private fun shareText(context: Context, text: String) {
  val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
  runCatching { context.startActivity(Intent.createChooser(send, "Share result").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
private fun Indicator(text: String) {
  Text(
    text,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSecondaryContainer,
    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 8.dp, vertical = 2.dp),
  )
}

// History -------------------------------------------------------------------------------------

@Composable
private fun HistorySheet(state: CalculatorUiState, onIntent: (CalculatorIntent) -> Unit, haptics: Haptics, onDismiss: () -> Unit) {
  val clipboard = LocalClipboardManager.current
  var query by rememberSaveable { mutableStateOf("") }
  // Pinned calculations first, then the newest; filtered by the search text.
  val history = remember(state.history, query) {
    state.history.filter { query.isBlank() || it.expression.contains(query, true) || it.result.contains(query, true) }
      .sortedByDescending { it.pinned }
  }
  val snackbar = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Box {
    Column {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
      SectionHeader("History", Icons.Rounded.History, Modifier.weight(1f))
      if (history.isNotEmpty()) TextButton(onClick = { haptics.confirm(); onIntent(CalculatorIntent.ClearHistory) }) { Text("Clear all") }
    }
    if (state.history.size > 4) OutlinedTextField(
      value = query, onValueChange = { query = it }, singleLine = true,
      placeholder = { Text("Search history") },
      leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
      trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Clear, contentDescription = "Clear search") } },
      shape = RoundedCornerShape(28.dp),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
    if (history.isEmpty()) {
      Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ShapeBadge(if (query.isNotBlank()) Icons.Rounded.Search else Icons.Rounded.History, size = 72.dp)
        Text(
          if (query.isNotBlank()) "No calculations match" else if (state.settings.keepHistory) "No history yet" else "History is off",
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          if (query.isNotBlank()) "Try a different number or expression." else if (state.settings.keepHistory) "Your calculations will show up here." else "Turn it on in Settings > Data to keep your calculations.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    } else {
      val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
      // Stable, unique keys so rows animate into place when pinned, deleted or restored.
      val keys = remember(history) {
        val seen = HashMap<String, Int>()
        history.map { e -> val base = "${e.at}|${e.expression}"; val n = (seen[base] ?: 0) + 1; seen[base] = n; "$base#$n" }
      }
      LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
        itemsIndexed(history, key = { i, _ -> keys[i] }) { i, entry ->
          var rowMenu by remember { mutableStateOf(false) }
          val swipe = rememberSwipeToDismissBoxState(confirmValueChange = { target ->
            when (target) {
              SwipeToDismissBoxValue.EndToStart -> {
                haptics.confirm()
                onIntent(CalculatorIntent.DeleteHistory(entry))
                scope.launch {
                  snackbar.currentSnackbarData?.dismiss()
                  if (snackbar.showSnackbar("Calculation deleted", actionLabel = "Undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
                    onIntent(CalculatorIntent.RestoreHistory(entry))
                  }
                }
                true
              }
              SwipeToDismissBoxValue.StartToEnd -> { haptics.tick(); onIntent(CalculatorIntent.TogglePin(entry)); false }
              else -> false
            }
          })
          val shapes = ListItemDefaults.segmentedShapes(index = i, count = history.size)
          SwipeToDismissBox(
            state = swipe,
            modifier = Modifier.animateItem(),
            backgroundContent = {
              val toDelete = swipe.dismissDirection == SwipeToDismissBoxValue.EndToStart
              Box(
                Modifier.fillMaxSize().background(if (toDelete) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer).padding(horizontal = 24.dp),
                contentAlignment = if (toDelete) Alignment.CenterEnd else Alignment.CenterStart,
              ) {
                Icon(
                  if (toDelete) Icons.Rounded.Delete else Icons.Rounded.Star, contentDescription = null,
                  tint = if (toDelete) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                )
              }
            },
          ) {
            Box(Modifier.onSecondaryClick { rowMenu = true }) {
              SegmentedListItem(
                onClick = { onIntent(CalculatorIntent.UseHistory(entry)); onDismiss() },
                shapes = shapes,
                modifier = Modifier.handCursor(),
                overlineContent = { Text(prettyExpression(entry.expression, state.settings.grouping) + " =", maxLines = 2, overflow = TextOverflow.Ellipsis) },
                content = { Text(entry.result, style = tabularStyle(MaterialTheme.typography.titleLarge)) },
                supportingContent = { if (entry.at > 0) Text(dateFormat.format(Date(entry.at))) },
                trailingContent = {
                  Row {
                    IconButton(onClick = { haptics.tick(); onIntent(CalculatorIntent.TogglePin(entry)) }) {
                      Icon(if (entry.pinned) Icons.Rounded.Star else Icons.Rounded.StarBorder, contentDescription = if (entry.pinned) "Unpin" else "Pin")
                    }
                    IconButton(onClick = { haptics.tick(); clipboard.setText(AnnotatedString(entry.result.replace(",", ""))) }) {
                      Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy result")
                    }
                  }
                },
              )
              DropdownMenu(expanded = rowMenu, onDismissRequest = { rowMenu = false }) {
                DropdownMenuItem(text = { Text("Copy result") }, onClick = { rowMenu = false; clipboard.setText(AnnotatedString(entry.result.replace(",", ""))) })
                DropdownMenuItem(text = { Text(if (entry.pinned) "Unpin" else "Pin") }, onClick = { rowMenu = false; onIntent(CalculatorIntent.TogglePin(entry)) })
                DropdownMenuItem(text = { Text("Delete") }, onClick = { rowMenu = false; onIntent(CalculatorIntent.DeleteHistory(entry)) })
              }
            }
          }
        }
        item {
          Text(
            "Tap a calculation to continue from its result. Swipe right to pin, left to delete.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp, start = 8.dp),
          )
        }
      }
    }
    }
    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
  }
}
