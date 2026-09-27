@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package xyz.pepe.glacierclient.calculatorapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.EditNote
import kotlinx.coroutines.launch
import xyz.pepe.glacierclient.calculatorapp.ui.components.CalcButtonStyle
import xyz.pepe.glacierclient.calculatorapp.ui.components.CalculatorButton
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorIntent
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorMode
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorViewModel
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.HistoryEntry

private enum class AppTab(val label: String) { CALCULATOR("Calculator"), CONVERTER("Converter"), NOTES("Notes") }

/** Top-level host: three tabs (Calculator, Converter, Math notes), matching the iPad Calculator's
 *  mode toggle for converter access plus a dedicated notes tab. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val onIntent: (CalculatorIntent) -> Unit = viewModel::onIntent
    val tabs = AppTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tabs[pagerState.currentPage].label) },
                actions = {
                    if (tabs[pagerState.currentPage] == AppTab.CALCULATOR) {
                        IconButton(onClick = { onIntent(CalculatorIntent.ToggleMode) }) {
                            Icon(Icons.Default.Science, contentDescription = "Toggle scientific mode")
                        }
                        IconButton(onClick = { onIntent(CalculatorIntent.ToggleHistory(true)) }) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                    }
                    IconButton(onClick = { onIntent(CalculatorIntent.ToggleSettings(true)) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val icons = listOf(Icons.Default.Calculate, Icons.Default.Functions, Icons.Default.EditNote)
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        icon = { Icon(icons[index], contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().padding(innerPadding)) { page ->
            when (tabs[page]) {
                AppTab.CALCULATOR -> CalculatorPane(uiState = uiState, onIntent = onIntent)
                AppTab.CONVERTER -> ConverterScreen()
                AppTab.NOTES -> NotesScreen()
            }
        }
    }

    if (uiState.isHistoryOpen) {
        HistorySheet(
            history = uiState.history,
            onDismiss = { onIntent(CalculatorIntent.ToggleHistory(false)) },
            onSelect = { onIntent(CalculatorIntent.UseHistoryEntry(it)) },
            onDelete = { onIntent(CalculatorIntent.DeleteHistoryEntry(it)) },
            onClear = { onIntent(CalculatorIntent.ClearHistory) }
        )
    }

    if (uiState.isSettingsOpen) {
        SettingsDialog(
            hapticsEnabled = uiState.settings.hapticFeedbackEnabled,
            useRadians = uiState.settings.useRadians,
            keepHistory = uiState.settings.keepHistory,
            onHapticsChanged = { onIntent(CalculatorIntent.SetHaptics(it)) },
            onRadiansChanged = { onIntent(CalculatorIntent.SetUseRadians(it)) },
            onKeepHistoryChanged = { onIntent(CalculatorIntent.SetKeepHistory(it)) },
            onDismiss = { onIntent(CalculatorIntent.ToggleSettings(false)) }
        )
    }
}

@Composable
private fun CalculatorPane(
    uiState: xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorUiState,
    onIntent: (CalculatorIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        DisplayArea(display = uiState.display, isError = uiState.isError, modifier = Modifier.weight(1f))
        Keypad(mode = uiState.mode, onIntent = onIntent)
    }
}

@Composable
private fun DisplayArea(display: String, isError: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
        BasicText(
            text = display,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(8.dp),
            style = MaterialTheme.typography.displayMedium.copy(
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                fontSize = 48.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Visible
        )
    }
}

private val BASIC_ROWS = listOf(
    listOf("C" to CalcButtonStyle.FUNCTION, "(" to CalcButtonStyle.FUNCTION, ")" to CalcButtonStyle.FUNCTION, "÷" to CalcButtonStyle.OPERATOR),
    listOf("7" to CalcButtonStyle.NUMBER, "8" to CalcButtonStyle.NUMBER, "9" to CalcButtonStyle.NUMBER, "×" to CalcButtonStyle.OPERATOR),
    listOf("4" to CalcButtonStyle.NUMBER, "5" to CalcButtonStyle.NUMBER, "6" to CalcButtonStyle.NUMBER, "−" to CalcButtonStyle.OPERATOR),
    listOf("1" to CalcButtonStyle.NUMBER, "2" to CalcButtonStyle.NUMBER, "3" to CalcButtonStyle.NUMBER, "+" to CalcButtonStyle.OPERATOR),
    listOf("%" to CalcButtonStyle.FUNCTION, "0" to CalcButtonStyle.NUMBER, "." to CalcButtonStyle.NUMBER, "=" to CalcButtonStyle.ACCENT)
)

private val SCIENTIFIC_ROW_1 = listOf(
    "sin(" to CalcButtonStyle.FUNCTION, "cos(" to CalcButtonStyle.FUNCTION, "tan(" to CalcButtonStyle.FUNCTION, "^" to CalcButtonStyle.OPERATOR
)
private val SCIENTIFIC_ROW_2 = listOf(
    "ln(" to CalcButtonStyle.FUNCTION, "log(" to CalcButtonStyle.FUNCTION, "sqrt(" to CalcButtonStyle.FUNCTION, "!" to CalcButtonStyle.OPERATOR
)
private val SCIENTIFIC_ROW_3 = listOf(
    "pi" to CalcButtonStyle.FUNCTION, "e" to CalcButtonStyle.FUNCTION, "(" to CalcButtonStyle.FUNCTION, ")" to CalcButtonStyle.FUNCTION
)

@Composable
private fun Keypad(mode: CalculatorMode, onIntent: (CalculatorIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (mode == CalculatorMode.SCIENTIFIC) {
            KeypadRow(SCIENTIFIC_ROW_1, onIntent)
            KeypadRow(SCIENTIFIC_ROW_2, onIntent)
            KeypadRow(SCIENTIFIC_ROW_3, onIntent)
        }
        for (row in BASIC_ROWS) {
            KeypadRow(row, onIntent)
        }
    }
}

@Composable
private fun KeypadRow(row: List<Pair<String, CalcButtonStyle>>, onIntent: (CalculatorIntent) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((label, style) in row) {
            CalculatorButton(
                label = label,
                style = style,
                modifier = Modifier.weight(1f)
            ) {
                when (label.trim()) {
                    "C" -> onIntent(CalculatorIntent.ClearAll)
                    "=" -> onIntent(CalculatorIntent.Evaluate)
                    else -> onIntent(CalculatorIntent.InputToken(label))
                }
            }
        }
    }
}

@Composable
private fun HistorySheet(
    history: List<HistoryEntry>,
    onDismiss: () -> Unit,
    onSelect: (HistoryEntry) -> Unit,
    onDelete: (HistoryEntry) -> Unit,
    onClear: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("History", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onClear) { Text("Clear all") }
        }
        if (history.isEmpty()) {
            Text(
                "No calculations yet",
                modifier = Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(history) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.result, style = MaterialTheme.typography.titleMedium) },
                        supportingContent = { Text(entry.expression) },
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(entry) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { clipboard.setText(AnnotatedString(entry.result)) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy result")
                                }
                                IconButton(onClick = { onDelete(entry) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete entry")
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    hapticsEnabled: Boolean,
    useRadians: Boolean,
    keepHistory: Boolean,
    onHapticsChanged: (Boolean) -> Unit,
    onRadiansChanged: (Boolean) -> Unit,
    onKeepHistoryChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            SegmentedListItem(
                checked = hapticsEnabled,
                onCheckedChange = onHapticsChanged,
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 3),
                trailingContent = { Switch(checked = hapticsEnabled, onCheckedChange = null) }
            ) { Text("Haptic feedback") }
            SegmentedListItem(
                checked = useRadians,
                onCheckedChange = onRadiansChanged,
                shapes = ListItemDefaults.segmentedShapes(index = 1, count = 3),
                trailingContent = { Switch(checked = useRadians, onCheckedChange = null) },
                supportingContent = { Text("Off uses degrees for sin/cos/tan") }
            ) { Text("Use radians") }
            SegmentedListItem(
                checked = keepHistory,
                onCheckedChange = onKeepHistoryChanged,
                shapes = ListItemDefaults.segmentedShapes(index = 2, count = 3),
                trailingContent = { Switch(checked = keepHistory, onCheckedChange = null) }
            ) { Text("Keep calculation history") }
        }
    }
}
