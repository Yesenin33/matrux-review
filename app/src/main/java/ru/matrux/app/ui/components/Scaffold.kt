package ru.matrux.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import ru.matrux.app.R
import ru.matrux.app.ui.theme.Matrux

enum class Tab(val label: Int, val icon: Lucide) {
    HOME(R.string.tab_home, Lucide.House),
    CATALOG(R.string.tab_catalog, Lucide.List),
    (R.string.tab_, Lucide.Joystick),
    KNOWLEDGE(R.string.tab_knowledge, Lucide.BookOpen),
    SUBSCRIPTION(R.string.tab_subscription, Lucide.CreditCard),
}

@Composable
fun TabScreen(
    tab: Tab?,
    onTab: (Tab) -> Unit,
    scroll: Boolean = false,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Matrux.colors.canvas),
    ) {
        AppBackdrop()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = Matrux.space.page)
                    .padding(top = Matrux.space.unit),
                content = content,
            )
            if (bottom != null) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Matrux.space.page)
                        .padding(top = Matrux.space.unit * 2),
                    content = bottom,
                )
            }
            if (tab != null) NavBar(tab, onTab)
        }
    }
}

@Composable
fun NavBar(current: Tab, onTab: (Tab) -> Unit) {
    val colors = Matrux.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .navigationBarsPadding()
            .height(Matrux.space.navBar),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { tab ->
            val tint = if (tab == current) colors.accent else colors.muted
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics { selected = tab == current }
                    .clickable(role = Role.Tab) { onTab(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                LucideIcon(tab.icon, tint)
                Spacer(Modifier.height(Matrux.space.unit / 2))
                Text(stringResource(tab.label), style = Matrux.type.caption, color = tint)
            }
        }
    }
}

@Composable
fun ScreenTitle(title: String, sub: String?) {
    Text(title, style = Matrux.type.display, color = Matrux.colors.ink)
    if (sub != null) {
        Spacer(Modifier.height(Matrux.space.unit))
        Text(sub, style = Matrux.type.body, color = Matrux.colors.muted)
    }
}
