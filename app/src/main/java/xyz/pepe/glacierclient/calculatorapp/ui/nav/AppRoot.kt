@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package xyz.pepe.glacierclient.calculatorapp.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import xyz.pepe.glacierclient.calculatorapp.ui.components.handCursor
import xyz.pepe.glacierclient.calculatorapp.ui.components.rememberReducedMotion
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import xyz.pepe.glacierclient.calculatorapp.domain.model.NavLabels
import xyz.pepe.glacierclient.calculatorapp.domain.model.NavStyle
import xyz.pepe.glacierclient.calculatorapp.ui.components.GlassBorder
import xyz.pepe.glacierclient.calculatorapp.ui.components.glass
import xyz.pepe.glacierclient.calculatorapp.ui.components.rememberHaptics
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorIntent
import xyz.pepe.glacierclient.calculatorapp.ui.mvi.CalculatorViewModel
import xyz.pepe.glacierclient.calculatorapp.ui.notes.NoteEditorScreen
import xyz.pepe.glacierclient.calculatorapp.ui.notes.NotesListScreen
import xyz.pepe.glacierclient.calculatorapp.ui.screens.CalculatorScreen
import xyz.pepe.glacierclient.calculatorapp.ui.screens.ConverterScreen
import xyz.pepe.glacierclient.calculatorapp.ui.screens.ToolsScreen
import xyz.pepe.glacierclient.calculatorapp.ui.settings.SettingsHost
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private enum class Tab(val label: String, val icon: ImageVector) {
  CALCULATOR("Calculator", Icons.Rounded.Calculate),
  CONVERTER("Converter", Icons.Rounded.SwapHoriz),
  TOOLS("Tools", Icons.Rounded.Build),
  NOTES("Math Notes", Icons.Rounded.EditNote),
}

/** Single-activity root, same shell as Pepe's Clock: bottom bar (classic or floating) on phones,
 *  a navigation rail on screens ≥ 600 dp wide; Settings and a note open full-screen on top. */
@Composable
fun AppRoot(vm: CalculatorViewModel) {
  val state by vm.uiState.collectAsStateWithLifecycle()
  val onIntent: (CalculatorIntent) -> Unit = vm::onIntent
  val haptics = rememberHaptics(state.settings.haptics)
  var tabIndex by rememberSaveable { mutableIntStateOf(Tab.CALCULATOR.ordinal) }
  var showSettings by rememberSaveable { mutableStateOf(false) }
  var historyOpen by rememberSaveable { mutableStateOf(false) }
  var historyIdx by remember { mutableIntStateOf(-1) }
  val clipboard = LocalClipboardManager.current
  val focusRequester = remember { FocusRequester() }
  val reduced = rememberReducedMotion()
  var openNoteId by rememberSaveable { mutableStateOf<Long?>(null) }
  val wide = LocalConfiguration.current.screenWidthDp >= 600
  val openSettings = { showSettings = true }
  var toolsEditRequest by remember { mutableStateOf(false) }

  BackHandler(enabled = showSettings) { showSettings = false }
  if (showSettings) {
    SettingsHost(
      state, onIntent, haptics,
      onEditTools = { showSettings = false; tabIndex = Tab.TOOLS.ordinal; toolsEditRequest = true },
    ) { showSettings = false }
    return
  }

  val openNote = openNoteId?.let { id -> state.notes.firstOrNull { it.id == id } }
  if (openNote != null) {
    val close = {
      // An empty note isn't worth keeping.
      if (openNote.body.isBlank()) onIntent(CalculatorIntent.DeleteNote(openNote.id))
      openNoteId = null
    }
    BackHandler(onBack = close)
    NoteEditorScreen(openNote, state, onIntent, haptics, onBack = close)
    return
  }

  BackHandler(enabled = tabIndex != Tab.CALCULATOR.ordinal) { tabIndex = Tab.CALCULATOR.ordinal }
  val select: (Int) -> Unit = { haptics.tick(); tabIndex = it }
  val floating = !wide && state.settings.navStyle == NavStyle.FLOATING
  LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

  // Physical keyboard: calculator typing on the Calculator tab, Ctrl shortcuts everywhere.
  val onKey: (KeyEvent) -> Boolean = key@{ e ->
    if (e.type != KeyEventType.KeyDown) return@key false
    if (e.isCtrlPressed) {
      return@key when (e.key) {
        Key.One -> { select(0); true }
        Key.Two -> { select(1); true }
        Key.Three -> { select(2); true }
        Key.Four -> { select(3); true }
        Key.Comma -> { openSettings(); true }
        Key.E -> { tabIndex = Tab.TOOLS.ordinal; toolsEditRequest = true; true }
        Key.H -> { tabIndex = Tab.CALCULATOR.ordinal; historyOpen = true; true }
        Key.Backspace -> if (tabIndex == Tab.CALCULATOR.ordinal) { onIntent(CalculatorIntent.ClearAll); true } else false
        Key.C -> if (tabIndex == Tab.CALCULATOR.ordinal) {
          clipboard.setText(AnnotatedString((state.preview ?: state.expression).replace(",", "")))
          true
        } else false
        Key.V -> if (tabIndex == Tab.CALCULATOR.ordinal) {
          clipboard.getText()?.text?.let { onIntent(CalculatorIntent.Paste(it)) }
          true
        } else false
        else -> false
      }
    }
    if (tabIndex != Tab.CALCULATOR.ordinal || historyOpen) return@key false
    when (e.key) {
      Key.Enter, Key.NumPadEnter -> { onIntent(CalculatorIntent.Evaluate); true }
      Key.Backspace -> { onIntent(CalculatorIntent.Backspace); true }
      Key.Delete, Key.Escape -> { onIntent(CalculatorIntent.ClearAll); true }
      Key.DirectionUp -> {
        if (state.history.isNotEmpty()) {
          historyIdx = (historyIdx + 1).coerceAtMost(state.history.lastIndex)
          onIntent(CalculatorIntent.UseHistory(state.history[historyIdx]))
        }
        true
      }
      Key.DirectionDown -> {
        if (historyIdx > 0 && state.history.isNotEmpty()) {
          historyIdx--
          onIntent(CalculatorIntent.UseHistory(state.history[historyIdx.coerceAtMost(state.history.lastIndex)]))
        } else { historyIdx = -1; onIntent(CalculatorIntent.ClearAll) }
        true
      }
      else -> {
        val ch = e.utf16CodePoint.toChar()
        when {
          e.isAltPressed || e.isMetaPressed -> false
          ch in "0123456789.+-*/^()%" -> { onIntent(CalculatorIntent.Input(ch.toString())); true }
          ch == '=' -> { onIntent(CalculatorIntent.Evaluate); true }
          ch == 'x' || ch == 'X' -> { onIntent(CalculatorIntent.Input("*")); true }
          ch == 'm' || ch == 'M' -> { onIntent(CalculatorIntent.MemoryAdd); true }
          ch == 'r' || ch == 'R' -> { onIntent(CalculatorIntent.MemoryRecall); true }
          else -> false
        }
      }
    }
  }

  Scaffold(
    modifier = Modifier.focusRequester(focusRequester).focusable().onPreviewKeyEvent(onKey),
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = {
      if (!wide && !floating) {
        ShortNavigationBar {
          Tab.entries.forEachIndexed { i, tab ->
            ShortNavigationBarItem(
              selected = tabIndex == i,
              onClick = { select(i) },
              icon = { Icon(tab.icon, contentDescription = null) },
              label = { Text(tab.label) },
              modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
            )
          }
        }
      }
    },
  ) { padding ->
    Row(Modifier.fillMaxSize().padding(padding)) {
      if (wide) {
        NavigationRail {
          Tab.entries.forEachIndexed { i, tab ->
            NavigationRailItem(
              selected = tabIndex == i,
              onClick = { select(i) },
              icon = { Icon(tab.icon, contentDescription = null) },
              label = { Text(tab.label) },
            )
          }
        }
      }
      Box(Modifier.fillMaxSize()) {
        Box(
          Modifier.fillMaxSize().padding(bottom = if (floating) 92.dp else 0.dp).pointerInput(tabIndex) {
            var dx = 0f
            detectHorizontalDragGestures(onDragStart = { dx = 0f }, onDragEnd = {
              if (dx < -200f && tabIndex < Tab.entries.lastIndex) select(tabIndex + 1)
              else if (dx > 200f && tabIndex > 0) select(tabIndex - 1)
            }) { _, d -> dx += d }
          },
        ) {
          AnimatedContent(
            targetState = tabIndex,
            transitionSpec = {
              if (reduced) EnterTransition.None togetherWith ExitTransition.None
              else {
                val dir = if (targetState > initialState) 1 else -1
                (fadeIn(tween(240, delayMillis = 80)) + slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { dir * it / 12 }) togetherWith
                  (fadeOut(tween(80)) + slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -dir * it / 12 })
              }
            },
            label = "tabs",
          ) { index ->
            when (Tab.entries[index]) {
              Tab.CALCULATOR -> CalculatorScreen(state, onIntent, haptics, openSettings, historyOpen) { historyOpen = it }
              Tab.CONVERTER -> ConverterScreen(state, onIntent, haptics, openSettings)
              Tab.TOOLS -> ToolsScreen(
                state, haptics, openSettings,
                update = { f -> onIntent(CalculatorIntent.UpdateSettings(f)) },
                editRequested = toolsEditRequest, onEditConsumed = { toolsEditRequest = false },
              )
              Tab.NOTES -> NotesListScreen(
                notes = state.notes,
                haptics = haptics,
                onOpen = { openNoteId = it },
                onCreate = { body ->
                  val id = System.currentTimeMillis()
                  onIntent(CalculatorIntent.CreateNote(id, body))
                  openNoteId = id
                },
                onDelete = { onIntent(CalculatorIntent.DeleteNote(it.id)) },
                openSettings = openSettings,
              )
            }
          }
        }
        if (floating) FloatingNavBar(
          selected = tabIndex, labels = state.settings.navLabels, onSelect = select, glassEffects = state.settings.glassEffects,
          modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
        )
      }
    }
  }
}

/** iOS-style floating pill: rounded, elevated, with a highlighted capsule behind the selected tab. */
@Composable
private fun FloatingNavBar(selected: Int, labels: NavLabels, onSelect: (Int) -> Unit, glassEffects: Boolean, modifier: Modifier = Modifier) {
  val shape = RoundedCornerShape(36.dp)
  Surface(
    modifier = if (glassEffects) modifier.glass(shape) else modifier,
    shape = shape,
    color = if (glassEffects) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
    border = if (glassEffects) GlassBorder else null,
    shadowElevation = 12.dp,
  ) {
    Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      Tab.entries.forEachIndexed { i, tab ->
        val on = i == selected
        val showLabel = when (labels) {
          NavLabels.ALL -> true
          NavLabels.SELECTED -> on
          NavLabels.NONE -> false
        }
        Surface(
          onClick = { onSelect(i) },
          shape = RoundedCornerShape(30.dp),
          color = animateColorAsState(if (on) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent, tween(220), label = "pill").value,
          modifier = Modifier
            .handCursor()
            .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
            .semantics { role = Role.Tab; this.selected = on; contentDescription = tab.label }
            .testTag("nav_tab_${tab.name.lowercase()}"),
        ) {
          Column(
            Modifier.height(60.dp).padding(horizontal = 16.dp).animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            val tint = if (on) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            Icon(tab.icon, contentDescription = null, tint = tint)
            if (showLabel) Text(tab.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, color = tint)
          }
        }
      }
    }
  }
}
