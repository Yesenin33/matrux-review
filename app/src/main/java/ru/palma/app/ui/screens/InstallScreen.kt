package ru.palma.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.palma.app.R
import ru.palma.app.install.InstallState
import ru.palma.app.ui.components.ErrorWords
import ru.palma.app.ui.components.GhostButton
import ru.palma.app.ui.components.Note
import ru.palma.app.ui.components.PrimaryButton
import ru.palma.app.ui.components.ProgressBar
import ru.palma.app.ui.components.ScreenTitle
import ru.palma.app.ui.components.Tab
import ru.palma.app.ui.components.TabScreen
import ru.palma.app.ui.etaText
import ru.palma.app.ui.failureBody
import ru.palma.app.ui.failureTitle
import ru.palma.app.ui.sizeText
import ru.palma.app.ui.theme.Palma

data class InstallActions(
    val onCancel: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onConfirmMetered: () -> Unit = {},
    val onWaitWifi: () -> Unit = {},
    val onLaunch: () -> Unit = {},
    val onHome: () -> Unit = {},
    val onCatalog: () -> Unit = {},
    val onTab: (Tab) -> Unit = {},
)

@Composable
fun InstallScreen(state: InstallState, metered: Boolean, actions: InstallActions) {
    TabScreen(Tab.CATALOG, actions.onTab) {
        Spacer(Modifier.height(Palma.space.block))
        when (state) {
            InstallState.Idle -> Idle(actions)
            is InstallState.ConfirmMetered -> Metered(state, actions)
            is InstallState.Running -> Running(state, metered, actions)
            is InstallState.Failed -> Failed(state, actions)
            is InstallState.Done -> Done(state, actions)
        }
        Spacer(Modifier.height(Palma.space.unit * 2))
    }
}

@Composable
private fun ColumnScope.Bottom(content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.weight(1f))
    Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit * 2), content = content)
}

@Composable
private fun ColumnScope.Idle(actions: InstallActions) {
    ScreenTitle(stringResource(R.string.install_title_idle), null)
    Bottom { PrimaryButton(stringResource(R.string.home_open_catalog), actions.onCatalog) }
}

@Composable
private fun ColumnScope.Metered(state: InstallState.ConfirmMetered, actions: InstallActions) {
    ScreenTitle(state.image.name, null)
    Spacer(Modifier.height(Palma.space.block))
    Text(stringResource(R.string.install_metered_title), style = Palma.type.title, color = Palma.colors.ink)
    Spacer(Modifier.height(Palma.space.unit))
    Text(stringResource(R.string.install_metered_detail, sizeText(state.image.sizeBytes)), style = Palma.type.body, color = Palma.colors.muted)
    Bottom {
        Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit)) {
            PrimaryButton(stringResource(R.string.install_metered_go), actions.onConfirmMetered)
            GhostButton(stringResource(R.string.install_metered_wait), actions.onWaitWifi)
        }
    }
}

@Composable
private fun ColumnScope.Running(state: InstallState.Running, metered: Boolean, actions: InstallActions) {
    ScreenTitle(
        state.image.name,
        stringResource(if (state.waitingForNetwork) R.string.install_waiting_sub else R.string.install_running_sub),
    )
    Spacer(Modifier.height(Palma.space.block))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("${state.percent}%", style = Palma.type.bodyStrong, color = Palma.colors.ink)
        Text(
            state.etaSeconds?.let { stringResource(R.string.install_eta, etaText(it)) } ?: stringResource(R.string.install_eta_unknown),
            style = Palma.type.caption,
            color = Palma.colors.muted,
        )
    }
    Spacer(Modifier.height(Palma.space.unit + Palma.space.unit / 4))
    ProgressBar(state.fraction)
    Spacer(Modifier.height(Palma.space.block))
    Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit * 2)) {
        Text(stringResource(R.string.install_downloaded, sizeText(state.downloaded), sizeText(state.total)), style = Palma.type.body, color = Palma.colors.ink)
        Text(
            stringResource(R.string.install_unpacked, sizeText(state.unpacked), sizeText(state.total)),
            style = Palma.type.body,
            color = if (state.unpacked > 0) Palma.colors.ink else Palma.colors.muted,
        )
    }
    Bottom {
        GhostButton(stringResource(R.string.install_cancel), actions.onCancel)
    }
}

@Composable
private fun ColumnScope.Failed(state: InstallState.Failed, actions: InstallActions) {
    ScreenTitle(state.image.name, null)
    Spacer(Modifier.height(Palma.space.block))
    ErrorWords(failureTitle(state.failure), failureBody(state.failure))
    if (state.downloaded > 0) {
        Spacer(Modifier.height(Palma.space.unit * 2))
        Text(stringResource(R.string.install_kept, sizeText(state.downloaded)), style = Palma.type.caption, color = Palma.colors.muted)
    }
    Bottom {
        Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit)) {
            PrimaryButton(stringResource(R.string.install_retry), actions.onRetry)
            GhostButton(stringResource(R.string.install_cancel), actions.onCancel)
        }
    }
}

@Composable
private fun ColumnScope.Done(state: InstallState.Done, actions: InstallActions) {
    ScreenTitle(state.image.name, stringResource(R.string.install_done_detail))
    Spacer(Modifier.height(Palma.space.block))
    ProgressBar(1f)
    Spacer(Modifier.height(Palma.space.unit + Palma.space.unit / 4))
    Text(stringResource(R.string.install_done), style = Palma.type.bodyStrong, color = Palma.colors.success)
    Bottom {
        Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit)) {
            PrimaryButton(stringResource(R.string.home_launch), actions.onLaunch)
            GhostButton(stringResource(R.string.install_to_home), actions.onHome)
        }
    }
}
