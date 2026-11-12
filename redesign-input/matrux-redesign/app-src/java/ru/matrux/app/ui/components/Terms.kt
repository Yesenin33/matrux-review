package ru.matrux.app.ui.components

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
import ru.matrux.app.billing.Plan
import ru.matrux.app.billing.Terms
import ru.matrux.app.ui.discountText
import ru.matrux.app.ui.perMonthText
import ru.matrux.app.ui.rubText
import ru.matrux.app.ui.termLabel
import ru.matrux.app.ui.theme.Matrux
import ru.matrux.app.ui.theme.cardShadow

@Composable
fun TermSwitch(terms: Terms, onSelect: (Plan) -> Unit, modifier: Modifier = Modifier) {
    val offers = terms.offers
    if (offers.isEmpty()) return
    val colors = Matrux.colors
    val space = Matrux.space
    val motion = Matrux.motion
    val index = offers.indexOfFirst { it.plan == terms.selected }.coerceAtLeast(0)
    val position by animateFloatAsState(
        targetValue = index.toFloat(),
        animationSpec = tween(motion.duration(motion.short), easing = motion.easing),
        label = "term",
    )
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(Matrux.shapes.control)
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
                .cardShadow(Matrux.shapes.radiusSegment)
                .clip(Matrux.shapes.segment)
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
                        .clip(Matrux.shapes.segment)
                        .selectable(selected, role = Role.RadioButton) { onSelect(offer.plan) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        termLabel(offer.plan),
                        style = if (selected) Matrux.type.bodyStrong else Matrux.type.body,
                        color = tint,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    offer.discountPercent?.let {
                        Text(
                            discountText(it),
                            style = if (selected) Matrux.type.captionStrong else Matrux.type.caption,
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
        Text(rubText(price), style = Matrux.type.display, color = Matrux.colors.ink)
        val perMonth = offer.perMonthRub
        if (perMonth != null && offer.plan.months > 1) {
            Spacer(Modifier.height(Matrux.space.unit / 2))
            Text(perMonthText(perMonth), style = Matrux.type.caption, color = Matrux.colors.muted)
        }
    }
}

@Composable
fun TermPicker(terms: Terms, onSelect: (Plan) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        TermSwitch(terms, onSelect)
        Spacer(Modifier.height(Matrux.space.card))
        TermPrice(terms)
    }
}
