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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.matrux.app.BuildConfig
import ru.matrux.app.R
import ru.matrux.app.billing.Plan
import ru.matrux.app.billing.PlanOffer
import ru.matrux.app.data.Tier
import ru.matrux.app.ui.components.Card
import ru.matrux.app.ui.components.Glyph
import ru.matrux.app.ui.components.GlyphTile
import ru.matrux.app.ui.components.Group
import ru.matrux.app.ui.components.LargeTitle
import ru.matrux.app.ui.components.ListRow
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.components.Plaque
import ru.matrux.app.ui.components.PrimaryButton
import ru.matrux.app.ui.components.SoftButton
import ru.matrux.app.ui.components.Tab
import ru.matrux.app.ui.components.TabScreen
import ru.matrux.app.ui.components.TermPicker
import ru.matrux.app.ui.failureTitle
import ru.matrux.app.ui.state.AccountUi
import ru.matrux.app.ui.state.SubLine
import ru.matrux.app.ui.state.SubscriptionUi
import ru.matrux.app.ui.state.paidSub
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
    val colors = Matrux.colors
    TabScreen(Tab.SUBSCRIPTION, actions.onTab, scroll = true, bottom = {
        ui.failure?.let {
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.Center) {
                Plaque(failureTitle(it), colors.dangerSolid, icon = Lucide.CircleAlert)
            }
        }
        if (ui.paid) {
            if (ui.status?.tier == Tier.PRO) {
                SoftButton(
                    stringResource(R.string.subs_manage),
                    actions.onManage,
                    Modifier.fillMaxWidth(),
                    danger = true,
                    height = Matrux.space.button,
                )
            }
        } else {
            ProBuy(ui, actions)
        }
        Spacer(Modifier.height(Matrux.space.unit))
    }) {
        Spacer(Modifier.height(Matrux.space.unit))
        LargeTitle(stringResource(R.string.subs_title), trailing = { StateTag(ui) })
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ProTier(ui, actions.onSelect)
            ui.account?.let { AccountGroup(it, actions) }
            FreeTier(ui)
        }
        Spacer(Modifier.height(Matrux.space.block))
    }
}

@Composable
private fun StateTag(ui: SubscriptionUi) {
    val colors = Matrux.colors
    val text = subLineText(ui.sub)
    val modifier = Modifier.widthIn(max = 190.dp)
    when {
        ui.sub.paidSub() -> Plaque(text, colors.successSolid, modifier, icon = Lucide.Check)
        ui.sub is SubLine.Revoked -> Plaque(text, colors.dangerSolid, modifier, icon = Lucide.CircleAlert)
        else -> Plaque(text, colors.graySolid, modifier)
    }
}

@Composable
private fun FreeTier(ui: SubscriptionUi) {
    val colors = Matrux.colors
    val current = ui.status?.tier == Tier.FREE || ui.status == null
    Card {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.subs_free), style = Matrux.type.title, color = colors.ink)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.subs_free_price), style = Matrux.type.value.copy(fontSize = 15.sp), color = colors.muted)
            Spacer(Modifier.weight(1f))
            if (current) {
                Plaque(stringResource(R.string.subs_current), colors.infoSolid)
            } else {
                Plaque(stringResource(R.string.subs_free_term), colors.graySolid)
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Benefit(stringResource(R.string.subs_free_1), colors.success)
            Benefit(stringResource(R.string.subs_free_2), colors.success)
        }
    }
}

@Composable
private fun ProTier(ui: SubscriptionUi, onSelect: (Plan) -> Unit) {
    val colors = Matrux.colors
    Card {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.pro_title), style = Matrux.type.title, color = colors.ink, modifier = Modifier.weight(1f))
            if (ui.paid) Plaque(stringResource(R.string.subs_current), colors.infoSolid)
        }
        Spacer(Modifier.height(14.dp))
        TermPicker(ui.terms, onSelect)
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Benefit(stringResource(R.string.pro_open_systems), colors.accent)
            Benefit(stringResource(R.string.offer_courses), colors.accent)
            Benefit(stringResource(R.string.offer_sheets), colors.accent)
            Benefit(stringResource(R.string.offer_offline), colors.accent)
        }
    }
}

@Composable
private fun Benefit(text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LucideIcon(Lucide.Check, tint, size = 20.dp)
        Text(text, style = Matrux.type.body.copy(fontSize = 15.sp, lineHeight = 20.sp), color = Matrux.colors.ink, modifier = Modifier.weight(1f))
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
private fun AccountGroup(account: AccountUi, actions: SubscriptionActions) {
    val colors = Matrux.colors
    val info = account.info
    if (info == null && !BuildConfig.AUTH_REQUIRED) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Group {
            ListRow(
                title = stringResource(R.string.account_vkid),
                subtitle = info?.phone,
                leading = { GlyphTile(Glyph.Users, Color(0xFF1677E5), size = Matrux.space.tile) },
                trailing = {
                    if (info != null) {
                        Box(
                            Modifier
                                .height(Matrux.space.touch)
                                .clickable(role = Role.Button, onClick = actions.onSignOut),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(stringResource(R.string.account_sign_out), style = Matrux.type.bodyStrong, color = colors.danger)
                        }
                    } else {
                        Box(
                            Modifier
                                .height(34.dp)
                                .clip(Matrux.shapes.pill)
                                .background(colors.action)
                                .clickable(role = Role.Button) { if (!account.busy) actions.onSignIn() }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                stringResource(if (account.busy) R.string.account_signing_in else R.string.account_sign_in),
                                style = Matrux.type.bodyStrong.copy(fontSize = 14.sp, lineHeight = 18.sp),
                                color = colors.onAction,
                                maxLines = 1,
                            )
                        }
                    }
                },
            )
        }
        account.failure?.let { Plaque(failureTitle(it), colors.dangerSolid, icon = Lucide.CircleAlert) }
    }
}
