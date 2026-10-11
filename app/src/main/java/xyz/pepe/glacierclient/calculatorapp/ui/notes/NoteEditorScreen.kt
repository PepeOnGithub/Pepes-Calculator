@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.calculatorapp.ui.notes

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round
import xyz.pepe.glacierclient.calculatorapp.data.CurrencyRatesSource
import xyz.pepe.glacierclient.calculatorapp.data.NOTES_APP_PACKAGE
import xyz.pepe.glacierclient.calculatorapp.domain.MathNotes
import xyz.pepe.glacierclient.calculatorapp.domain.NoteVariable
import xyz.pepe.glacierclient.calculatorapp.domain.NumberFormat
import xyz.pepe.glacierclient.calculatorapp.domain.model.MathNote
import xyz.pepe.glacierclient.calculatorapp.ui.components.Haptics
import xyz.pepe.glacierclient.calculatorapp.ui.components.PageScaffold
import xyz.pepe.glacierclient.calculatorapp.ui.components.SectionTitle
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorIntent
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorUiState
import xyz.pepe.glacierclient.calculatorapp.ui.theme.resultColor
import xyz.pepe.glacierclient.calculatorapp.ui.theme.tabularStyle

/** Symbols that are awkward on a phone keyboard, one tap away above it. */
private val QUICK_KEYS = listOf("=", "+", "−", "×", "÷", "^", "(", ")", "√", "π", "%", "x", "y = ", " in ", "sum =")

/**
 * One Math Notes page, modelled on iPadOS Math Notes: free-form text where any line ending in "="
 * gets its answer written in right after it (in amber), live as you type. Variables defined on one
 * line flow into the lines below and get a slider for scrubbing; "y = …" lines are drawn in a
 * graph that updates as you drag those sliders. The pencil opens a handwriting pad.
 */
@Composable
fun NoteEditorScreen(
  note: MathNote,
  state: CalculatorUiState,
  onIntent: (CalculatorIntent) -> Unit,
  haptics: Haptics,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  var value by remember(note.id) { mutableStateOf(TextFieldValue(note.body, TextRange(note.body.length))) }
  var showInk by remember { mutableStateOf(false) }
  var menu by remember { mutableStateOf(false) }
  val focus = remember { FocusRequester() }
  val settings = state.settings
  val style = settings.numberStyle
  val currency = remember(state.rates) { CurrencyRatesSource.units(state.rates) }
  val analysis = remember(value.text, settings, currency) {
    MathNotes.analyze(value.text, settings.useRadians, style, currency, settings.solveWithoutEquals)
  }
  val accent = resultColor()
  val transformation = remember(analysis.results, accent) { ResultsTransformation(analysis.results, accent) }

  fun setText(new: TextFieldValue) {
    value = new
    onIntent(CalculatorIntent.UpdateNote(note.id, new.text))
  }
  fun insert(text: String) {
    val t = value.text
    val start = value.selection.min
    val end = value.selection.max
    val out = t.substring(0, start) + text + t.substring(end)
    setText(TextFieldValue(out, TextRange(start + text.length)))
  }

  LaunchedEffect(note.id) { if (note.body.isEmpty()) focus.requestFocus() }

  PageScaffold(
    title = note.title,
    onBack = onBack,
    actions = {
      IconButton(onClick = { haptics.tick(); onIntent(CalculatorIntent.PrepareInkModel); showInk = true }) {
        Icon(Icons.Rounded.Draw, contentDescription = "Handwrite")
      }
      IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More") }
      DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
        DropdownMenuItem(
          text = { Text("Send to Pepe's Notes") },
          leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
          onClick = { menu = false; exportToNotes(context, withResults(value.text, analysis.results)) },
        )
        DropdownMenuItem(
          text = { Text("Delete note") },
          leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
          onClick = { menu = false; haptics.confirm(); onIntent(CalculatorIntent.DeleteNote(note.id)); onBack() },
        )
      }
    },
  ) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))) {
      Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        BasicTextField(
          value = value,
          onValueChange = { setText(it) },
          visualTransformation = transformation,
          textStyle = tabularStyle(MaterialTheme.typography.titleLarge).copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, lineHeight = 34.sp),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, autoCorrectEnabled = false),
          modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp).padding(horizontal = 24.dp, vertical = 12.dp).focusRequester(focus),
          decorationBox = { inner ->
            if (value.text.isEmpty()) {
              Text(
                "Type math like 12 × 4 =\nor name a value: price = 12",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 34.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              )
            }
            inner()
          },
        )

        val sliders = analysis.variables.filter { it.literal }
        if (sliders.isNotEmpty()) {
          SectionTitle("Variables")
          sliders.forEach { v ->
            VariableSlider(v, style.copy(decimals = null), haptics) { newValue ->
              val text = MathNotes.setVariable(value.text, v, newValue, style)
              setText(TextFieldValue(text, TextRange(value.selection.start.coerceAtMost(text.length))))
            }
          }
        }

        if (analysis.graphs.isNotEmpty()) {
          SectionTitle("Graph")
          GraphView(
            graphs = analysis.graphs,
            environment = analysis.environment,
            useRadians = settings.useRadians,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
          )
        }
      }

      // Quick symbols above the keyboard.
      Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(QUICK_KEYS) { key ->
            AssistChip(onClick = { haptics.tick(); insert(key) }, label = { Text(key.trim(), fontWeight = FontWeight.Medium) })
          }
        }
      }
    }
  }

  if (showInk) {
    InkPadSheet(
      modelStatus = state.inkModelStatus,
      recognizing = state.isRecognizingInk,
      haptics = haptics,
      onRecognize = { strokes, onText -> onIntent(CalculatorIntent.RecognizeInk(strokes, onText)) },
      onInsert = { text ->
        // Handwritten lines go on their own line, like writing on the next row of the page.
        val atLineStart = value.selection.min == 0 || value.text.getOrNull(value.selection.min - 1) == '\n'
        insert(if (atLineStart) text else "\n$text")
      },
      onRetryModel = { onIntent(CalculatorIntent.PrepareInkModel) },
      onDismiss = { showInk = false },
    )
  }
}

@Composable
private fun VariableSlider(v: NoteVariable, style: xyz.pepe.glacierclient.calculatorapp.domain.NumberStyle, haptics: Haptics, onChange: (Double) -> Unit) {
  // The range is fixed from the value when the slider first appears, so dragging doesn't move it.
  val range = remember(v.name, v.line) {
    val span = max(abs(v.value) * 2, 10.0)
    if (v.value >= 0) 0f..span.toFloat() else -span.toFloat()..0f
  }
  val integer = remember(v.name, v.line) { v.value == round(v.value) }
  Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(
      "${v.name} = ${NumberFormat.format(v.value, style)}",
      style = tabularStyle(MaterialTheme.typography.titleMedium),
      modifier = Modifier.width(132.dp),
      maxLines = 1,
    )
    Slider(
      value = v.value.toFloat().coerceIn(range.start, range.endInclusive),
      onValueChange = {
        val snapped = if (integer) round(it.toDouble()) else round(it * 100.0) / 100.0
        if (snapped != v.value) { haptics.tick(); onChange(snapped) }
      },
      valueRange = range,
      modifier = Modifier.weight(1f),
    )
  }
}

/**
 * Draws each line's answer right after its "=" without putting it in the text itself — the text
 * stays exactly what was typed; answers are display-only and the cursor skips over them.
 */
private class ResultsTransformation(private val results: List<String?>, private val color: Color) : VisualTransformation {
  override fun filter(text: AnnotatedString): TransformedText {
    val inserts = ArrayList<Pair<Int, Int>>() // original offset → inserted length
    var offset = 0
    val out = buildAnnotatedString {
      text.text.split('\n').forEachIndexed { i, line ->
        if (i > 0) { append('\n'); offset++ }
        append(line)
        offset += line.length
        val r = results.getOrNull(i) ?: return@forEachIndexed
        val endsWithEquals = line.trimEnd().endsWith("=")
        val insert = when {
          endsWithEquals && line.endsWith(" ") -> r
          endsWithEquals -> " $r"
          else -> "  = $r"
        }
        withStyle(SpanStyle(color = if (endsWithEquals) color else color.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)) { append(insert) }
        inserts.add(offset to insert.length)
      }
    }
    val mapping = object : OffsetMapping {
      override fun originalToTransformed(offset: Int): Int = offset + inserts.filter { it.first < offset }.sumOf { it.second }
      override fun transformedToOriginal(offset: Int): Int {
        var shift = 0
        for ((at, len) in inserts) {
          val start = at + shift
          if (offset <= start) return offset - shift
          if (offset <= start + len) return at
          shift += len
        }
        return (offset - shift).coerceIn(0, text.length)
      }
    }
    return TransformedText(out, mapping)
  }

  override fun equals(other: Any?) = other is ResultsTransformation && other.results == results && other.color == color
  override fun hashCode() = results.hashCode() * 31 + color.hashCode()
}

/** The note as plain text with its answers filled in — what gets shared. */
private fun withResults(text: String, results: List<String?>): String =
  text.split('\n').mapIndexed { i, line ->
    val r = results.getOrNull(i) ?: return@mapIndexed line
    if (line.trimEnd().endsWith("=")) "${line.trimEnd()} $r" else "$line = $r"
  }.joinToString("\n")

/** Sends text to Pepe's Notes (its share-intent filter makes a new note), or the system share sheet. */
fun exportToNotes(context: Context, text: String) {
  val intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_SUBJECT, "Math notes")
    putExtra(Intent.EXTRA_TEXT, text)
    setPackage(NOTES_APP_PACKAGE)
  }
  if (intent.resolveActivity(context.packageManager) != null) {
    context.startActivity(intent)
  } else {
    intent.setPackage(null)
    runCatching { context.startActivity(Intent.createChooser(intent, "Export math notes")) }
  }
}

/** All notes, with answers, separated by rules — for Settings › Data › Export. */
fun allNotesAsText(state: CalculatorUiState): String {
  val currency = CurrencyRatesSource.units(state.rates)
  return state.notes.joinToString("\n\n———\n\n") { n ->
    val a = MathNotes.analyze(n.body, state.settings.useRadians, state.settings.numberStyle, currency, state.settings.solveWithoutEquals)
    withResults(n.body, a.results)
  }
}
