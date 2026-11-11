package ru.matrux.app.ui.screens

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
import ru.matrux.app.BuildConfig
import ru.matrux.app.R
import ru.matrux.app.billing.Plan
import ru.matrux.app.billing.PlanOffer
import ru.matrux.app.data.Tier
import ru.matrux.app.ui.components.Bullet
import ru.matrux.app.ui.components.Card
import ru.matrux.app.ui.components.ErrorWords
import ru.matrux.app.ui.components.GhostButton
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.components.PrimaryButton
import ru.matrux.app.ui.components.ScreenTitle
import ru.matrux.app.ui.components.Tab
import ru.matrux.app.ui.components.TabScreen
import ru.matrux.app.ui.components.Tappable
import ru.matrux.app.ui.components.TermPicker
import ru.matrux.app.ui.failureBody
import ru.matrux.app.ui.failureTitle
import ru.matrux.app.ui.state.AccountUi
import ru.matrux.app.ui.state.SubscriptionUi
import ru.matrux.app.ui.subLineText
import ru.matrux.app.ui.theme.Matrux

data class SubscriptionActions(
    val onSelect: (Plan) -> Unit = {},
    val onBuy: (PlanOffer) -> Unit = {},
    val onManage: () -> Unit = {},
    val onTab: (Tab) -> Unit = {},
    val onSignIn: () -> Unit = {},
    val onSignOut: () -> Unit = {},
)

@Composable
fun SubscriptionScreen(ui: SubscriptionUi, actions: SubscriptionActions) {
    TabScreen(Tab.SUBSCRIPTION, actions.onTab, scroll = true, bottom = {
        ui.failure?.let {
            ErrorWords(failureTitle(it), failureBody(it))
            Spacer(Modifier.height(Matrux.space.unit * 2))
        }
        ProBuy(ui, actions)
        if (ui.status?.tier == Tier.PRO) GhostButton(stringResource(R.string.subs_manage), actions.onManage)
        Spacer(Modifier.height(Matrux.space.unit))
    }) {
        Spacer(Modifier.height(Matrux.space.unit))
        ScreenTitle(stringResource(R.string.subs_title), subLineText(ui.sub))
        Spacer(Modifier.height(Matrux.space.block))
        Column(verticalArrangement = Arrangement.spacedBy(Matrux.space.block)) {
            FreeTier(ui)
            ProTier(ui, actions.onSelect)
        }
        Spacer(Modifier.height(Matrux.space.block))
        ui.account?.let {
            Spacer(Modifier.height(Matrux.space.unit))
            AccountLine(it, actions)
        }
        Spacer(Modifier.height(Matrux.space.unit * 2))
    }
}

@Composable
private fun FreeTier(ui: SubscriptionUi) {
    val current = ui.status?.tier == Tier.FREE || ui.status == null
    Card {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.subs_free), style = Matrux.type.title, color = Matrux.colors.ink)
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(if (current) R.string.subs_current else R.string.subs_free_term),
                style = Matrux.type.caption,
                color = Matrux.colors.muted,
            )
        }
        Spacer(Modifier.height(Matrux.space.unit))
        Text(stringResource(R.string.subs_free_price), style = Matrux.type.title, color = Matrux.colors.ink)
        Spacer(Modifier.height(Matrux.space.unit + Matrux.space.unit / 2))
        Column(verticalArrangement = Arrangement.spacedBy(Matrux.space.unit)) {
            Bullet(stringResource(R.string.subs_free_1))
            Bullet(stringResource(R.string.subs_free_2))
        }
    }
}

@Composable
private fun ProTier(ui: SubscriptionUi, onSelect: (Plan) -> Unit) {
    Card {
        Text(stringResource(R.string.pro_title), style = Matrux.type.title, color = Matrux.colors.ink)
        Spacer(Modifier.height(Matrux.space.unit * 2))
        TermPicker(ui.terms, onSelect)
        Spacer(Modifier.height(Matrux.space.card))
        BenefitLines()
    }
}

@Composable
private fun BenefitLines() {
    Column(verticalArrangement = Arrangement.spacedBy(Matrux.space.unit + Matrux.space.unit / 2)) {
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
        LucideIcon(icon, Matrux.colors.inkSoft, size = Matrux.space.iconSmall)
        Spacer(Modifier.width(Matrux.space.unit + Matrux.space.unit / 2))
        Text(stringResource(text), style = Matrux.type.body, color = Matrux.colors.inkSoft, modifier = Modifier.weight(1f))
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
private fun AccountLine(account: AccountUi, actions: SubscriptionActions) {
    val colors = Matrux.colors
    val info = account.info
    if (info == null) {
        if (BuildConfig.AUTH_REQUIRED) {
            GhostButton(
                stringResource(if (account.busy) R.string.account_signing_in else R.string.account_sign_in),
                { if (!account.busy) actions.onSignIn() },
            )
        }
    } else {
        Row(
            Modifier.fillMaxWidth().heightIn(min = Matrux.space.touch),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.account_vkid), style = Matrux.type.body, color = colors.ink)
                info.phone?.let { Text(it, style = Matrux.type.caption, color = colors.muted) }
            }
            Tappable(actions.onSignOut) {
                Text(stringResource(R.string.account_sign_out), style = Matrux.type.body, color = colors.accent)
            }
        }
    }
    account.failure?.let {
        Spacer(Modifier.height(Matrux.space.unit))
        ErrorWords(failureTitle(it), failureBody(it))
    }
}
