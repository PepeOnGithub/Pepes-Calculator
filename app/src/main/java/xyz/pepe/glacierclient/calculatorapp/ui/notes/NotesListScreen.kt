@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.calculatorapp.ui.notes

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.pepe.glacierclient.calculatorapp.domain.model.MathNote
import xyz.pepe.glacierclient.calculatorapp.ui.components.BadgeShape
import xyz.pepe.glacierclient.calculatorapp.ui.components.Haptics
import xyz.pepe.glacierclient.calculatorapp.ui.components.PageScaffold
import xyz.pepe.glacierclient.calculatorapp.ui.components.SegmentedGroup
import xyz.pepe.glacierclient.calculatorapp.ui.components.ShapeBadge
import xyz.pepe.glacierclient.calculatorapp.ui.components.rowIconColors

/** A worked example that shows off everything a note can do — offered on the empty state. */
val SAMPLE_NOTE = """
  Trip budget
  flights = 420
  hotel = 3 × 95
  food = 4 × 38
  flights + hotel + food =

  Split 3 ways: (flights + hotel + food) / 3 =
  Tip: 18% × 64 =
  Drive: 240 km in mi =
  Cash: 150 USD in EUR =

  a = 1
  y = a·x² − 2x − 3
  y = sin(x·20)·3
""".trimIndent()

@Composable
fun NotesListScreen(
  notes: List<MathNote>,
  haptics: Haptics,
  onOpen: (Long) -> Unit,
  onCreate: (body: String) -> Unit,
  onDelete: (MathNote) -> Unit,
  openSettings: () -> Unit,
) {
  PageScaffold(
    title = "Math Notes",
    actions = { IconButton(onClick = openSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") } },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { haptics.confirm(); onCreate("") },
        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
        text = { Text("New note") },
      )
    },
  ) { padding ->
    if (notes.isEmpty()) {
      Column(
        Modifier.fillMaxSize().padding(padding).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
      ) {
        ShapeBadge(Icons.Rounded.EditNote, shape = BadgeShape.COOKIE, size = 112.dp)
        Text("Write math, get answers", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
          "Type or handwrite a line ending in “=” and the answer appears. Name values (price = 12) and reuse them, " +
            "convert units (5 km in mi =), and write y = … to draw a graph.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
        FilledTonalButton(onClick = { haptics.tick(); onCreate(SAMPLE_NOTE) }) { Text("Open an example") }
      }
      return@PageScaffold
    }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)) {
      item {
        SegmentedGroup(notes.size, Modifier.fillMaxWidth()) { i, shapes ->
          val note = notes[i]
          val preview = note.body.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.drop(1).take(2).joinToString(" · ")
          SegmentedListItem(
            onClick = { onOpen(note.id) },
            shapes = shapes,
            leadingContent = {
              val (c, o) = rowIconColors(i)
              ShapeBadge(Icons.Rounded.EditNote, shape = BadgeShape.CIRCLE, size = 40.dp, container = c, content = o)
            },
            content = { Text(note.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
              val time = DateUtils.getRelativeTimeSpanString(note.updatedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
              Text(if (preview.isEmpty()) time.toString() else "$time · $preview", maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            trailingContent = {
              IconButton(onClick = { haptics.confirm(); onDelete(note) }) { Icon(Icons.Rounded.Delete, contentDescription = "Delete note") }
            },
          )
        }
      }
    }
  }
}
