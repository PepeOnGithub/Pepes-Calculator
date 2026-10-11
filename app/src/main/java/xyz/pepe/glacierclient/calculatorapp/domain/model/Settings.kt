package xyz.pepe.glacierclient.calculatorapp.domain.model

import xyz.pepe.glacierclient.calculatorapp.domain.NumberStyle

/** Bottom navigation: the classic bar, or a floating pill (same options as Pepe's Clock / Notes). */
enum class NavStyle { FLOATING, CLASSIC }

/** Which tabs of the floating bar show their text label. */
enum class NavLabels { SELECTED, ALL, NONE }

enum class AngleUnit { DEG, RAD }

data class AppSettings(
  // Calculation
  val angleUnit: AngleUnit = AngleUnit.DEG,
  /** null = automatic (up to 10 places, trailing zeros dropped). */
  val decimals: Int? = null,
  val grouping: Boolean = true,
  /** Show decimal answers also as a fraction ("0.75" = 3/4). */
  val showFractions: Boolean = false,
  /** Show the MC / MR / M+ / M− row above the keypad. */
  val memoryKeys: Boolean = true,
  // Math Notes
  /** Off: only lines ending in "=" are solved (like iPad). On: every line with math is. */
  val solveWithoutEquals: Boolean = false,
  // Converter
  val autoUpdateRates: Boolean = true,
  // Appearance
  val wallpaperColors: Boolean = false,
  val haptics: Boolean = true,
  val navStyle: NavStyle = NavStyle.CLASSIC,
  val navLabels: NavLabels = NavLabels.SELECTED,
  /** Translucent floating nav bar; off uses a solid card. */
  val glassEffects: Boolean = true,
  /** A click sound on every key press. */
  val keySounds: Boolean = false,
  /** Bigger digits in the calculator display. */
  val largeDisplay: Boolean = false,
  // Tools
  val tipPercent: Int = 15,
  val taxPercent: Int = 10,
  /** The Tools tab's arrangement, see [xyz.pepe.glacierclient.calculatorapp.domain.ToolsLayout]. Empty = default. */
  val toolsLayout: String = "",
  // Data
  val keepHistory: Boolean = true,
) {
  val useRadians get() = angleUnit == AngleUnit.RAD
  val numberStyle get() = NumberStyle(decimals, grouping)
}

data class HistoryEntry(val expression: String, val result: String, val at: Long = System.currentTimeMillis(), val pinned: Boolean = false)

data class MathNote(val id: Long, val body: String, val updatedAt: Long) {
  val title: String get() = body.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }?.take(60) ?: "New note"
}

/** Exchange rates as units-per-USD, with where and when they came from. */
data class CurrencyRates(val perUsd: Map<String, Double>, val date: String, val fetchedAt: Long, val live: Boolean)
