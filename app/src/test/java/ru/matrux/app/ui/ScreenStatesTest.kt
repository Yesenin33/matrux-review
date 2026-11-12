package ru.matrux.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.matrux.app.billing.Plan
import ru.matrux.app.billing.Terms
import ru.matrux.app.billing.PlanOffer
import ru.matrux.app.data.Catalog
import ru.matrux.app.data.Failure
import ru.matrux.app.data.ImageInfo
import ru.matrux.app.data.Manifest
import ru.matrux.app.data.SubscriptionStatus
import ru.matrux.app.data.Tier
import ru.matrux.app.shell.LaunchState
import ru.matrux.app.shell.LaunchStep
import ru.matrux.app.ui.state.SystemAction
import ru.matrux.app.ui.state.CatalogUi
import ru.matrux.app.ui.state.LaunchStepState
import ru.matrux.app.ui.state.Load
import ru.matrux.app.ui.state.SubLine
import ru.matrux.app.ui.state.SubscriptionUi
import ru.matrux.app.ui.state.actions
import ru.matrux.app.ui.state.catalogImageSha
import ru.matrux.app.ui.state.catalogUi
import ru.matrux.app.ui.state.launchFraction
import ru.matrux.app.ui.state.launchSteps
import ru.matrux.app.ui.state.subLine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ScreenStatesTest {
    private val zone = ZoneOffset.UTC
    private val alpine = ImageInfo("alpine", "Alpine", "alpine", "none", 25_000_000, "a".repeat(64), 512, false, "/a")
    private val debian = ImageInfo("debian-12", "Debian 12", "debian", "xfce", 300_000_000, "b".repeat(64), 2048, true, "/d")
    private val catalog = Load.Ready(Catalog(Manifest(1, "t", listOf(alpine, debian)), offline = false))
    private val pro = SubscriptionStatus(Tier.PRO, Instant.parse("2026-12-15T10:00:00Z"), Instant.parse("2026-11-08T10:00:00Z"))

    @Test
    fun subscriptionLines() {
        assertEquals(SubLine.Free, subLine(SubscriptionStatus.FREE, null, zone))
        assertEquals(SubLine.Revoked, subLine(pro, Failure.NoAccess, zone))
        assertEquals(SubLine.Offline(null), subLine(null, Failure.NoConnection, zone))
        assertEquals(SubLine.Offline(LocalDate.of(2026, 11, 8)), subLine(SubscriptionStatus.FREE.copy(checkedAt = Instant.parse("2026-11-08T10:00:00Z")), Failure.NoConnection, zone))
        assertEquals(SubLine.ProUntil(LocalDate.of(2026, 12, 15)), subLine(pro, Failure.NoConnection, zone))
        assertEquals(SubLine.Unknown, subLine(null, null, zone))
    }

    @Test
    fun expiredProLocksImmediatelyWithoutGrace() {
        val expired = SubscriptionStatus(Tier.PRO, Instant.parse("2026-12-15T10:00:00Z"), Instant.parse("2026-12-01T00:00:00Z"))
        val beforeEnd = Instant.parse("2026-12-14T00:00:00Z").toEpochMilli()
        val afterEnd = Instant.parse("2026-12-15T11:00:00Z").toEpochMilli()
        assertEquals(SubLine.ProUntil(LocalDate.of(2026, 12, 15)), subLine(expired, Failure.NoConnection, zone, beforeEnd))
        assertEquals(SubLine.Free, subLine(expired, Failure.NoConnection, zone, afterEnd))
        assertEquals(SubLine.Free, subLine(expired, null, zone, afterEnd))
        val locked = catalogUi(catalog, expired, null, null, null, now = afterEnd) as CatalogUi.Ready
        assertEquals(listOf(false, true), locked.rows.map { it.locked })
        assertEquals(SystemAction.SUBSCRIBE, locked.rows[1].actions().first())
        val open = catalogUi(catalog, expired, null, null, null, now = beforeEnd) as CatalogUi.Ready
        assertEquals(listOf(false, false), open.rows.map { it.locked })
        assertTrue(expired.endsAt(afterEnd))
        assertFalse(expired.endsAt(beforeEnd))
        assertEquals(0L, expired.daysLeft(afterEnd)?.coerceAtLeast(0L))
        assertEquals(3L, expired.daysLeft(Instant.parse("2026-12-12T10:00:00Z").toEpochMilli()))
        assertFalse(expired.activeAt(afterEnd))
    }

    @Test
    fun catalogLocksProWithoutSubscription() {
        val ui = catalogUi(catalog, SubscriptionStatus.FREE, null, null, null) as CatalogUi.Ready
        assertEquals(listOf(false, true), ui.rows.map { it.locked })
        assertEquals(SystemAction.INSTALL, ui.rows[0].actions().first())
        assertEquals(SystemAction.SUBSCRIBE, ui.rows[1].actions().first())
        assertEquals(listOf(null, null), ui.rows.map { it.estimateSeconds })
    }

    @Test
    fun catalogUnlocksWithPro() {
        val ui = catalogUi(catalog, pro, null, "alpine", 5_000_000) as CatalogUi.Ready
        assertEquals(listOf(false, false), ui.rows.map { it.locked })
        assertTrue(ui.rows[0].active)
        assertEquals(SystemAction.INSTALL, ui.rows[1].actions().first())
        assertEquals(120L, ui.rows[1].estimateSeconds)
    }

    @Test
    fun catalogOffersSwitchForAnotherInstalledSystem() {
        val ui = catalogUi(catalog, pro, null, "alpine", null, mapOf("debian-12" to "b".repeat(64))) as CatalogUi.Ready
        val row = ui.rows[1]
        assertTrue(row.installed)
        assertEquals(false, row.active)
        assertEquals(false, row.outdated)
        assertEquals(SystemAction.SWITCH, row.actions().first())
        assertEquals(true, row.switchable)
        assertEquals(false, row.reinstallable)
        assertEquals(SystemAction.REINSTALL, ui.rows[0].actions().first())
    }

    @Test
    fun catalogOutdatedInstalledKeepsSwitchAndReinstall() {
        val ui = catalogUi(catalog, pro, null, "alpine", null, mapOf("debian-12" to "0".repeat(64))) as CatalogUi.Ready
        val row = ui.rows[1]
        assertTrue(row.installed)
        assertTrue(row.outdated)
        assertEquals(listOf(SystemAction.SWITCH, SystemAction.REINSTALL), row.actions())
        assertEquals(true, row.reinstallable)
    }

    @Test
    fun catalogActiveRowOffersReinstall() {
        val ui = catalogUi(catalog, pro, null, "debian-12", null, mapOf("debian-12" to "b".repeat(64))) as CatalogUi.Ready
        val row = ui.rows[1]
        assertTrue(row.active)
        assertEquals(listOf(SystemAction.LAUNCH, SystemAction.REINSTALL), row.actions())
        assertEquals(listOf(SystemAction.OPEN, SystemAction.STOP), row.copy(running = true).actions())
        assertEquals(false, row.switchable)
        assertEquals(true, row.reinstallable)
    }

    @Test
    fun catalogRelocksWhenRevoked() {
        val ui = catalogUi(catalog, pro, Failure.NoAccess, null, null) as CatalogUi.Ready
        assertTrue(ui.revoked)
        assertTrue(ui.rows[1].locked)
    }

    @Test
    fun catalogStates() {
        assertEquals(CatalogUi.Loading, catalogUi(Load.Loading, null, null, null, null))
        assertEquals(CatalogUi.Failed(Failure.BadSignature), catalogUi(Load.Failed(Failure.BadSignature), null, null, null, null))
        assertEquals(CatalogUi.Empty, catalogUi(Load.Ready(Catalog(Manifest(1, "t", emptyList()), false)), null, null, null, null))
    }

    @Test
    fun catalogWithoutFilesStaysInstallable() {
        val ui = catalogUi(catalog, pro, null, "alpine", 5_000_000) as CatalogUi.Ready
        assertEquals(SystemAction.REINSTALL, ui.rows[0].actions().first())
        assertEquals(SystemAction.INSTALL, ui.rows[1].actions().first())
        assertEquals(listOf(false, false), ui.rows.map { it.installed })
    }

    @Test
    fun catalogImageShaComesFromManifest() {
        assertEquals(catalog.value.manifest.images[0].sha256, catalogImageSha(catalog, "alpine"))
        assertEquals(null, catalogImageSha(catalog, "missing"))
        assertEquals(null, catalogImageSha(Load.Loading, "alpine"))
    }

    @Test
    fun launchStepsFollowState() {
        val running = launchSteps(LaunchState.Running(LaunchStep.PROOT)).map { it.second }
        assertEquals(listOf(LaunchStepState.DONE, LaunchStepState.DONE, LaunchStepState.ACTIVE, LaunchStepState.WAITING), running)
        val failed = launchSteps(LaunchState.Failed(LaunchStep.ROOTFS, Failure.NotInstalled)).map { it.second }
        assertEquals(listOf(LaunchStepState.DONE, LaunchStepState.FAILED, LaunchStepState.WAITING, LaunchStepState.WAITING), failed)
        assertEquals(0.5f, launchFraction(LaunchState.Running(LaunchStep.PROOT)))
        assertEquals(1f, launchFraction(LaunchState.Ready(ru.matrux.app.shell.DisplayMode.X11, false)))
    }

    private val offers = listOf(
        PlanOffer(Plan.PRO_MONTH, "palma_pro_month", 349, 0),
        PlanOffer(Plan.PRO_QUARTER, "palma_pro_quarter", 890, 0, 15),
        PlanOffer(Plan.PRO_HALF_YEAR, "palma_pro_half_year", 1466, 0, 30),
    )

    private fun subs(status: SubscriptionStatus?, plan: Plan) = SubscriptionUi(status, SubLine.Unknown, Terms(offers, plan), false, null)

    @Test
    fun subscriptionButtons() {
        assertTrue(subs(SubscriptionStatus.FREE, Plan.PRO_MONTH).canBuy)
        assertTrue(subs(SubscriptionStatus.FREE, Plan.PRO_HALF_YEAR).canBuy)
        assertEquals(false, subs(pro, Plan.PRO_MONTH).canBuy)
        assertEquals(false, subs(pro, Plan.PRO_HALF_YEAR).copy(now = Instant.parse("2026-11-20T00:00:00Z").toEpochMilli()).canBuy)
    }

    @Test
    fun noBillingMeansNoPurchase() {
        assertEquals(false, subs(SubscriptionStatus.FREE, Plan.PRO_MONTH).copy(billingReady = false).canBuy)
        val blank = subs(SubscriptionStatus.FREE, Plan.PRO_MONTH).copy(terms = Terms(listOf(PlanOffer(Plan.PRO_MONTH, "", 349, 0)), Plan.PRO_MONTH))
        assertEquals(false, blank.canBuy)
    }

    @Test
    fun configuredPlansMatchPriceList() {
        val offers = ru.matrux.app.billing.Offers.fromBuildConfig()
        assertEquals(listOf(349L, 890L, 1466L), offers.map { it.priceRub })
        assertEquals(listOf(1, 3, 6), offers.map { it.plan.months })
        assertEquals(listOf(null, null, null), offers.map { it.discountPercent })
    }

    @Test
    fun onlyThreeProPlans() {
        assertEquals(listOf("PRO_MONTH", "PRO_QUARTER", "PRO_HALF_YEAR"), Plan.entries.map { it.name })
    }
}
