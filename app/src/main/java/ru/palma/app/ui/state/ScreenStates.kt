package ru.palma.app.ui.state

import ru.palma.app.billing.Plan
import ru.palma.app.billing.PlanOffer
import ru.palma.app.data.Catalog
import ru.palma.app.data.Failure
import ru.palma.app.data.ImageInfo
import ru.palma.app.data.InstalledSystem
import ru.palma.app.data.SubscriptionStatus
import ru.palma.app.data.Tier
import ru.palma.app.install.InstallEstimate
import ru.palma.app.install.InstallState
import ru.palma.app.session.SessionState
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

sealed interface HomeUi {
    val sub: SubLine

    data class Loading(override val sub: SubLine) : HomeUi
    data class Empty(val suggestion: ImageInfo, val freeBytes: Long, override val sub: SubLine) : HomeUi
    data class NoCatalog(val failure: Failure, override val sub: SubLine) : HomeUi
    data class Ready(
        val system: InstalledSystem,
        val freeBytes: Long,
        val running: Boolean,
        override val sub: SubLine,
    ) : HomeUi
    data class Installing(val progress: InstallState.Running, override val sub: SubLine) : HomeUi
    data class Revoked(override val sub: SubLine) : HomeUi
}

fun homeUi(
    installed: InstalledSystem?,
    installedReady: Boolean,
    catalog: Load<Catalog>,
    install: InstallState,
    session: SessionState,
    status: SubscriptionStatus?,
    statusFailure: Failure?,
    freeBytes: Long,
    zone: ZoneId,
): HomeUi {
    val sub = subLine(status, statusFailure, zone)
    if (statusFailure == Failure.NoAccess) return HomeUi.Revoked(sub)
    if (install is InstallState.Running) return HomeUi.Installing(install, sub)
    if (installed != null && installedReady) {
        val running = session is SessionState.Live || session is SessionState.Launching
        return HomeUi.Ready(installed, freeBytes, running, sub)
    }
    return when (catalog) {
        Load.Loading -> HomeUi.Loading(sub)
        is Load.Failed -> HomeUi.NoCatalog(catalog.failure, sub)
        is Load.Ready -> suggestion(catalog.value.manifest.images, installed?.imageId)
            ?.let { HomeUi.Empty(it, freeBytes, sub) }
            ?: HomeUi.NoCatalog(Failure.ImageUnavailable, sub)
    }
}

fun suggestion(images: List<ImageInfo>, preferId: String? = null): ImageInfo? =
    preferId?.let { id -> images.firstOrNull { it.id == id } }
        ?: images.firstOrNull { it.distro.equals("debian", ignoreCase = true) && it.desktop != "none" }
        ?: images.firstOrNull()

data class CatalogRow(
    val image: ImageInfo,
    val locked: Boolean,
    val installed: Boolean,
    val estimateSeconds: Long?,
)

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
    installedId: String?,
    installedSha: String?,
    bytesPerSecond: Long?,
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
                    CatalogRow(
                        image = image,
                        locked = image.pro && !paid,
                        installed = image.id == installedId && (installedSha == null || image.sha256.equals(installedSha, ignoreCase = true)),
                        estimateSeconds = InstallEstimate.seconds(image.sizeBytes, bytesPerSecond),
                    )
                },
                offline = catalog.value.offline,
                revoked = statusFailure == Failure.NoAccess,
            )
        }
    }
}

enum class CatalogAction { INSTALL, REINSTALL, SUBSCRIBE }

fun CatalogRow.action(): CatalogAction = when {
    installed -> CatalogAction.REINSTALL
    locked -> CatalogAction.SUBSCRIBE
    else -> CatalogAction.INSTALL
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

data class SubscriptionUi(
    val status: SubscriptionStatus?,
    val sub: SubLine,
    val offers: List<PlanOffer>,
    val selected: Plan,
    val busy: Boolean,
    val failure: Failure?,
) {
    val paid: Boolean get() = status?.paid == true
    val trialAvailable: Boolean get() = !paid && offers.any { it.plan == selected && it.trialDays > 0 }
    val selectedOffer: PlanOffer? get() = offers.firstOrNull { it.plan == selected }
    val canBuy: Boolean get() = selectedOffer?.let { it.productId.isNotBlank() } == true &&
        !(status?.tier == Tier.LIFETIME) &&
        !paid
}

fun Instant.localDate(zone: ZoneId): LocalDate = atZone(zone).toLocalDate()
