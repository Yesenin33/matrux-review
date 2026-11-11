package ru.matrux.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import ru.matrux.app.R
import ru.matrux.app.ui.components.Card
import ru.matrux.app.ui.components.DistroLogo
import ru.matrux.app.ui.components.ErrorWords
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.components.Note
import ru.matrux.app.ui.components.Pill
import ru.matrux.app.ui.components.PrimaryButton
import ru.matrux.app.ui.components.ScreenTitle
import ru.matrux.app.ui.components.Tab
import ru.matrux.app.ui.components.TabScreen
import ru.matrux.app.ui.distroLogo
import ru.matrux.app.ui.imageLogo
import ru.matrux.app.ui.failureBody
import ru.matrux.app.ui.failureTitle
import ru.matrux.app.ui.imageDescription
import ru.matrux.app.ui.roughDurationText
import ru.matrux.app.ui.sizeText
import ru.matrux.app.ui.state.CatalogRow
import ru.matrux.app.ui.state.SystemAction
import ru.matrux.app.ui.state.actions
import ru.matrux.app.ui.state.CatalogUi
import ru.matrux.app.ui.theme.Matrux

private const val BUILD_CHARS = 8

@Composable
fun CatalogScreen(
    ui: CatalogUi,
    onAction: (CatalogRow, SystemAction) -> Unit,
    onRetry: () -> Unit,
    onTab: (Tab) -> Unit,
) {
    TabScreen(Tab.CATALOG, onTab, scroll = true) {
        Spacer(Modifier.height(Matrux.space.unit))
        val sub = when (ui) {
            is CatalogUi.Ready -> if (ui.rows.size == 3) stringResource(R.string.catalog_sub) else stringResource(R.string.catalog_sub_count, ui.rows.size)
            CatalogUi.Loading -> stringResource(R.string.catalog_loading)
            else -> null
        }
        ScreenTitle(stringResource(R.string.catalog_title), sub)
        Spacer(Modifier.height(Matrux.space.block))
        when (ui) {
            CatalogUi.Loading -> Unit
            CatalogUi.Empty -> ErrorWordsMuted(stringResource(R.string.catalog_empty_title), stringResource(R.string.catalog_empty_detail))
            is CatalogUi.Failed -> {
                ErrorWords(failureTitle(ui.failure), failureBody(ui.failure))
                Spacer(Modifier.height(Matrux.space.block))
                PrimaryButton(stringResource(R.string.install_retry), onRetry)
            }
            is CatalogUi.Ready -> {
                if (ui.revoked) {
                    Note(Lucide.CircleAlert, stringResource(R.string.fail_no_access))
                    Spacer(Modifier.height(Matrux.space.unit * 2))
                }
                if (ui.offline) {
                    Note(Lucide.WifiOff, stringResource(R.string.catalog_offline))
                    Spacer(Modifier.height(Matrux.space.unit * 2))
                }
                Column(verticalArrangement = Arrangement.spacedBy(Matrux.space.unit * 2)) {
                    ui.rows.forEach { row -> DistroCard(row) { action -> onAction(row, action) } }
                }
            }
        }
    }
}

@Composable
private fun ErrorWordsMuted(title: String, body: String) {
    Text(title, style = Matrux.type.title, color = Matrux.colors.ink)
    Spacer(Modifier.height(Matrux.space.unit / 2))
    Text(body, style = Matrux.type.body, color = Matrux.colors.muted)
}

@Composable
private fun DistroCard(row: CatalogRow, onAction: (SystemAction) -> Unit) {
    val image = row.image
    Card {
        Row(horizontalArrangement = Arrangement.spacedBy(Matrux.space.unit * 2), verticalAlignment = Alignment.Top) {
            DistroLogo(imageLogo(image))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(image.name, style = Matrux.type.title, color = Matrux.colors.ink, modifier = Modifier.weight(1f))
                    Tag(row)
                }
                Spacer(Modifier.height(Matrux.space.unit / 2))
                val description = imageDescription(image)?.let { stringResource(it) }
                    ?: if (image.desktop.equals("none", true)) stringResource(R.string.image_terminal)
                    else stringResource(R.string.image_desktop, image.desktop.uppercase())
                Text(description, style = Matrux.type.body, color = Matrux.colors.muted)
                Spacer(Modifier.height(Matrux.space.unit))
                Text(metaLine(row), style = Matrux.type.caption, color = Matrux.colors.muted)
            }
        }
        Spacer(Modifier.height(Matrux.space.unit * 2))
        Row(horizontalArrangement = Arrangement.spacedBy(Matrux.space.unit)) {
            row.actions().forEachIndexed { index, action ->
                CardAction(actionLabel(action), main = index == 0, modifier = Modifier.weight(1f)) { onAction(action) }
            }
        }
    }
}

@Composable
private fun metaLine(row: CatalogRow): String {
    val parts = listOfNotNull(
        sizeText(row.image.sizeBytes),
        stringResource(R.string.catalog_build, row.image.sha256.take(BUILD_CHARS)),
        row.estimateSeconds?.takeIf { !row.installed }?.let { roughDurationText(it) },
    )
    return parts.joinToString(" · ")
}

@Composable
private fun actionLabel(action: SystemAction): String = stringResource(
    when (action) {
        SystemAction.OPEN -> R.string.catalog_open
        SystemAction.STOP -> R.string.catalog_stop
        SystemAction.LAUNCH -> R.string.catalog_launch
        SystemAction.SWITCH -> R.string.catalog_switch
        SystemAction.REINSTALL -> R.string.catalog_reinstall
        SystemAction.INSTALL -> R.string.catalog_install
        SystemAction.SUBSCRIBE -> R.string.catalog_subscribe
    },
)

@Composable
private fun CardAction(text: String, main: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = Matrux.colors
    Box(
        modifier
            .height(Matrux.space.touch)
            .clip(Matrux.shapes.control)
            .background(if (main) colors.accentTint else colors.canvas)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Matrux.type.bodyStrong, color = if (main) colors.accent else colors.ink, maxLines = 1)
    }
}

@Composable
private fun Tag(row: CatalogRow) {
    when {
        row.running -> Pill(stringResource(R.string.catalog_running), Matrux.colors.success, Matrux.colors.successTint)
        row.locked -> LucideIcon(Lucide.Lock, Matrux.colors.muted, size = Matrux.space.iconSmall)
        row.installed -> Pill(stringResource(R.string.catalog_installed), Matrux.colors.success, Matrux.colors.successTint)
        row.image.pro -> Unit
        else -> Pill(stringResource(R.string.catalog_free), Matrux.colors.success, Matrux.colors.successTint)
    }
}
