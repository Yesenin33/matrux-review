package ru.matrux.app.ui.screens

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
import ru.matrux.app.R
import ru.matrux.app.shell.LaunchState
import ru.matrux.app.shell.LaunchStep
import ru.matrux.app.ui.components.BusyPanel
import ru.matrux.app.ui.components.GhostButton
import ru.matrux.app.ui.components.PrimaryButton
import ru.matrux.app.ui.components.ProgressRing
import ru.matrux.app.ui.failureBody
import ru.matrux.app.ui.failureTitle
import ru.matrux.app.ui.state.LaunchStepState
import ru.matrux.app.ui.state.launchFraction
import ru.matrux.app.ui.state.launchSteps
import ru.matrux.app.ui.stepBusy
import ru.matrux.app.ui.theme.Matrux

@Composable
fun LaunchScreen(state: LaunchState, onRetry: () -> Unit, onBack: () -> Unit) {
    val failed = state as? LaunchState.Failed
    val motion = Matrux.motion
    val steps = launchSteps(state)
    Column(
        Modifier
            .fillMaxSize()
            .background(Matrux.colors.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Matrux.space.page),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        val done = steps.count { it.second == LaunchStepState.DONE }
        ProgressRing(launchFraction(state), Matrux.space.ring) {
            Text(
                stringResource(R.string.launch_counter, (done + 1).coerceAtMost(steps.size), steps.size),
                style = Matrux.type.title,
                color = Matrux.colors.ink,
            )
        }
        Spacer(Modifier.height(Matrux.space.block + Matrux.space.unit))
        Text(
            stringResource(if (failed != null) R.string.launch_failed_title else R.string.launch_title),
            style = Matrux.type.display,
            color = Matrux.colors.ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Matrux.space.unit))
        if (failed != null) {
            Text(
                failureTitle(failed.failure),
                style = Matrux.type.title,
                color = Matrux.colors.danger,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Matrux.space.unit / 2))
            Text(
                failureBody(failed.failure),
                style = Matrux.type.body,
                color = Matrux.colors.muted,
                textAlign = TextAlign.Center,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Matrux.space.block))
            BusyPanel(stringResource(stepBusy(failed.step)), failed = true, modifier = Modifier.fillMaxWidth())
        } else {
            Spacer(Modifier.height(Matrux.space.block))
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
            Column(verticalArrangement = Arrangement.spacedBy(Matrux.space.unit)) {
                PrimaryButton(stringResource(R.string.launch_retry), onRetry)
                GhostButton(stringResource(R.string.launch_back), onBack)
            }
        } else {
            GhostButton(stringResource(R.string.launch_back), onBack)
        }
        Spacer(Modifier.height(Matrux.space.unit * 2))
    }
}
