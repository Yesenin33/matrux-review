package ru.matrux.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.matrux.app.billing.Offers
import ru.matrux.app.billing.Plan
import ru.matrux.app.billing.PlanChoice
import ru.matrux.app.billing.PlanOffer
import ru.matrux.app.billing.ServerPlan
import ru.matrux.app.data.ImageInfo
import ru.matrux.app.data.SubscriptionStatus
import ru.matrux.app.data.Tier
import ru.matrux.app.learn.AssetLibrary
import ru.matrux.app.ui.screens.HomeActions
import ru.matrux.app.ui.screens.KnowledgeActions
import ru.matrux.app.ui.state.LockedItem
import ru.matrux.app.ui.state.SubscriptionUi
import ru.matrux.app.ui.state.homeSections
import ru.matrux.app.ui.state.proOffer
import ru.matrux.app.ui.state.subLine
import java.io.File
import java.time.Instant
import java.time.ZoneOffset

class HomeTermsTest {
    private val courses = AssetLibrary.library.courses
    private val sheets = AssetLibrary.library.sheets
    private val zone = ZoneOffset.UTC
    private val alpine = ImageInfo("alpine", "Alpine", "alpine", "none", 25_000_000, "a".repeat(64), 512, false, "/a")
    private val debian = ImageInfo("debian-12", "Debian 12", "debian", "xfce", 300_000_000, "b".repeat(64), 2048, true, "/d")
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
    private val screens = File("src/main/java/ru/matrux/app/ui/screens")

    private fun subs(choice: PlanChoice, status: SubscriptionStatus = SubscriptionStatus.FREE) =
        SubscriptionUi(status, subLine(status, null, zone), choice.terms.value, false, null)

    @Test
    fun homeHasTwoSectionsInOrder() {
        val free = homeSections(sheets, paid = false)
        assertEquals(listOf("basics", "advanced"), free.map { it.id })
        assertEquals(listOf("start", "linux", "terms"), free[0].rows.map { it.section.id })
        assertFalse(free[0].locked)
        assertTrue(free[0].rows.none { it.locked })
        assertEquals(
            listOf("files", "git", "hardware", "packages", "users", "services", "network", "disks", "logs", "scripts"),
            free[1].rows.map { it.section.id },
        )
        assertTrue(free[1].locked)
        assertTrue(free[1].rows.all { it.locked })
    }

    @Test
    fun advancedSectionOpensWithPro() {
        val paid = homeSections(sheets, paid = true)
        assertFalse(paid[0].locked)
        assertTrue(paid[0].rows.none { it.locked })
    }

    @Test
    fun sectionsKeepSheetTitlesFromAssets() {
        val rows = homeSections(sheets, paid = false).flatMap { it.rows }
        assertEquals(13, rows.size)
        assertTrue(rows.all { it.section.title.isNotBlank() })
    }

    @Test
    fun homeScreenHasNoSubscriptionControls() {
        val source = File(screens, "HomeScreen.kt").readText()
        listOf("home_buy", "TermPicker", "home_locked", "home_inside", "home_revoked", "catalog_launch").forEach {
            assertFalse(it, source.contains(it))
        }
        val actions = HomeActions::class.java.declaredFields.map { it.name }.filterNot { it.startsWith("$") }.toSet()
        assertEquals(setOf("onSection", "onSubscribe", "onTab", "dark", "onTheme"), actions)
    }

    @Test
    fun knowledgeShowsCourses() {
        val source = File(screens, "KnowledgeScreen.kt").readText()
        assertTrue(source.contains("CourseCardRow"))
        assertTrue(source.contains("knowledge_title"))
        val actions = KnowledgeActions::class.java.declaredFields.map { it.name }.filterNot { it.startsWith("$") }.toSet()
        assertEquals(setOf("onCourse", "onTab"), actions)
    }

    @Test
    fun catalogShowsLockInsteadOfProBadge() {
        val source = File(screens, "CatalogScreen.kt").readText()
        assertFalse(source.contains("BadgeCheck"))
        assertTrue(source.contains("Lucide.Lock"))
    }

    @Test
    fun subscriptionHasNoOnboardingEntry() {
        val source = File(screens, "SubscriptionScreen.kt").readText()
        assertFalse(source.contains("subs_onboarding"))
        assertFalse(source.contains("onOnboarding"))
        assertTrue(source.contains("TermPicker(ui.terms"))
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
    fun subscriptionKeepsTermValue() {
        val choice = PlanChoice(local)
        choice.applyServer(server)
        choice.select(Plan.PRO_QUARTER)
        assertEquals(Plan.PRO_QUARTER, subs(choice).terms.selected)
        assertEquals(890L, subs(choice).selectedOffer?.priceRub)
        assertEquals(297L, subs(choice).terms.selectedOffer?.perMonthRub)
    }

    @Test
    fun rublesGroupThousands() {
        assertEquals("349", Format.rubles(349))
        assertEquals("1\u00A0466", Format.rubles(1466))
    }

    @Test
    fun lockedWindowListsLiveContent() {
        val offer = proOffer(listOf(alpine, debian), courses, sheets)
        assertEquals(listOf(LockedItem.SYSTEMS, LockedItem.COURSES, LockedItem.OFFLINE), offer.items)
        assertEquals(listOf("Debian 12"), offer.systems)
        assertEquals(courses.map { it.title }, offer.courses)
        val closed = courses.sumOf { it.lessonCount - it.modules.first().lessons.size }
        assertEquals(closed, offer.lessons)
        assertEquals(listOf(LockedItem.COURSES, LockedItem.OFFLINE), proOffer(listOf(alpine), courses, sheets).items)
        assertTrue(offer.items.size in 1..6)
    }
}
