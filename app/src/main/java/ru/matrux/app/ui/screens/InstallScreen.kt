package ru.matrux.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.matrux.app.R
import ru.matrux.app.data.Failure
import ru.matrux.app.install.InstallState
import ru.matrux.app.ui.components.DistroLogo
import ru.matrux.app.ui.components.GhostButton
import ru.matrux.app.ui.components.Group
import ru.matrux.app.ui.components.GroupDivider
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.Plaque
import ru.matrux.app.ui.components.PrimaryButton
import ru.matrux.app.ui.components.ProgressBar
import ru.matrux.app.ui.components.StateBlock
import ru.matrux.app.ui.components.Tab
import ru.matrux.app.ui.components.TabScreen
import ru.matrux.app.ui.components.ValueRow
import ru.matrux.app.ui.etaText
import ru.matrux.app.ui.failureTitle
import ru.matrux.app.ui.imageLogo
import ru.matrux.app.ui.sizeText
import ru.matrux.app.ui.theme.Matrux

data class InstallActions(
    val onCancel: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onConfirmMetered: () -> Unit = {},
    val onWaitWifi: () -> Unit = {},
    val onLaunch: () -> Unit = {},
    val onHome: () -> Unit = {},
    val onCatalog: () -> Unit = {},
    val onTab: (Tab) -> Unit = {},
    val onSubscribe: () -> Unit = {},
)

@Composable
fun InstallScreen(state: InstallState, metered: Boolean, actions: InstallActions) {
    TabScreen(
        Tab.CATALOG,
        actions.onTab,
        scroll = true,
        bottom = {
            when (state) {
                InstallState.Idle -> PrimaryButton(stringResource(R.string.home_open_catalog), actions.onCatalog)
                is InstallState.ConfirmMetered -> {
                    PrimaryButton(stringResource(R.string.install_metered_go), actions.onConfirmMetered)
                    GhostButton(stringResource(R.string.install_metered_wait), actions.onWaitWifi)
                }
                is InstallState.Running -> GhostButton(stringResource(R.string.install_cancel), actions.onCancel)
                is InstallState.Failed -> {
                    if (state.failure == Failure.NeedSubscription) {
                        PrimaryButton(stringResource(R.string.pro_buy), actions.onSubscribe)
                    } else {
                        PrimaryButton(stringResource(R.string.install_retry), actions.onRetry)
                    }
                    GhostButton(stringResource(R.string.install_cancel), actions.onCancel)
                }
                is InstallState.Done -> {
                    PrimaryButton(stringResource(R.string.home_launch), actions.onLaunch)
                    GhostButton(stringResource(R.string.install_to_home), actions.onHome)
                }
            }
            Spacer(Modifier.height(Matrux.space.unit))
        },
    ) {
        Spacer(Modifier.height(Matrux.space.unit * 2))
        when (state) {
            InstallState.Idle -> StateBlock(Lucide.Package, Matrux.colors.graySolid, stringResource(R.string.install_title_idle))
            is InstallState.ConfirmMetered -> Metered(state)
            is InstallState.Running -> Running(state)
            is InstallState.Failed -> Failed(state)
            is InstallState.Done -> Done(state)
        }
        Spacer(Modifier.height(Matrux.space.block))
    }
}

@Composable
private fun Head(name: String, logo: Int?, badge: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        DistroLogo(logo)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                name,
                style = Matrux.type.display.copy(fontSize = 28.sp, lineHeight = 34.sp),
                color = Matrux.colors.ink,
                maxLines = 1,
            )
            badge()
        }
    }
}

@Composable
private fun Big(percent: Int, color: Color = Matrux.colors.bar) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(percent.toString(), style = Matrux.type.big, color = Matrux.colors.ink)
            Text(
                " %",
                style = Matrux.type.display.copy(fontSize = 28.sp),
                color = Matrux.colors.muted,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        ProgressBar(percent / 100f, color = color)
    }
}

@Composable
private fun Rows(downloaded: String, unpacked: String, eta: String?) {
    Group {
        ValueRow(stringResource(R.string.install_label_downloaded), downloaded)
        GroupDivider(inset = Matrux.space.card)
        ValueRow(stringResource(R.string.install_label_unpacked), unpacked)
        if (eta != null) {
            GroupDivider(inset = Matrux.space.card)
            ValueRow(stringResource(R.string.install_label_eta), eta)
        }
    }
}

@Composable
private fun Metered(state: InstallState.ConfirmMetered) {
    val colors = Matrux.colors
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Head(state.image.name, imageLogo(state.image)) {
            Plaque(stringResource(R.string.install_metered_title), colors.graySolid)
        }
        Group { ValueRow(stringResource(R.string.install_size), sizeText(state.image.sizeBytes)) }
    }
}

@Composable
private fun Running(state: InstallState.Running) {
    val colors = Matrux.colors
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Head(state.image.name, imageLogo(state.image)) {
            if (state.waitingForNetwork) {
                Plaque(stringResource(R.string.fail_no_connection_title), colors.graySolid, icon = Lucide.WifiOff)
            } else {
                Plaque(stringResource(R.string.install_state_running), colors.infoSolid)
            }
        }
        Big(state.percent, if (state.waitingForNetwork) colors.graySolid else colors.bar)
        Rows(
            stringResource(R.string.install_of, sizeText(state.downloaded), sizeText(state.total)),
            stringResource(R.string.install_of, sizeText(state.unpacked), sizeText(state.total)),
            state.etaSeconds?.let { etaText(it) },
        )
    }
}

@Composable
private fun Failed(state: InstallState.Failed) {
    val colors = Matrux.colors
    val total = state.image.sizeBytes
    val percent = if (total > 0) (state.downloaded * 100 / total).toInt().coerceIn(0, 100) else 0
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Head(state.image.name, imageLogo(state.image)) {
            Plaque(failureTitle(state.failure), colors.dangerSolid, icon = Lucide.CircleAlert)
        }
        if (state.downloaded > 0) {
            Big(percent, colors.dangerSolid)
            Group {
                ValueRow(
                    stringResource(R.string.install_label_downloaded),
                    stringResource(R.string.install_of, sizeText(state.downloaded), sizeText(total)),
                )
            }
        }
    }
}

@Composable
private fun Done(state: InstallState.Done) {
    val colors = Matrux.colors
    val total = sizeText(state.image.sizeBytes)
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Head(state.image.name, imageLogo(state.image)) {
            Plaque(stringResource(R.string.install_done), colors.successSolid, icon = Lucide.Check)
        }
        Big(100, colors.successSolid)
        Rows(
            stringResource(R.string.install_of, total, total),
            stringResource(R.string.install_of, total, total),
            null,
        )
    }
}
