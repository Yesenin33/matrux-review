package ru.palma.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.palma.app.R
import ru.palma.app.billing.Plan
import ru.palma.app.billing.PlanOffer
import ru.palma.app.data.Tier
import ru.palma.app.ui.components.Bullet
import ru.palma.app.ui.components.Card
import ru.palma.app.ui.components.ScreenTitle
import ru.palma.app.ui.components.Tab
import ru.palma.app.ui.components.TabScreen
import ru.palma.app.ui.state.SubscriptionUi
import ru.palma.app.ui.subLineText
import ru.palma.app.ui.theme.Palma

data class SubscriptionActions(
    val onSelect: (Plan) -> Unit = {},
    val onBuy: (PlanOffer) -> Unit = {},
    val onManage: () -> Unit = {},
    val onTab: (Tab) -> Unit = {},
)

@Composable
fun SubscriptionScreen(ui: SubscriptionUi, actions: SubscriptionActions) {
    TabScreen(Tab.SUBSCRIPTION, actions.onTab, scroll = true, bottom = {
        ProBuy(ui, actions)
        Spacer(Modifier.height(Palma.space.unit))
    }) {
        Spacer(Modifier.height(Palma.space.unit))
        ScreenTitle(stringResource(R.string.subs_title), subLineText(ui.sub))
        Spacer(Modifier.height(Palma.space.block))
        Column(verticalArrangement = Arrangement.spacedBy(Palma.space.block)) {
            FreeTier(ui)
            ProTier(ui, actions.onSelect)
        }
        Spacer(Modifier.height(Palma.space.unit * 2))
    }
}

@Composable
private fun FreeTier(ui: SubscriptionUi) {
    val current = ui.status?.tier == Tier.FREE || ui.status == null
    Card {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.subs_free), style = Palma.type.title, color = Palma.colors.ink)
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(if (current) R.string.subs_current else R.string.subs_free_term),
                style = Palma.type.caption,
                color = Palma.colors.muted,
            )
        }
        Spacer(Modifier.height(Palma.space.unit))
        Text(stringResource(R.string.subs_free_price), style = Palma.type.title, color = Palma.colors.ink)
        Spacer(Modifier.height(Palma.space.unit + Palma.space.unit / 2))
        Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit)) {
            Bullet(stringResource(R.string.subs_free_1))
            Bullet(stringResource(R.string.subs_free_2))
        }
    }
}

@Composable
private fun ProTier(ui: SubscriptionUi, onSelect: (Plan) -> Unit) {
    val colors = Palma.colors
    Card {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.pro_title), style = Palma.type.title, color = colors.ink)
            Spacer(Modifier.weight(1f))
            ui.selectedOffer?.let { offer ->
                Text(offer.perMonth, style = Palma.type.caption, color = colors.muted)
            }
        }
        Spacer(Modifier.height(Palma.space.unit + Palma.space.unit / 2))
        ProTermRows(ui, onSelect)
        Spacer(Modifier.height(Palma.space.unit + Palma.space.unit / 2))
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.hairline))
        Spacer(Modifier.height(Palma.space.unit + Palma.space.unit / 2))
        BenefitLines()
    }
}
