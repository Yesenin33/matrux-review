package ru.matrux.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Палитра варианта B («Tux»): белый фон, серые группы, чёрные кнопки, янтарный акцент.
 * Имена старых токенов сохранены, поэтому экраны, которые не переписывались, получают новый вид сами.
 * [success] и [danger] безопасны как цвет текста и значков; для сплошных плашек с белым текстом
 * есть [successSolid] и [dangerSolid].
 */
@Immutable
data class MatruxColors(
    val canvas: Color = Color(0xFFFFFFFF),
    val surface: Color = Color(0xFFF2F2F7),
    val surfaceAlt: Color = Color(0xFFE6E6EB),
    val ink: Color = Color(0xFF1D1D1F),
    val inkSoft: Color = Color(0xFF48484A),
    val muted: Color = Color(0xFF5F5F64),
    val accent: Color = Color(0xFF8A4B00),
    val accentPress: Color = Color(0xFF6F3C00),
    val accentTint: Color = Color(0xFFE6E6EB),
    val success: Color = Color(0xFF177A45),
    val successTint: Color = Color(0xFFE3F1E8),
    val warning: Color = Color(0xFFB25000),
    val hairline: Color = Color(0xFFDCDCE2),
    val danger: Color = Color(0xFFC8102E),
    val onAccent: Color = Color(0xFFFFFFFF),

    val action: Color = Color(0xFF1D1D1F),
    val actionPress: Color = Color(0xFF3A3A3C),
    val onAction: Color = Color(0xFFFFFFFF),

    val successSolid: Color = Color(0xFF177A45),
    val dangerSolid: Color = Color(0xFFC8102E),
    val infoSolid: Color = Color(0xFF1D1D1F),
    val graySolid: Color = Color(0xFF636366),
    val proSolid: Color = Color(0xFFA85400),
    val bar: Color = Color(0xFFC26A00),

    val terminal: Color = Color(0xFF1D1D1F),
    val terminalEdge: Color = Color(0xFF1D1D1F),
    val terminalInk: Color = Color(0xFFF5F5F7),
    val terminalDim: Color = Color(0xFFA1A1A6),
    val terminalOk: Color = Color(0xFFFFB340),
    val terminalError: Color = Color(0xFFFF6B6B),
    val terminalAccent: Color = Color(0xFFF5F5F7),
    val terminalButton: Color = Color(0xFFFFB340),
    val onTerminalButton: Color = Color(0xFF1D1D1F),

    val scrim: Color = Color(0x5C000000),
    val navBar: Color = Color(0xF0FFFFFF),
)

val LightColors = MatruxColors()

val DarkColors = MatruxColors(
    canvas = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surfaceAlt = Color(0xFF2C2C2E),
    ink = Color(0xFFF5F5F7),
    inkSoft = Color(0xFFD1D1D6),
    muted = Color(0xFFA1A1A6),
    accent = Color(0xFFFFB340),
    accentPress = Color(0xFFE6A030),
    accentTint = Color(0xFF2C2C2E),
    success = Color(0xFF30D158),
    successTint = Color(0xFF12301D),
    warning = Color(0xFFFFB340),
    hairline = Color(0xFF38383A),
    danger = Color(0xFFFF6B6B),
    onAccent = Color(0xFF1D1D1F),

    action = Color(0xFFFFB340),
    actionPress = Color(0xFFE6A030),
    onAction = Color(0xFF1D1D1F),

    successSolid = Color(0xFF1A7F45),
    dangerSolid = Color(0xFFC8102E),
    infoSolid = Color(0xFF48484A),
    graySolid = Color(0xFF636366),
    proSolid = Color(0xFFA85400),
    bar = Color(0xFFFFB340),

    terminal = Color(0xFF1C1C1E),
    terminalEdge = Color(0xFF38383A),
    terminalInk = Color(0xFFF5F5F7),
    terminalDim = Color(0xFFA1A1A6),
    terminalOk = Color(0xFFFFB340),
    terminalError = Color(0xFFFF6B6B),
    terminalAccent = Color(0xFFF5F5F7),
    terminalButton = Color(0xFFFFB340),
    onTerminalButton = Color(0xFF1D1D1F),

    scrim = Color(0x99000000),
    navBar = Color(0xF0161618),
)
