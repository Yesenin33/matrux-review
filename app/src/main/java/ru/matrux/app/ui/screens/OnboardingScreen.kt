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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import ru.matrux.app.R
import ru.matrux.app.ui.components.Card
import ru.matrux.app.ui.components.LucideIcon
import ru.matrux.app.ui.components.PrimaryButton
import ru.matrux.app.ui.imageLogo
import ru.matrux.app.ui.state.OnboardUi
import ru.matrux.app.ui.state.Slide
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
            Slide.COMMANDS -> CommandsSlide(ui)
        }
    }
}

@Composable
private fun Heading(title: String, lead: String?) {
    Text(title, style = Matrux.type.display, color = Matrux.colors.ink)
    if (lead != null) {
        Spacer(Modifier.height(Matrux.space.unit))
        Text(lead, style = Matrux.type.body, color = Matrux.colors.muted)
    }
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
        Image(
            painterResource(R.drawable.desktop_real),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(space.onboardArt)
                .clip(Matrux.shapes.control),
        )
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
private fun CommandsSlide(ui: OnboardUi) {
    Heading(stringResource(R.string.onboard_commands_title), null)
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
