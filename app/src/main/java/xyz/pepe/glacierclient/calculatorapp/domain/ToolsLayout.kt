package xyz.pepe.glacierclient.calculatorapp.domain

import xyz.pepe.glacierclient.ecosystem.CardSize
import xyz.pepe.glacierclient.ecosystem.CardSlot
import xyz.pepe.glacierclient.ecosystem.DashboardLayout

/**
 * The Tools tab's user-editable arrangement: one tile per tool (stable ids, never renamed), each FULL (2x1) or
 * HALF (1x1), visible or hidden, plus a set of pinned tools. Pure Kotlin so it is unit-testable.
 * Stored as one string: `<DashboardLayout.encode()>|<pinned ids, comma separated>`.
 */
object ToolsLayout {
  /** Default order and sizes. Ids are the lower-case names of the tool screens; add new tools at the END. */
  val DEFAULTS: List<CardSlot> = listOf(
    CardSlot("percent", CardSize.HALF),
    CardSlot("tip", CardSize.HALF),
    CardSlot("tax", CardSize.HALF),
    CardSlot("loan", CardSize.HALF),
    CardSlot("compound", CardSize.FULL),
    CardSlot("bmi", CardSize.HALF),
    CardSlot("age", CardSize.HALF),
    CardSlot("dates", CardSize.FULL),
    CardSlot("bases", CardSize.HALF),
    CardSlot("primes", CardSize.HALF),
    CardSlot("stats", CardSize.HALF),
    CardSlot("random", CardSize.HALF),
  )

  val IDS: List<String> get() = DEFAULTS.map { it.id }

  data class State(val layout: DashboardLayout, val pinned: Set<String>)

  fun default(): State = State(DashboardLayout.of(DEFAULTS), emptySet())

  /** Parses a stored string and reconciles it with [DEFAULTS]; never throws. Unknown/blank input gives the default. */
  fun load(raw: String?): State {
    if (raw.isNullOrBlank()) return default()
    val parts = raw.split('|', limit = 2)
    val layout = DashboardLayout.reconcile(DashboardLayout.decode(parts[0]), DEFAULTS)
    val known = layout.slots.map { it.id }.toSet()
    val pinned = parts.getOrNull(1).orEmpty().split(',').filter { it in known }.toSet()
    return State(layout, pinned)
  }

  fun encode(s: State): String = s.layout.encode() + "|" + s.pinned.joinToString(",")

  /** Pins [id] (and moves it to the front) or unpins it (it keeps its place). */
  fun togglePin(s: State, id: String): State {
    val slot = s.layout.slot(id) ?: return s
    return if (id in s.pinned) s.copy(pinned = s.pinned - id)
    else if (slot.hidden) s
    else State(s.layout.moveTo(id, 0), s.pinned + id)
  }

  /** Hides a tool (it also loses its pin). */
  fun hide(s: State, id: String): State = State(s.layout.setHidden(id, true), s.pinned - id)

  /** Brings a hidden tool back at the end of the page. */
  fun show(s: State, id: String): State = State(s.layout.setHidden(id, false).moveTo(id, Int.MAX_VALUE), s.pinned)
}
