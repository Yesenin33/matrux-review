package ru.matrux.app.ui.learn

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.viewinterop.AndroidView
import ru.matrux.app.ui.theme.Matrux

private const val EDITOR_LINES = 6

@Composable
fun CodeBlock(code: String, lang: String, modifier: Modifier = Modifier) {
    val colors = Matrux.colors
    Box(
        modifier
            .fillMaxWidth()
            .clip(Matrux.shapes.control)
            .background(colors.canvas)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Matrux.space.unit + Matrux.space.unit / 2, vertical = Matrux.space.unit + Matrux.space.unit / 2),
    ) {
        Text(Highlight.annotate(code, lang, colors.accent), style = Matrux.type.code, color = colors.ink, softWrap = false)
    }
}

@Composable
fun CodeEditor(value: String, lang: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, lines: Int = EDITOR_LINES) {
    val colors = Matrux.colors
    val accent = colors.accent
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = Matrux.type.code.copy(color = colors.ink),
        cursorBrush = SolidColor(accent),
        minLines = lines,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrect = false,
            keyboardType = KeyboardType.Ascii,
        ),
        visualTransformation = { text -> TransformedText(Highlight.annotate(text.text, lang, accent), OffsetMapping.Identity) },
        modifier = modifier
            .fillMaxWidth()
            .clip(Matrux.shapes.control)
            .background(colors.canvas)
            .padding(horizontal = Matrux.space.unit + Matrux.space.unit / 2, vertical = Matrux.space.unit + Matrux.space.unit / 2),
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPreview(html: String, modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) return
    AndroidView(
        factory = { context -> WebView(context).apply { settings.javaScriptEnabled = true } },
        update = { view -> view.loadDataWithBaseURL(WebChecker.BASE, html, "text/html", "utf-8", null) },
        modifier = modifier
            .fillMaxWidth()
            .height(Matrux.space.preview)
            .clip(Matrux.shapes.control),
    )
}
