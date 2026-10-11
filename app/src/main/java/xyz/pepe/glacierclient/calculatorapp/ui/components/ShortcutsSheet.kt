@file:OptIn(ExperimentalMaterial3Api::class)

package xyz.pepe.glacierclient.calculatorapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val SHORTCUTS = listOf(
  "Calculator" to listOf(
    "0-9 . + - * / ^ ( ) %" to "Type the expression",
    "Enter or =" to "Calculate",
    "Backspace" to "Delete the last character",
    "Delete or Esc" to "Clear",
    "Ctrl+Backspace" to "Clear everything",
    "Ctrl+C / Ctrl+V" to "Copy the result / paste an expression",
    "Up / Down" to "Walk through history",
    "M, R" to "Memory add / recall",
  ),
  "App" to listOf(
    "Ctrl+H" to "Open history",
    "Ctrl+1, 2, 3, 4" to "Calculator, Converter, Tools, Math Notes",
    "Ctrl+," to "Open settings",
    "Ctrl+E" to "Edit the Tools layout (then Alt+Up / Alt+Down moves the focused tool)",
  ),
  "Mouse" to listOf(
    "Right-click the display" to "Copy, paste, share, send to Notes",
    "Scroll the display" to "Toggle large digits",
  ),
)

/** Lists the physical-keyboard and mouse shortcuts. */
@Composable
fun ShortcutsSheet(onDismiss: () -> Unit) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      SectionHeader("Keyboard shortcuts", Icons.Rounded.Keyboard, Modifier.padding(horizontal = 4.dp))
      SHORTCUTS.forEach { (group, items) ->
        GlassCard(Modifier.fillMaxWidth(), headerTitle = group) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { (keys, what) ->
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(keys, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                Text(what, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
              }
            }
          }
        }
      }
    }
  }
}
