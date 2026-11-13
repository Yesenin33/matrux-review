package ru.matrux.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import ru.matrux.app.R
import ru.matrux.app.billing.Plan
import ru.matrux.app.data.Failure
import ru.matrux.app.shell.LaunchStep
import ru.matrux.app.ui.state.SubLine

data class FailureText(val title: Int, val body: Int)

fun failureText(failure: Failure): FailureText = when (failure) {
    Failure.NoConnection -> FailureText(R.string.fail_no_connection_title, R.string.fail_no_connection)
    Failure.NeedSubscription -> FailureText(R.string.fail_need_sub_title, R.string.fail_need_sub)
    Failure.NoAccess -> FailureText(R.string.fail_no_access_title, R.string.fail_no_access)
    Failure.ImageUnavailable -> FailureText(R.string.fail_unavailable_title, R.string.fail_unavailable)
    Failure.ImageNotFound -> FailureText(R.string.fail_not_found_title, R.string.fail_not_found)
    Failure.BadSignature -> FailureText(R.string.fail_signature_title, R.string.fail_signature)
    Failure.UnknownSigningKey -> FailureText(R.string.fail_signing_key_title, R.string.fail_signing_key)
    Failure.BrokenManifest -> FailureText(R.string.fail_manifest_title, R.string.fail_manifest)
    Failure.ChecksumMismatch -> FailureText(R.string.fail_checksum_title, R.string.fail_checksum)
    Failure.NotEnoughSpace -> FailureText(R.string.fail_space_title, R.string.fail_space)
    Failure.BrokenArchive -> FailureText(R.string.fail_archive_title, R.string.fail_archive)
    Failure.PurchaseNotFound -> FailureText(R.string.fail_purchase_title, R.string.fail_purchase)
    Failure.PurchaseOnOtherDevice -> FailureText(R.string.fail_purchase_bound_title, R.string.fail_purchase_bound)
    Failure.BillingUnavailable -> FailureText(R.string.fail_billing_title, R.string.fail_billing)
    Failure.PurchaseOnOtherAccount -> FailureText(R.string.fail_purchase_account_title, R.string.fail_purchase_account)
    Failure.Cancelled -> FailureText(R.string.fail_cancelled_title, R.string.fail_cancelled)
    Failure.SignInUnavailable -> FailureText(R.string.fail_signin_title, R.string.fail_signin)
    Failure.SignInRejected -> FailureText(R.string.fail_signin_rejected_title, R.string.fail_signin_rejected)
    Failure.SignedOut -> FailureText(R.string.fail_signed_out_title, R.string.fail_signed_out)
    Failure.EngineMissing -> FailureText(R.string.fail_engine_title, R.string.fail_engine)
    Failure.EngineCrashed -> FailureText(R.string.fail_engine_crash_title, R.string.fail_engine_crash)
    is Failure.Engine -> FailureText(R.string.fail_engine_crash_title, R.string.fail_engine_crash)
    Failure.SessionMissing -> FailureText(R.string.fail_session_title, R.string.fail_session)
    Failure.DisplayUnavailable -> FailureText(R.string.fail_display_title, R.string.fail_display)
    is Failure.Display -> FailureText(R.string.fail_display_title, R.string.fail_display)
    Failure.NotInstalled -> FailureText(R.string.fail_not_installed_title, R.string.fail_not_installed)
    is Failure.Server -> FailureText(R.string.fail_server_title, R.string.fail_server)
}

@Composable
fun failureTitle(failure: Failure): String = stringResource(failureText(failure).title)

@Composable
fun failureBody(failure: Failure): String {
    val text = failureText(failure)
    return when (failure) {
        is Failure.Server -> stringResource(text.body, failure.status)
        is Failure.Engine -> failure.output.ifBlank { stringResource(text.body) }
        is Failure.Display -> failure.output.ifBlank { stringResource(text.body) }
        else -> stringResource(text.body)
    }
}

@Composable
fun sizeText(bytes: Long): String = when (val size = Format.size(bytes)) {
    is Format.Size.Megabytes -> stringResource(R.string.unit_mb, size.number)
    is Format.Size.Gigabytes -> stringResource(R.string.unit_gb, size.number)
}

@Composable
fun roughDurationText(seconds: Long): String = when (val d = Format.roughDuration(seconds)) {
    is Format.Duration.Seconds -> pluralStringResource(R.plurals.unit_seconds, d.seconds, d.seconds)
    is Format.Duration.Minutes -> pluralStringResource(R.plurals.unit_minutes, d.minutes, d.minutes)
    is Format.Duration.MinSec -> stringResource(R.string.eta_min_sec, d.minutes, d.seconds)
}

@Composable
fun etaText(seconds: Long): String = when (val d = Format.eta(seconds)) {
    is Format.Duration.Seconds -> stringResource(R.string.eta_sec, d.seconds)
    is Format.Duration.MinSec -> stringResource(R.string.eta_min_sec, d.minutes, d.seconds)
    is Format.Duration.Minutes -> pluralStringResource(R.plurals.unit_minutes, d.minutes, d.minutes)
}

@Composable
fun termLabel(plan: Plan): String = stringResource(
    when (plan) {
        Plan.PRO_MONTH -> R.string.pro_term_1
        Plan.PRO_QUARTER -> R.string.pro_term_3
        Plan.PRO_HALF_YEAR -> R.string.pro_term_6
    },
)

@Composable
fun rubText(amount: Long): String = stringResource(R.string.price_rub, Format.rubles(amount))

@Composable
fun perMonthText(amount: Long): String = stringResource(R.string.price_per_month, Format.rubles(amount))

@Composable
fun discountText(percent: Int): String = stringResource(R.string.price_discount, percent)

@Composable
fun subLineText(line: SubLine): String = when (line) {
    SubLine.Unknown -> stringResource(R.string.sub_unknown)
    SubLine.Free -> stringResource(R.string.sub_free)
    is SubLine.ProUntil -> stringResource(R.string.sub_pro_until, Format.dayMonth(line.date))
    SubLine.Revoked -> stringResource(R.string.sub_revoked)
    is SubLine.Offline -> line.checked?.let { stringResource(R.string.sub_offline, Format.dayMonth(it)) }
        ?: stringResource(R.string.sub_offline_never)
}

fun stepLabel(step: LaunchStep): Int = when (step) {
    LaunchStep.SUBSCRIPTION -> R.string.launch_step_subscription
    LaunchStep.ROOTFS -> R.string.launch_step_rootfs
    LaunchStep.PROOT -> R.string.launch_step_proot
    LaunchStep.DISPLAY -> R.string.launch_step_display
}

fun stepBusy(step: LaunchStep): Int = when (step) {
    LaunchStep.SUBSCRIPTION -> R.string.launch_busy_subscription
    LaunchStep.ROOTFS -> R.string.launch_busy_rootfs
    LaunchStep.PROOT -> R.string.launch_busy_proot
    LaunchStep.DISPLAY -> R.string.launch_busy_display
}

fun distroLogo(distro: String): Int? = when {
    distro.lowercase().startsWith("alpine") -> R.drawable.logo_alpine
    distro.lowercase().startsWith("debian") -> R.drawable.logo_debian
    distro.lowercase().startsWith("ubuntu") -> R.drawable.logo_ubuntu
    distro.lowercase().startsWith("arch") -> R.drawable.logo_arch
    else -> null
}

fun imageLogo(image: ru.matrux.app.data.ImageInfo): Int? = when (image.id) {
    "dev-xfce", "dev", "debian-dev", "razrabotka" -> R.drawable.logo_dev
    else -> distroLogo(image.distro)
}

fun imageDescription(image: ru.matrux.app.data.ImageInfo): Int? = when (image.id) {
    "alpine" -> R.string.image_alpine
    "debian-12", "debian" -> R.string.image_debian
    "dev", "debian-dev", "razrabotka" -> R.string.image_dev
    else -> null
}

/** Логотип системы по идентификатору и названию: для «Главной» и экрана запуска. */
fun systemLogo(imageId: String, name: String): Int? = when (imageId) {
    "dev-xfce", "dev", "debian-dev", "razrabotka" -> R.drawable.logo_dev
    else -> distroLogo(name)
}

/** Короткое имя системы для приглашения оболочки: «debian-12» → «debian». */
fun systemHost(imageId: String): String = imageId.takeWhile { it.isLetter() }.ifEmpty { "linux" }
