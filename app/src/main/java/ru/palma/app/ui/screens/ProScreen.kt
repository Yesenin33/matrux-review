package ru.palma.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ru.palma.app.R
import ru.palma.app.billing.Plan
import ru.palma.app.billing.PlanOffer
import ru.palma.app.data.Tier
import ru.palma.app.ui.components.Card
import ru.palma.app.ui.components.ErrorWords
import ru.palma.app.ui.components.GhostButton
import ru.palma.app.ui.components.Lucide
import ru.palma.app.ui.components.LucideIcon
import ru.palma.app.ui.components.PenguinMark
import ru.palma.app.ui.components.PrimaryButton
import ru.palma.app.ui.failureBody
import ru.palma.app.ui.failureTitle
import ru.palma.app.ui.state.SubscriptionUi
import ru.palma.app.ui.theme.Palma

@Composable
fun ProScreen(ui: SubscriptionUi, actions: SubscriptionActions, onBack: () -> Unit) {
    val colors = Palma.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.surface)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(Palma.space.touch)
                .clickable(role = Role.Button, onClick = onBack)
                .padding(horizontal = Palma.space.page),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LucideIcon(Lucide.ChevronLeft, colors.accent)
            Spacer(Modifier.width(Palma.space.unit))
            Text(stringResource(R.string.section_back), style = Palma.type.body, color = colors.accent)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Palma.space.page),
        ) {
            ProHead()
            Spacer(Modifier.height(Palma.space.block))
            ProTerms(ui, actions.onSelect)
            Spacer(Modifier.height(Palma.space.block))
            ProBenefits()
            Spacer(Modifier.height(Palma.space.block))
        }
        Column(Modifier.padding(horizontal = Palma.space.page)) {
            if (ui.failure != null) {
                ErrorWords(failureTitle(ui.failure), failureBody(ui.failure))
                Spacer(Modifier.height(Palma.space.unit * 2))
            }
            ProBuy(ui, actions)
            if (ui.status?.tier == Tier.PRO) {
                GhostButton(stringResource(R.string.subs_manage), actions.onManage)
            }
            Spacer(Modifier.height(Palma.space.unit * 2))
        }
    }
}

@Composable
fun ProHead() {
    val colors = Palma.colors
    Spacer(Modifier.height(Palma.space.unit * 2))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(88.dp)
                .clip(Palma.shapes.pill)
                .background(colors.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            PenguinMark(size = 54.dp, silhouette = true, tint = colors.ink)
        }
        Spacer(Modifier.width(Palma.space.block))
        Text(stringResource(R.string.pro_title), style = Palma.type.display, color = colors.ink)
    }
}

@Composable
fun ProTerms(ui: SubscriptionUi, onSelect: (Plan) -> Unit) {
    Card { ProTermRows(ui, onSelect) }
}

@Composable
fun ProTermRows(ui: SubscriptionUi, onSelect: (Plan) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit)) {
        ui.offers.forEach { offer ->
            ProTerm(offer, offer.plan == ui.selected, ui.status?.tier == Tier.PRO) { onSelect(offer.plan) }
        }
    }
}

@Composable
private fun ProTerm(offer: PlanOffer, selected: Boolean, paid: Boolean, onSelect: () -> Unit) {
    val colors = Palma.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(Palma.shapes.control)
            .background(if (selected) colors.accentTint else colors.surfaceAlt)
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = Palma.space.unit + Palma.space.unit / 2, vertical = Palma.space.unit),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(termLabel(offer.plan), style = Palma.type.body, color = colors.ink)
        Spacer(Modifier.weight(1f))
        if (offer.plan.discountPercent > 0) {
            Discount(offer.plan.discountPercent)
            Spacer(Modifier.width(Palma.space.unit))
        } else if (paid) {
            Text(stringResource(R.string.subs_current), style = Palma.type.caption, color = colors.muted)
            Spacer(Modifier.width(Palma.space.unit))
        }
        Text(offer.price, style = Palma.type.bodyStrong, color = colors.ink)
        Spacer(Modifier.width(Palma.space.unit))
        if (selected) LucideIcon(Lucide.BadgeCheck, colors.accent, size = Palma.space.iconSmall)
    }
}

@Composable
private fun Discount(percent: Int) {
    Box(
        Modifier
            .clip(Palma.shapes.pill)
            .background(Palma.colors.successTint)
            .padding(horizontal = Palma.space.unit, vertical = Palma.space.unit / 2),
    ) {
        Text(
            if (percent == 15) stringResource(R.string.pro_off_15) else stringResource(R.string.pro_off_30),
            style = Palma.type.captionStrong,
            color = Palma.colors.success,
        )
    }
}

@Composable
private fun termLabel(plan: Plan): String = stringResource(
    when (plan) {
        Plan.PRO_MONTH -> R.string.pro_term_1
        Plan.PRO_QUARTER -> R.string.pro_term_3
        Plan.PRO_HALF_YEAR -> R.string.pro_term_6
    },
)

@Composable
fun ProBenefits() {
    Card { BenefitLines() }
}

@Composable
fun BenefitLines() {
    Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit + Palma.space.unit / 2)) {
        Benefit(Lucide.Code, R.string.pro_open_1)
        Benefit(Lucide.GitBranch, R.string.pro_open_2)
        Benefit(Lucide.Cpu, R.string.pro_open_3)
        Benefit(Lucide.List, R.string.pro_open_4)
    }
}

@Composable
private fun Benefit(icon: Lucide, text: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LucideIcon(icon, Palma.colors.accent)
        Spacer(Modifier.width(Palma.space.unit + Palma.space.unit / 2))
        Text(stringResource(text), style = Palma.type.body, color = Palma.colors.inkSoft, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ProBuy(ui: SubscriptionUi, actions: SubscriptionActions) {
    val offer = ui.selectedOffer ?: return
    val label = when {
        ui.busy -> stringResource(R.string.subs_busy)
        ui.trialAvailable -> stringResource(R.string.subs_try, offer.trialDays)
        else -> stringResource(R.string.pro_buy)
    }
    PrimaryButton(label, { actions.onBuy(offer) }, enabled = !ui.busy && ui.canBuy)
}
