package ru.palma.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import ru.palma.app.R
import ru.palma.app.billing.Plan
import ru.palma.app.billing.PlanOffer
import ru.palma.app.data.Tier
import ru.palma.app.ui.components.Bullet
import ru.palma.app.ui.components.Card
import ru.palma.app.ui.components.ErrorWords
import ru.palma.app.ui.components.GhostButton
import ru.palma.app.ui.components.Lucide
import ru.palma.app.ui.components.LucideIcon
import ru.palma.app.ui.components.PrimaryButton
import ru.palma.app.ui.components.ScreenTitle
import ru.palma.app.ui.components.Tab
import ru.palma.app.ui.components.TabScreen
import ru.palma.app.ui.failureBody
import ru.palma.app.ui.failureTitle
import ru.palma.app.ui.state.SubLine
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
        Bottom(ui, actions)
        Spacer(Modifier.height(Palma.space.unit * 2))
    }) {
        Spacer(Modifier.height(Palma.space.unit))
        val sub = when (ui.sub) {
            is SubLine.ProUntil, SubLine.Lifetime, SubLine.Revoked, is SubLine.Offline -> subLineText(ui.sub)
            else -> stringResource(R.string.subs_sub)
        }
        ScreenTitle(stringResource(R.string.subs_title), sub)
        Spacer(Modifier.height(Palma.space.block))
        Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit + Palma.space.unit / 2)) {
            FreeTier(current = ui.status?.tier == Tier.FREE || ui.status == null)
            ProTier(ui, actions)
            LifetimeTier(ui, actions)
        }
        Spacer(Modifier.height(Palma.space.unit * 2))
    }
}

@Composable
private fun TierCard(selected: Boolean, onClick: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    val modifier = Modifier
        .then(if (selected) Modifier.border(Palma.space.unit / 4, Palma.colors.accent, Palma.shapes.card) else Modifier)
        .then(if (onClick != null) Modifier.clickable(role = Role.RadioButton, onClick = onClick) else Modifier)
    Card(modifier, content = content)
}

@Composable
private fun Head(title: String, side: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = Palma.type.title, color = Palma.colors.ink)
        side()
    }
}

@Composable
private fun Side(text: String) {
    Text(text, style = Palma.type.caption, color = Palma.colors.muted)
}

@Composable
private fun Price(text: String, unit: String?) {
    Spacer(Modifier.height(Palma.space.unit / 2))
    Row(verticalAlignment = Alignment.Bottom) {
        Text(text, style = Palma.type.title, color = Palma.colors.ink)
        if (unit != null) Text(" / $unit", style = Palma.type.body, color = Palma.colors.muted)
    }
}

@Composable
private fun Bullets(vararg lines: Int) {
    Spacer(Modifier.height(Palma.space.unit))
    Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit / 2)) {
        lines.forEach { Bullet(stringResource(it)) }
    }
}

@Composable
private fun FreeTier(current: Boolean) {
    TierCard(selected = false, onClick = null) {
        Head(stringResource(R.string.subs_free)) { Side(stringResource(if (current) R.string.subs_current else R.string.subs_free_term)) }
        Price(stringResource(R.string.subs_free_price), null)
        Bullets(R.string.subs_free_1, R.string.subs_free_2)
    }
}

@Composable
private fun ProTier(ui: SubscriptionUi, actions: SubscriptionActions) {
    val selected = ui.selected == Plan.PRO_MONTH || ui.selected == Plan.PRO_YEAR
    val month = ui.offers.first { it.plan == Plan.PRO_MONTH }
    val year = ui.offers.first { it.plan == Plan.PRO_YEAR }
    TierCard(selected, onClick = { if (!selected) actions.onSelect(Plan.PRO_MONTH) }) {
        Head(stringResource(R.string.subs_pro)) {
            when {
                ui.status?.tier == Tier.PRO -> Side(stringResource(R.string.subs_current))
                ui.trialAvailable || (!ui.paid && month.trialDays > 0) -> Pill(stringResource(R.string.subs_trial, month.trialDays))
            }
        }
        val yearly = ui.selected == Plan.PRO_YEAR
        val shown = if (yearly) year else month
        if (shown.price.isNotBlank()) {
            Price(shown.price, stringResource(if (yearly) R.string.subs_unit_year else R.string.subs_unit_month))
        } else {
            Spacer(Modifier.height(Palma.space.unit / 2))
            Text(stringResource(R.string.subs_price_store), style = Palma.type.body, color = Palma.colors.muted)
        }
        Period(
            stringResource(if (yearly) R.string.subs_switch_month else R.string.subs_switch_year),
        ) { actions.onSelect(if (yearly) Plan.PRO_MONTH else Plan.PRO_YEAR) }
        Bullets(R.string.subs_pro_1, R.string.subs_pro_2)
    }
}

@Composable
private fun Period(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Palma.space.touch)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Palma.type.body, color = Palma.colors.accent)
    }
}

@Composable
private fun LifetimeTier(ui: SubscriptionUi, actions: SubscriptionActions) {
    val offer = ui.offers.first { it.plan == Plan.LIFETIME }
    val selected = ui.selected == Plan.LIFETIME
    TierCard(selected, onClick = { actions.onSelect(Plan.LIFETIME) }) {
        Head(stringResource(R.string.subs_lifetime)) {
            Side(stringResource(if (ui.status?.tier == Tier.LIFETIME) R.string.subs_current else R.string.subs_lifetime_term))
        }
        if (offer.price.isNotBlank()) Price(offer.price, null) else Side(stringResource(R.string.subs_price_store))
        Bullets(R.string.subs_lifetime_1, R.string.subs_lifetime_2)
    }
}

@Composable
private fun Pill(text: String) {
    Box(
        Modifier
            .clip(Palma.shapes.pill)
            .background(Palma.colors.accentTint)
            .padding(horizontal = Palma.space.unit + Palma.space.unit / 2, vertical = Palma.space.unit / 2),
    ) {
        Text(text, style = Palma.type.captionStrong, color = Palma.colors.accent)
    }
}

@Composable
private fun Bottom(ui: SubscriptionUi, actions: SubscriptionActions) {
    val offer = ui.selectedOffer
    if (ui.failure != null) {
        ErrorWords(failureTitle(ui.failure), failureBody(ui.failure))
        Spacer(Modifier.height(Palma.space.unit * 2))
    }
    if (offer != null && ui.canBuy) {
        val label = when {
            ui.busy -> stringResource(R.string.subs_busy)
            offer.plan == Plan.LIFETIME -> stringResource(R.string.subs_buy_lifetime)
            ui.trialAvailable -> stringResource(R.string.subs_try, offer.trialDays)
            else -> stringResource(R.string.subs_buy_pro)
        }
        PrimaryButton(label, { actions.onBuy(offer) }, enabled = !ui.busy)
    }
    if (ui.status?.tier == Tier.PRO) {
        GhostButton(stringResource(R.string.subs_manage), actions.onManage)
    }
}
