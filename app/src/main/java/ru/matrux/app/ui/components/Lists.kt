package ru.matrux.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.matrux.app.ui.theme.Matrux

/** Сгруппированный список в стиле iOS: одна серая поверхность, строки разделены тонкой линией. */
@Composable
fun Group(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Matrux.shapes.control)
            .background(Matrux.colors.surface),
        content = content,
    )
}

@Composable
fun GroupDivider(inset: Dp = Matrux.space.rowInset) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = inset)
            .height(1.dp)
            .background(Matrux.colors.hairline),
    )
}

@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    titleColor: Color = Matrux.colors.ink,
    titleMaxLines: Int = 2,
) {
    val colors = Matrux.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Matrux.space.row)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = Matrux.space.card, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = Matrux.type.bodyStrong,
                color = titleColor,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(subtitle, style = Matrux.type.caption, color = colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/** Строка «название — значение»: значение моноширинным шрифтом. */
@Composable
fun ValueRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Matrux.space.row)
            .padding(horizontal = Matrux.space.card, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = Matrux.type.body, color = Matrux.colors.ink)
        Spacer(Modifier.width(12.dp))
        Text(value, style = Matrux.type.value, color = Matrux.colors.muted, maxLines = 1)
    }
}

@Composable
fun LargeTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text,
            style = Matrux.type.display,
            color = Matrux.colors.ink,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = Matrux.type.title,
            color = Matrux.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/** Тёмная панель с приглашением оболочки: фирменный элемент «Linux». */
@Composable
fun TerminalPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = Matrux.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Matrux.shapes.card)
            .background(colors.terminal)
            .border(1.dp, colors.terminalEdge, Matrux.shapes.card)
            .padding(Matrux.space.card),
        content = content,
    )
}

/** Строка вида `matrux@debian:~$ команда`. */
@Composable
fun PromptLine(host: String, modifier: Modifier = Modifier, command: String = "") {
    val colors = Matrux.colors
    val prompt = androidx.compose.ui.text.buildAnnotatedString {
        pushStyle(androidx.compose.ui.text.SpanStyle(color = colors.terminalOk))
        append(host)
        pop()
        pushStyle(androidx.compose.ui.text.SpanStyle(color = colors.terminalDim))
        append(":")
        pop()
        pushStyle(androidx.compose.ui.text.SpanStyle(color = colors.terminalAccent))
        append("~")
        pop()
        pushStyle(androidx.compose.ui.text.SpanStyle(color = colors.terminalDim))
        append("$ ")
        pop()
        pushStyle(androidx.compose.ui.text.SpanStyle(color = colors.terminalInk))
        append(command)
        pop()
    }
    Text(prompt, style = Matrux.type.code.copy(fontSize = 14.sp), modifier = modifier, maxLines = 1)
}


/** Крупный значок состояния по центру и название: пусто, ошибка, ожидание. */
@Composable
fun StateBlock(
    icon: Lucide,
    background: Color,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color = Matrux.colors.ink,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                .background(background),
            contentAlignment = Alignment.Center,
        ) {
            LucideIcon(icon, Color.White, size = 32.dp)
        }
        Text(
            title,
            style = Matrux.type.title.copy(fontSize = 22.sp, lineHeight = 28.sp),
            color = titleColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/** Полоса состояния на всю ширину: «Нет связи», «Нет доступа». */
@Composable
fun StatusBanner(text: String, icon: Lucide, background: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(Matrux.shapes.control)
            .background(background)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LucideIcon(icon, Color.White, size = 18.dp)
        Text(text, style = Matrux.type.bodyStrong.copy(fontSize = 15.sp, lineHeight = 20.sp), color = Color.White, maxLines = 1)
    }
}
