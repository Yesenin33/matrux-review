package ru.matrux.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.matrux.app.R
import ru.matrux.app.ui.components.Card
import ru.matrux.app.ui.components.DistroLogo
import ru.matrux.app.ui.components.LargeTitle
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.Plaque
import ru.matrux.app.ui.components.PrimaryButton
import ru.matrux.app.ui.components.SoftButton
import ru.matrux.app.ui.components.Spinner
import ru.matrux.app.ui.components.StateBlock
import ru.matrux.app.ui.components.StatusBanner
import ru.matrux.app.ui.components.Tab
import ru.matrux.app.ui.components.TabScreen
import ru.matrux.app.ui.failureTitle
import ru.matrux.app.ui.imageLogo
import ru.matrux.app.ui.sizeText
import ru.matrux.app.ui.state.CatalogRow
import ru.matrux.app.ui.state.CatalogUi
import ru.matrux.app.ui.state.SystemAction
import ru.matrux.app.ui.state.actions
import ru.matrux.app.ui.theme.Matrux

private const val BUILD_CHARS = 8

@Composable
fun CatalogScreen(
    ui: CatalogUi,
    onAction: (CatalogRow, SystemAction) -> Unit,
    onRetry: () -> Unit,
    onTab: (Tab) -> Unit,
) {
    val colors = Matrux.colors
    val failed = ui as? CatalogUi.Failed
    TabScreen(
        Tab.CATALOG,
        onTab,
        scroll = true,
        bottom = if (failed != null) {
            { PrimaryButton(stringResource(R.string.install_retry), onRetry) }
        } else {
            null
        },
    ) {
        Spacer(Modifier.height(Matrux.space.unit))
        LargeTitle(
            stringResource(R.string.catalog_title),
            trailing = if (ui == CatalogUi.Loading) {
                { Spinner() }
            } else {
                null
            },
        )
        Spacer(Modifier.height(14.dp))
        when (ui) {
            CatalogUi.Loading -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(3) { Skeleton() }
            }
            CatalogUi.Empty -> StateBlock(Lucide.List, colors.graySolid, stringResource(R.string.catalog_empty_title))
            is CatalogUi.Failed -> StateBlock(
                Lucide.CircleAlert,
                colors.dangerSolid,
                failureTitle(ui.failure),
                titleColor = colors.danger,
            )
            is CatalogUi.Ready -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (ui.revoked) {
                    StatusBanner(stringResource(R.string.fail_no_access_title), Lucide.CircleAlert, colors.dangerSolid)
                }
                if (ui.offline) {
                    StatusBanner(stringResource(R.string.fail_no_connection_title), Lucide.WifiOff, colors.graySolid)
                }
                ui.rows.forEach { row -> DistroCard(row) { action -> onAction(row, action) } }
            }
        }
        Spacer(Modifier.height(Matrux.space.block))
    }
}

@Composable
private fun Skeleton() {
    val colors = Matrux.colors
    Card {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(Matrux.space.logo).clip(Matrux.shapes.control).background(colors.surfaceAlt))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.width(120.dp).height(16.dp).clip(Matrux.shapes.segment).background(colors.surfaceAlt))
                Box(Modifier.width(190.dp).height(12.dp).clip(Matrux.shapes.segment).background(colors.surfaceAlt))
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(44.dp).clip(Matrux.shapes.pill).background(colors.surfaceAlt))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DistroCard(row: CatalogRow, onAction: (SystemAction) -> Unit) {
    val image = row.image
    val colors = Matrux.colors
    Card(tint = colors.surface) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            DistroLogo(imageLogo(image))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(image.name, style = Matrux.type.title, color = colors.ink, maxLines = 1, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Tag(row)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (image.desktop.equals("none", true)) {
                            stringResource(R.string.image_terminal)
                        } else {
                            stringResource(R.string.image_desktop, image.desktop.uppercase())
                        },
                        style = Matrux.type.caption.copy(fontSize = 14.sp),
                        color = colors.muted,
                    )
                    Text(sizeText(image.sizeBytes), style = Matrux.type.caption.copy(fontFamily = Matrux.type.code.fontFamily), color = colors.muted)
                    Text(
                        stringResource(R.string.catalog_build, image.sha256.take(BUILD_CHARS)),
                        style = Matrux.type.caption.copy(fontFamily = Matrux.type.code.fontFamily),
                        color = colors.muted,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.actions().forEachIndexed { index, action ->
                CardAction(action, main = index == 0, modifier = Modifier.weight(1f)) { onAction(action) }
            }
        }
    }
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
private fun CardAction(action: SystemAction, main: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = Matrux.colors
    val text = actionLabel(action)
    val primary = main && action != SystemAction.SUBSCRIBE && action != SystemAction.STOP
    if (primary) {
        Box(
            modifier
                .height(44.dp)
                .clip(Matrux.shapes.control)
                .background(colors.action)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, style = Matrux.type.bodyStrong, color = colors.onAction, maxLines = 1)
        }
    } else {
        SoftButton(text, onClick, modifier, danger = action == SystemAction.STOP, height = 44.dp)
    }
}

@Composable
private fun Tag(row: CatalogRow) {
    val colors = Matrux.colors
    when {
        row.running -> Plaque(stringResource(R.string.catalog_running), colors.infoSolid)
        row.locked -> Plaque(stringResource(R.string.section_pro), colors.proSolid, icon = Lucide.Lock)
        row.installed -> Plaque(stringResource(R.string.catalog_installed), colors.successSolid)
        row.image.pro -> Unit
        else -> Plaque(stringResource(R.string.catalog_free), colors.successSolid)
    }
}
