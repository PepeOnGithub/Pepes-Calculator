@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package xyz.pepe.glacierclient.calculatorapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import xyz.pepe.glacierclient.calculatorapp.domain.ToolsLayout
import xyz.pepe.glacierclient.calculatorapp.ui.components.BadgeShape
import xyz.pepe.glacierclient.calculatorapp.ui.components.Haptics
import xyz.pepe.glacierclient.calculatorapp.ui.components.ShapeBadge
import xyz.pepe.glacierclient.calculatorapp.ui.components.handCursor
import xyz.pepe.glacierclient.calculatorapp.ui.components.rowIconColors
import xyz.pepe.glacierclient.ecosystem.CardSize
import xyz.pepe.glacierclient.ecosystem.CardSlot

/** What a tile shows: [short] in compact (HALF) tiles, [title] + [summary] in wide (FULL) tiles. */
internal class TileInfo(
  val id: String,
  val title: String,
  val short: String,
  val summary: String,
  val icon: ImageVector,
  val colorIndex: Int,
  val shape: BadgeShape,
)

/** Live drag bookkeeping, shared by all tiles of one grid. */
@Stable
private class DragState {
  var id by mutableStateOf<String?>(null)
  var start by mutableStateOf(Offset.Zero)
  var total by mutableStateOf(Offset.Zero)
  var lastSwap = Offset.Zero
  var lastTranslation = Offset.Zero
  val bounds = HashMap<String, Rect>()
}

/**
 * The Tools tab grid: HALF tiles share a row, FULL tiles span it (rows come from the shared DashboardLayout).
 * In [editing] mode tiles get chrome: outline, tap-to-select with size handle and actions, long-press-drag to
 * reorder, Alt+Up/Down for keyboard users, and an "Available tools" tray for hidden ones.
 * [onChange] receives every new state; `persist` is false while a drag is still in progress.
 */
@Composable
internal fun ToolsGrid(
  tiles: Map<String, TileInfo>,
  state: ToolsLayout.State,
  editing: Boolean,
  haptics: Haptics,
  reduced: Boolean,
  onOpen: (String) -> Unit,
  onChange: (ToolsLayout.State, Boolean) -> Unit,
) {
  val cur by rememberUpdatedState(state)
  val d = remember { DragState() }
  var selected by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(editing) { if (!editing) { selected = null; d.id = null } }
  val density = LocalDensity.current
  val swapGap = with(density) { 12.dp.toPx() }
  val gap = 12.dp
  val rows = state.layout.rows().map { r -> r.filter { tiles.containsKey(it.id) } }.filter { it.isNotEmpty() }
  val visibleIds = state.layout.visible.map { it.id }

  fun change(s: ToolsLayout.State, persist: Boolean = true) = onChange(s, persist)

  fun maybeReorder(id: String) {
    val me = d.bounds[id] ?: return
    if ((d.total - d.lastSwap).getDistance() < swapGap) return
    val center = d.start + d.total + Offset(me.width / 2f, me.height / 2f)
    val vis = cur.layout.visible
    val target = vis.firstOrNull { it.id != id && d.bounds[it.id]?.contains(center) == true } ?: return
    val idx = vis.indexOfFirst { it.id == target.id }
    d.lastSwap = d.total
    change(ToolsLayout.State(cur.layout.moveTo(id, idx), cur.pinned), persist = false)
  }

  Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
      val full = maxWidth
      TileFlow(rows.map { it.size }, gap) {
        rows.forEach { row ->
          row.forEach { slot ->
            val info = tiles.getValue(slot.id)
            key(slot.id) {
              val index = visibleIds.indexOf(slot.id)
              ToolTile(
                info = info, slot = slot, pinned = slot.id in state.pinned,
                targetWidth = (full - gap * (row.size - 1)) / row.size,
                editing = editing, selected = selected == slot.id,
                canUp = index > 0, canDown = index < visibleIds.lastIndex,
                d = d, reduced = reduced,
                onOpen = { haptics.tick(); onOpen(slot.id) },
                onSelect = { selected = if (selected == slot.id) null else slot.id },
                onMove = { delta -> change(ToolsLayout.State(cur.layout.moveBy(slot.id, delta), cur.pinned)); haptics.tick() },
                onPin = { haptics.tick(); change(ToolsLayout.togglePin(cur, slot.id)) },
                onHide = { haptics.confirm(); selected = null; change(ToolsLayout.hide(cur, slot.id)) },
                onToggleSize = { haptics.tick(); change(ToolsLayout.State(cur.layout.toggleSize(slot.id), cur.pinned)) },
                onDragStart = {
                  d.id = slot.id; d.start = d.bounds[slot.id]?.topLeft ?: Offset.Zero
                  d.total = Offset.Zero; d.lastSwap = Offset.Zero; selected = slot.id; haptics.tick()
                },
                onDrag = { amount -> d.total += amount; maybeReorder(slot.id) },
                onDragEnd = { if (d.id != null) { d.id = null; haptics.confirm(); change(cur) } },
              )
            }
          }
        }
      }
    }
    if (editing) {
      AvailableTray(state.layout.hiddenSlots.filter { tiles.containsKey(it.id) }, tiles, reduced) { id ->
        haptics.tick(); change(ToolsLayout.show(cur, id))
      }
    }
  }
}

/** Lays children out in rows of the given counts; every child sizes itself (width), rows share the tallest height. */
@Composable
private fun TileFlow(rowCounts: List<Int>, gap: Dp, content: @Composable () -> Unit) {
  Layout(content, Modifier.fillMaxWidth()) { measurables, constraints ->
    val g = gap.roundToPx()
    val loose = Constraints(maxWidth = constraints.maxWidth)
    var i = 0
    var y = 0
    val placed = ArrayList<Triple<androidx.compose.ui.layout.Placeable, Int, Int>>()
    for ((r, count) in rowCounts.withIndex()) {
      val group = measurables.subList(i, i + count)
      i += count
      val first = group.map { it.measure(loose) }
      val rowH = first.maxOf { it.height }
      // A Measurable may only be measured once per pass, so tiles are measured once and top-aligned.
      var x = 0
      for (p in first) { placed += Triple(p, x, y); x += p.width + g }
      y += rowH + if (r < rowCounts.lastIndex) g else 0
    }
    layout(constraints.maxWidth, y) { placed.forEach { (p, x, py) -> p.place(x, py) } }
  }
}

@Composable
private fun ToolTile(
  info: TileInfo,
  slot: CardSlot,
  pinned: Boolean,
  targetWidth: Dp,
  editing: Boolean,
  selected: Boolean,
  canUp: Boolean,
  canDown: Boolean,
  d: DragState,
  reduced: Boolean,
  onOpen: () -> Unit,
  onSelect: () -> Unit,
  onMove: (Int) -> Unit,
  onPin: () -> Unit,
  onHide: () -> Unit,
  onToggleSize: () -> Unit,
  onDragStart: () -> Unit,
  onDrag: (Offset) -> Unit,
  onDragEnd: () -> Unit,
) {
  val cs = MaterialTheme.colorScheme
  val scope = rememberCoroutineScope()
  val dragging = d.id == slot.id
  val isFull = slot.size == CardSize.FULL
  val width by animateDpAsState(targetWidth, if (reduced) snap() else spring(dampingRatio = 0.85f, stiffness = 450f), label = "tileWidth")
  val chrome by animateFloatAsState(if (editing) 1f else 0f, if (reduced) snap() else tween(200), label = "chrome")
  val off = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
  val lastPos = remember { arrayOfNulls<Offset>(1) }
  val jump = with(LocalDensity.current) { 40.dp.toPx() }
  var wasDragging by remember { mutableStateOf(false) }
  LaunchedEffect(dragging) {
    if (dragging) off.snapTo(Offset.Zero)
    else if (wasDragging) {
      if (reduced) off.snapTo(Offset.Zero)
      else { off.snapTo(d.lastTranslation); off.animateTo(Offset.Zero, spring(dampingRatio = 0.8f, stiffness = 400f)) }
    }
    wasDragging = dragging
  }
  val onDragStartS by rememberUpdatedState(onDragStart)
  val onDragS by rememberUpdatedState(onDrag)
  val onDragEndS by rememberUpdatedState(onDragEnd)
  val onMoveS by rememberUpdatedState(onMove)
  val shape = RoundedCornerShape(24.dp)
  val container = if (pinned) lerp(cs.surfaceContainerHigh, cs.primaryContainer, 0.7f) else cs.surfaceContainerHigh
  val borderColor = (if (selected) cs.primary else cs.outline).copy(alpha = chrome * (if (selected) 1f else 0.6f))
  val colors = rowIconColors(info.colorIndex)

  Box(
    Modifier
      .onPlaced { lc ->
        val p = lc.positionInParent()
        d.bounds[slot.id] = Rect(p, lc.size.toSize())
        val prev = lastPos[0]
        lastPos[0] = p
        if (prev != null && !reduced && d.id != slot.id && (p - prev).getDistance() > jump) {
          // The tile jumped to another slot: keep it where it was and glide to the new place.
          scope.launch(start = CoroutineStart.UNDISPATCHED) {
            off.snapTo(off.value - (p - prev))
            off.animateTo(Offset.Zero, spring(dampingRatio = 0.8f, stiffness = 380f))
          }
        }
      }
      .zIndex(if (dragging) 1f else 0f)
      .graphicsLayer {
        var t = off.value
        if (dragging) {
          t = d.start + d.total - (d.bounds[slot.id]?.topLeft ?: d.start)
          d.lastTranslation = t
        }
        translationX = t.x
        translationY = t.y
        val s = if (dragging) 1.04f else 1f
        scaleX = s
        scaleY = s
        alpha = if (dragging) 0.94f else 1f
      }
      .width(width)
      .onKeyEvent { ev ->
        if (editing && ev.type == KeyEventType.KeyDown && ev.isAltPressed) {
          when (ev.key) {
            Key.DirectionUp -> { onMoveS(-1); true }
            Key.DirectionDown -> { onMoveS(1); true }
            else -> false
          }
        } else false
      }
      .clip(shape)
      .background(container)
      .border(BorderStroke(if (selected) 2.dp else 1.dp, borderColor), shape)
      .then(
        if (editing) Modifier
          .clickable(onClickLabel = "Select ${info.short}") { onSelect() }
          .pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
              onDragStart = { onDragStartS() },
              onDrag = { change, amount -> change.consume(); onDragS(amount) },
              onDragEnd = { onDragEndS() },
              onDragCancel = { onDragEndS() },
            )
          }
        else Modifier.handCursor().clickable(onClickLabel = "Open ${info.title}", onClick = onOpen),
      ),
  ) {
    Column(Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = if (editing && selected) 32.dp else 12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 2.dp)) {
        ShapeBadge(info.icon, shape = info.shape, size = 40.dp, container = colors.first, content = colors.second)
        Column(Modifier.weight(1f)) {
          Text(
            if (isFull) info.title else info.short,
            style = if (isFull) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
            maxLines = if (isFull) 1 else 2, overflow = TextOverflow.Ellipsis,
          )
          if (isFull) Text(info.summary, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (pinned && !(editing && selected)) Icon(Icons.Rounded.Star, contentDescription = "Pinned", tint = cs.primary, modifier = Modifier.size(16.dp))
      }
      AnimatedVisibility(
        visible = editing && selected,
        enter = if (reduced) EnterTransition.None else fadeIn(tween(180)) + expandVertically(),
        exit = if (reduced) ExitTransition.None else fadeOut(tween(120)) + shrinkVertically(),
      ) {
        FlowRow(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.Center) {
          TileAction(Icons.Rounded.KeyboardArrowUp, "Move ${info.short} earlier", canUp) { onMove(-1) }
          TileAction(Icons.Rounded.KeyboardArrowDown, "Move ${info.short} later", canDown) { onMove(1) }
          TileAction(if (pinned) Icons.Rounded.Star else Icons.Rounded.StarBorder, if (pinned) "Unpin ${info.short}" else "Pin ${info.short} to the top", true, if (pinned) cs.primary else cs.onSurfaceVariant) { onPin() }
          TileAction(Icons.Rounded.VisibilityOff, "Hide ${info.short}", true) { onHide() }
          Icon(Icons.Rounded.DragIndicator, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp).size(20.dp))
        }
      }
    }
    // Right-edge size handle: tap to toggle, or drag left (compact) / right (wide).
    if (editing && selected) {
      Box(
        Modifier
          .align(Alignment.CenterEnd)
          .size(width = 30.dp, height = 64.dp)
          .clickable(role = Role.Button, onClickLabel = if (isFull) "Make ${info.short} compact" else "Make ${info.short} wide") { onToggleSize() }
          .pointerInput(slot.size) {
            var acc = 0f
            val threshold = 28.dp.toPx()
            detectHorizontalDragGestures(
              onDragStart = { acc = 0f },
              onHorizontalDrag = { change, dx ->
                change.consume()
                acc += dx
                if ((acc < -threshold && isFull) || (acc > threshold && !isFull)) { acc = 0f; onToggleSize() }
              },
            )
          },
        contentAlignment = Alignment.Center,
      ) {
        Box(Modifier.size(width = 6.dp, height = 32.dp).clip(RoundedCornerShape(3.dp)).background(cs.primary))
      }
    }
  }
}

@Composable
private fun TileAction(icon: ImageVector, label: String, enabled: Boolean, tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant, onClick: () -> Unit) {
  IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp)) {
    Icon(icon, contentDescription = label, tint = if (enabled) tint else tint.copy(alpha = 0.35f), modifier = Modifier.size(20.dp))
  }
}

/** Hidden tools, each with a + to bring it back. */
@Composable
private fun AvailableTray(hidden: List<CardSlot>, tiles: Map<String, TileInfo>, reduced: Boolean, onAdd: (String) -> Unit) {
  val cs = MaterialTheme.colorScheme
  Column(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(cs.surfaceContainer)
      .animateContentSize(if (reduced) snap() else spring(dampingRatio = 0.85f, stiffness = 500f)).padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text("Available tools", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
    if (hidden.isEmpty()) {
      Text("Every tool is on your page. Hide one with the eye icon and it will wait here.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
    }
    hidden.forEach { slot ->
      val info = tiles.getValue(slot.id)
      val colors = rowIconColors(info.colorIndex)
      key(slot.id) {
        Row(
          Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClickLabel = "Add ${info.title}") { onAdd(slot.id) }.padding(horizontal = 4.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          ShapeBadge(info.icon, shape = info.shape, size = 36.dp, container = colors.first, content = colors.second)
          Column(Modifier.weight(1f)) {
            Text(info.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(info.summary, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          Icon(Icons.Rounded.Add, contentDescription = "Add ${info.title}", tint = cs.primary)
        }
      }
    }
  }
}
