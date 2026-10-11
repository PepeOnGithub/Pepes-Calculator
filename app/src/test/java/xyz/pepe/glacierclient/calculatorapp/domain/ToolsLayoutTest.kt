package xyz.pepe.glacierclient.calculatorapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.pepe.glacierclient.ecosystem.CardSize

class ToolsLayoutTest {
  @Test fun defaultIdsAreUniqueAndValid() {
    val ids = ToolsLayout.IDS
    assertEquals(12, ids.size)
    assertEquals(ids.size, ids.toSet().size)
    assertTrue(ids.all { Regex("[a-z]{1,20}").matches(it) })
  }

  @Test fun blankOrGarbageGivesDefault() {
    assertEquals(ToolsLayout.default(), ToolsLayout.load(null))
    assertEquals(ToolsLayout.default(), ToolsLayout.load(""))
    assertEquals(ToolsLayout.default().layout, ToolsLayout.load("!!not a layout!!").layout)
  }

  @Test fun roundTripKeepsOrderSizeHiddenAndPins() {
    var s = ToolsLayout.default()
    s = ToolsLayout.State(s.layout.moveTo("bmi", 0).toggleSize("tip").setHidden("random", true), s.pinned)
    s = ToolsLayout.togglePin(s, "dates")
    val back = ToolsLayout.load(ToolsLayout.encode(s))
    assertEquals(s, back)
    assertEquals("dates", back.layout.visible.first().id)
    assertTrue("dates" in back.pinned)
  }

  @Test fun newToolsAreAppendedAndRemovedOnesDropped() {
    val saved = "percent:F:0,gone:H:0,tip:H:1|gone,tip"
    val s = ToolsLayout.load(saved)
    assertEquals(ToolsLayout.IDS.toSet(), s.layout.slots.map { it.id }.toSet())
    assertEquals("percent", s.layout.slots.first().id)
    assertEquals(CardSize.FULL, s.layout.slot("percent")?.size)
    assertTrue(s.layout.slot("tip")!!.hidden)
    assertEquals(setOf("tip"), s.pinned)
    assertEquals(CardSize.FULL, s.layout.slot("compound")?.size)
  }

  @Test fun hideUnpinsAndShowAppendsToEnd() {
    var s = ToolsLayout.togglePin(ToolsLayout.default(), "age")
    s = ToolsLayout.hide(s, "age")
    assertFalse("age" in s.pinned)
    assertTrue(s.layout.slot("age")!!.hidden)
    s = ToolsLayout.show(s, "age")
    assertEquals("age", s.layout.visible.last().id)
  }

  @Test fun pinMovesToFrontAndHiddenCannotBePinned() {
    var s = ToolsLayout.togglePin(ToolsLayout.default(), "random")
    assertEquals("random", s.layout.visible.first().id)
    s = ToolsLayout.togglePin(s, "random")
    assertTrue(s.pinned.isEmpty())
    val h = ToolsLayout.hide(ToolsLayout.default(), "tax")
    assertTrue(ToolsLayout.togglePin(h, "tax").pinned.isEmpty())
  }

  @Test fun defaultRowsPairHalves() {
    val rows = ToolsLayout.default().layout.rows()
    assertEquals(ToolsLayout.IDS.size, rows.sumOf { it.size })
    assertEquals(listOf("percent", "tip"), rows[0].map { it.id })
  }
}
