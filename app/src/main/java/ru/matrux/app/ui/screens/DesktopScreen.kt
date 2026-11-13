package ru.matrux.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.min
import ru.matrux.app.R
import ru.matrux.app.data.Failure
import ru.matrux.app.session.Frame
import ru.matrux.app.shell.DisplayMode
import ru.matrux.app.ui.Keys
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.components.TerminalButton
import ru.matrux.app.ui.components.Tappable
import ru.matrux.app.ui.failureBody
import ru.matrux.app.ui.theme.Matrux

enum class InputMode { Touch, Mouse }

data class DesktopUi(
    val title: String,
    val mode: DisplayMode,
    val offline: Boolean,
    val frame: StateFlow<Frame?>?,
    val lost: Failure?,
    val hasFrame: Boolean = false,
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
    val onTap: (x: Int, y: Int) -> Unit = { _, _ -> },
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
fun DesktopScreen(ui: DesktopUi, actions: DesktopActions, openShade: Boolean = false) {
    var shadeOpen by remember { mutableStateOf(openShade) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            ui.lost != null -> Lost(ui.lost, actions)
            ui.frame != null && ui.hasFrame -> Stage(ui.frame, ui.input, actions)
            ui.mode == DisplayMode.X11 -> Waiting(stringResource(R.string.desktop_x11_native))
            else -> Waiting(stringResource(R.string.desktop_waiting))
        }
        if (ui.lost != null) return@Box
        if (ui.stuck) {
            Report(ui.report, actions)
            return@Box
        }
        KeyboardSink(ui.keyboard, actions.onKey, actions.onChar)
        if (shadeOpen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures { shadeOpen = false } },
            )
        }
        TopShade(ui, actions, shadeOpen, { shadeOpen = it }, Modifier.align(Alignment.TopCenter))
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
private fun TopShade(
    ui: DesktopUi,
    actions: DesktopActions,
    open: Boolean,
    onOpen: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var travel by remember { mutableStateOf(0f) }
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = SHADE_INSET),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(width = SHADE_GRAB_WIDTH, height = SHADE_GRAB_HEIGHT)
                .clip(Matrux.shapes.pill)
                .background(Color.Black.copy(alpha = 0.62f))
                .border(1.dp, Color.White.copy(alpha = 0.22f), Matrux.shapes.pill)
                .pointerInput(Unit) { detectTapGestures { onOpen(!open) } }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { travel = 0f },
                        onDragEnd = { travel = 0f },
                        onDragCancel = { travel = 0f },
                    ) { change, delta ->
                        travel += delta.y
                        if (travel > SHADE_SNAP && !open) onOpen(true)
                        if (travel < -SHADE_SNAP && open) onOpen(false)
                        change.consume()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            LucideIcon(
                if (open) Lucide.ChevronUp else Lucide.ChevronDown,
                Color.White.copy(alpha = 0.85f),
                size = 18.dp,
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(tween(Matrux.motion.duration(Matrux.motion.short), easing = Matrux.motion.easing)) + fadeIn(),
            exit = shrinkVertically(tween(Matrux.motion.duration(Matrux.motion.short), easing = Matrux.motion.easing)) + fadeOut(),
        ) {
            ShadePanel(ui, actions, Modifier.padding(top = Matrux.space.unit))
        }
    }
}

@Composable
private fun ShadePanel(ui: DesktopUi, actions: DesktopActions, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Matrux.space.unit / 2)
            .clip(Matrux.shapes.card)
            .background(Color.Black.copy(alpha = 0.86f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), Matrux.shapes.card)
            .padding(Matrux.space.unit),
        verticalArrangement = Arrangement.spacedBy(Matrux.space.unit / 2),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Matrux.space.unit / 2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShadeAction(
                Lucide.Terminal,
                stringResource(R.string.shade_console),
                false,
                Modifier.weight(1f),
                actions.onConsole,
            )
            ShadeAction(
                Lucide.Keyboard,
                stringResource(R.string.shade_keyboard),
                ui.keyboard,
                Modifier.weight(1f),
                actions.onToggleKeyboard,
            )
            ShadeAction(
                if (ui.input == InputMode.Touch) Lucide.Hand else Lucide.Mouse,
                stringResource(R.string.shade_mouse),
                ui.input == InputMode.Mouse,
                Modifier.weight(1f),
                actions.onToggleInput,
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Matrux.space.unit / 3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarButton(Lucide.Joystick, ui.pad, actions.onTogglePad)
            BarButton(Lucide.Globe, ui.russian, actions.onToggleLayout)
            BarButton(Lucide.Sliders, false, actions.onSettings)
            BarButton(Lucide.RotateCw, false, actions.onRotate)
            Box(Modifier.weight(1f))
            BarButton(Lucide.ChevronDown, false, actions.onMinimize)
            BarButton(Lucide.X, false, actions.onClose)
        }
    }
}

@Composable
private fun ShadeAction(
    icon: Lucide,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Tappable(onClick, modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(SHADE_ACTION_HEIGHT)
                .clip(Matrux.shapes.pill)
                .background(if (active) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = if (active) 0.92f else 0.22f), Matrux.shapes.pill),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LucideIcon(icon, if (active) Color.Black else Color.White, size = 20.dp)
            Spacer(Modifier.width(Matrux.space.unit / 2))
            Text(label, style = Matrux.type.caption, color = if (active) Color.Black else Color.White, maxLines = 1)
        }
    }
}

@Composable
private fun BarButton(icon: Lucide, active: Boolean, onClick: () -> Unit) {
    Tappable(onClick) {
        Box(
            Modifier
                .height(28.dp)
                .clip(Matrux.shapes.pill)
                .background(if (active) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = if (active) 0.92f else 0.22f), Matrux.shapes.pill)
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
            .padding(horizontal = Matrux.space.unit, vertical = Matrux.space.unit / 2),
        horizontalArrangement = Arrangement.spacedBy(Matrux.space.unit / 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Trackpad(Modifier.weight(1f), actions)
        Column(
            Modifier.height(Matrux.space.touch * 2),
            verticalArrangement = Arrangement.spacedBy(Matrux.space.unit / 2),
        ) {
            PadButton(stringResource(R.string.pad_right), Modifier.weight(1f)) { actions.onClick(2) }
            PadButton(stringResource(R.string.pad_left), Modifier.weight(1f)) { actions.onClick(1) }
        }
        Column(
            Modifier.height(Matrux.space.touch * 2),
            verticalArrangement = Arrangement.spacedBy(Matrux.space.unit / 2),
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
                .size(Matrux.space.touch * 2)
                .clip(Matrux.shapes.control)
                .background(Color.Black.copy(alpha = 0.55f))
                .border(1.dp, Color.White.copy(alpha = 0.25f), Matrux.shapes.control),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, style = Matrux.type.caption, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Trackpad(modifier: Modifier = Modifier, actions: DesktopActions) {
    val current by rememberUpdatedState(actions)
    Box(
        modifier
            .height(Matrux.space.touch * 2)
            .clip(Matrux.shapes.control)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), Matrux.shapes.control)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    val mouse = MouseGesture { current }
                    while (true) mouse.handle(awaitPointerEvent().changes)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        LucideIcon(Lucide.Pointer, Color.White.copy(alpha = 0.3f), size = Matrux.space.iconLarge)
    }
}

private class MouseGesture(private val actions: () -> DesktopActions) {
    private var drag = 0f
    private var scroll = 0f
    private var restX = 0f
    private var restY = 0f
    private var two = false

    fun handle(changes: List<PointerInputChange>) {
        val change = changes.firstOrNull { it.pressed } ?: changes.firstOrNull() ?: return
        if (changes.count { it.pressed } >= 2) two = true
        val delta = change.position - change.previousPosition
        if (change.pressed && delta.getDistance() > 0f) {
            if (two) {
                scroll += delta.y
                val steps = (scroll / SCROLL_STEP).toInt()
                if (steps != 0) {
                    actions().onScroll(steps)
                    scroll -= steps * SCROLL_STEP
                }
            } else {
                restX += delta.x * PAD_SPEED
                restY += delta.y * PAD_SPEED
                val dx = restX.toInt()
                val dy = restY.toInt()
                restX -= dx
                restY -= dy
                if (dx != 0 || dy != 0) actions().onMoveCursor(dx, dy)
                drag += delta.getDistance()
            }
        }
        if (changes.none { it.pressed } && changes.any { it.previousPressed }) {
            if (drag < TAP_SLOP) actions().onClick(if (two) 2 else 1)
            drag = 0f
            scroll = 0f
            restX = 0f
            restY = 0f
            two = false
        }
        changes.forEach { it.consume() }
    }
}

private class TouchGesture(
    private val actions: () -> DesktopActions,
    private val point: (Offset) -> Pair<Int, Int>?,
) {
    private var downAt = Offset.Zero
    private var downPoint: Pair<Int, Int>? = null
    private var dragging = false

    fun handle(changes: List<PointerInputChange>) {
        val change = changes.firstOrNull() ?: return
        val now = point(change.position)
        when {
            change.pressed && !change.previousPressed -> {
                downAt = change.position
                downPoint = now
                dragging = false
            }
            change.pressed && !dragging && (change.position - downAt).getDistance() > TAP_SLOP -> {
                val start = downPoint
                if (start != null && now != null) {
                    dragging = true
                    actions().onPointer(start.first, start.second, 1)
                    actions().onPointer(now.first, now.second, 1)
                }
            }
            change.pressed && dragging -> if (now != null) actions().onPointer(now.first, now.second, 1)
            !change.pressed && change.previousPressed -> {
                val start = downPoint
                when {
                    dragging -> {
                        val end = now ?: start
                        if (end != null) actions().onPointer(end.first, end.second, 0)
                    }
                    start != null -> actions().onTap(start.first, start.second)
                }
                dragging = false
                downPoint = null
            }
        }
        changes.forEach { it.consume() }
    }
}

@Composable
private fun Stage(frames: StateFlow<Frame?>, input: InputMode, actions: DesktopActions) {
    val frame = frames.collectAsState()
    val current by rememberUpdatedState(actions)
    var box by remember { mutableStateOf(IntSize.Zero) }
    val target = remember { RectF() }
    val gesture = Modifier.pointerInput(input) {
        awaitPointerEventScope {
            if (input == InputMode.Mouse) {
                current.onMoveCursor(0, 0)
                val mouse = MouseGesture { current }
                while (true) mouse.handle(awaitPointerEvent().changes)
            } else {
                val touch = TouchGesture({ current }) { offset ->
                    frame.value?.bitmap?.let { guestPoint(offset, box, it) }
                }
                while (true) touch.handle(awaitPointerEvent().changes)
            }
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { box = it }
            .then(gesture)
            .drawBehind {
                val bitmap = frame.value?.bitmap ?: return@drawBehind
                val scale = min(size.width / bitmap.width, size.height / bitmap.height)
                val left = (size.width - bitmap.width * scale) / 2
                val top = (size.height - bitmap.height * scale) / 2
                target.set(left, top, left + bitmap.width * scale, top + bitmap.height * scale)
                drawIntoCanvas { it.nativeCanvas.drawBitmap(bitmap, null, target, framePaint) }
            },
    )
}

private val framePaint = Paint(Paint.FILTER_BITMAP_FLAG)

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
    val reset = remember { TextFieldValue(SENTINEL, TextRange(SENTINEL.length)) }
    var field by remember { mutableStateOf(reset) }
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
                if (value.composition != null) {
                    field = value
                    return@BasicTextField
                }
                val text = value.text
                repeat(SENTINEL.length - text.count { it == SPACER }) { onKey(Keys.BACKSPACE) }
                text.filter { it != SPACER }.forEach(onLetter)
                field = reset
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
            style = Matrux.type.body,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(Matrux.space.page),
        )
    }
}

@Composable
private fun Lost(failure: Failure, actions: DesktopActions) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(Matrux.space.page),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Matrux.space.unit),
        ) {
            Text(stringResource(R.string.desktop_lost_title), style = Matrux.type.title, color = Color.White)
            Spacer(Modifier.height(Matrux.space.unit))
            TerminalButton(stringResource(R.string.desktop_restart), actions.onRestart)
        }
    }
}

@Composable
private fun Report(report: String, actions: DesktopActions) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(Matrux.space.page),
        verticalArrangement = Arrangement.spacedBy(Matrux.space.unit),
    ) {
        Text(stringResource(R.string.desktop_stuck_title), style = Matrux.type.title, color = Color.White)
        TerminalButton(stringResource(R.string.desktop_restart), actions.onRestart)
        Ghost(stringResource(R.string.desktop_report_refresh), actions.onReport)
        Text(
            report.ifBlank { stringResource(R.string.desktop_stuck_empty) },
            style = Matrux.type.code.copy(fontSize = 12.sp, lineHeight = 18.sp),
            color = Color.White.copy(alpha = 0.75f),
            maxLines = 30,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Ghost(text: String, onClick: () -> Unit) {
    Tappable(onClick, Modifier.fillMaxWidth().heightIn(min = Matrux.space.button)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(text, style = Matrux.type.bodyStrong, color = Color.White)
        }
    }
}

private const val PAD_SPEED = 1.8f
private const val SHADE_SNAP = 26f
private val SHADE_INSET = 14.dp
private val SHADE_GRAB_WIDTH = 132.dp
private val SHADE_GRAB_HEIGHT = 30.dp
private val SHADE_ACTION_HEIGHT = 38.dp
private const val SCROLL_STEP = 28f
private const val TAP_SLOP = 14f
private const val SPACER = '\u200B'
private val SENTINEL = SPACER.toString().repeat(4)
