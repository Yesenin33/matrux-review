package ru.matrux.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.matrux.app.BuildConfig
import ru.matrux.app.R
import ru.matrux.app.ui.components.DistroLogo
import ru.matrux.app.ui.components.Glyph
import ru.matrux.app.ui.components.GlyphTile
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.PrimaryButton
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.components.PromptLine
import ru.matrux.app.ui.components.TerminalPanel
import ru.matrux.app.ui.components.sectionStyle
import ru.matrux.app.ui.state.OnboardUi
import ru.matrux.app.ui.state.Slide
import ru.matrux.app.ui.theme.Matrux

@Composable
fun OnboardingOverlay(
    visible: Boolean,
    ui: OnboardUi,
    onFinish: () -> Unit,
    onSubscribe: () -> Unit,
) {
    val motion = Matrux.motion
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(motion.duration(motion.medium), easing = motion.easing)),
        exit = fadeOut(tween(motion.duration(motion.short), easing = motion.easing)),
    ) {
        OnboardingScreen(ui, onFinish, onSubscribe)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    ui: OnboardUi,
    onFinish: () -> Unit,
    onSubscribe: () -> Unit,
    initialPage: Int = 0,
) {
    val pager = rememberPagerState(initialPage = initialPage) { ui.slides.size }
    val scope = rememberCoroutineScope()
    val go: (Int) -> Unit = { page -> scope.launch { pager.animateScrollToPage(page) } }
    BackHandler { if (pager.currentPage > 0) go(pager.currentPage - 1) }
    val slide = ui.slides[pager.currentPage.coerceIn(0, ui.last)]

    Box(Modifier.fillMaxSize().background(Matrux.colors.canvas)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
                Box(Modifier.fillMaxSize().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                    SlideContent(slide = ui.slides[page], onSubscribe = onSubscribe, onStart = onFinish)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                Dots(ui.slides.size, pager.currentPage, Modifier.weight(1f))
                if (slide != Slide.START) {
                    RoundNext { if (pager.currentPage == ui.last) onFinish() else go(pager.currentPage + 1) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SlideContent(slide: Slide, onSubscribe: () -> Unit, onStart: () -> Unit) {
    when (slide) {
        Slide.WELCOME -> WelcomeSlide()
        Slide.CATALOG -> CatalogSlide()
        Slide.BASICS -> BasicsSlide()
        Slide.LEVEL -> LevelSlide(onSubscribe)
        Slide.START -> StartSlide(onStart)
    }
}

@Composable
private fun WelcomeSlide() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Image(painterResource(R.drawable.logo_penguin), contentDescription = null, modifier = Modifier.size(168.dp))
        Title(stringResource(R.string.onboard_welcome_title), 30)
        TerminalPanel {
            PromptLine(stringResource(R.string.terminal_host_none), command = stringResource(R.string.onboard_term_cmd))
            Text(
                stringResource(R.string.onboard_term_out),
                style = Matrux.type.code.copy(fontSize = 14.sp, lineHeight = 24.sp),
                color = Matrux.colors.terminalInk,
            )
            PromptLine(stringResource(R.string.terminal_host_none))
        }
    }
}

@Composable
private fun CatalogSlide() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                R.drawable.logo_alpine,
                R.drawable.logo_debian,
                R.drawable.logo_ubuntu,
                R.drawable.logo_arch,
                R.drawable.logo_dev,
            ).forEach { DistroLogo(it, size = 56.dp) }
        }
        Title(stringResource(R.string.onboard_catalog_title), 30)
        Text(
            stringResource(R.string.onboard_catalog_names),
            style = Matrux.type.bodyStrong.copy(fontSize = 17.sp, lineHeight = 24.sp),
            color = Matrux.colors.accent,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BasicsSlide() {
    val ids = listOf("start", "linux", "terms", "files", "git", "hardware", "packages", "users")
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        FlowRow(
            Modifier.padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            maxItemsInEachRow = 4,
        ) {
            ids.forEach { id ->
                val style = sectionStyle(id)
                GlyphTile(style.glyph, style.color, size = 56.dp, corner = 14.dp)
            }
        }
        Title(stringResource(R.string.onboard_basics_title), 30)
    }
}

@Composable
private fun LevelSlide(onSubscribe: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Title(stringResource(R.string.onboard_level_title), 28)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlanCard(
                modifier = Modifier.weight(1f),
                pro = false,
                name = stringResource(R.string.onboard_free_title),
                price = "0 ₽",
                features = listOf(
                    stringResource(R.string.onboard_free_1),
                    stringResource(R.string.onboard_free_2),
                    stringResource(R.string.onboard_free_3),
                ),
            )
            PlanCard(
                modifier = Modifier.weight(1f),
                pro = true,
                name = stringResource(R.string.onboard_pro_title),
                price = BuildConfig.PRICE_PRO_MONTH,
                features = listOf(
                    stringResource(R.string.onboard_pro_1),
                    stringResource(R.string.onboard_pro_2),
                    stringResource(R.string.onboard_pro_3),
                ),
            )
        }
        PrimaryButton(stringResource(R.string.onboard_buy_pro), onSubscribe)
    }
}

@Composable
private fun StartSlide(onStart: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Image(painterResource(R.drawable.logo_penguin), contentDescription = null, modifier = Modifier.size(132.dp))
        Title(stringResource(R.string.onboard_start_title), 30)
        PrimaryButton(stringResource(R.string.onboard_start), onStart)
    }
}

@Composable
private fun PlanCard(modifier: Modifier, pro: Boolean, name: String, price: String, features: List<String>) {
    val colors = Matrux.colors
    val shape = Matrux.shapes.card
    val background = if (pro) colors.terminal else colors.surface
    val ink = if (pro) colors.terminalInk else colors.ink
    val tick = if (pro) colors.terminalOk else colors.success
    Column(
        modifier
            .clip(shape)
            .background(background)
            .then(if (pro) Modifier.border(1.dp, colors.terminalEdge, shape) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(name, style = Matrux.type.title.copy(fontSize = 17.sp, lineHeight = 22.sp), color = ink)
        Text(price, style = Matrux.type.display.copy(fontSize = 28.sp, lineHeight = 32.sp), color = ink, maxLines = 1)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            features.forEach { feature ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    LucideIcon(Lucide.Check, tick, size = 18.dp)
                    Text(feature, style = Matrux.type.body.copy(fontSize = 14.sp, lineHeight = 19.sp), color = ink)
                }
            }
        }
    }
}

@Composable
private fun RoundNext(onClick: () -> Unit) {
    val colors = Matrux.colors
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(colors.action)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        LucideIcon(Lucide.ChevronRight, colors.onAction, size = 24.dp)
    }
}

@Composable
private fun Title(text: String, size: Int) {
    Text(
        text,
        style = Matrux.type.display.copy(fontSize = size.sp, lineHeight = (size + 6).sp),
        color = Matrux.colors.ink,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Dots(count: Int, current: Int, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.onboard_position, current + 1, count)
    Row(
        modifier.semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val width by animateDpAsState(
                targetValue = if (index == current) 20.dp else 8.dp,
                animationSpec = tween(240),
                label = "dot",
            )
            Box(
                Modifier
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (index == current) Matrux.colors.action else Matrux.colors.hairline),
            )
        }
    }
}
