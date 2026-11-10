package ru.palma.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ru.palma.app.R

@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: Int) = Font(
    R.font.manrope,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Manrope = FontFamily(manrope(400), manrope(500), manrope(600), manrope(700))

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Inter = FontFamily(inter(400), inter(600))

val Mono = FontFamily(Font(R.font.mono, weight = FontWeight.Medium))

@Immutable
data class PalmaType(
    val display: TextStyle = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).em,
    ),
    val title: TextStyle = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.01).em,
    ),
    val lead: TextStyle = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.005).em,
    ),
    val body: TextStyle = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    val bodyStrong: TextStyle = body.copy(fontWeight = FontWeight.SemiBold),
    val code: TextStyle = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.01).em,
        fontFeatureSettings = "liga 0, calt 0",
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.005.em,
    ),
    val captionStrong: TextStyle = caption.copy(fontWeight = FontWeight.SemiBold),
)

fun PalmaType.inFamily(family: FontFamily): PalmaType = copy(
    display = display.copy(fontFamily = family),
    title = title.copy(fontFamily = family),
    lead = lead.copy(fontFamily = family),
    body = body.copy(fontFamily = family),
    bodyStrong = bodyStrong.copy(fontFamily = family),
    caption = caption.copy(fontFamily = family),
    captionStrong = captionStrong.copy(fontFamily = family),
)
