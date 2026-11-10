package ru.palma.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import ru.palma.app.ui.components.Tappable
import ru.palma.app.ui.components.TermPicker
import ru.palma.app.ui.failureBody
import ru.palma.app.ui.failureTitle
import ru.palma.app.ui.state.AccountUi
import ru.palma.app.ui.state.SubscriptionUi
import ru.palma.app.ui.subLineText
import ru.palma.app.ui.theme.Palma

data class SubscriptionActions(
    val onSelect: (Plan) -> Unit = {},
    val onBuy: (PlanOffer) -> Unit = {},
    val onManage: () -> Unit = {},
    val onTab: (Tab) -> Unit = {},
    val onSignIn: () -> Unit = {},
    val onSignOut: () -> Unit = {},
    val onOnboarding: () -> Unit = {},
)

@Composable
fun SubscriptionScreen(ui: SubscriptionUi, actions: SubscriptionActions) {
    TabScreen(Tab.SUBSCRIPTION, actions.onTab, scroll = true, bottom = {
        ui.failure?.let {
            ErrorWords(failureTitle(it), failureBody(it))
            Spacer(Modifier.height(Palma.space.unit * 2))
        }
        ProBuy(ui, actions)
        if (ui.status?.tier == Tier.PRO) GhostButton(stringResource(R.string.subs_manage), actions.onManage)
        Spacer(Modifier.height(Palma.space.unit))
    }) {
        Spacer(Modifier.height(Palma.space.unit))
        ScreenTitle(stringResource(R.string.subs_title), subLineText(ui.sub))
        Spacer(Modifier.height(Palma.space.block))
        Column(verticalArrangement = Arrangement.spacedBy(Palma.space.block)) {
            FreeTier(ui)
            ProTier(ui, actions.onSelect)
        }
        Spacer(Modifier.height(Palma.space.block))
        OnboardingLine(actions.onOnboarding)
        ui.account?.let {
            Spacer(Modifier.height(Palma.space.unit))
            AccountLine(it, actions)
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
    Card {
        Text(stringResource(R.string.pro_title), style = Palma.type.title, color = Palma.colors.ink)
        Spacer(Modifier.height(Palma.space.unit * 2))
        TermPicker(ui.terms, onSelect)
        Spacer(Modifier.height(Palma.space.card))
        BenefitLines()
    }
}

@Composable
private fun BenefitLines() {
    Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit + Palma.space.unit / 2)) {
        Benefit(Lucide.Terminal, R.string.pro_open_systems)
        Benefit(Lucide.Code, R.string.pro_open_1)
        Benefit(Lucide.GitBranch, R.string.pro_open_2)
        Benefit(Lucide.Cpu, R.string.pro_open_3)
        Benefit(Lucide.List, R.string.pro_open_4)
    }
}

@Composable
private fun Benefit(icon: Lucide, text: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LucideIcon(icon, Palma.colors.inkSoft, size = Palma.space.iconSmall)
        Spacer(Modifier.width(Palma.space.unit + Palma.space.unit / 2))
        Text(stringResource(text), style = Palma.type.body, color = Palma.colors.inkSoft, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ProBuy(ui: SubscriptionUi, actions: SubscriptionActions) {
    val offer = ui.selectedOffer ?: return
    val label = when {
        ui.busy -> stringResource(R.string.subs_busy)
        !ui.billingReady && !ui.paid -> stringResource(R.string.pro_unavailable)
        ui.trialAvailable -> stringResource(R.string.subs_try, offer.trialDays)
        else -> stringResource(R.string.pro_buy)
    }
    PrimaryButton(label, { actions.onBuy(offer) }, enabled = !ui.busy && ui.canBuy)
}

@Composable
private fun OnboardingLine(onOpen: () -> Unit) {
    Tappable(onOpen, Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.subs_onboarding, stringResource(R.string.app_name)),
            style = Palma.type.body,
            color = Palma.colors.ink,
            modifier = Modifier.weight(1f),
        )
        LucideIcon(Lucide.ChevronRight, Palma.colors.muted, size = Palma.space.iconSmall)
    }
}

@Composable
private fun AccountLine(account: AccountUi, actions: SubscriptionActions) {
    val colors = Palma.colors
    val info = account.info
    if (info == null) {
        GhostButton(
            stringResource(if (account.busy) R.string.account_signing_in else R.string.account_sign_in),
            { if (!account.busy) actions.onSignIn() },
        )
    } else {
        Row(
            Modifier.fillMaxWidth().heightIn(min = Palma.space.touch),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.account_vkid), style = Palma.type.body, color = colors.ink)
                info.phone?.let { Text(it, style = Palma.type.caption, color = colors.muted) }
            }
            Tappable(actions.onSignOut) {
                Text(stringResource(R.string.account_sign_out), style = Palma.type.body, color = colors.accent)
            }
        }
    }
    account.failure?.let {
        Spacer(Modifier.height(Palma.space.unit))
        ErrorWords(failureTitle(it), failureBody(it))
    }
}
