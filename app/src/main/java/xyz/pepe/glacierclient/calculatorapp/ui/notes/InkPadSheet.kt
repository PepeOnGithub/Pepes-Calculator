@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.calculatorapp.ui.notes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import xyz.pepe.glacierclient.calculatorapp.ui.components.Haptics
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.InkModelStatus
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.InkPoint
import xyz.pepe.glacierclient.calculatorapp.ui.theme.resultColor

/**
 * Handwriting pad: write a line with a finger or stylus. After a short pause the strokes are
 * recognized on-device (ML Kit) and the reading is shown; Insert puts it into the note, where it
 * is solved like typed text.
 */
@Composable
fun InkPadSheet(
  modelStatus: InkModelStatus,
  recognizing: Boolean,
  haptics: Haptics,
  onRecognize: (List<List<InkPoint>>, (String?) -> Unit) -> Unit,
  onInsert: (String) -> Unit,
  onRetryModel: () -> Unit,
  onDismiss: () -> Unit,
) {
  val strokes = remember { mutableStateListOf<List<InkPoint>>() }
  var current by remember { mutableStateOf<List<InkPoint>>(emptyList()) }
  var reading by remember { mutableStateOf<String?>(null) }
  var generation by remember { mutableIntStateOf(0) } // bumps on every edit; debounces recognition
  val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  LaunchedEffect(generation, modelStatus) {
    if (strokes.isEmpty() || modelStatus != InkModelStatus.READY) return@LaunchedEffect
    delay(700)
    val snapshot = strokes.toList()
    val gen = generation
    onRecognize(snapshot) { text -> if (gen == generation) reading = text }
  }

  // Sheet gestures off, so a downward stroke draws instead of dragging the sheet away.
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, sheetGesturesEnabled = false) {
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Handwrite", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = { if (strokes.isNotEmpty()) { strokes.removeAt(strokes.lastIndex); reading = null; generation++ } }, enabled = strokes.isNotEmpty()) {
          Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo stroke")
        }
        TextButton(onClick = { strokes.clear(); reading = null; generation++ }, enabled = strokes.isNotEmpty()) { Text("Clear") }
      }
      val status = when (modelStatus) {
        InkModelStatus.DOWNLOADING, InkModelStatus.IDLE -> "Getting the handwriting model ready (one-time download)…"
        InkModelStatus.FAILED -> "Couldn't download the handwriting model. Check your connection."
        InkModelStatus.READY -> "Write one line, e.g. 24 × 3 =  — it's read as you go."
      }
      Text(status, style = MaterialTheme.typography.bodySmall,
        color = if (modelStatus == InkModelStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)

      val ink = MaterialTheme.colorScheme.onSurface
      val guide = MaterialTheme.colorScheme.outlineVariant
      Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Canvas(
          Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .pointerInput(Unit) {
              awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                current = listOf(InkPoint(down.position.x, down.position.y, down.uptimeMillis))
                while (true) {
                  val event = awaitPointerEvent()
                  val change = event.changes.firstOrNull { it.id == down.id } ?: break
                  if (!change.pressed) break
                  if (change.positionChange() != Offset.Zero) {
                    current = current + InkPoint(change.position.x, change.position.y, change.uptimeMillis)
                  }
                  change.consume()
                }
                if (current.isNotEmpty()) strokes.add(current)
                current = emptyList()
                reading = null
                generation++
              }
            },
        ) {
          // A baseline to write on.
          drawLine(guide, Offset(24f, size.height * 0.72f), Offset(size.width - 24f, size.height * 0.72f), strokeWidth = 2f)
          (strokes + listOf(current)).forEach { s ->
            if (s.isEmpty()) return@forEach
            if (s.size == 1) { drawCircle(ink, 4f, Offset(s[0].x, s[0].y)); return@forEach }
            val path = Path().apply {
              moveTo(s[0].x, s[0].y)
              // Smooth with midpoints (quadratic) so fast strokes don't look polygonal.
              for (i in 1 until s.size) {
                val mid = Offset((s[i - 1].x + s[i].x) / 2, (s[i - 1].y + s[i].y) / 2)
                quadraticTo(s[i - 1].x, s[i - 1].y, mid.x, mid.y)
              }
              lineTo(s.last().x, s.last().y)
            }
            drawPath(path, ink, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
          }
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          reading?.let { "Reads: $it" } ?: if (recognizing) "Reading…" else " ",
          style = MaterialTheme.typography.titleMedium,
          color = resultColor(),
          modifier = Modifier.weight(1f),
        )
        if (recognizing) LoadingIndicator(Modifier.size(36.dp))
        Spacer(Modifier.size(8.dp))
        if (modelStatus == InkModelStatus.FAILED) {
          Button(onClick = onRetryModel) { Text("Retry") }
        } else {
          Button(
            onClick = {
              haptics.confirm()
              val r = reading
              if (r != null) { onInsert(r); onDismiss() }
              else onRecognize(strokes.toList()) { text -> if (text != null) { onInsert(text); onDismiss() } }
            },
            enabled = strokes.isNotEmpty() && modelStatus == InkModelStatus.READY,
          ) { Text("Insert") }
        }
      }
    }
  }
}
