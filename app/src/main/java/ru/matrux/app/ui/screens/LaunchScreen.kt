package ru.matrux.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.matrux.app.R
import ru.matrux.app.data.Failure
import ru.matrux.app.shell.LaunchState
import ru.matrux.app.shell.LaunchStep
import ru.matrux.app.ui.components.DistroLogo
import ru.matrux.app.ui.components.GhostButton
import ru.matrux.app.ui.components.Lucide
import ru.matrux.app.ui.components.Plaque
import ru.matrux.app.ui.components.PrimaryButton
import ru.matrux.app.ui.components.TerminalPanel
import ru.matrux.app.ui.failureTitle
import ru.matrux.app.ui.stepBusy
import ru.matrux.app.ui.stepLabel
import ru.matrux.app.ui.state.LaunchStepState
import ru.matrux.app.ui.state.launchSteps
import ru.matrux.app.ui.theme.Matrux

@Composable
fun LaunchScreen(state: LaunchState, onRetry: () -> Unit, onBack: () -> Unit, logo: Int? = null) {
    val colors = Matrux.colors
    val failed = state as? LaunchState.Failed
    val steps = launchSteps(state)
    val done = steps.count { it.second == LaunchStepState.DONE }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Matrux.space.page),
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(Modifier.height(32.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DistroLogo(logo, size = 64.dp)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text(
                        stringResource(if (failed != null) R.string.launch_failed_title else R.string.launch_title),
                        style = Matrux.type.display.copy(fontSize = 30.sp, lineHeight = 36.sp),
                        color = colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.launch_counter, (done + 1).coerceAtMost(steps.size), steps.size),
                        style = Matrux.type.value.copy(fontSize = 15.sp),
                        color = colors.muted,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                    )
                }
                if (failed != null) {
                    Plaque(failureTitle(failed.failure), colors.dangerSolid, icon = Lucide.CircleAlert)
                } else {
                    val current = (state as? LaunchState.Running)?.step ?: LaunchStep.DISPLAY
                    Plaque(stringResource(stepBusy(current)), colors.infoSolid)
                }
            }
            BootLog(steps)
            failed?.let { diagnostics(it.failure) }?.let { output ->
                TerminalPanel {
                    Text(
                        output,
                        style = Matrux.type.code.copy(fontSize = 12.sp, lineHeight = 18.sp),
                        color = colors.terminalError,
                        maxLines = 8,
                    )
                }
            }
        }
        Column(
            Modifier.padding(top = Matrux.space.unit * 2, bottom = Matrux.space.unit),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (failed != null) PrimaryButton(stringResource(R.string.launch_retry), onRetry)
            GhostButton(stringResource(R.string.launch_back), onBack)
        }
    }
}

/** Сырой вывод процесса: это данные диагностики, а не поясняющий текст. */
private fun diagnostics(failure: Failure): String? = when (failure) {
    is Failure.Engine -> failure.output
    is Failure.Display -> failure.output
    else -> null
}?.takeIf { it.isNotBlank() }

@Composable
private fun BootLog(steps: List<Pair<LaunchStep, LaunchStepState>>) {
    val colors = Matrux.colors
    TerminalPanel {
        steps.forEach { (step, state) ->
            val (mark, markColor) = when (state) {
                LaunchStepState.DONE -> "  ok  " to colors.terminalOk
                LaunchStepState.ACTIVE -> " .... " to colors.terminalAccent
                LaunchStepState.FAILED -> " fail " to colors.terminalError
                LaunchStepState.WAITING -> "      " to colors.terminalDim
            }
            val textColor = when (state) {
                LaunchStepState.DONE, LaunchStepState.ACTIVE -> colors.terminalInk
                LaunchStepState.FAILED -> colors.terminalError
                LaunchStepState.WAITING -> colors.terminalDim
            }
            val label = stringResource(stepLabel(step))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.terminalDim)) { append("[") }
                    withStyle(SpanStyle(color = markColor, fontWeight = FontWeight.SemiBold)) { append(mark) }
                    withStyle(SpanStyle(color = colors.terminalDim)) { append("] ") }
                    withStyle(SpanStyle(color = textColor)) { append(label) }
                },
                style = Matrux.type.code.copy(fontSize = 14.sp, lineHeight = 28.sp),
                softWrap = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
