package ru.palma.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.palma.app.billing.Offers
import ru.palma.app.billing.Plan
import ru.palma.app.billing.PlanChoice
import ru.palma.app.billing.PlanOffer
import ru.palma.app.billing.ServerPlan
import ru.palma.app.data.Catalog
import ru.palma.app.data.ImageInfo
import ru.palma.app.data.Manifest
import ru.palma.app.data.SubscriptionStatus
import ru.palma.app.data.Tier
import ru.palma.app.learn.AssetLibrary
import ru.palma.app.learn.Progress
import ru.palma.app.ui.screens.HomeActions
import ru.palma.app.ui.state.HomeBlock
import ru.palma.app.ui.state.Load
import ru.palma.app.ui.state.LockedItem
import ru.palma.app.ui.state.SubscriptionUi
import ru.palma.app.ui.state.homeBlocks
import ru.palma.app.ui.state.homeUi
import ru.palma.app.ui.state.knowledgeUi
import ru.palma.app.ui.state.proOffer
import ru.palma.app.ui.state.subLine
import java.io.File
import java.time.Instant
import java.time.ZoneOffset

class HomeTermsTest {
    private val courses = AssetLibrary.library.courses
    private val sheets = AssetLibrary.library.sheets
    private val zone = ZoneOffset.UTC
    private val alpine = ImageInfo("alpine", "Alpine", "alpine", "none", 25_000_000, "a".repeat(64), 512, false, "/a")
    private val debian = ImageInfo("debian-12", "Debian 12", "debian", "xfce", 300_000_000, "b".repeat(64), 2048, true, "/d")
    private val catalog = Load.Ready(Catalog(Manifest(1, "t", listOf(alpine, debian)), offline = false))
    private val pro = SubscriptionStatus(Tier.PRO, Instant.parse("2026-12-15T10:00:00Z"), null)
    private val local = listOf(
        PlanOffer(Plan.PRO_MONTH, "palma_pro_month", null, 0),
        PlanOffer(Plan.PRO_QUARTER, "palma_pro_quarter", null, 0),
        PlanOffer(Plan.PRO_HALF_YEAR, "palma_pro_half_year", null, 0),
    )
    private val server = listOf(
        ServerPlan("palma_pro_month", 1, 349, null),
        ServerPlan("palma_pro_quarter", 3, 890, 15),
        ServerPlan("palma_pro_half_year", 6, 1466, 30),
    )

    private fun subs(choice: PlanChoice, status: SubscriptionStatus = SubscriptionStatus.FREE) =
        SubscriptionUi(status, subLine(status, null, zone), choice.terms.value, false, null)

    private fun home(choice: PlanChoice, status: SubscriptionStatus = SubscriptionStatus.FREE) =
        homeUi(catalog, status, null, choice.terms.value, courses, sheets, zone)

    @Test
    fun homeAndSubscriptionShareOneTermValue() {
        val choice = PlanChoice(local)
        choice.applyServer(server)
        assertSame(home(choice).terms, subs(choice).terms)
        choice.select(Plan.PRO_QUARTER)
        assertEquals(Plan.PRO_QUARTER, home(choice).terms.selected)
        assertEquals(Plan.PRO_QUARTER, subs(choice).terms.selected)
        assertEquals(890L, subs(choice).selectedOffer?.priceRub)
        assertEquals(297L, home(choice).terms.selectedOffer?.perMonthRub)
        choice.select(Plan.PRO_HALF_YEAR)
        assertEquals(home(choice).terms, subs(choice).terms)
        assertEquals(244L, subs(choice).selectedOffer?.perMonthRub)
    }

    @Test
    fun bothScreensDrawTheSameTermComponent() {
        val screens = File("src/main/java/ru/palma/app/ui/screens")
        listOf("HomeScreen.kt", "SubscriptionScreen.kt").forEach { name ->
            val source = File(screens, name).readText()
            assertTrue(name, source.contains("TermPicker(ui.terms"))
        }
        val all = listOf("HomeScreen.kt", "SubscriptionScreen.kt").joinToString("\n") { File(screens, it).readText() }
        assertFalse(all.contains("fun ProTerm"))
        assertFalse(all.contains("Role.RadioButton"))
    }

    @Test
    fun discountsComeOnlyFromServer() {
        val choice = PlanChoice(local)
        assertEquals(listOf(null, null, null), choice.terms.value.offers.map { it.discountPercent })
        assertEquals(listOf(null, null, null), choice.terms.value.offers.map { it.priceRub })
        choice.applyServer(server)
        assertEquals(listOf(null, 15, 30), choice.terms.value.offers.map { it.discountPercent })
        assertEquals(listOf(349L, 890L, 1466L), choice.terms.value.offers.map { it.priceRub })
        val flat = Offers.merge(local, server.map { it.copy(discountPercent = 0) })
        assertEquals(listOf(null, null, null), flat.map { it.discountPercent })
    }

    @Test
    fun rublesGroupThousands() {
        assertEquals("349", Format.rubles(349))
        assertEquals("1 466", Format.rubles(1466))
    }

    @Test
    fun homeHasNoSystemControls() {
        val free = home(PlanChoice(local))
        assertEquals(listOf(HomeBlock.BUY, HomeBlock.TERMS, HomeBlock.INSIDE, HomeBlock.LOCKED), homeBlocks(free))
        val paid = home(PlanChoice(local), pro)
        assertEquals(listOf(HomeBlock.STATUS, HomeBlock.INSIDE), homeBlocks(paid))
        val actions = HomeActions::class.java.declaredFields.map { it.name }.filterNot { it.startsWith("$") }.toSet()
        assertEquals(setOf("onBuy", "onSelect", "onLocked", "onReRegister", "onTab"), actions)
        val source = File("src/main/java/ru/palma/app/ui/screens/HomeScreen.kt").readText()
        listOf("catalog_launch", "catalog_stop", "catalog_switch", "catalog_reinstall", "home_launch", "home_return").forEach {
            assertFalse(it, source.contains(it))
        }
    }

    @Test
    fun homeInsideUsesLiveCatalogAndKnowledge() {
        val ui = home(PlanChoice(local))
        assertEquals(listOf("Alpine", "Debian 12"), ui.systems.map { it.name })
        assertEquals(courses.size, ui.courses.size)
        assertEquals(courses.sumOf { it.lessonCount }, ui.lessons)
        assertEquals(sheets.size, ui.sheets)
        assertEquals(emptyList<ImageInfo>(), homeUi(Load.Loading, null, null, PlanChoice(local).terms.value, courses, sheets, zone).systems)
    }

    @Test
    fun knowledgeLocksCoursesButNotSheetsWithoutPro() {
        val free = knowledgeUi(courses, sheets, paid = false, progress = Progress())
        assertTrue(free.courses.all { it.locked })
        assertTrue(free.rows.none { it.locked })
        assertTrue(free.offerOnEnter)
        assertFalse(knowledgeUi(courses, sheets, paid = false, progress = Progress(offerSeen = true)).offerOnEnter)
        val paid = knowledgeUi(courses, sheets, paid = true, progress = Progress())
        assertTrue(paid.courses.none { it.locked })
        assertTrue(paid.rows.none { it.locked })
        assertFalse(paid.offerOnEnter)
    }

    @Test
    fun lockedWindowListsLiveContent() {
        val offer = proOffer(listOf(alpine, debian), courses)
        assertEquals(listOf(LockedItem.SYSTEMS, LockedItem.COURSES, LockedItem.OFFLINE), offer.items)
        assertEquals(listOf("Debian 12"), offer.systems)
        assertEquals(courses.map { it.title }, offer.courses)
        val closed = courses.sumOf { it.lessonCount - it.modules.first().lessons.size }
        assertEquals(closed, offer.lessons)
        assertEquals(listOf(LockedItem.COURSES, LockedItem.OFFLINE), proOffer(listOf(alpine), courses).items)
        assertTrue(offer.items.size in 1..6)
    }
}
