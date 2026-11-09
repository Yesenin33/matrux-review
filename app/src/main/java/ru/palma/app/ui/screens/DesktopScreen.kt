package ru.palma.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt
import ru.palma.app.R
import ru.palma.app.data.Failure
import ru.palma.app.shell.DisplayMode
import ru.palma.app.ui.Keys
import ru.palma.app.ui.components.Lucide
import ru.palma.app.ui.components.LucideIcon
import ru.palma.app.ui.components.PrimaryButton
import ru.palma.app.ui.components.Tappable
import ru.palma.app.ui.failureBody
import ru.palma.app.ui.theme.Palma

enum class InputMode { Touch, Mouse }

data class DesktopUi(
    val title: String,
    val mode: DisplayMode,
    val offline: Boolean,
    val frame: Bitmap?,
    val frameStamp: Long = 0L,
    val lost: Failure?,
    val stuck: Boolean = false,
    val report: String = "",
    val input: InputMode = InputMode.Touch,
    val pad: Boolean = false,
    val keyboard: Boolean = false,
    val russian: Boolean = true,
    val net: String = "",
)

data class DesktopActions(
    val onClose: () -> Unit = {},
    val onMinimize: () -> Unit = {},
    val onRestart: () -> Unit = {},
    val onReport: () -> Unit = {},
    val onPointer: (x: Int, y: Int, buttons: Int) -> Unit = { _, _, _ -> },
    val onMoveCursor: (dx: Int, dy: Int) -> Unit = { _, _ -> },
    val onClick: (button: Int) -> Unit = {},
    val onScroll: (steps: Int) -> Unit = {},
    val onKey: (keysym: Int) -> Unit = {},
    val onChar: (letter: Char) -> Unit = {},
    val onToggleInput: () -> Unit = {},
    val onTogglePad: () -> Unit = {},
    val onToggleKeyboard: () -> Unit = {},
    val onToggleLayout: () -> Unit = {},
    val onConsole: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onRotate: () -> Unit = {},
    val onNetwork: () -> Unit = {},
)

@Composable
fun DesktopScreen(ui: DesktopUi, actions: DesktopActions) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            ui.lost != null -> Lost(ui.lost, actions)
            ui.frame != null -> Stage(ui.frame, ui.frameStamp, ui.input, actions)
            ui.mode == DisplayMode.X11 -> Waiting(stringResource(R.string.desktop_x11_native))
            else -> Waiting(stringResource(R.string.desktop_waiting))
        }
        if (ui.lost != null) return@Box
        if (ui.stuck) {
            Report(ui.report, actions)
            return@Box
        }
        KeyboardSink(ui.keyboard, actions.onKey, actions.onChar)
        ControlBar(ui, actions, Modifier.align(Alignment.TopCenter))
        if (ui.pad) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                CursorPad(actions)
            }
        }
    }
}

@Composable
private fun ControlBar(ui: DesktopUi, actions: DesktopActions, modifier: Modifier = Modifier) {
    Row(
        modifier
            .padding(horizontal = Palma.space.unit / 2, vertical = Palma.space.unit / 2)
            .fillMaxWidth()
            .clip(Palma.shapes.pill)
            .background(Color.Black.copy(alpha = 0.80f))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(Palma.space.unit / 3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarButton(Lucide.Keyboard, ui.keyboard, actions.onToggleKeyboard)
        BarButton(
            if (ui.input == InputMode.Touch) Lucide.Hand else Lucide.Mouse,
            ui.input == InputMode.Mouse,
            actions.onToggleInput,
        )
        BarButton(Lucide.Joystick, ui.pad, actions.onTogglePad)
        BarButton(Lucide.Terminal, false, actions.onConsole)
        BarButton(Lucide.Sliders, false, actions.onSettings)
        BarButton(Lucide.Globe, ui.russian, actions.onToggleLayout)
        BarButton(Lucide.RotateCw, false, actions.onRotate)
        Box(Modifier.weight(1f))
        BarButton(Lucide.ChevronDown, false, actions.onMinimize)
        BarButton(Lucide.X, false, actions.onClose)
    }
}

@Composable
private fun BarButton(icon: Lucide, active: Boolean, onClick: () -> Unit) {
    Tappable(onClick) {
        Box(
            Modifier
                .height(28.dp)
                .clip(Palma.shapes.pill)
                .background(if (active) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = if (active) 0.92f else 0.22f), Palma.shapes.pill)
                .padding(horizontal = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            LucideIcon(icon, if (active) Color.Black else Color.White, size = 20.dp)
        }
    }
}

@Composable
private fun CursorPad(actions: DesktopActions) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Palma.space.unit, vertical = Palma.space.unit / 2),
        horizontalArrangement = Arrangement.spacedBy(Palma.space.unit / 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Trackpad(Modifier.weight(1f), actions)
        Column(
            Modifier.height(Palma.space.touch * 2),
            verticalArrangement = Arrangement.spacedBy(Palma.space.unit / 2),
        ) {
            PadButton("ПКМ", Modifier.weight(1f)) { actions.onClick(2) }
            PadButton("ЛКМ", Modifier.weight(1f)) { actions.onClick(1) }
        }
        Column(
            Modifier.height(Palma.space.touch * 2),
            verticalArrangement = Arrangement.spacedBy(Palma.space.unit / 2),
        ) {
            PadButton("▲", Modifier.weight(1f)) { actions.onScroll(-3) }
            PadButton("▼", Modifier.weight(1f)) { actions.onScroll(3) }
        }
    }
}

@Composable
private fun PadButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Tappable(onClick, modifier) {
        Box(
            Modifier
                .size(Palma.space.touch * 2)
                .clip(Palma.shapes.control)
                .background(Color.Black.copy(alpha = 0.55f))
                .border(1.dp, Color.White.copy(alpha = 0.25f), Palma.shapes.control),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, style = Palma.type.caption, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Trackpad(modifier: Modifier = Modifier, actions: DesktopActions) {
    Box(
        modifier
            .height(Palma.space.touch * 2)
            .clip(Palma.shapes.control)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), Palma.shapes.control)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var drag = 0f
                    var scroll = 0f
                    var two = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (event.changes.count { it.pressed } >= 2) two = true
                        val delta = change.position - change.previousPosition
                        if (change.pressed && delta.getDistance() > 0f) {
                            if (two) {
                                scroll += delta.y
                                val steps = (scroll / SCROLL_STEP).toInt()
                                if (steps != 0) {
                                    actions.onScroll(steps)
                                    scroll -= steps * SCROLL_STEP
                                }
                            } else {
                                actions.onMoveCursor(
                                    (delta.x * PAD_SPEED).roundToInt(),
                                    (delta.y * PAD_SPEED).roundToInt(),
                                )
                                drag += delta.getDistance()
                            }
                        }
                        if (!change.pressed && change.previousPressed) {
                            if (drag < TAP_SLOP) actions.onClick(if (two) 2 else 1)
                            drag = 0f
                            scroll = 0f
                            two = false
                        }
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        LucideIcon(Lucide.Pointer, Color.White.copy(alpha = 0.3f), size = Palma.space.iconLarge)
    }
}

@Composable
private fun Stage(frame: Bitmap, stamp: Long, input: InputMode, actions: DesktopActions) {
    var box by remember { mutableStateOf(IntSize.Zero) }
    val gesture = Modifier.pointerInput(input, frame.width, frame.height) {
        awaitPointerEventScope {
            var drag = TAP_SLOP
            var scroll = 0f
            var two = false
            if (input == InputMode.Mouse) actions.onMoveCursor(0, 0)
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: continue
                if (event.changes.count { it.pressed } >= 2) two = true
                val delta = change.position - change.previousPosition
                if (input == InputMode.Touch) {
                    val point = guestPoint(change.position, box, frame)
                    if (point != null) {
                        actions.onPointer(point.first, point.second, if (change.pressed) 1 else 0)
                    }
                } else {
                    if (change.pressed && delta.getDistance() > 0f) {
                        if (two) {
                            scroll += delta.y
                            val steps = (scroll / SCROLL_STEP).toInt()
                            if (steps != 0) {
                                actions.onScroll(steps)
                                scroll -= steps * SCROLL_STEP
                            }
                        } else {
                            actions.onMoveCursor(
                                (delta.x * PAD_SPEED).roundToInt(),
                                (delta.y * PAD_SPEED).roundToInt(),
                            )
                            drag += delta.getDistance()
                        }
                    }
                    if (!change.pressed && change.previousPressed) {
                        if (drag < TAP_SLOP) actions.onClick(if (two) 2 else 1)
                        drag = 0f
                        scroll = 0f
                        two = false
                    }
                }
                change.consume()
            }
        }
    }
    Image(
        bitmap = remember(stamp) { frame.asImageBitmap() },
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { box = it }
            .then(gesture),
    )
}

private fun guestPoint(offset: Offset, box: IntSize, frame: Bitmap): Pair<Int, Int>? {
    if (box.width == 0 || box.height == 0) return null
    val scale = min(box.width / frame.width.toFloat(), box.height / frame.height.toFloat())
    val left = (box.width - frame.width * scale) / 2
    val top = (box.height - frame.height * scale) / 2
    val x = ((offset.x - left) / scale).toInt()
    val y = ((offset.y - top) / scale).toInt()
    if (x !in 0 until frame.width || y !in 0 until frame.height) return null
    return x to y
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun KeyboardSink(on: Boolean, onKey: (Int) -> Unit, onLetter: (Char) -> Unit) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var field by remember { mutableStateOf("") }
    LaunchedEffect(on) {
        if (on) {
            focus.requestFocus()
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }
    if (!on) return
    Box(Modifier.fillMaxSize()) {
        BasicTextField(
            value = field,
            onValueChange = { value ->
                val added = value.length - field.length
                when {
                    added < 0 -> repeat(-added) { onKey(Keys.BACKSPACE) }
                    added > 0 -> value.takeLast(added).forEach { letter -> onLetter(letter) }
                }
                field = ""
            },
            keyboardOptions = KeyboardOptions(autoCorrect = false, keyboardType = KeyboardType.Text),
            keyboardActions = KeyboardActions(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(1.dp)
                .alpha(0.01f)
                .focusRequester(focus),
        )
    }
}

@Composable
private fun Waiting(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = Palma.type.body,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(Palma.space.page),
        )
    }
}

@Composable
private fun Lost(failure: Failure, actions: DesktopActions) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(Palma.space.page),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Palma.space.unit),
        ) {
            Text(stringResource(R.string.desktop_lost_title), style = Palma.type.title, color = Color.White)
            Text(
                failureBody(failure),
                style = Palma.type.body,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Palma.space.unit))
            PrimaryButton(stringResource(R.string.desktop_restart), actions.onRestart)
        }
    }
}

@Composable
private fun Report(report: String, actions: DesktopActions) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(Palma.space.page),
        verticalArrangement = Arrangement.spacedBy(Palma.space.unit),
    ) {
        Text(stringResource(R.string.desktop_stuck_title), style = Palma.type.title, color = Color.White)
        PrimaryButton(stringResource(R.string.desktop_restart), actions.onRestart)
        Ghost(stringResource(R.string.desktop_report_refresh), actions.onReport)
        Text(
            report.ifBlank { stringResource(R.string.desktop_stuck_empty) },
            style = Palma.type.caption,
            color = Color.White.copy(alpha = 0.75f),
            maxLines = 30,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Ghost(text: String, onClick: () -> Unit) {
    Tappable(onClick, Modifier.fillMaxWidth().heightIn(min = Palma.space.button)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(text, style = Palma.type.bodyStrong, color = Color.White)
        }
    }
}

private const val PAD_SPEED = 1.8f
private const val SCROLL_STEP = 28f
private const val TAP_SLOP = 14f
