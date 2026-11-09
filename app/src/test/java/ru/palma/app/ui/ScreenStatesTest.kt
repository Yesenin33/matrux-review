package ru.palma.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.palma.app.billing.Plan
import ru.palma.app.billing.PlanOffer
import ru.palma.app.data.Catalog
import ru.palma.app.data.Failure
import ru.palma.app.data.ImageInfo
import ru.palma.app.data.InstalledSystem
import ru.palma.app.data.Manifest
import ru.palma.app.data.SubscriptionStatus
import ru.palma.app.data.Tier
import ru.palma.app.install.InstallState
import ru.palma.app.session.SessionState
import ru.palma.app.shell.LaunchState
import ru.palma.app.shell.LaunchStep
import ru.palma.app.ui.state.CatalogAction
import ru.palma.app.ui.state.CatalogUi
import ru.palma.app.ui.state.HomeUi
import ru.palma.app.ui.state.LaunchStepState
import ru.palma.app.ui.state.Load
import ru.palma.app.ui.state.SubLine
import ru.palma.app.ui.state.SubscriptionUi
import ru.palma.app.ui.state.action
import ru.palma.app.ui.state.catalogImageSha
import ru.palma.app.ui.state.catalogUi
import ru.palma.app.ui.state.homeUi
import ru.palma.app.ui.state.launchFraction
import ru.palma.app.ui.state.launchSteps
import ru.palma.app.ui.state.subLine
import ru.palma.app.ui.state.suggestion
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ScreenStatesTest {
    private val zone = ZoneOffset.UTC
    private val alpine = ImageInfo("alpine", "Alpine", "alpine", "none", 25_000_000, "a".repeat(64), 512, false, "/a")
    private val debian = ImageInfo("debian-12", "Debian 12", "debian", "xfce", 300_000_000, "b".repeat(64), 2048, true, "/d")
    private val catalog = Load.Ready(Catalog(Manifest(1, "t", listOf(alpine, debian)), offline = false))
    private val pro = SubscriptionStatus(Tier.PRO, Instant.parse("2026-12-15T10:00:00Z"), Instant.parse("2026-11-08T10:00:00Z"))
    private val installed = InstalledSystem("debian-12", "Debian 12", "debian", "xfce", 300_000_000, "b".repeat(64), 12_000)

    private fun home(
        installed: InstalledSystem? = null,
        ready: Boolean = true,
        catalog: Load<Catalog> = this.catalog,
        install: InstallState = InstallState.Idle,
        session: SessionState = SessionState.Stopped,
        status: SubscriptionStatus? = pro,
        failure: Failure? = null,
    ) = homeUi(installed, ready, catalog, install, session, status, failure, 24_100_000_000, zone)

    @Test
    fun homeSuggestsDebianWhenNothingInstalled() {
        val ui = home() as HomeUi.Empty
        assertEquals("Debian 12", ui.suggestion.name)
        assertEquals(SubLine.ProUntil(LocalDate.of(2026, 12, 15)), ui.sub)
    }

    @Test
    fun homeSuggestsInstalledImageWhenOutdated() {
        val outdatedDebian = InstalledSystem("debian-12", "Debian 12", "debian", "xfce", 300_000_000, "b".repeat(64), 12_000)
        assertEquals("Debian 12", (home(installed = outdatedDebian, ready = false) as HomeUi.Empty).suggestion.name)
        val outdatedAlpine = InstalledSystem("alpine", "Alpine", "alpine", "none", 25_000_000, "a".repeat(64), 12_000)
        assertEquals("Alpine", (home(installed = outdatedAlpine, ready = false) as HomeUi.Empty).suggestion.name)
    }

    @Test
    fun suggestionPrefersCurrentImageThenDebian() {
        val images = listOf(alpine, debian)
        assertEquals("alpine", suggestion(images, "alpine")?.id)
        assertEquals("debian-12", suggestion(images)?.id)
        assertEquals(null, suggestion(emptyList(), "alpine"))
    }

    @Test
    fun homeReadyWhenInstalled() {
        val ui = home(installed = installed) as HomeUi.Ready
        assertEquals(false, ui.running)
        assertEquals(true, (home(installed = installed, session = SessionState.Live("D", "xfce", ru.palma.app.shell.DisplayMode.VNC, false)) as HomeUi.Ready).running)
    }

    @Test
    fun homeIgnoresRecordWithoutFiles() {
        assertTrue(home(installed = installed, ready = false) is HomeUi.Empty)
    }

    @Test
    fun homeShowsInstallProgress() {
        val running = InstallState.Running(debian, 150_000_000, 0, 60)
        assertEquals(25, (home(install = running) as HomeUi.Installing).progress.percent)
    }

    @Test
    fun homeLoadingAndErrors() {
        assertTrue(home(catalog = Load.Loading) is HomeUi.Loading)
        assertEquals(Failure.NoConnection, (home(catalog = Load.Failed(Failure.NoConnection)) as HomeUi.NoCatalog).failure)
        val empty = Load.Ready(Catalog(Manifest(1, "t", emptyList()), false))
        assertTrue(home(catalog = empty) is HomeUi.NoCatalog)
    }

    @Test
    fun homeRevokedWins() {
        assertTrue(home(installed = installed, failure = Failure.NoAccess) is HomeUi.Revoked)
    }

    @Test
    fun subscriptionLines() {
        assertEquals(SubLine.Free, subLine(SubscriptionStatus.FREE, null, zone))
        assertEquals(SubLine.Lifetime, subLine(SubscriptionStatus(Tier.LIFETIME, null, null), null, zone))
        assertEquals(SubLine.Revoked, subLine(pro, Failure.NoAccess, zone))
        assertEquals(SubLine.Offline(null), subLine(null, Failure.NoConnection, zone))
        assertEquals(SubLine.Offline(LocalDate.of(2026, 11, 8)), subLine(SubscriptionStatus.FREE.copy(checkedAt = Instant.parse("2026-11-08T10:00:00Z")), Failure.NoConnection, zone))
        assertEquals(SubLine.ProUntil(LocalDate.of(2026, 12, 15)), subLine(pro, Failure.NoConnection, zone))
        assertEquals(SubLine.Unknown, subLine(null, null, zone))
    }

    @Test
    fun catalogLocksProWithoutSubscription() {
        val ui = catalogUi(catalog, SubscriptionStatus.FREE, null, null, null, null) as CatalogUi.Ready
        assertEquals(listOf(false, true), ui.rows.map { it.locked })
        assertEquals(CatalogAction.INSTALL, ui.rows[0].action())
        assertEquals(CatalogAction.SUBSCRIBE, ui.rows[1].action())
        assertEquals(listOf(null, null), ui.rows.map { it.estimateSeconds })
    }

    @Test
    fun catalogUnlocksWithPro() {
        val ui = catalogUi(catalog, pro, null, "alpine", null, 5_000_000) as CatalogUi.Ready
        assertEquals(listOf(false, false), ui.rows.map { it.locked })
        assertEquals(CatalogAction.REINSTALL, ui.rows[0].action())
        assertEquals(120L, ui.rows[1].estimateSeconds)
    }

    @Test
    fun catalogRelocksWhenRevoked() {
        val ui = catalogUi(catalog, pro, Failure.NoAccess, null, null, null) as CatalogUi.Ready
        assertTrue(ui.revoked)
        assertTrue(ui.rows[1].locked)
    }

    @Test
    fun catalogStates() {
        assertEquals(CatalogUi.Loading, catalogUi(Load.Loading, null, null, null, null, null))
        assertEquals(CatalogUi.Failed(Failure.BadSignature), catalogUi(Load.Failed(Failure.BadSignature), null, null, null, null, null))
        assertEquals(CatalogUi.Empty, catalogUi(Load.Ready(Catalog(Manifest(1, "t", emptyList()), false)), null, null, null, null, null))
    }

    @Test
    fun catalogMarksOutdatedImageAsInstallable() {
        val ui = catalogUi(catalog, pro, null, "alpine", "0".repeat(64), 5_000_000) as CatalogUi.Ready
        assertEquals(CatalogAction.INSTALL, ui.rows[0].action())
        assertEquals(CatalogAction.INSTALL, ui.rows[1].action())
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
        assertEquals(1f, launchFraction(LaunchState.Ready(ru.palma.app.shell.DisplayMode.X11, false)))
    }

    private val offers = listOf(
        PlanOffer(Plan.PRO_MONTH, "m", "299 ₽", 7),
        PlanOffer(Plan.PRO_YEAR, "y", "", 7),
        PlanOffer(Plan.LIFETIME, "l", "2 990 ₽", 0),
    )

    private fun subs(status: SubscriptionStatus?, plan: Plan) = SubscriptionUi(status, SubLine.Unknown, offers, plan, false, null)

    @Test
    fun subscriptionButtons() {
        assertTrue(subs(SubscriptionStatus.FREE, Plan.PRO_MONTH).trialAvailable)
        assertTrue(subs(SubscriptionStatus.FREE, Plan.PRO_MONTH).canBuy)
        assertEquals(false, subs(SubscriptionStatus.FREE, Plan.LIFETIME).trialAvailable)
        assertEquals(false, subs(pro, Plan.PRO_MONTH).canBuy)
        assertEquals(true, subs(pro, Plan.LIFETIME).canBuy)
        assertEquals(false, subs(SubscriptionStatus(Tier.LIFETIME, null, null), Plan.LIFETIME).canBuy)
    }

    @Test
    fun noWeeklyPlan() {
        assertEquals(listOf("PRO_MONTH", "PRO_YEAR", "LIFETIME"), Plan.entries.map { it.name })
    }
}
