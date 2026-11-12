package ru.matrux.app.ui.state

import ru.matrux.app.R
import ru.matrux.app.billing.Terms
import ru.matrux.app.billing.PlanOffer
import ru.matrux.app.data.Catalog
import ru.matrux.app.data.Failure
import ru.matrux.app.data.ImageInfo
import ru.matrux.app.ui.knowledge.KnowledgeSection
import ru.matrux.app.learn.Access
import ru.matrux.app.learn.Course
import ru.matrux.app.account.AccountInfo
import ru.matrux.app.data.SubscriptionStatus
import ru.matrux.app.data.TrustedClock
import ru.matrux.app.data.Tier
import ru.matrux.app.install.InstallEstimate
import ru.matrux.app.shell.LaunchState
import ru.matrux.app.shell.LaunchStep
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
    data class Failed(val failure: Failure) : Load<Nothing>
}

sealed interface SubLine {
    data object Unknown : SubLine
    data object Free : SubLine
    data class ProUntil(val date: LocalDate) : SubLine
    data object Revoked : SubLine
    data class Offline(val checked: LocalDate?) : SubLine
}

fun subLine(
    status: SubscriptionStatus?,
    failure: Failure?,
    zone: ZoneId,
    now: Long = TrustedClock.now(),
): SubLine = when {
    failure == Failure.NoAccess -> SubLine.Revoked
    status == null && failure == Failure.NoConnection -> SubLine.Offline(null)
    status == null -> SubLine.Unknown
    status.tier == Tier.PRO && status.expiresAt != null && !status.activeAt(now) -> SubLine.Free
    status.tier == Tier.PRO && status.expiresAt != null -> SubLine.ProUntil(status.expiresAt.atZone(zone).toLocalDate())
    status.tier == Tier.PRO -> SubLine.Free
    failure == Failure.NoConnection -> SubLine.Offline(status.checkedAt?.atZone(zone)?.toLocalDate())
    else -> SubLine.Free
}

fun SubLine.paidSub(): Boolean = this is SubLine.ProUntil

data class HomeSection(
    val id: String,
    val title: Int,
    val rows: List<SectionRow>,
    val locked: Boolean,
)

private val BASICS_SHEETS = listOf("start", "linux", "terms")
private val ADVANCED_SHEETS = listOf(
    "files", "git", "hardware", "packages", "users", "services",
    "network", "disks", "logs", "scripts",
)

private fun sheetRows(sections: List<KnowledgeSection>, ids: List<String>, locked: Boolean): List<SectionRow> =
    ids.mapNotNull { id -> sections.firstOrNull { it.id == id }?.let { SectionRow(it, locked) } }

fun homeSections(sections: List<KnowledgeSection>, paid: Boolean): List<HomeSection> = listOf(
    HomeSection("basics", R.string.home_basics, sheetRows(sections, BASICS_SHEETS, locked = false), locked = false),
    HomeSection("advanced", R.string.home_advanced, sheetRows(sections, ADVANCED_SHEETS, locked = !paid), locked = !paid),
)

enum class LockedItem { COURSES, SYSTEMS, SHEETS, OFFLINE }

data class ProOfferUi(
    val systems: List<String>,
    val courses: List<String>,
    val lessons: Int,
    val sheets: List<String> = emptyList(),
) {
    val items: List<LockedItem> get() = buildList {
        if (systems.isNotEmpty()) add(LockedItem.SYSTEMS)
        if (courses.isNotEmpty() && lessons > 0) add(LockedItem.COURSES)
        if (sheets.isNotEmpty()) add(LockedItem.SHEETS)
        add(LockedItem.OFFLINE)
    }
}

fun proOffer(images: List<ImageInfo>, courses: List<Course>, sections: List<KnowledgeSection>): ProOfferUi = ProOfferUi(
    systems = images.filter { it.pro }.map { it.name },
    courses = courses.map { it.title },
    lessons = courses.sumOf { course -> course.lessonCount - Access.openLessons(course, paid = false).size },
    sheets = if (Access.cheatSheetLocked()) sections.filter { it.pro }.map { it.title } else emptyList(),
)

data class CatalogRow(
    val image: ImageInfo,
    val locked: Boolean,
    val active: Boolean,
    val installed: Boolean,
    val outdated: Boolean,
    val estimateSeconds: Long?,
    val running: Boolean = false,
) {
    val switchable: Boolean get() = installed && !active
    val reinstallable: Boolean get() = active || (installed && outdated)
}

sealed interface CatalogUi {
    data object Loading : CatalogUi
    data object Empty : CatalogUi
    data class Failed(val failure: Failure) : CatalogUi
    data class Ready(val rows: List<CatalogRow>, val offline: Boolean, val revoked: Boolean) : CatalogUi
}

fun catalogImageSha(catalog: Load<Catalog>, imageId: String?): String? =
    if (catalog is Load.Ready && imageId != null) {
        catalog.value.manifest.images.firstOrNull { it.id == imageId }?.sha256
    } else {
        null
    }

fun catalogUi(
    catalog: Load<Catalog>,
    status: SubscriptionStatus?,
    statusFailure: Failure?,
    activeId: String?,
    bytesPerSecond: Long?,
    installedShas: Map<String, String> = emptyMap(),
    running: Boolean = false,
    now: Long = TrustedClock.now(),
): CatalogUi = when (catalog) {
    Load.Loading -> CatalogUi.Loading
    is Load.Failed -> CatalogUi.Failed(catalog.failure)
    is Load.Ready -> {
        val images = catalog.value.manifest.images
        if (images.isEmpty()) {
            CatalogUi.Empty
        } else {
            val paid = status?.activeAt(now) == true && statusFailure != Failure.NoAccess
            CatalogUi.Ready(
                rows = images.map { image ->
                    val marker = installedShas[image.id]
                    CatalogRow(
                        image = image,
                        locked = image.pro && !paid,
                        active = image.id == activeId,
                        installed = marker != null,
                        outdated = marker != null && !image.sha256.equals(marker, ignoreCase = true),
                        estimateSeconds = InstallEstimate.seconds(image.sizeBytes, bytesPerSecond),
                        running = running && image.id == activeId,
                    )
                },
                offline = catalog.value.offline,
                revoked = statusFailure == Failure.NoAccess,
            )
        }
    }
}

enum class SystemAction { OPEN, STOP, LAUNCH, SWITCH, REINSTALL, INSTALL, SUBSCRIBE }

fun CatalogRow.actions(): List<SystemAction> = when {
    active && running -> listOf(SystemAction.OPEN, SystemAction.STOP)
    active && installed -> listOf(SystemAction.LAUNCH, SystemAction.REINSTALL)
    active -> listOf(SystemAction.REINSTALL)
    switchable && outdated -> listOf(SystemAction.SWITCH, SystemAction.REINSTALL)
    switchable -> listOf(SystemAction.SWITCH)
    locked -> listOf(SystemAction.SUBSCRIBE)
    else -> listOf(SystemAction.INSTALL)
}

enum class LaunchStepState { DONE, ACTIVE, FAILED, WAITING }

fun launchSteps(state: LaunchState): List<Pair<LaunchStep, LaunchStepState>> {
    val (current, failed) = when (state) {
        is LaunchState.Running -> state.step to false
        is LaunchState.Failed -> state.step to true
        is LaunchState.Ready -> null to false
    }
    return LaunchStep.entries.map { step ->
        step to when {
            current == null || step.ordinal < current.ordinal -> LaunchStepState.DONE
            step == current -> if (failed) LaunchStepState.FAILED else LaunchStepState.ACTIVE
            else -> LaunchStepState.WAITING
        }
    }
}

fun launchFraction(state: LaunchState): Float = when (state) {
    is LaunchState.Running -> state.step.ordinal.toFloat() / LaunchStep.entries.size
    is LaunchState.Failed -> state.step.ordinal.toFloat() / LaunchStep.entries.size
    is LaunchState.Ready -> 1f
}

data class AccountUi(
    val info: AccountInfo?,
    val available: Boolean,
    val busy: Boolean = false,
    val failure: Failure? = null,
)

data class SubscriptionUi(
    val status: SubscriptionStatus?,
    val sub: SubLine,
    val terms: Terms,
    val busy: Boolean,
    val failure: Failure?,
    val billingReady: Boolean = true,
    val account: AccountUi? = null,
    val now: Long = TrustedClock.now(),
) {
    val paid: Boolean get() = status?.activeAt(now) == true
    val trialAvailable: Boolean get() = !paid && (selectedOffer?.trialDays ?: 0) > 0
    val selectedOffer: PlanOffer? get() = terms.selectedOffer
    val canBuy: Boolean get() = billingReady &&
        selectedOffer?.let { it.productId.isNotBlank() } == true &&
        !(status?.activeAt(now) ?: false) &&
        !paid
}

fun Instant.localDate(zone: ZoneId): LocalDate = atZone(zone).toLocalDate()


