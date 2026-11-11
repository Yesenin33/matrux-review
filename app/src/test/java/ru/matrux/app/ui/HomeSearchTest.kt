package ru.matrux.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.matrux.app.learn.AssetLibrary
import ru.matrux.app.ui.screens.matches
import ru.matrux.app.ui.state.homeSections

class HomeSearchTest {
    private val sheets = AssetLibrary.library.sheets
    private val free = homeSections(sheets, paid = false)
    private val pro = homeSections(sheets, paid = true)

    private fun pool(sections: List<ru.matrux.app.ui.state.HomeSection>) =
        sections.first { it.id == "basics" }.rows +
            if (sections.first { it.id == "advanced" }.locked) emptyList()
            else sections.first { it.id == "advanced" }.rows

    @Test
    fun freeSearchSeesOnlyBasics() {
        val rows = pool(free)
        assertEquals(3, rows.size)
        assertTrue(rows.none { it.locked })
        assertTrue(rows.none { it.section.id == "files" })
        assertNull(matches(rows.first { it.section.id == "linux" }, "docker"))
    }

    @Test
    fun searchBySectionTitle() {
        val rows = pool(pro)
        val linux = rows.first { it.section.id == "linux" }
        assertEquals(linux.section.size, matches(linux, "команды"))
        assertEquals(linux.section.size, matches(linux, "КОМАНДЫ"))
    }

    @Test
    fun searchByEntryText() {
        val git = pool(pro).first { it.section.id == "git" }
        val entry = git.section.groups.first().entries.first()
        val needle = entry.title.take(6).lowercase()
        assertTrue((matches(git, needle) ?: 0) > 0)
        assertNull(matches(git, "такого-текста-нет-нигде"))
    }

    @Test
    fun proPoolCoversEverySection() {
        val rows = pool(pro)
        assertEquals(sheets.size, rows.size)
        assertTrue(rows.none { it.locked })
    }
}
