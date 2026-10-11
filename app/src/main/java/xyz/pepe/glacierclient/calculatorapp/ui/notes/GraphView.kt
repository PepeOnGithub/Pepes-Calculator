package xyz.pepe.glacierclient.calculatorapp.ui.notes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import xyz.pepe.glacierclient.calculatorapp.domain.CalculatorEngine
import xyz.pepe.glacierclient.calculatorapp.domain.NoteGraph
import xyz.pepe.glacierclient.calculatorapp.domain.NumberFormat
import xyz.pepe.glacierclient.calculatorapp.domain.NumberStyle

private const val DEFAULT_SPAN = 20f

/** Plots every "y = …" line of a note. Drag to pan, pinch to zoom, double-tap to reset. */
@Composable
fun GraphView(graphs: List<NoteGraph>, environment: Map<String, Double>, useRadians: Boolean, modifier: Modifier = Modifier) {
  val cs = MaterialTheme.colorScheme
  val palette = listOf(cs.primary, cs.tertiary, Color(0xFF7FB2FF), Color(0xFFFF8FB1), cs.secondary, cs.error)
  var center by remember { mutableStateOf(Offset.Zero) } // world coordinates at the middle
  var span by remember { mutableFloatStateOf(DEFAULT_SPAN) } // world units across the width
  val measurer = rememberTextMeasurer()
  val labelStyle = MaterialTheme.typography.labelSmall.copy(color = cs.onSurfaceVariant)
  val grid = cs.outlineVariant.copy(alpha = 0.45f)
  val axis = cs.onSurfaceVariant.copy(alpha = 0.8f)

  Surface(shape = MaterialTheme.shapes.extraLarge, color = cs.surfaceContainer, modifier = modifier.fillMaxWidth()) {
    Column {
      Canvas(
        Modifier
          .fillMaxWidth()
          .height(300.dp)
          .clip(MaterialTheme.shapes.extraLarge)
          .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
              val upp = span / size.width
              span = (span / zoom).coerceIn(0.01f, 1e6f)
              center = Offset(center.x - pan.x * upp, center.y + pan.y * upp)
            }
          }
          .pointerInput(Unit) { detectTapGestures(onDoubleTap = { center = Offset.Zero; span = DEFAULT_SPAN }) },
      ) {
        val w = size.width
        val h = size.height
        val upp = span / w
        fun sx(x: Double) = ((x - center.x) / upp + w / 2).toFloat()
        fun sy(y: Double) = (h / 2 - (y - center.y) / upp).toFloat()
        val xMin = center.x - w / 2 * upp
        val xMax = center.x + w / 2 * upp
        val yMin = center.y - h / 2 * upp
        val yMax = center.y + h / 2 * upp

        // Grid on "nice" steps (1, 2, 5 × 10ⁿ), about 8 lines across.
        val raw = span / 8.0
        val mag = 10.0.pow(floor(log10(raw)))
        val step = listOf(1.0, 2.0, 5.0, 10.0).map { it * mag }.first { it >= raw }
        val fmt = NumberStyle(decimals = null, grouping = false)
        var gx = ceil(xMin / step) * step
        while (gx <= xMax) {
          val px = sx(gx)
          drawLine(if (abs(gx) < step / 2) axis else grid, Offset(px, 0f), Offset(px, h), strokeWidth = if (abs(gx) < step / 2) 2f else 1f)
          if (abs(gx) > step / 2) {
            val label = measurer.measure(NumberFormat.format(gx, fmt), labelStyle)
            val ly = sy(0.0).coerceIn(4f, h - label.size.height - 4f) + 4f
            drawText(label, topLeft = Offset(px - label.size.width / 2f, ly.coerceAtMost(h - label.size.height.toFloat())))
          }
          gx += step
        }
        var gy = ceil(yMin / step) * step
        while (gy <= yMax) {
          val py = sy(gy)
          drawLine(if (abs(gy) < step / 2) axis else grid, Offset(0f, py), Offset(w, py), strokeWidth = if (abs(gy) < step / 2) 2f else 1f)
          if (abs(gy) > step / 2) {
            val label = measurer.measure(NumberFormat.format(gy, fmt), labelStyle)
            val lx = (sx(0.0) + 6f).coerceIn(4f, w - label.size.width - 4f)
            drawText(label, topLeft = Offset(lx, py - label.size.height / 2f))
          }
          gy += step
        }

        // Curves: sample every 2 px; lift the pen across gaps and asymptotes.
        graphs.forEachIndexed { i, g ->
          val path = Path()
          var penDown = false
          var lastY = 0f
          var px = 0f
          while (px <= w) {
            val x = (xMin + px * upp).toDouble()
            val y = CalculatorEngine.evaluateOrNull(g.expression, useRadians, environment + ("x" to x))
            if (y == null) { penDown = false } else {
              val py = sy(y)
              if (penDown && abs(py - lastY) < h * 2) path.lineTo(px, py) else path.moveTo(px, py)
              penDown = py > -h * 4 && py < h * 5
              lastY = py
            }
            px += 2f
          }
          drawPath(path, palette[i % palette.size], style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
      }
      // Legend
      Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        graphs.forEachIndexed { i, g ->
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(palette[i % palette.size]))
            Text("${g.label} = ${g.expression}", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
          }
        }
        Text("Drag to pan · pinch to zoom · double-tap to reset", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
      }
    }
  }
}
