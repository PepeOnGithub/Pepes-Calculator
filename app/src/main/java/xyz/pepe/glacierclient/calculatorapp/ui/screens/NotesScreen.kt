package xyz.pepe.glacierclient.calculatorapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import xyz.pepe.glacierclient.calculatorapp.domain.CalculatorEngine

/**
 * A typed approximation of the iPad Calculator's "Math Notes": a running list of lines. Finish
 * a line with "=" and it solves live as you edit; "y = f(x)" lines can be graphed; a running
 * total sums the plain-number lines since the last blank line.
 *
 * This does not include Apple Pencil handwriting recognition — turning ink strokes into
 * expressions needs an on-device handwriting/math-OCR model, which is out of scope here. What's
 * real: live evaluation as you type, per-line graphing, and auto-summing a list of numbers.
 */
@Composable
fun NotesScreen() {
    val lines = remember { mutableStateListOf("") }
    var graphedLine by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Math notes", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { lines.add("") }) {
                Icon(Icons.Default.Add, contentDescription = "Add line")
            }
        }
        Text(
            "Type an expression ending in \"=\" to solve it live. A \"y = ...\" line can be " +
                "graphed. Tap Σ to total the numbers typed above.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            items(lines.size) { index ->
                val line = lines[index]
                val solved = solveLine(line)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = line,
                        onValueChange = { lines[index] = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        trailingIcon = if (solved != null) {
                            { Text(solved, style = MaterialTheme.typography.titleMedium) }
                        } else null
                    )
                    if (isGraphable(line)) {
                        IconButton(onClick = { graphedLine = if (graphedLine == index) null else index }) {
                            Icon(Icons.Default.ShowChart, contentDescription = "Graph this line")
                        }
                    }
                }
                if (graphedLine == index) {
                    GraphCard(expression = line.substringAfter('=', line))
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    val total = sumTrailingNumbers(lines)
                    IconButton(onClick = { lines.add("Total = ${formatSum(total)}") }) {
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
