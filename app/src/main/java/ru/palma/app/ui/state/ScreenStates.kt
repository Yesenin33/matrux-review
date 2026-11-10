package ru.palma.app.ui.state

import ru.palma.app.billing.Terms
import ru.palma.app.billing.PlanOffer
import ru.palma.app.data.Catalog
import ru.palma.app.data.Failure
import ru.palma.app.data.ImageInfo
import ru.palma.app.ui.knowledge.KnowledgeSection
import ru.palma.app.learn.Access
import ru.palma.app.learn.Course
import ru.palma.app.account.AccountInfo
import ru.palma.app.data.SubscriptionStatus
import ru.palma.app.data.Tier
import ru.palma.app.install.InstallEstimate
import ru.palma.app.shell.LaunchState
import ru.palma.app.shell.LaunchStep
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
    data object Lifetime : SubLine
    data object Revoked : SubLine
    data class Offline(val checked: LocalDate?) : SubLine
}

fun subLine(status: SubscriptionStatus?, failure: Failure?, zone: ZoneId): SubLine = when {
    failure == Failure.NoAccess -> SubLine.Revoked
    status == null && failure == Failure.NoConnection -> SubLine.Offline(null)
    status == null -> SubLine.Unknown
    status.tier == Tier.LIFETIME -> SubLine.Lifetime
    status.tier == Tier.PRO && status.expiresAt != null -> SubLine.ProUntil(status.expiresAt.atZone(zone).toLocalDate())
    status.tier == Tier.PRO -> SubLine.Lifetime
    failure == Failure.NoConnection -> SubLine.Offline(status.checkedAt?.atZone(zone)?.toLocalDate())
    else -> SubLine.Free
}

fun SubLine.paidSub(): Boolean = this is SubLine.ProUntil || this is SubLine.Lifetime

data class HomeUi(
    val sub: SubLine,
    val terms: Terms,
    val systems: List<ImageInfo>,
    val courses: List<Course>,
    val sheets: Int,
    val locked: ProOfferUi = ProOfferUi(emptyList(), emptyList(), 0),
) {
    val paid: Boolean get() = sub.paidSub()
    val revoked: Boolean get() = sub == SubLine.Revoked
    val lessons: Int get() = courses.sumOf { it.lessonCount }
}

enum class HomeBlock { REVOKED, BUY, STATUS, TERMS, INSIDE, LOCKED }

fun homeBlocks(ui: HomeUi): List<HomeBlock> = buildList {
    if (ui.revoked) add(HomeBlock.REVOKED)
    if (ui.paid) {
        add(HomeBlock.STATUS)
        add(HomeBlock.INSIDE)
    } else {
        add(HomeBlock.BUY)
        add(HomeBlock.TERMS)
        add(HomeBlock.INSIDE)
        add(HomeBlock.LOCKED)
    }
}

fun homeUi(
    catalog: Load<Catalog>,
    status: SubscriptionStatus?,
    statusFailure: Failure?,
    terms: Terms,
    courses: List<Course>,
    sections: List<KnowledgeSection>,
    zone: ZoneId,
): HomeUi {
    val images = (catalog as? Load.Ready)?.value?.manifest?.images.orEmpty()
    return HomeUi(
        sub = subLine(status, statusFailure, zone),
        terms = terms,
        systems = images,
        courses = courses,
        sheets = sections.size,
        locked = proOffer(images, courses, sections),
    )
}

enum class LockedItem { COURSES, SYSTEMS, SHEETS, OFFLINE }

data class ProOfferUi(
    val systems: List<String>,
    val courses: List<String>,
    val lessons: Int,
    val sheets: List<String> = emptyList(),
) {
    val items: List<LockedItem> get() = buildList {
        if (courses.isNotEmpty() && lessons > 0) add(LockedItem.COURSES)
        if (systems.isNotEmpty()) add(LockedItem.SYSTEMS)
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
): CatalogUi = when (catalog) {
    Load.Loading -> CatalogUi.Loading
    is Load.Failed -> CatalogUi.Failed(catalog.failure)
    is Load.Ready -> {
        val images = catalog.value.manifest.images
        if (images.isEmpty()) {
            CatalogUi.Empty
        } else {
            val paid = status?.paid == true && statusFailure != Failure.NoAccess
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
) {
    val paid: Boolean get() = status?.paid == true
    val trialAvailable: Boolean get() = !paid && (selectedOffer?.trialDays ?: 0) > 0
    val selectedOffer: PlanOffer? get() = terms.selectedOffer
    val canBuy: Boolean get() = billingReady &&
        selectedOffer?.let { it.productId.isNotBlank() } == true &&
        !(status?.tier == Tier.LIFETIME) &&
        !paid
}

fun Instant.localDate(zone: ZoneId): LocalDate = atZone(zone).toLocalDate()

sealed interface GateSystem {
    data class Running(val name: String) : GateSystem
    data class Stopped(val name: String) : GateSystem
    data class Installing(val name: String, val percent: Int) : GateSystem
    data object Absent : GateSystem
}

data class SignInUi(
    val system: GateSystem,
    val signedIn: Boolean,
    val busy: Boolean,
    val available: Boolean,
) {
    val enabled: Boolean get() = available && !busy && !signedIn
}
