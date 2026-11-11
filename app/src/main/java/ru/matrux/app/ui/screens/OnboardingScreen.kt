package ru.matrux.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import ru.matrux.app.R
import ru.matrux.app.billing.PlanOffer
import ru.matrux.app.ui.components.Card
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.components.PrimaryButton
import ru.matrux.app.ui.discountText
import ru.matrux.app.ui.imageLogo
import ru.matrux.app.ui.rubText
import ru.matrux.app.ui.state.OnboardUi
import ru.matrux.app.ui.state.Slide
import ru.matrux.app.ui.termLabel
import ru.matrux.app.ui.theme.Inter
import ru.matrux.app.ui.theme.LocalMatruxType
import ru.matrux.app.ui.theme.Matrux
import ru.matrux.app.ui.theme.inFamily

@Composable
fun OnboardingOverlay(visible: Boolean, ui: OnboardUi, onFinish: () -> Unit, onLeave: (() -> Unit)?) {
    val motion = Matrux.motion
    val lift = with(LocalDensity.current) { Matrux.space.lift.roundToPx() }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(motion.duration(motion.medium), easing = motion.easing)) +
            slideInVertically(tween(motion.duration(motion.medium), easing = motion.easing)) { lift },
        exit = fadeOut(tween(motion.duration(motion.short), easing = motion.easing)),
    ) {
        OnboardingScreen(ui, onFinish, onLeave = onLeave)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(ui: OnboardUi, onFinish: () -> Unit, initialPage: Int = 0, onLeave: (() -> Unit)? = null) {
    val pager = rememberPagerState(initialPage = initialPage) { ui.slides.size }
    val scope = rememberCoroutineScope()
    val motion = Matrux.motion
    val go: (Int) -> Unit = { page ->
        scope.launch { pager.animateScrollToPage(page, animationSpec = tween(motion.duration(motion.medium), easing = motion.easing)) }
    }
    BackHandler {
        when {
            pager.currentPage > 0 -> go(pager.currentPage - 1)
            onLeave != null -> onLeave()
        }
    }
    CompositionLocalProvider(LocalMatruxType provides Matrux.type.inFamily(Inter)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Matrux.colors.canvas)
                .clickable(remember { MutableInteractionSource() }, null) {}
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
                SlidePage(ui, ui.slides[page])
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = Matrux.space.page)) {
                Dots(ui.slides.size, pager.currentPage)
                Spacer(Modifier.height(Matrux.space.block))
                val last = pager.currentPage == ui.last
                PrimaryButton(
                    stringResource(if (last) R.string.onboard_start else R.string.onboard_next),
                    { if (last) onFinish() else go(pager.currentPage + 1) },
                )
                Spacer(Modifier.height(Matrux.space.page))
            }
        }
    }
}

@Composable
private fun Dots(count: Int, current: Int) {
    val colors = Matrux.colors
    val label = stringResource(R.string.onboard_position, current + 1, count)
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(Matrux.space.unit, Alignment.CenterHorizontally),
    ) {
        repeat(count) { index ->
            val active by animateFloatAsState(
                targetValue = if (index == current) 1f else 0f,
                animationSpec = tween(Matrux.motion.duration(Matrux.motion.short), easing = Matrux.motion.easing),
                label = "dot",
            )
            Box(Modifier.size(Matrux.space.dot)) {
                Box(Modifier.matchParentSize().clip(CircleShape).background(colors.hairline))
                Box(Modifier.matchParentSize().graphicsLayer { alpha = active }.clip(CircleShape).background(colors.ink))
            }
        }
    }
}

@Composable
private fun SlidePage(ui: OnboardUi, slide: Slide) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Matrux.space.page)
            .padding(top = Matrux.space.block, bottom = Matrux.space.block),
    ) {
        when (slide) {
            Slide.DESK -> DeskSlide(ui)
            Slide.SHEETS -> SheetsSlide(ui)
            Slide.LANGUAGES -> LanguagesSlide(ui)
            Slide.PRO -> ProSlide(ui)
        }
    }
}

@Composable
private fun Heading(title: String, lead: String) {
    Text(title, style = Matrux.type.display, color = Matrux.colors.ink)
    Spacer(Modifier.height(Matrux.space.unit))
    Text(lead, style = Matrux.type.body, color = Matrux.colors.muted)
    Spacer(Modifier.height(Matrux.space.block))
}

@Composable
private fun DeskSlide(ui: OnboardUi) {
    val space = Matrux.space
    Heading(
        stringResource(R.string.onboard_desk_title),
        stringResource(R.string.onboard_desk_lead, stringResource(R.string.app_name)),
    )
    Card {
        Box(
            Modifier
                .fillMaxWidth()
                .height(space.onboardArt)
                .clip(Matrux.shapes.control),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Image(
                painterResource(R.drawable.wallpaper_xfce_blue),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Image(
                painterResource(R.drawable.penguin_hero),
                contentDescription = null,
                modifier = Modifier.size(space.onboardMark).padding(top = space.unit),
            )
        }
        if (ui.systems.isNotEmpty()) {
            Spacer(Modifier.height(space.unit * 2))
            ui.systems.forEach { image ->
                Row(Modifier.fillMaxWidth().heightIn(min = space.onboardRow), verticalAlignment = Alignment.CenterVertically) {
                    val logo = imageLogo(image)
                    Box(Modifier.size(space.logoMark), contentAlignment = Alignment.Center) {
                        if (logo != null) Image(painterResource(logo), contentDescription = null, modifier = Modifier.size(space.logoMark))
                    }
                    Spacer(Modifier.width(space.unit * 2))
                    Text(image.name, style = Matrux.type.body, color = Matrux.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SheetsSlide(ui: OnboardUi) {
    Heading(stringResource(R.string.onboard_sheets_title), stringResource(R.string.onboard_sheets_lead))
    if (ui.sheets.isEmpty()) return
    Card {
        ui.sheets.forEach { section ->
            Row(Modifier.fillMaxWidth().heightIn(min = Matrux.space.onboardRow), verticalAlignment = Alignment.CenterVertically) {
                LucideIcon(sectionIcon(section.id), Matrux.colors.inkSoft, size = Matrux.space.iconSmall)
                Spacer(Modifier.width(Matrux.space.unit * 2))
                Text(section.title, style = Matrux.type.body, color = Matrux.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun LanguagesSlide(ui: OnboardUi) {
    val space = Matrux.space
    Heading(stringResource(R.string.onboard_languages_title), stringResource(R.string.onboard_languages_lead))
    if (ui.courses.isEmpty()) return
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(space.unit * 2)) {
            ui.courses.forEach { course ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(space.logoTile).clip(Matrux.shapes.control).background(Matrux.colors.canvas),
                        contentAlignment = Alignment.Center,
                    ) {
                        LucideIcon(courseIcon(course.id), Matrux.colors.inkSoft, size = space.iconSmall)
                    }
                    Spacer(Modifier.width(space.unit * 2))
                    Column(Modifier.weight(1f)) {
                        Text(course.title, style = Matrux.type.bodyStrong, color = Matrux.colors.ink)
                        Text(
                            course.outcome,
                            style = Matrux.type.caption,
                            color = Matrux.colors.muted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProSlide(ui: OnboardUi) {
    val space = Matrux.space
    Heading(stringResource(R.string.onboard_pro_title), stringResource(R.string.onboard_pro_lead))
    if (ui.terms.offers.isNotEmpty()) {
        Card {
            Column(verticalArrangement = Arrangement.spacedBy(space.unit)) {
                ui.terms.offers.forEach { OfferRow(it) }
            }
        }
        Spacer(Modifier.height(space.block))
    }
    Column(verticalArrangement = Arrangement.spacedBy(space.unit * 2)) {
        if (ui.offer.courses.isNotEmpty()) {
            Opens(
                Lucide.BookOpen,
                stringResource(R.string.offer_courses),
                ui.offer.courses.joinToString(" · ") + " · " + pluralStringResource(R.plurals.offer_lessons, ui.offer.lessons, ui.offer.lessons),
            )
        }
        if (ui.systems.isNotEmpty()) {
            Opens(Lucide.Terminal, stringResource(R.string.offer_systems), ui.systems.joinToString(" · ") { it.name })
        }
    }
}

@Composable
private fun OfferRow(offer: PlanOffer) {
    Row(Modifier.fillMaxWidth().heightIn(min = Matrux.space.touch), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(termLabel(offer.plan), style = Matrux.type.body, color = Matrux.colors.ink)
            offer.discountPercent?.let { Text(discountText(it), style = Matrux.type.caption, color = Matrux.colors.muted) }
        }
        val price = offer.priceRub
        if (price != null) {
            Text(rubText(price), style = Matrux.type.title, color = Matrux.colors.ink)
        } else {
            Text(stringResource(R.string.subs_price_store), style = Matrux.type.caption, color = Matrux.colors.muted)
        }
    }
}

@Composable
private fun Opens(icon: Lucide, title: String, detail: String) {
    Row(verticalAlignment = Alignment.Top) {
        LucideIcon(icon, Matrux.colors.inkSoft)
        Spacer(Modifier.width(Matrux.space.unit + Matrux.space.unit / 2))
        Column(Modifier.weight(1f)) {
            Text(title, style = Matrux.type.bodyStrong, color = Matrux.colors.ink)
            Text(detail, style = Matrux.type.caption, color = Matrux.colors.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
