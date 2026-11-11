package ru.matrux.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.components.Tab
import ru.matrux.app.ui.components.TabScreen
import ru.matrux.app.ui.state.HomeSection
import ru.matrux.app.ui.state.SectionRow
import ru.matrux.app.ui.theme.Matrux

data class HomeActions(
    val onSection: (SectionRow) -> Unit = {},
    val onSubscribe: () -> Unit = {},
    val onTab: (Tab) -> Unit = {},
)

@Composable
fun HomeScreen(sections: List<HomeSection>, actions: HomeActions) {
    TabScreen(Tab.HOME, actions.onTab, scroll = true) {
        Spacer(Modifier.height(Matrux.space.unit * 2))
        sections.forEachIndexed { index, section ->
            if (index > 0) Spacer(Modifier.height(Matrux.space.block))
            Head(section)
            Spacer(Modifier.height(Matrux.space.unit + Matrux.space.unit / 2))
            Column(verticalArrangement = Arrangement.spacedBy(Matrux.space.unit * 2)) {
                section.rows.forEach { row ->
                    SectionCard(row, paid = false) {
                        if (section.locked) actions.onSubscribe() else actions.onSection(row)
                    }
                }
            }
        }
        Spacer(Modifier.height(Matrux.space.block))
    }
}

@Composable
private fun Head(section: HomeSection) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(section.title), style = Matrux.type.bodyStrong, color = Matrux.colors.muted)
        if (section.locked) {
            Spacer(Modifier.width(Matrux.space.unit))
            LucideIcon(Lucide.Lock, Matrux.colors.muted, size = Matrux.space.iconSmall)
        }
    }
}
