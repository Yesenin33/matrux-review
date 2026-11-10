package ru.palma.app.ui.screens

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
import ru.palma.app.R
import ru.palma.app.ui.components.Card
import ru.palma.app.ui.components.DistroLogo
import ru.palma.app.ui.components.ErrorWords
import ru.palma.app.ui.components.Lucide
import ru.palma.app.ui.components.LucideIcon
import ru.palma.app.ui.components.Note
import ru.palma.app.ui.components.Pill
import ru.palma.app.ui.components.PrimaryButton
import ru.palma.app.ui.components.ScreenTitle
import ru.palma.app.ui.components.Tab
import ru.palma.app.ui.components.TabScreen
import ru.palma.app.ui.distroLogo
import ru.palma.app.ui.imageLogo
import ru.palma.app.ui.failureBody
import ru.palma.app.ui.failureTitle
import ru.palma.app.ui.imageDescription
import ru.palma.app.ui.roughDurationText
import ru.palma.app.ui.sizeText
import ru.palma.app.ui.state.CatalogRow
import ru.palma.app.ui.state.SystemAction
import ru.palma.app.ui.state.actions
import ru.palma.app.ui.state.CatalogUi
import ru.palma.app.ui.theme.Palma

private const val BUILD_CHARS = 8

@Composable
fun CatalogScreen(
    ui: CatalogUi,
    onAction: (CatalogRow, SystemAction) -> Unit,
    onRetry: () -> Unit,
    onTab: (Tab) -> Unit,
) {
    TabScreen(Tab.CATALOG, onTab, scroll = true) {
        Spacer(Modifier.height(Palma.space.unit))
        val sub = when (ui) {
            is CatalogUi.Ready -> if (ui.rows.size == 3) stringResource(R.string.catalog_sub) else stringResource(R.string.catalog_sub_count, ui.rows.size)
            CatalogUi.Loading -> stringResource(R.string.catalog_loading)
            else -> null
        }
        ScreenTitle(stringResource(R.string.catalog_title), sub)
        Spacer(Modifier.height(Palma.space.block))
        when (ui) {
            CatalogUi.Loading -> Unit
            CatalogUi.Empty -> ErrorWordsMuted(stringResource(R.string.catalog_empty_title), stringResource(R.string.catalog_empty_detail))
            is CatalogUi.Failed -> {
                ErrorWords(failureTitle(ui.failure), failureBody(ui.failure))
                Spacer(Modifier.height(Palma.space.block))
                PrimaryButton(stringResource(R.string.install_retry), onRetry)
            }
            is CatalogUi.Ready -> {
                if (ui.revoked) {
                    Note(Lucide.CircleAlert, stringResource(R.string.fail_no_access))
                    Spacer(Modifier.height(Palma.space.unit * 2))
                }
                if (ui.offline) {
                    Note(Lucide.WifiOff, stringResource(R.string.catalog_offline))
                    Spacer(Modifier.height(Palma.space.unit * 2))
                }
                Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit * 2)) {
                    ui.rows.forEach { row -> DistroCard(row) { action -> onAction(row, action) } }
                }
            }
        }
    }
}

@Composable
private fun ErrorWordsMuted(title: String, body: String) {
    Text(title, style = Palma.type.title, color = Palma.colors.ink)
    Spacer(Modifier.height(Palma.space.unit / 2))
    Text(body, style = Palma.type.body, color = Palma.colors.muted)
}

@Composable
private fun DistroCard(row: CatalogRow, onAction: (SystemAction) -> Unit) {
    val image = row.image
    Card {
        Row(horizontalArrangement = Arrangement.spacedBy(Palma.space.unit * 2), verticalAlignment = Alignment.Top) {
            DistroLogo(imageLogo(image))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(image.name, style = Palma.type.title, color = Palma.colors.ink, modifier = Modifier.weight(1f))
                    Tag(row)
                }
                Spacer(Modifier.height(Palma.space.unit / 2))
                val description = imageDescription(image)?.let { stringResource(it) }
                    ?: if (image.desktop.equals("none", true)) stringResource(R.string.image_terminal)
                    else stringResource(R.string.image_desktop, image.desktop.uppercase())
                Text(description, style = Palma.type.body, color = Palma.colors.muted)
                Spacer(Modifier.height(Palma.space.unit))
                Text(metaLine(row), style = Palma.type.caption, color = Palma.colors.muted)
            }
        }
        Spacer(Modifier.height(Palma.space.unit * 2))
        Row(horizontalArrangement = Arrangement.spacedBy(Palma.space.unit)) {
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
    val colors = Palma.colors
    Box(
        modifier
            .height(Palma.space.touch)
            .clip(Palma.shapes.control)
            .background(if (main) colors.accentTint else colors.canvas)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Palma.type.bodyStrong, color = if (main) colors.accent else colors.ink, maxLines = 1)
    }
}

@Composable
private fun Tag(row: CatalogRow) {
    when {
        row.running -> Pill(stringResource(R.string.catalog_running), Palma.colors.success, Palma.colors.successTint)
        row.locked -> LucideIcon(Lucide.Lock, Palma.colors.muted, size = Palma.space.iconSmall)
        row.installed -> Pill(stringResource(R.string.catalog_installed), Palma.colors.success, Palma.colors.successTint)
        row.image.pro -> Unit
        else -> Pill(stringResource(R.string.catalog_free), Palma.colors.success, Palma.colors.successTint)
    }
}
