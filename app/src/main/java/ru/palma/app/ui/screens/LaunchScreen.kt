package ru.palma.app.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import ru.palma.app.R
import ru.palma.app.shell.LaunchState
import ru.palma.app.shell.LaunchStep
import ru.palma.app.ui.components.BusyPanel
import ru.palma.app.ui.components.GhostButton
import ru.palma.app.ui.components.PrimaryButton
import ru.palma.app.ui.components.ProgressRing
import ru.palma.app.ui.failureBody
import ru.palma.app.ui.failureTitle
import ru.palma.app.ui.state.LaunchStepState
import ru.palma.app.ui.state.launchFraction
import ru.palma.app.ui.state.launchSteps
import ru.palma.app.ui.stepBusy
import ru.palma.app.ui.theme.Palma

@Composable
fun LaunchScreen(state: LaunchState, onRetry: () -> Unit, onBack: () -> Unit) {
    val failed = state as? LaunchState.Failed
    val motion = Palma.motion
    val steps = launchSteps(state)
    Column(
        Modifier
            .fillMaxSize()
            .background(Palma.colors.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Palma.space.page),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        val done = steps.count { it.second == LaunchStepState.DONE }
        ProgressRing(launchFraction(state), Palma.space.ring) {
            Text(
                stringResource(R.string.launch_counter, (done + 1).coerceAtMost(steps.size), steps.size),
                style = Palma.type.title,
                color = Palma.colors.ink,
            )
        }
        Spacer(Modifier.height(Palma.space.block + Palma.space.unit))
        Text(
            stringResource(if (failed != null) R.string.launch_failed_title else R.string.launch_title),
            style = Palma.type.display,
            color = Palma.colors.ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Palma.space.unit))
        if (failed != null) {
            Text(
                failureTitle(failed.failure),
                style = Palma.type.title,
                color = Palma.colors.danger,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Palma.space.unit / 2))
            Text(
                failureBody(failed.failure),
                style = Palma.type.body,
                color = Palma.colors.muted,
                textAlign = TextAlign.Center,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Palma.space.block))
            BusyPanel(stringResource(stepBusy(failed.step)), failed = true, modifier = Modifier.fillMaxWidth())
        } else {
            Spacer(Modifier.height(Palma.space.block))
            val current = (state as? LaunchState.Running)?.step ?: LaunchStep.DISPLAY
            Crossfade(
                targetState = current,
                animationSpec = tween(motion.duration(motion.medium), easing = motion.easing),
                label = "launchStep",
            ) { step ->
                BusyPanel(
                    title = stringResource(stepBusy(step)),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (failed != null) {
            Column(verticalArrangement = Arrangement.spacedBy(Palma.space.unit)) {
                PrimaryButton(stringResource(R.string.launch_retry), onRetry)
                GhostButton(stringResource(R.string.launch_back), onBack)
            }
        } else {
            GhostButton(stringResource(R.string.launch_back), onBack)
        }
        Spacer(Modifier.height(Palma.space.unit * 2))
    }
}
