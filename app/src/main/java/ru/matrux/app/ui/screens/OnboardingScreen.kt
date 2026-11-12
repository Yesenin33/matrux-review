package ru.matrux.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.state.OnboardUi
import ru.matrux.app.ui.state.Slide
import ru.matrux.app.ui.theme.Inter
import ru.matrux.app.ui.theme.Matrux

private val OnboardNavy: Color @Composable @ReadOnlyComposable get() = Matrux.colors.ink
private val OnboardSub: Color @Composable @ReadOnlyComposable get() = Matrux.colors.inkSoft
private val OnboardBlue: Color @Composable @ReadOnlyComposable get() = Matrux.colors.accent
private val OnboardBlueDeep: Color @Composable @ReadOnlyComposable get() = Matrux.colors.accentPress
private val OnboardDotIdle: Color @Composable @ReadOnlyComposable get() = Matrux.colors.hairline
private val OnboardPage: Color @Composable @ReadOnlyComposable get() = Matrux.colors.canvas
private val OnboardLine: Color @Composable @ReadOnlyComposable get() = Matrux.colors.hairline

private val Navy: Color @Composable @ReadOnlyComposable get() = OnboardNavy
private val Sub: Color @Composable @ReadOnlyComposable get() = OnboardSub
private val Blue: Color @Composable @ReadOnlyComposable get() = OnboardBlue
private val BlueDeep: Color @Composable @ReadOnlyComposable get() = OnboardBlueDeep
private val DotIdle: Color @Composable @ReadOnlyComposable get() = OnboardDotIdle
private val Page: Color @Composable @ReadOnlyComposable get() = OnboardPage
private val Line: Color @Composable @ReadOnlyComposable get() = OnboardLine

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

    val compact = LocalConfiguration.current.screenHeightDp < 480
    val pageAlpha = if (Matrux.colors.canvas.luminance() < 0.4f) 0.10f else 0.75f

    Box(Modifier.fillMaxSize().background(Page)) {
        Image(
            painterResource(R.drawable.onboard_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize().alpha(pageAlpha),
        )
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SlideCard(slide = ui.slides[page], compact = compact, onSubscribe = onSubscribe, onStart = onFinish)
                }
            }
            Spacer(Modifier.height(if (compact) 8.dp else 18.dp))
            Dots(ui.slides.size, pager.currentPage)
            Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
            if (compact && slide == Slide.LEVEL) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    PrimaryBlue(stringResource(R.string.onboard_buy_pro), onSubscribe)
                }
            } else if (compact && slide == Slide.START) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    PrimaryBlue(stringResource(R.string.onboard_start), onFinish)
                }
            } else if (slide == Slide.START) {
                Spacer(Modifier.height(58.dp))
            } else {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    RoundNext { if (pager.currentPage == ui.last) onFinish() else go(pager.currentPage + 1) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SlideCard(slide: Slide, compact: Boolean, onSubscribe: () -> Unit, onStart: () -> Unit) {
    Surface(
        color = Matrux.colors.surface,
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 12.dp,
        modifier = if (compact) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(horizontal = if (compact) 20.dp else 22.dp, vertical = if (compact) 12.dp else 28.dp),
        ) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                when (slide) {
                    Slide.WELCOME -> WelcomeSlide(compact)
                    Slide.CATALOG -> CatalogSlide(compact)
                    Slide.BASICS -> BasicsSlide(compact)
                    Slide.LEVEL -> LevelSlide(compact)
                    Slide.START -> StartSlide(compact)
                }
            }
            if (!compact) {
                when (slide) {
                    Slide.LEVEL -> {
                        Spacer(Modifier.height(20.dp))
                        PrimaryBlue(stringResource(R.string.onboard_buy_pro), onSubscribe)
                    }
                    Slide.START -> {
                        Spacer(Modifier.height(24.dp))
                        PrimaryBlue(stringResource(R.string.onboard_start), onStart)
                    }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun WelcomeSlide(compact: Boolean) {
    if (compact) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.logo_penguin), contentDescription = null, modifier = Modifier.size(104.dp))
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Title(stringResource(R.string.onboard_welcome_title), 24)
                Spacer(Modifier.height(10.dp))
                Lead(stringResource(R.string.onboard_welcome_lead))
            }
        }
        return
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.logo_penguin), contentDescription = null, modifier = Modifier.size(132.dp))
        Spacer(Modifier.height(30.dp))
        Title(stringResource(R.string.onboard_welcome_title), 28)
        Spacer(Modifier.height(14.dp))
        Lead(stringResource(R.string.onboard_welcome_lead))
    }
}

@Composable
private fun CatalogSlide(compact: Boolean) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)) {
            Image(painterResource(R.drawable.logo_mark), contentDescription = null, modifier = Modifier.size(if (compact) 44.dp else 58.dp))
            Image(painterResource(R.drawable.logo_debian), contentDescription = null, modifier = Modifier.size(if (compact) 40.dp else 54.dp))
            Image(painterResource(R.drawable.logo_ubuntu), contentDescription = null, modifier = Modifier.size(if (compact) 40.dp else 54.dp))
            Image(painterResource(R.drawable.logo_dev), contentDescription = null, modifier = Modifier.size(if (compact) 40.dp else 54.dp))
        }
        Spacer(Modifier.height(if (compact) 12.dp else 24.dp))
        Divider()
        Spacer(Modifier.height(if (compact) 12.dp else 22.dp))
        Title(stringResource(R.string.onboard_catalog_title), if (compact) 23 else 27)
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
        Text(
            stringResource(R.string.onboard_catalog_names),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Blue,
            textAlign = TextAlign.Center,
            fontFamily = Inter,
        )
        Spacer(Modifier.height(12.dp))
        Lead(stringResource(R.string.onboard_catalog_lead))
    }
}

@Composable
private fun BasicsSlide(compact: Boolean) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LucideIcon(Lucide.Terminal, BlueDeep, size = if (compact) 36.dp else 46.dp)
            Spacer(Modifier.width(24.dp))
            VerticalLine()
            Spacer(Modifier.width(24.dp))
            LucideIcon(Lucide.BookOpen, BlueDeep, size = if (compact) 36.dp else 46.dp)
            Spacer(Modifier.width(24.dp))
            VerticalLine()
            Spacer(Modifier.width(24.dp))
            LucideIcon(Lucide.Code, BlueDeep, size = if (compact) 36.dp else 46.dp)
        }
        Spacer(Modifier.height(if (compact) 14.dp else 30.dp))
        Title(stringResource(R.string.onboard_basics_title), if (compact) 23 else 27)
        Spacer(Modifier.height(if (compact) 10.dp else 16.dp))
        Box(Modifier.width(64.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Blue))
        Spacer(Modifier.height(if (compact) 12.dp else 20.dp))
        Lead(stringResource(R.string.onboard_basics_lead))
    }
}

@Composable
private fun LevelSlide(compact: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        if (!compact) {
            Image(painterResource(R.drawable.logo_mark), contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(18.dp))
        }
        Text(
            stringResource(R.string.onboard_level_title),
            fontSize = if (compact) 21.sp else 25.sp,
            lineHeight = if (compact) 27.sp else 32.sp,
            fontWeight = FontWeight.Bold,
            color = Navy,
            fontFamily = Inter,
        )
        Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
        Text(stringResource(R.string.onboard_level_lead), fontSize = if (compact) 14.sp else 16.sp, color = Sub, fontFamily = Inter)
        Spacer(Modifier.height(if (compact) 12.dp else 22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlanCard(
                modifier = Modifier.weight(1f),
                highlighted = false,
                icon = Lucide.Package,
                name = stringResource(R.string.onboard_free_title),
                price = "0 ₽",
                features = listOf(
                    stringResource(R.string.onboard_free_1),
                    stringResource(R.string.onboard_free_2),
                    stringResource(R.string.onboard_free_3),
                ),
                compact = compact,
            )
            PlanCard(
                modifier = Modifier.weight(1f),
                highlighted = true,
                icon = Lucide.BadgeCheck,
                name = stringResource(R.string.onboard_pro_title),
                price = BuildConfig.PRICE_PRO_MONTH,
                features = listOf(
                    stringResource(R.string.onboard_pro_1),
                    stringResource(R.string.onboard_pro_2),
                    stringResource(R.string.onboard_pro_3),
                ),
                compact = compact,
            )
        }
    }
}

@Composable
private fun StartSlide(compact: Boolean) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.rocket), contentDescription = null, modifier = Modifier.size(if (compact) 96.dp else 160.dp))
        Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
        Title(stringResource(R.string.onboard_start_title), if (compact) 24 else 28)
        Spacer(Modifier.height(14.dp))
        Lead(stringResource(R.string.onboard_start_lead))
    }
}

@Composable
private fun PlanCard(
    modifier: Modifier,
    highlighted: Boolean,
    icon: Lucide,
    name: String,
    price: String,
    features: List<String>,
    compact: Boolean = false,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (highlighted) Matrux.colors.accentTint else Matrux.colors.surfaceAlt)
            .padding(horizontal = 10.dp, vertical = if (compact) 10.dp else 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(if (compact) 36.dp else 52.dp).clip(CircleShape).background(if (highlighted) Blue else Matrux.colors.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            LucideIcon(icon, if (highlighted) Color.White else Blue, size = if (compact) 18.dp else 26.dp)
        }
        Spacer(Modifier.height(if (compact) 6.dp else 14.dp))
        Text(name, fontSize = if (compact) 17.sp else 22.sp, fontWeight = FontWeight.Bold, color = if (highlighted) Blue else Navy, fontFamily = Inter)
        Spacer(Modifier.height(2.dp))
        Text(price, fontSize = if (compact) 19.sp else 26.sp, fontWeight = FontWeight.Bold, color = if (highlighted) Blue else Navy, fontFamily = Inter)
        if (!compact) {
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Spacer(Modifier.height(12.dp))
        features.forEach { feature ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 2.dp)) {
                    LucideIcon(Lucide.Check, Blue, size = 16.dp)
                }
                Spacer(Modifier.width(5.dp))
                Text(feature, fontSize = 11.sp, lineHeight = 15.sp, color = Navy, fontFamily = Inter)
            }
        }
        }
    }
}

@Composable
private fun PrimaryBlue(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Blue, BlueDeep)))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = Inter)
        Spacer(Modifier.width(10.dp))
        LucideIcon(Lucide.ChevronRight, Color.White, size = 22.dp)
    }
}

@Composable
private fun RoundNext(onClick: () -> Unit) {
    Box(
        Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Blue, BlueDeep)))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        LucideIcon(Lucide.ChevronRight, Color.White, size = 26.dp)
    }
}

@Composable
private fun Title(text: String, size: Int) {
    Text(
        text,
        fontSize = size.sp,
        lineHeight = (size + 8).sp,
        fontWeight = FontWeight.Bold,
        color = Navy,
        textAlign = TextAlign.Center,
        fontFamily = Inter,
    )
}

@Composable
private fun Lead(text: String) {
    Text(
        text,
        fontSize = 17.sp,
        color = Sub,
        textAlign = TextAlign.Center,
        lineHeight = 24.sp,
        fontFamily = Inter,
    )
}

@Composable
private fun Divider() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(Line))
        Box(Modifier.padding(horizontal = 10.dp).size(7.dp).clip(CircleShape).background(Blue))
        Box(Modifier.weight(1f).height(1.dp).background(Line))
    }
}

@Composable
private fun VerticalLine() {
    Box(Modifier.width(1.dp).height(44.dp).background(Line))
}

@Composable
private fun Dots(count: Int, current: Int) {
    val label = stringResource(R.string.onboard_position, current + 1, count)
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        repeat(count) { index ->
            val active by animateFloatAsState(
                targetValue = if (index == current) 1f else 0f,
                animationSpec = tween(240),
                label = "dot",
            )
            Box(Modifier.size(11.dp)) {
                Box(Modifier.matchParentSize().clip(CircleShape).background(DotIdle))
                Box(Modifier.matchParentSize().graphicsLayer { alpha = active }.clip(CircleShape).background(Blue))
            }
        }
    }
}
