package ru.matrux.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ru.matrux.app.R

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Inter = FontFamily(inter(400), inter(500), inter(600), inter(700))

/** Манропа больше не используется: оставлена как псевдоним, чтобы старый код компилировался. */
val Manrope: FontFamily get() = Inter

val Mono = FontFamily(Font(R.font.mono, weight = FontWeight.Medium))

@Immutable
data class MatruxType(
    val display: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.02).em,
    ),
    val title: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.015).em,
    ),
    val lead: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.01).em,
    ),
    val body: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.01).em,
    ),
    val bodyStrong: TextStyle = body.copy(fontWeight = FontWeight.SemiBold),
    val code: TextStyle = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = "liga 0, calt 0",
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    val captionStrong: TextStyle = caption.copy(fontWeight = FontWeight.SemiBold),
    val value: TextStyle = TextStyle(
        fontFamily = Mono,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = "liga 0, calt 0",
    ),
    val big: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 60.sp,
        letterSpacing = (-0.03).em,
        fontFeatureSettings = "tnum",
    ),
    val price: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.03).em,
        fontFeatureSettings = "tnum",
    ),
)

fun MatruxType.inFamily(family: FontFamily): MatruxType = copy(
    display = display.copy(fontFamily = family),
    title = title.copy(fontFamily = family),
    lead = lead.copy(fontFamily = family),
    body = body.copy(fontFamily = family),
    bodyStrong = bodyStrong.copy(fontFamily = family),
    caption = caption.copy(fontFamily = family),
    captionStrong = captionStrong.copy(fontFamily = family),
)
