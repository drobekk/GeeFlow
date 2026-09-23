package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.brew.model.ProfileExecutionTrace
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import org.koin.core.annotation.Factory
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant

@Factory
class ObserveBrewDataUseCase(
    private val coordinator: ProfileExecutionCoordinator,
    private val provider: DeviceControllerProvider,
    private val getSelectedUserUseCase: GetSelectedUserUseCase,
) {
    @Suppress("CyclomaticComplexMethod")
    operator fun invoke(deviceId: Long): Flow<BrewSession> {
        val telemetry = provider.getController(deviceId).deviceState
        val deviceState = telemetry.combine(coordinator.state) { state, run ->
            if (run.active && run.deviceId == deviceId) state.copy(brewStatus = DeviceState.BrewStatus.Profile) else state
        }
        val sessionFlow = flow {
            val acc = Accumulator()
            deviceState.collect { state ->
                val finalState = if (acc.isBrewing && acc.trace == null) {
                    telemetry.awaitFinalBrewTelemetry(stopped = state, previousTelemetryTime = acc.lastTelemetryTime)
                } else {
                    state
                }
                accumulateBrewData(
                    acc = acc,
                    state = finalState,
                    trace = coordinator.state.value.takeIf { it.active && it.deviceId == deviceId }?.trace,
                )
                val run = coordinator.state.value
                if (run.deviceId == deviceId && (run.active || acc.trace != null)) acc.trace = run.trace
                emit(acc.toSession())
            }
        }

        return combine(sessionFlow, getSelectedUserUseCase()) { session, user ->
            session.copy(userId = user?.id ?: 0L)
        }.distinctUntilChanged()
    }

    private fun Accumulator.toSession() = BrewSession(
        userId = 0L,
        mode = status.toBrewMode(),
        elapsedSeconds = timeInSeconds,
        startTime = startTime,
        inProgress = isBrewing,
        dataPoints = data.toMap(),
        recording = recorder?.snapshot(),
        executionTrace = trace,
    )

    private fun DeviceState.BrewStatus.isBrewing() = this == DeviceState.BrewStatus.Manual ||
        this == DeviceState.BrewStatus.Profile || this == DeviceState.BrewStatus.FreeVariable

    private fun DeviceState.BrewStatus.toBrewMode(): BrewMode = when (this) {
        DeviceState.BrewStatus.Profile -> BrewMode.Profile
        DeviceState.BrewStatus.FreeVariable -> BrewMode.Freehand
        else -> BrewMode.Manual
    }

    private fun accumulateBrewData(acc: Accumulator, state: DeviceState, trace: ProfileExecutionTrace?): Accumulator {
        val status = state.brewStatus
        val currentlyBrewing = status.isBrewing()

        if (currentlyBrewing) {
            val now = Clock.System.now()
            if (!acc.isBrewing) {
                acc.reset(trace?.startedAt ?: now, state)
            }

            acc.recorder?.capture(state)
            val elapsed = acc.startTime?.let { now - it } ?: Duration.ZERO
            val elapsedSeconds = elapsed.toDouble(DurationUnit.SECONDS).toFloat()

            acc.timeInSeconds = elapsedSeconds.toInt()
            val currentPoint = state.toDataPoint(acc.lastPoint)
            val currentTick = (elapsedSeconds * TICKS_PER_SECOND).roundToInt()
            val prevTick = acc.lastTick
            val prevPoint = acc.lastPoint

            if (prevPoint != null && currentTick > prevTick) {
                for (tick in (prevTick + 1)..currentTick) {
                    val t = (tick - prevTick).toFloat() / (currentTick - prevTick)
                    acc.data[tick / TICKS_PER_SECOND_FLOAT] = BrewDataPoint(
                        pressure = lerp(prevPoint.pressure, currentPoint.pressure, t),
                        weight = lerp(prevPoint.weight, currentPoint.weight, t),
                        volume = lerp(prevPoint.volume, currentPoint.volume, t),
                        flowRate = lerp(prevPoint.flowRate, currentPoint.flowRate, t),
                        weightRate = lerp(prevPoint.weightRate, currentPoint.weightRate, t),
                    )
                }
            } else {
                acc.data[currentTick / TICKS_PER_SECOND_FLOAT] = currentPoint
            }
            acc.lastTick = currentTick
            acc.lastPoint = currentPoint
            acc.lastTelemetryTime = state.telemetryTime
        } else {
            if (acc.isBrewing && status == DeviceState.BrewStatus.Idle) {
                acc.lastPoint?.let { previous ->
                    val finalPoint = previous.withFinalTotals(state)
                    acc.data[acc.lastTick / TICKS_PER_SECOND_FLOAT] = finalPoint
                    acc.lastPoint = finalPoint
                }
            }
            acc.isBrewing = false
        }
        return acc
    }

    private fun DeviceState.toDataPoint(previous: BrewDataPoint?) = BrewDataPoint(
        pressure = pressure ?: 0f,
        weight = weight ?: previous?.weight ?: 0f,
        volume = volume ?: previous?.volume ?: 0f,
        flowRate = flowRate ?: 0f,
        weightRate = weightRate ?: 0f,
    )

    private class Accumulator {
        var trace: ProfileExecutionTrace? = null
        val data = mutableMapOf<Float, BrewDataPoint>()
        var recorder: FreeHandRecorder? = null
        var isBrewing = false
        var startTime: Instant? = null
        var status: DeviceState.BrewStatus = DeviceState.BrewStatus.Idle
        var timeInSeconds: Int = 0
        var lastTick: Int = -1
        var lastPoint: BrewDataPoint? = null
        var lastTelemetryTime: Instant? = null

        fun reset(now: Instant, state: DeviceState) {
            trace = null
            val status = state.brewStatus
            recorder = if (status == DeviceState.BrewStatus.FreeVariable) {
                FreeHandRecorder(now, state.freeHandControlMode)
            } else {
                null
            }
            data.clear()
            isBrewing = true
            startTime = now
            this.status = status
            lastTick = -1
            lastPoint = null
            lastTelemetryTime = null
        }
    }
}

private const val TICKS_PER_SECOND = 10
private const val TICKS_PER_SECOND_FLOAT = 10f

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
