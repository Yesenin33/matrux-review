package ru.matrux.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.matrux.app.R
import ru.matrux.app.ui.components.Glyph
import ru.matrux.app.ui.components.GlyphTile
import ru.matrux.app.ui.components.Group
import ru.matrux.app.ui.components.GroupDivider
import ru.matrux.app.ui.components.ListRow
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.components.PrimaryButton
import ru.matrux.app.ui.state.LockedItem
import ru.matrux.app.ui.state.ProOfferUi
import ru.matrux.app.ui.theme.Matrux

@Composable
fun ProOfferOverlay(visible: Boolean, ui: ProOfferUi, onSubscribe: () -> Unit, onClose: () -> Unit) {
    val motion = Matrux.motion
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(motion.duration(motion.medium), easing = motion.easing)) +
            slideInVertically(tween(motion.duration(motion.medium), easing = motion.easing)) { it / 4 },
        exit = fadeOut(tween(motion.duration(motion.short), easing = motion.easing)) +
            slideOutVertically(tween(motion.duration(motion.short), easing = motion.easing)) { it / 4 },
    ) {
        ProOfferWindow(ui, onSubscribe, onClose)
    }
}

/** Нижняя шторка в стиле iOS: что открывает Pro. */
@Composable
fun ProOfferWindow(ui: ProOfferUi, onSubscribe: () -> Unit, onClose: () -> Unit) {
    val colors = Matrux.colors
    val closeLabel = stringResource(R.string.offer_close)
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(colors.canvas)
                .clickable(remember { MutableInteractionSource() }, null) {}
                .navigationBarsPadding()
                .padding(start = Matrux.space.page, end = Matrux.space.page, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(colors.hairline),
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.offer_title),
                    style = Matrux.type.display.copy(fontSize = 28.sp, lineHeight = 34.sp),
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceAlt)
                        .semantics { contentDescription = closeLabel }
                        .clickable(role = Role.Button, onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    LucideIcon(Lucide.X, colors.muted, size = 14.dp)
                }
            }
            Group {
                val items = ui.items
                items.forEachIndexed { index, item ->
                    if (index > 0) GroupDivider()
                    OfferLine(item, ui)
                }
            }
            PrimaryButton(stringResource(R.string.pro_buy), onSubscribe)
        }
    }
}

@Composable
private fun OfferLine(item: LockedItem, ui: ProOfferUi) {
    when (item) {
        LockedItem.SYSTEMS -> ListRow(
            title = stringResource(R.string.offer_systems),
            subtitle = ui.systems.joinToString(", "),
            leading = { GlyphTile(Glyph.Box, Color(0xFF1677E5), size = Matrux.space.tile) },
        )
        LockedItem.COURSES -> ListRow(
            title = stringResource(R.string.offer_courses),
            subtitle = ui.courses.joinToString(", "),
            leading = { GlyphTile(Glyph.Book, Color(0xFFC2570A), size = Matrux.space.tile) },
        )
        LockedItem.SHEETS -> ListRow(
            title = stringResource(R.string.offer_sheets),
            leading = { GlyphTile(Glyph.Doc, Color(0xFF8944D9), size = Matrux.space.tile) },
        )
        LockedItem.OFFLINE -> ListRow(
            title = stringResource(R.string.offer_offline),
            leading = {
                Box(
                    Modifier
                        .size(Matrux.space.tile)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E8A4C)),
                    contentAlignment = Alignment.Center,
                ) {
                    LucideIcon(Lucide.WifiOff, Color.White, size = 20.dp)
                }
            },
        )
    }
}
