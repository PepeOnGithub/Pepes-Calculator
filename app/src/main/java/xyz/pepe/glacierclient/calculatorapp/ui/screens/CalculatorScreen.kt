@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.calculatorapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import xyz.pepe.glacierclient.calculatorapp.data.NOTES_APP_PACKAGE
import xyz.pepe.glacierclient.calculatorapp.ui.components.BadgeShape
import xyz.pepe.glacierclient.calculatorapp.ui.components.CalcButtonStyle
import xyz.pepe.glacierclient.calculatorapp.ui.components.CalculatorButton
import xyz.pepe.glacierclient.calculatorapp.ui.components.SegmentedGroup
import xyz.pepe.glacierclient.calculatorapp.ui.components.ShapeBadge
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorIntent
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorMode
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorUiState
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorViewModel
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.HistoryEntry
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.SettingsSection

private enum class AppTab(val label: String) { CALCULATOR("Calculator"), CONVERTER("Converter"), NOTES("Notes") }

/** Top-level host: three tabs (Calculator, Converter, Math notes), matching the iPad Calculator's
 *  mode toggle for converter access plus a dedicated notes tab. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val onIntent: (CalculatorIntent) -> Unit = viewModel::onIntent

    if (uiState.isSettingsOpen) {
        SettingsHost(uiState = uiState, onIntent = onIntent)
        return
    }

    val tabs = AppTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    BackHandler(enabled = pagerState.currentPage != 0) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(tabs[pagerState.currentPage].label) },
                subtitle = { Text("Basic, scientific, converter & notes") },
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            ShortNavigationBar {
                val icons = listOf(Icons.Default.Calculate, Icons.Default.Functions, Icons.Default.EditNote)
                tabs.forEachIndexed { index, tab ->
                    ShortNavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        icon = { Icon(icons[index], contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().padding(innerPadding)) { page ->
            when (tabs[page]) {
                AppTab.CALCULATOR -> CalculatorPane(uiState = uiState, onIntent = onIntent)
                AppTab.CONVERTER -> ConverterScreen()
                AppTab.NOTES -> NotesScreen(
                    lines = uiState.notesLines,
                    inkModelStatus = uiState.inkModelStatus,
                    isRecognizingInk = uiState.isRecognizingInk,
                    onLineChanged = { index, value -> onIntent(CalculatorIntent.SetNotesLine(index, value)) },
                    onAddLine = { text -> onIntent(CalculatorIntent.AddNotesLine(text)) },
                    onPrepareInk = { onIntent(CalculatorIntent.PrepareInkModel) },
                    onRecognizeInk = { line, strokes -> onIntent(CalculatorIntent.RecognizeInk(line, strokes)) }
                )
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
}

@Composable
private fun CalculatorPane(uiState: CalculatorUiState, onIntent: (CalculatorIntent) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                .testTag("calculator_display")
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

// One column of scientific keys, stacked beside the basic grid rather than added above it —
// matching a real scientific calculator's side panel instead of a taller vertical keypad.
private val SCIENTIFIC_COLUMN = listOf(
    "sin(" to CalcButtonStyle.FUNCTION,
    "cos(" to CalcButtonStyle.FUNCTION,
    "tan(" to CalcButtonStyle.FUNCTION,
    "ln(" to CalcButtonStyle.FUNCTION,
    "log(" to CalcButtonStyle.FUNCTION,
    "sqrt(" to CalcButtonStyle.FUNCTION,
    "^" to CalcButtonStyle.OPERATOR,
    "!" to CalcButtonStyle.OPERATOR,
    "pi" to CalcButtonStyle.FUNCTION,
    "e" to CalcButtonStyle.FUNCTION
)

@Composable
private fun Keypad(mode: CalculatorMode, onIntent: (CalculatorIntent) -> Unit) {
    if (mode == CalculatorMode.SCIENTIFIC) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(0.85f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((label, style) in SCIENTIFIC_COLUMN) {
                    CalculatorButton(label = label, style = style, modifier = Modifier.fillMaxWidth()) {
                        onIntent(CalculatorIntent.InputToken(label))
                    }
                }
            }
            Column(modifier = Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in BASIC_ROWS) {
                    KeypadRow(row, onIntent)
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in BASIC_ROWS) {
                KeypadRow(row, onIntent)
            }
        }
    }
}

@Composable
private fun KeypadRow(row: List<Pair<String, CalcButtonStyle>>, onIntent: (CalculatorIntent) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((label, style) in row) {
            CalculatorButton(label = label, style = style, modifier = Modifier.weight(1f)) {
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

/** Settings — a Pixel-style category list on the main page, each row navigating to its own full
 *  sub-page, exactly like Notes/Clock/Weather's SettingsHost instead of one flat popup. */
@Composable
private fun SettingsHost(uiState: CalculatorUiState, onIntent: (CalculatorIntent) -> Unit) {
    val onOpen: (SettingsSection?) -> Unit = { onIntent(CalculatorIntent.ToggleSettingsSection(it)) }
    val onBack: () -> Unit = {
        if (uiState.settingsSection == null) onIntent(CalculatorIntent.ToggleSettings(false)) else onOpen(null)
    }
    when (uiState.settingsSection) {
        null -> SettingsMainPage(onBack = onBack, onOpen = onOpen)
        SettingsSection.APPEARANCE -> AppearancePage(
            wallpaperColorsEnabled = uiState.settings.wallpaperColorsEnabled,
            onWallpaperColorsChanged = { onIntent(CalculatorIntent.SetWallpaperColors(it)) },
            onBack = onBack
        )
        SettingsSection.CALCULATION -> CalculationPage(
            hapticsEnabled = uiState.settings.hapticFeedbackEnabled,
            useRadians = uiState.settings.useRadians,
            onHapticsChanged = { onIntent(CalculatorIntent.SetHaptics(it)) },
            onRadiansChanged = { onIntent(CalculatorIntent.SetUseRadians(it)) },
            onBack = onBack
        )
        SettingsSection.DATA -> DataPage(
            keepHistory = uiState.settings.keepHistory,
            notesLines = uiState.notesLines,
            onKeepHistoryChanged = { onIntent(CalculatorIntent.SetKeepHistory(it)) },
            onBack = onBack
        )
    }
}

@Composable
private fun SettingsScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    testTag: String,
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .testTag(testTag),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title) },
                subtitle = { Text(subtitle) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding -> content(innerPadding) }
}

@Composable
private fun CategoryRow(index: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = 3),
        leadingContent = { ShapeBadge(icon, shape = BadgeShape.CIRCLE, tintIndex = index) },
        supportingContent = { Text(subtitle) }
    ) { Text(title) }
}

@Composable
private fun SettingsMainPage(onBack: () -> Unit, onOpen: (SettingsSection) -> Unit) {
    SettingsScaffold("Settings", "Appearance, calculation and data", onBack, "settings_page") { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
            SegmentedGroup {
                CategoryRow(0, Icons.Default.Palette, "Appearance", "Wallpaper colors") { onOpen(SettingsSection.APPEARANCE) }
                CategoryRow(1, Icons.Default.Science, "Calculation", "Haptics and angle units") { onOpen(SettingsSection.CALCULATION) }
                CategoryRow(2, Icons.Default.History, "Data", "History and exporting math notes") { onOpen(SettingsSection.DATA) }
            }
        }
    }
}

@Composable
private fun AppearancePage(wallpaperColorsEnabled: Boolean, onWallpaperColorsChanged: (Boolean) -> Unit, onBack: () -> Unit) {
    SettingsScaffold("Appearance", "Wallpaper colors", onBack, "settings_appearance") { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
            SegmentedGroup {
                SegmentedListItem(
                    checked = wallpaperColorsEnabled,
                    onCheckedChange = onWallpaperColorsChanged,
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                    leadingContent = { ShapeBadge(Icons.Default.Palette, shape = BadgeShape.CIRCLE, tintIndex = 0) },
                    trailingContent = { Switch(checked = wallpaperColorsEnabled, onCheckedChange = null) },
                    supportingContent = { Text("Off uses the fixed brand-green theme; on pulls colors from your wallpaper") }
                ) { Text("Wallpaper colors") }
            }
        }
    }
}

@Composable
private fun CalculationPage(
    hapticsEnabled: Boolean,
    useRadians: Boolean,
    onHapticsChanged: (Boolean) -> Unit,
    onRadiansChanged: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    SettingsScaffold("Calculation", "Haptics and angle units", onBack, "settings_calculation") { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
            SegmentedGroup {
                SegmentedListItem(
                    checked = hapticsEnabled,
                    onCheckedChange = onHapticsChanged,
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                    leadingContent = { ShapeBadge(Icons.Default.Vibration, shape = BadgeShape.CIRCLE, tintIndex = 0) },
                    trailingContent = { Switch(checked = hapticsEnabled, onCheckedChange = null) }
                ) { Text("Haptic feedback") }
                SegmentedListItem(
                    checked = useRadians,
                    onCheckedChange = onRadiansChanged,
                    shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                    leadingContent = { ShapeBadge(Icons.Default.Science, shape = BadgeShape.CIRCLE, tintIndex = 1) },
                    trailingContent = { Switch(checked = useRadians, onCheckedChange = null) },
                    supportingContent = { Text("Off uses degrees for sin/cos/tan") }
                ) { Text("Use radians") }
            }
        }
    }
}

@Composable
private fun DataPage(
    keepHistory: Boolean,
    notesLines: List<String>,
    onKeepHistoryChanged: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    SettingsScaffold("Data", "History and exporting math notes", onBack, "settings_data") { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
            SegmentedGroup {
                SegmentedListItem(
                    checked = keepHistory,
                    onCheckedChange = onKeepHistoryChanged,
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                    leadingContent = { ShapeBadge(Icons.Default.History, shape = BadgeShape.CIRCLE, tintIndex = 0) },
                    trailingContent = { Switch(checked = keepHistory, onCheckedChange = null) }
                ) { Text("Keep calculation history") }
                SegmentedListItem(
                    onClick = { exportNotesToPepesNotes(context, notesLines) },
                    shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                    leadingContent = { ShapeBadge(Icons.Default.Share, shape = BadgeShape.CIRCLE, tintIndex = 1) },
                    supportingContent = { Text("Sends the Math notes page to Pepe's Notes as a new note") }
                ) { Text("Export math notes to Pepe's Notes") }
            }
        }
    }
}

/** Sends the current Math notes page to Pepe's Notes (xyz.pepe.glacierclient.notesapp) via a
 *  plain ACTION_SEND — Notes already has a text/plain share-intent filter that turns shared text
 *  into a new note (see its ShareIntake), so this needs no changes on that side. Falls back to
 *  the normal system share sheet if Notes isn't installed. */
private fun exportNotesToPepesNotes(context: android.content.Context, notesLines: List<String>) {
    val text = notesLinesToPlainText(notesLines)
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, "Math notes")
        putExtra(android.content.Intent.EXTRA_TEXT, text)
        setPackage(NOTES_APP_PACKAGE)
    }
    val resolved = intent.resolveActivity(context.packageManager) != null
    if (resolved) {
        context.startActivity(intent)
    } else {
        intent.setPackage(null)
        context.startActivity(android.content.Intent.createChooser(intent, "Export math notes"))
    }
}
