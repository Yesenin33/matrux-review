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
        sections.firstOrNull { it.id == "basics" }?.rows.orEmpty() +
            sections.firstOrNull { it.id == "advanced" }?.let { adv ->
                if (adv.locked) emptyList() else adv.rows
            }.orEmpty()

    @Test
    fun freeSearchSeesOnlyBasics() {
        val rows = pool(free)
        assertEquals(listOf("start", "linux", "terms"), rows.map { it.section.id })
        assertTrue(rows.none { it.locked })
    }

    @Test
    fun searchBySectionTitle() {
        val rows = pool(pro)
        val files = rows.first { it.section.id == "files" }
        assertEquals(files.section.size, matches(files, "файл"))
        assertEquals(files.section.size, matches(files, "ФАЙЛ"))
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
        assertEquals(13, rows.size)
        assertTrue(rows.none { it.locked })
    }
}
