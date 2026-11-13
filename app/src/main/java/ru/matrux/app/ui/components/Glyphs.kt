package ru.matrux.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Заливные глифы разделов и курсов. Строка с префиксом «~» заливается по правилу even-odd. */
enum class Glyph(vararg val paths: String) {
    Rocket(
        "M12 2c3 2.2 4.5 5.6 4.5 9.5V16h-9v-4.5C7.5 7.6 9 4.2 12 2zM7.5 13l-3 3v3l3-1.5zM16.5 13l3 3v3l-3-1.5zM10 17.5h4L12 22z",
    ),
    Terminal(
        "~M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2zm2.2 4.6 3.4 3.4-3.4 3.4 1.4 1.4 4.8-4.8-4.8-4.8zM12.5 15v2H18v-2z",
    ),
    Book(
        "M3 5.5C5.5 4.3 8.5 4.3 11.25 5.6v13.6C8.5 17.9 5.5 17.9 3 19.1zM12.75 5.6C15.5 4.3 18.5 4.3 21 5.5v13.6c-2.5-1.2-5.5-1.2-8.25.1z",
    ),
    Folder(
        "M3 6a2 2 0 0 1 2-2h4.2l2 2.2H19a2 2 0 0 1 2 2V18a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
    ),
    Git(
        "M4,5.5a2.5,2.5 0 1,0 5,0a2.5,2.5 0 1,0 -5,0z",
        "M4,18.5a2.5,2.5 0 1,0 5,0a2.5,2.5 0 1,0 -5,0z",
        "M15,8.5a2.5,2.5 0 1,0 5,0a2.5,2.5 0 1,0 -5,0z",
        "M5.5 7h2v10h-2z",
        "M16.5 10.5h2c0 3.5-2.5 5.2-6 5.5l-5 .4v-2l4.8-.4c2.6-.2 4.2-1.1 4.2-3.5z",
    ),
    Cpu(
        "~M7 5h10a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2zm2.5 4.5v5h5v-5z",
        "M8.5 2H10v2.2H8.5zM14 2h1.5v2.2H14zM8.5 19.8H10V22H8.5zM14 19.8h1.5V22H14zM2 8.5h2.2V10H2zM2 14h2.2v1.5H2zM19.8 8.5H22V10h-2.2zM19.8 14H22v1.5h-2.2z",
    ),
    Box(
        "M3 4h18v4H3z",
        "~M4 9.5h16V18a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2zm5 2.5v2h6v-2z",
    ),
    Users(
        "M5.5,8a3.5,3.5 0 1,0 7,0a3.5,3.5 0 1,0 -7,0z",
        "M2 19c0-3.3 3.1-5.5 7-5.5s7 2.2 7 5.5v1H2z",
        "M14.9,9a2.6,2.6 0 1,0 5.2,0a2.6,2.6 0 1,0 -5.2,0z",
        "M17.3 13.4c2.8.1 4.7 1.9 4.7 4.6v1h-4.5v-1c0-1.8-.7-3.3-1.9-4.4.5-.1 1.1-.2 1.7-.2z",
    ),
    Gear(
        "~M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6s3.6,1.62 3.6,3.6S13.98,15.6 12,15.6z",
    ),
    Network(
        "M9 3h6v5h-2v3h6v5h2v5h-6v-5h2v-3H7v3h2v5H3v-5h2v-5h6V8H9z",
    ),
    Disk(
        "~M4 4h16a1.5 1.5 0 0 1 1.5 1.5v4A1.5 1.5 0 0 1 20 11H4a1.5 1.5 0 0 1-1.5-1.5v-4A1.5 1.5 0 0 1 4 4zm1.5 2.5v2h2v-2zM4 13h16a1.5 1.5 0 0 1 1.5 1.5v4A1.5 1.5 0 0 1 20 20H4a1.5 1.5 0 0 1-1.5-1.5v-4A1.5 1.5 0 0 1 4 13zm1.5 2.5v2h2v-2z",
    ),
    Doc(
        "~M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2zm2 4.5v2h10v-2zm0 3.5v2h10v-2zm0 3.5v2h6v-2z",
    ),
    Code(
        "M8.6 6.2 10 7.6 5.6 12l4.4 4.4-1.4 1.4L2.8 12zM15.4 6.2 14 7.6l4.4 4.4-4.4 4.4 1.4 1.4 5.8-5.8z",
    ),
    Db(
        "M12 3c4.4 0 8 1.3 8 3s-3.6 3-8 3-8-1.3-8-3 3.6-3 8-3zM4 8.6c1.6 1.2 4.6 1.9 8 1.9s6.4-.7 8-1.9V12c0 1.7-3.6 3-8 3s-8-1.3-8-3zM4 14.6c1.6 1.2 4.6 1.9 8 1.9s6.4-.7 8-1.9V18c0 1.7-3.6 3-8 3s-8-1.3-8-3z",
    ),
}

private fun Glyph.vector(): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    paths.forEach { raw ->
        val evenOdd = raw.startsWith("~")
        builder.addPath(
            pathData = PathParser().parsePathString(raw.removePrefix("~")).toNodes(),
            pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
            fill = SolidColor(Color.Black),
        )
    }
    return builder.build()
}

@Composable
fun GlyphIcon(glyph: Glyph, tint: Color, size: Dp, modifier: Modifier = Modifier) {
    val vector = remember(glyph) { glyph.vector() }
    Icon(vector, contentDescription = null, tint = tint, modifier = modifier.size(size))
}

/** Цветная плитка с белым заливным глифом. */
@Composable
fun GlyphTile(glyph: Glyph, background: Color, modifier: Modifier = Modifier, size: Dp = 32.dp, corner: Dp = 8.dp) {
    Box(
        modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(corner))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, Color.White, size * 0.62f)
    }
}

/** Глиф и цвет плитки раздела шпаргалок. Цвета подобраны так, чтобы белый глиф читался в обеих темах. */
data class GlyphStyle(val glyph: Glyph, val color: Color)

fun sectionStyle(id: String): GlyphStyle = when (id) {
    "start" -> GlyphStyle(Glyph.Rocket, Color(0xFF1677E5))
    "linux" -> GlyphStyle(Glyph.Terminal, Color(0xFF3A3A3C))
    "terms" -> GlyphStyle(Glyph.Book, Color(0xFFC2570A))
    "files" -> GlyphStyle(Glyph.Folder, Color(0xFF1677E5))
    "git" -> GlyphStyle(Glyph.Git, Color(0xFFD9382B))
    "hardware" -> GlyphStyle(Glyph.Cpu, Color(0xFF636366))
    "packages" -> GlyphStyle(Glyph.Box, Color(0xFF9A6A2F))
    "users" -> GlyphStyle(Glyph.Users, Color(0xFF1E8A4C))
    "services" -> GlyphStyle(Glyph.Gear, Color(0xFF5A5A5E))
    "network" -> GlyphStyle(Glyph.Network, Color(0xFF127F92))
    "disks" -> GlyphStyle(Glyph.Disk, Color(0xFF5856D6))
    "logs" -> GlyphStyle(Glyph.Doc, Color(0xFF8944D9))
    "scripts" -> GlyphStyle(Glyph.Code, Color(0xFFC7386F))
    else -> GlyphStyle(Glyph.Terminal, Color(0xFF3A3A3C))
}

fun courseStyle(id: String): GlyphStyle = when (id) {
    "linux" -> GlyphStyle(Glyph.Terminal, Color(0xFF3A3A3C))
    "python" -> GlyphStyle(Glyph.Code, Color(0xFF1677E5))
    "git" -> GlyphStyle(Glyph.Git, Color(0xFFD9382B))
    "sqlite" -> GlyphStyle(Glyph.Db, Color(0xFF127F92))
    "go" -> GlyphStyle(Glyph.Code, Color(0xFF127F92))
    "html" -> GlyphStyle(Glyph.Doc, Color(0xFFC2570A))
    "css" -> GlyphStyle(Glyph.Doc, Color(0xFF8944D9))
    "cpp" -> GlyphStyle(Glyph.Cpu, Color(0xFF636366))
    "scripts" -> GlyphStyle(Glyph.Terminal, Color(0xFF3A3A3C))
    else -> GlyphStyle(Glyph.Book, Color(0xFFC2570A))
}
