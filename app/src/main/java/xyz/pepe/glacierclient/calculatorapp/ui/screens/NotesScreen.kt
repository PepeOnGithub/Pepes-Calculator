package xyz.pepe.glacierclient.calculatorapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import xyz.pepe.glacierclient.calculatorapp.domain.CalculatorEngine
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.InkModelStatus

/**
 * A page-style approximation of the iPad Calculator's "Math Notes": one continuous scrollable
 * page of lines, live-solved inline as you type (finish a line with "="), with "y = f(x)" lines
 * graphable in place and a running Σ total.
 *
 * Handwriting is real: the pencil button opens an ink pad backed by Google ML Kit's on-device
 * Digital Ink Recognition (the same technology behind Gboard's handwriting keyboard) — you draw,
 * it recognizes the strokes as text and drops that text onto the line. It is not MyScript-style
 * 2D math layout recognition (no exponent/fraction placement understanding), but it is real
 * recognition of your actual handwriting, not a canned result.
 */
@Composable
fun NotesScreen(
    lines: List<String>,
    inkModelStatus: InkModelStatus,
    isRecognizingInk: Boolean,
    onLineChanged: (Int, String) -> Unit,
    onAddLine: (String) -> Unit,
    onPrepareInk: () -> Unit,
    onRecognizeInk: (Int, List<List<Pair<Float, Float>>>) -> Unit
) {
    var graphedLine by remember { mutableStateOf<Int?>(null) }
    var inkTargetLine by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Math notes", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { onAddLine("") }) {
                Icon(Icons.Default.Add, contentDescription = "Add line")
            }
        }
        Text(
            "Finish a line with \"=\" to solve it live. A \"y = ...\" line can be graphed. " +
                "Tap the pencil to handwrite a line. Export this page to Pepe's Notes from Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            LazyColumn(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                items(lines.size) { index ->
                    val line = lines[index]
                    val solved = solveLine(line)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BasicTextField(
                            value = line,
                            onValueChange = { onLineChanged(index, it) },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                        )
                        if (solved != null) {
                            Text(solved, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        if (isGraphable(line)) {
                            IconButton(onClick = { graphedLine = if (graphedLine == index) null else index }) {
                                Icon(Icons.Default.ShowChart, contentDescription = "Graph this line")
                            }
                        }
                        IconButton(onClick = {
                            inkTargetLine = index
                            onPrepareInk()
                        }) {
                            Icon(Icons.Default.Draw, contentDescription = "Handwrite this line")
                        }
                    }
                    if (graphedLine == index) {
                        GraphCard(expression = line.substringAfter('=', line))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        val total = sumTrailingNumbers(lines)
                        IconButton(onClick = { onAddLine("Total = ${formatSum(total)}") }) {
                            Icon(Icons.Default.Functions, contentDescription = "Sum lines above")
                        }
                        Text(
                            "Sum: ${formatSum(total)}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 4.dp, top = 12.dp)
                        )
                    }
                }
            }
        }
    }

    val target = inkTargetLine
    if (target != null) {
        InkPadDialog(
            modelStatus = inkModelStatus,
            isRecognizing = isRecognizingInk,
            onDismiss = { inkTargetLine = null },
            onDone = { strokes ->
                onRecognizeInk(target, strokes)
                inkTargetLine = null
            }
        )
    }
}

/** A real handwriting canvas: draws every finger/stylus stroke as you make it, then hands the
 *  whole set of strokes to ML Kit Digital Ink Recognition on "Recognize". */
@Composable
private fun InkPadDialog(
    modelStatus: InkModelStatus,
    isRecognizing: Boolean,
    onDismiss: () -> Unit,
    onDone: (List<List<Pair<Float, Float>>>) -> Unit
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Handwrite", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Cancel") }
                }
                when (modelStatus) {
                    InkModelStatus.DOWNLOADING -> Text(
                        "Downloading the handwriting model (once, needs a connection)…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    InkModelStatus.FAILED -> Text(
                        "Couldn't download the handwriting model — check your connection and try again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    else -> Text(
                        "Draw with your finger or a stylus, then tap Recognize.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val strokeColor = MaterialTheme.colorScheme.onSurface
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(vertical = 12.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset -> currentStroke = listOf(offset) },
                                onDrag = { change, _ -> currentStroke = currentStroke + change.position },
                                onDragEnd = {
                                    if (currentStroke.size > 1) strokes.add(currentStroke)
                                    currentStroke = emptyList()
                                }
                            )
                        }
                ) {
                    drawRect(color = strokeColor.copy(alpha = 0.06f), size = size)
                    val allStrokes = strokes + listOf(currentStroke)
                    for (stroke in allStrokes) {
                        for (i in 0 until stroke.size - 1) {
                            drawLine(strokeColor, stroke[i], stroke[i + 1], strokeWidth = 6f)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { strokes.clear(); currentStroke = emptyList() }) { Text("Clear") }
                    if (isRecognizing) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    } else {
                        Button(
                            onClick = {
                                onDone(strokes.map { stroke -> stroke.map { it.x to it.y } })
                            },
                            enabled = strokes.isNotEmpty() && modelStatus != InkModelStatus.FAILED
                        ) { Text("Recognize") }
                    }
                }
            }
        }
    }
}

/** Solves a line if it ends in "=" (or contains "y = ..." with no free variable other than y). */
private fun solveLine(line: String): String? {
    if (!line.trimEnd().endsWith("=")) return null
    // Strip a leading "y" assignment target if present, e.g. "y=2*3" -> "2*3".
    val expr = line.trimEnd().removeSuffix("=").removePrefix("y").trimStart('=', ' ')
    return try {
        val result = CalculatorEngine.evaluate(expr, useRadians = false)
        formatSum(result)
    } catch (e: CalculatorEngine.EvaluationException) {
        null
    }
}

private fun isGraphable(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.startsWith("y") && trimmed.contains("=") && trimmed.contains("x")
}

private fun sumTrailingNumbers(lines: List<String>): Double {
    var sum = 0.0
    for (line in lines.asReversed()) {
        if (line.isBlank()) break
        val value = line.trim().toDoubleOrNull() ?: continue
        sum += value
    }
    return sum
}

private fun formatSum(value: Double): String {
    return if (value == value.toLong().toDouble()) value.toLong().toString()
    else String.format("%.4f", value).trimEnd('0').trimEnd('.')
}

@Composable
private fun GraphCard(expression: String) {
    val formula = expression.substringAfter('=', expression).trim()
    Card(modifier = Modifier.fillMaxWidth().height(200.dp).padding(vertical = 8.dp)) {
        val primary = MaterialTheme.colorScheme.primary
        val gridColor = MaterialTheme.colorScheme.outlineVariant
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp).padding(12.dp)) {
            val w = size.width
            val h = size.height
            val xRange = -10.0..10.0
            val yRange = -10.0..10.0
            fun toPx(x: Double, y: Double): Offset {
                val px = ((x - xRange.start) / (xRange.endInclusive - xRange.start) * w).toFloat()
                val py = (h - (y - yRange.start) / (yRange.endInclusive - yRange.start) * h).toFloat()
                return Offset(px, py)
            }
            drawLine(gridColor, toPx(xRange.start, 0.0), toPx(xRange.endInclusive, 0.0))
            drawLine(gridColor, toPx(0.0, yRange.start), toPx(0.0, yRange.endInclusive))

            var previous: Offset? = null
            var x = xRange.start
            while (x <= xRange.endInclusive) {
                val y = try {
                    CalculatorEngine.evaluate(formula, useRadians = false, variables = mapOf("x" to x))
                } catch (e: CalculatorEngine.EvaluationException) {
                    x += 0.1
                    previous = null
                    continue
                }
                if (y in yRange) {
                    val point = toPx(x, y)
                    previous?.let { drawLine(primary, it, point, strokeWidth = 4f) }
                    previous = point
                } else {
                    previous = null
                }
                x += 0.1
            }
        }
    }
}

/** Joins the notes lines into plain text for exporting/sharing. */
fun notesLinesToPlainText(lines: List<String>): String =
    lines.filter { it.isNotBlank() }.joinToString("\n")
