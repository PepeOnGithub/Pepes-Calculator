package xyz.pepe.glacierclient.calculatorapp.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsIndexTest {
  private val sample = listOf(
    SettingEntry("Haptic feedback", "A light tick on keys", SettingsPage.APPEARANCE, listOf("vibration")),
    SettingEntry("Decimal places", "Rounding of answers", SettingsPage.CALCULATION, listOf("precision")),
    SettingEntry("Navigation bar", "Floating pill or classic bar", SettingsPage.APPEARANCE),
  )

  @Test fun blankQueryMatchesNothing() = assertTrue(SettingsIndex.search("  ", sample).isEmpty())

  @Test fun matchesTitleCaseInsensitively() =
    assertEquals(listOf("Haptic feedback"), SettingsIndex.search("HAPTIC", sample).map { it.title })

  @Test fun matchesWordPrefixAndSubstring() {
    assertEquals(listOf("Decimal places"), SettingsIndex.search("dec", sample).map { it.title })
    assertEquals(listOf("Decimal places"), SettingsIndex.search("ounding", sample).map { it.title })
  }

  @Test fun matchesKeywordsDescriptionAndPageName() {
    assertEquals(listOf("Haptic feedback"), SettingsIndex.search("vibrat", sample).map { it.title })
    assertEquals(listOf("Navigation bar"), SettingsIndex.search("pill", sample).map { it.title })
    assertEquals(2, SettingsIndex.search("appearance", sample).size)
  }

  @Test fun allWordsMustMatch() {
    assertEquals(listOf("Haptic feedback"), SettingsIndex.search("light keys", sample).map { it.title })
    assertTrue(SettingsIndex.search("light rounding", sample).isEmpty())
  }

  @Test fun titleMatchesRankFirst() {
    val r = SettingsIndex.search("bar", sample).map { it.title }
    assertEquals("Navigation bar", r.first())
  }

  @Test fun realIndexIsSortedAndFindsSettings() {
    val titles = SettingsIndex.entries.map { it.title.lowercase() }
    assertEquals(titles.sorted(), titles)
    assertTrue(SettingsIndex.search("tip").any { it.title == "Default tip" })
    assertTrue(SettingsIndex.search("zzzzqq").isEmpty())
  }
}
