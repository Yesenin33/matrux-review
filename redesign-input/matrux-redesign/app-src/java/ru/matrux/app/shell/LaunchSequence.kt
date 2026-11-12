package ru.matrux.app.shell

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.matrux.app.data.Failure
import ru.matrux.app.data.Failures

enum class LaunchStep { SUBSCRIPTION, ROOTFS, PROOT, DISPLAY }

sealed interface LaunchState {
    data class Running(val step: LaunchStep) : LaunchState {
        val done: Int get() = step.ordinal
    }
    data class Failed(val step: LaunchStep, val failure: Failure) : LaunchState
    data class Ready(val mode: DisplayMode, val offline: Boolean) : LaunchState
}

class LaunchSequence(
    private val checkSubscription: suspend () -> Boolean,
    private val prepareRootfs: suspend () -> Unit,
    private val startEngine: suspend () -> Unit,
    private val startDisplay: suspend () -> DisplayMode,
) {
    fun run(): Flow<LaunchState> = flow {
        var offline = false
        var mode = DisplayMode.X11
        for (step in LaunchStep.entries) {
            emit(LaunchState.Running(step))
            try {
                when (step) {
                    LaunchStep.SUBSCRIPTION -> offline = !checkSubscription()
                    LaunchStep.ROOTFS -> prepareRootfs()
                    LaunchStep.PROOT -> startEngine()
                    LaunchStep.DISPLAY -> mode = startDisplay()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                emit(LaunchState.Failed(step, Failures.fromThrowable(error)))
                return@flow
            }
        }
        emit(LaunchState.Ready(mode, offline))
    }
}
