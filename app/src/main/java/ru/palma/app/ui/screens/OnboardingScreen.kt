package ru.palma.app.ui.screens

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
import ru.palma.app.R
import ru.palma.app.billing.PlanOffer
import ru.palma.app.ui.components.Card
import ru.palma.app.ui.components.Lucide
import ru.palma.app.ui.components.LucideIcon
import ru.palma.app.ui.components.PrimaryButton
import ru.palma.app.ui.discountText
import ru.palma.app.ui.imageLogo
import ru.palma.app.ui.rubText
import ru.palma.app.ui.state.OnboardUi
import ru.palma.app.ui.state.Slide
import ru.palma.app.ui.termLabel
import ru.palma.app.ui.theme.Inter
import ru.palma.app.ui.theme.LocalPalmaType
import ru.palma.app.ui.theme.Palma
import ru.palma.app.ui.theme.inFamily

@Composable
fun OnboardingOverlay(visible: Boolean, ui: OnboardUi, onFinish: () -> Unit, onLeave: (() -> Unit)?) {
    val motion = Palma.motion
    val lift = with(LocalDensity.current) { Palma.space.lift.roundToPx() }
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
    val motion = Palma.motion
    val go: (Int) -> Unit = { page ->
        scope.launch { pager.animateScrollToPage(page, animationSpec = tween(motion.duration(motion.medium), easing = motion.easing)) }
    }
    BackHandler {
        when {
            pager.currentPage > 0 -> go(pager.currentPage - 1)
            onLeave != null -> onLeave()
        }
    }
    CompositionLocalProvider(LocalPalmaType provides Palma.type.inFamily(Inter)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Palma.colors.canvas)
                .clickable(remember { MutableInteractionSource() }, null) {}
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
                SlidePage(ui, ui.slides[page])
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = Palma.space.page)) {
                Dots(ui.slides.size, pager.currentPage)
                Spacer(Modifier.height(Palma.space.block))
                val last = pager.currentPage == ui.last
                PrimaryButton(
                    stringResource(if (last) R.string.onboard_start else R.string.onboard_next),
                    { if (last) onFinish() else go(pager.currentPage + 1) },
                )
                Spacer(Modifier.height(Palma.space.page))
            }
        }
    }
}

@Composable
private fun Dots(count: Int, current: Int) {
    val colors = Palma.colors
    val label = stringResource(R.string.onboard_position, current + 1, count)
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(Palma.space.unit, Alignment.CenterHorizontally),
    ) {
        repeat(count) { index ->
            val active by animateFloatAsState(
                targetValue = if (index == current) 1f else 0f,
                animationSpec = tween(Palma.motion.duration(Palma.motion.short), easing = Palma.motion.easing),
                label = "dot",
            )
            Box(Modifier.size(Palma.space.dot)) {
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
            .padding(horizontal = Palma.space.page)
            .padding(top = Palma.space.block, bottom = Palma.space.block),
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
    Text(title, style = Palma.type.display, color = Palma.colors.ink)
    Spacer(Modifier.height(Palma.space.unit))
    Text(lead, style = Palma.type.body, color = Palma.colors.muted)
    Spacer(Modifier.height(Palma.space.block))
}

@Composable
private fun DeskSlide(ui: OnboardUi) {
    val space = Palma.space
    Heading(
        stringResource(R.string.onboard_desk_title),
        stringResource(R.string.onboard_desk_lead, stringResource(R.string.app_name)),
    )
    Card {
        Box(
            Modifier
                .fillMaxWidth()
                .height(space.onboardArt)
                .clip(Palma.shapes.control),
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
                    Text(image.name, style = Palma.type.body, color = Palma.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            Row(Modifier.fillMaxWidth().heightIn(min = Palma.space.onboardRow), verticalAlignment = Alignment.CenterVertically) {
                LucideIcon(sectionIcon(section.id), Palma.colors.inkSoft, size = Palma.space.iconSmall)
                Spacer(Modifier.width(Palma.space.unit * 2))
                Text(section.title, style = Palma.type.body, color = Palma.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun LanguagesSlide(ui: OnboardUi) {
    val space = Palma.space
    Heading(stringResource(R.string.onboard_languages_title), stringResource(R.string.onboard_languages_lead))
    if (ui.courses.isEmpty()) return
    Card {
        Column(verticalArrangement = Arrangement.spacedBy(space.unit * 2)) {
            ui.courses.forEach { course ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(space.logoTile).clip(Palma.shapes.control).background(Palma.colors.canvas),
                        contentAlignment = Alignment.Center,
                    ) {
                        LucideIcon(courseIcon(course.id), Palma.colors.inkSoft, size = space.iconSmall)
                    }
                    Spacer(Modifier.width(space.unit * 2))
                    Column(Modifier.weight(1f)) {
                        Text(course.title, style = Palma.type.bodyStrong, color = Palma.colors.ink)
                        Text(
                            course.outcome,
                            style = Palma.type.caption,
                            color = Palma.colors.muted,
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
    val space = Palma.space
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
    Row(Modifier.fillMaxWidth().heightIn(min = Palma.space.touch), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(termLabel(offer.plan), style = Palma.type.body, color = Palma.colors.ink)
            offer.discountPercent?.let { Text(discountText(it), style = Palma.type.caption, color = Palma.colors.muted) }
        }
        val price = offer.priceRub
        if (price != null) {
            Text(rubText(price), style = Palma.type.title, color = Palma.colors.ink)
        } else {
            Text(stringResource(R.string.subs_price_store), style = Palma.type.caption, color = Palma.colors.muted)
        }
    }
}

@Composable
private fun Opens(icon: Lucide, title: String, detail: String) {
    Row(verticalAlignment = Alignment.Top) {
        LucideIcon(icon, Palma.colors.inkSoft)
        Spacer(Modifier.width(Palma.space.unit + Palma.space.unit / 2))
        Column(Modifier.weight(1f)) {
            Text(title, style = Palma.type.bodyStrong, color = Palma.colors.ink)
            Text(detail, style = Palma.type.caption, color = Palma.colors.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
