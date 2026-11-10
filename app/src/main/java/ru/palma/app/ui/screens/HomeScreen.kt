package ru.palma.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.palma.app.R
import ru.palma.app.data.ImageInfo
import ru.palma.app.ui.components.CenterCaption
import ru.palma.app.ui.components.ErrorWords
import ru.palma.app.ui.components.GhostButton
import ru.palma.app.ui.components.PrimaryButton
import ru.palma.app.ui.components.StatePanel
import ru.palma.app.ui.components.StatusDot
import ru.palma.app.ui.components.Tab
import ru.palma.app.ui.components.TabScreen
import ru.palma.app.ui.knowledge.knowledge
import ru.palma.app.ui.failureBody
import ru.palma.app.ui.failureTitle
import ru.palma.app.ui.roughDurationText
import ru.palma.app.ui.sizeText
import ru.palma.app.ui.state.HomeUi
import ru.palma.app.ui.state.paidSub
import ru.palma.app.ui.subLineText
import ru.palma.app.ui.theme.Palma

data class HomeActions(
    val onLaunch: () -> Unit = {},
    val onReturn: () -> Unit = {},
    val onStop: () -> Unit = {},
    val onInstall: (ImageInfo) -> Unit = {},
    val onOpenInstall: () -> Unit = {},
    val onCatalog: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onReRegister: () -> Unit = {},
    val onSection: (ru.palma.app.ui.knowledge.KnowledgeSection) -> Unit = {},
    val onTab: (Tab) -> Unit = {},
)

@Composable
fun HomeScreen(ui: HomeUi, actions: HomeActions) {
    TabScreen(Tab.HOME, actions.onTab, scroll = true) {
        Spacer(Modifier.height(Palma.space.block))
        when (ui) {
            is HomeUi.Ready -> Ready(ui, actions)
            is HomeUi.Empty -> Empty(ui, actions)
            is HomeUi.Installing -> Installing(ui, actions)
            is HomeUi.Loading -> Loading()
            is HomeUi.NoCatalog -> NoCatalog(ui, actions)
            is HomeUi.Revoked -> Revoked(actions)
        }
        Spacer(Modifier.height(Palma.space.block))
        SectionsBlock(knowledge, ui.sub.paidSub(), actions.onSection)
        Spacer(Modifier.height(Palma.space.unit * 2))
        CenterCaption(subLineText(ui.sub))
        Spacer(Modifier.height(Palma.space.unit * 2))
    }
}

@Composable
private fun Hero(title: String?, sub: String?) {
    if (title != null) {
        Text(title, style = Palma.type.display, color = Palma.colors.ink)
    }
    if (sub != null) {
        Spacer(Modifier.height(Palma.space.unit))
        Text(sub, style = Palma.type.body, color = Palma.colors.muted)
    }
    Spacer(Modifier.height(Palma.space.block))
}

@Composable
private fun StateLine(title: String, detail: String?, running: Boolean?) {
    StatePanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Palma.space.unit + Palma.space.unit / 2)) {
            if (running != null) StatusDot()
            Text(title, style = Palma.type.title, color = Palma.colors.ink)
        }
        if (detail != null) {
            Spacer(Modifier.height(Palma.space.unit))
            Text(detail, style = Palma.type.body, color = Palma.colors.muted)
        }
    }
}

@Composable
private fun ColumnScope.Actions(primary: String, onPrimary: () -> Unit, ghost: String?, onGhost: () -> Unit) {
    Spacer(Modifier.height(Palma.space.block))
    Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit)) {
        PrimaryButton(primary, onPrimary)
        if (ghost != null) GhostButton(ghost, onGhost)
    }
}

@Composable
private fun desktopLabel(desktop: String): String =
    if (desktop.equals("none", ignoreCase = true)) stringResource(R.string.image_terminal) else desktop.uppercase()

@Composable
private fun ColumnScope.Ready(ui: HomeUi.Ready, actions: HomeActions) {
    val system = ui.system
    Hero(
        title = system.name,
        sub = stringResource(R.string.home_sub_line, desktopLabel(system.desktop), sizeText(system.sizeBytes), sizeText(ui.freeBytes)),
    )
    if (ui.running) {
        Actions(stringResource(R.string.home_return), actions.onReturn, stringResource(R.string.home_stop), actions.onStop)
    } else {
        Actions(stringResource(R.string.home_launch), actions.onLaunch, stringResource(R.string.home_change), actions.onCatalog)
    }
}

@Composable
private fun ColumnScope.Empty(ui: HomeUi.Empty, actions: HomeActions) {
    val image = ui.suggestion
    Hero(
        title = image.name,
        sub = stringResource(R.string.home_sub_line, desktopLabel(image.desktop), sizeText(image.sizeBytes), sizeText(ui.freeBytes)),
    )
    StateLine(stringResource(R.string.home_empty_state), null, running = null)
    Actions(
        stringResource(R.string.home_install, image.name),
        { actions.onInstall(image) },
        stringResource(R.string.home_open_catalog),
        actions.onCatalog,
    )
}

@Composable
private fun ColumnScope.Installing(ui: HomeUi.Installing, actions: HomeActions) {
    val progress = ui.progress
    Hero(progress.image.name, null)
    StateLine(
        stringResource(R.string.home_installing_state, progress.percent),
        stringResource(R.string.install_downloaded, sizeText(progress.downloaded), sizeText(progress.total)),
        running = null,
    )
    Actions(stringResource(R.string.home_open_install), actions.onOpenInstall, null) {}
}

@Composable
private fun ColumnScope.Loading() {
    Hero(null, null)
    Spacer(Modifier.weight(1f))
}

@Composable
private fun ColumnScope.NoCatalog(ui: HomeUi.NoCatalog, actions: HomeActions) {
    Hero(null, null)
    ErrorWords(failureTitle(ui.failure), failureBody(ui.failure), Modifier.padding(top = Palma.space.unit))
    Actions(stringResource(R.string.install_retry), actions.onRetry, stringResource(R.string.home_open_catalog), actions.onCatalog)
}

@Composable
private fun ColumnScope.Revoked(actions: HomeActions) {
    ErrorWords(stringResource(R.string.home_revoked_title), stringResource(R.string.home_revoked_detail))
    Actions(stringResource(R.string.home_reregister), actions.onReRegister, null) {}
}
