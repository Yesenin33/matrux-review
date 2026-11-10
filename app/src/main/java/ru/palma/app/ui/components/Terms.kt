package ru.palma.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import ru.palma.app.billing.Plan
import ru.palma.app.billing.Terms
import ru.palma.app.ui.discountText
import ru.palma.app.ui.perMonthText
import ru.palma.app.ui.rubText
import ru.palma.app.ui.termLabel
import ru.palma.app.ui.theme.Palma
import ru.palma.app.ui.theme.cardShadow

@Composable
fun TermSwitch(terms: Terms, onSelect: (Plan) -> Unit, modifier: Modifier = Modifier) {
    val offers = terms.offers
    if (offers.isEmpty()) return
    val colors = Palma.colors
    val space = Palma.space
    val motion = Palma.motion
    val index = offers.indexOfFirst { it.plan == terms.selected }.coerceAtLeast(0)
    val position by animateFloatAsState(
        targetValue = index.toFloat(),
        animationSpec = tween(motion.duration(motion.short), easing = motion.easing),
        label = "term",
    )
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(Palma.shapes.control)
            .background(colors.canvas)
            .padding(space.trackInset),
    ) {
        val segment = maxWidth / offers.size
        val shift = with(LocalDensity.current) { segment.toPx() }
        Box(
            Modifier
                .width(segment)
                .height(space.touch)
                .graphicsLayer { translationX = position * shift }
                .cardShadow(Palma.shapes.radiusSegment)
                .clip(Palma.shapes.segment)
                .background(colors.surface),
        )
        Row(Modifier.fillMaxWidth().height(space.touch).selectableGroup()) {
            offers.forEach { offer ->
                val selected = offer.plan == terms.selected
                val tint = if (selected) colors.ink else colors.muted
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(Palma.shapes.segment)
                        .selectable(selected, role = Role.RadioButton) { onSelect(offer.plan) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        termLabel(offer.plan),
                        style = if (selected) Palma.type.bodyStrong else Palma.type.body,
                        color = tint,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    offer.discountPercent?.let {
                        Text(
                            discountText(it),
                            style = if (selected) Palma.type.captionStrong else Palma.type.caption,
                            color = tint,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TermPrice(terms: Terms, modifier: Modifier = Modifier) {
    val offer = terms.selectedOffer ?: return
    val price = offer.priceRub ?: return
    Column(modifier) {
        Text(rubText(price), style = Palma.type.display, color = Palma.colors.ink)
        val perMonth = offer.perMonthRub
        if (perMonth != null && offer.plan.months > 1) {
            Spacer(Modifier.height(Palma.space.unit / 2))
            Text(perMonthText(perMonth), style = Palma.type.caption, color = Palma.colors.muted)
        }
    }
}

@Composable
fun TermPicker(terms: Terms, onSelect: (Plan) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        TermSwitch(terms, onSelect)
        Spacer(Modifier.height(Palma.space.card))
        TermPrice(terms)
    }
}
