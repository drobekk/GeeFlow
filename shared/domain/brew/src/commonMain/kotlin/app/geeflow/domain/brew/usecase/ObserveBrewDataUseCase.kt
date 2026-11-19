package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.brew.model.ProfileExecutionTrace
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import org.koin.core.annotation.Factory
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
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(deviceId: Long): Flow<BrewSession> =
        provider.observeController(deviceId).flatMapLatest { controller -> observeSession(deviceId, controller) }

    @Suppress("CyclomaticComplexMethod")
    private fun observeSession(deviceId: Long, controller: DeviceController): Flow<BrewSession> {
        val telemetry = controller.deviceState
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
        endedAt = endTime,
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
                acc.reset(state.sessionOrigin(trace, now), state)
            }

            acc.recorder?.capture(state)
            val elapsed = acc.startTime?.let { now - it } ?: Duration.ZERO
            val elapsedSeconds = elapsed.toDouble(DurationUnit.SECONDS).toFloat()

            acc.timeInSeconds = elapsedSeconds.toInt().coerceAtLeast(0)
            acc.captureMeasurement(state, now)
        } else {
            if (acc.isBrewing && status == DeviceState.BrewStatus.Idle) {
                acc.lastPoint?.let { previous ->
                    val finalPoint = previous.withFinalTotals(state)
                    acc.data[acc.lastSampleSeconds] = finalPoint
                    acc.lastPoint = finalPoint
                }
                acc.endTime = state.telemetryTime ?: Clock.System.now()
            }
            acc.isBrewing = false
        }
        return acc
    }

    private fun DeviceState.sessionOrigin(trace: ProfileExecutionTrace?, now: Instant) =
        trace?.startedAt ?: statusTime ?: telemetryTime ?: now

    private fun Accumulator.captureMeasurement(state: DeviceState, now: Instant) {
        val sampleIsCurrent = state.telemetryTime?.let { it >= requireNotNull(startTime) } ?: true
        if (!sampleIsCurrent) return
        val currentPoint = state.toDataPoint(lastPoint)
        val freshTelemetry = state.telemetryTime != lastTelemetryTime
        val previousPoint = lastPoint
        val controlMeasurementChanged = previousPoint == null ||
            currentPoint.pressure != previousPoint.pressure ||
            currentPoint.weight != previousPoint.weight ||
            currentPoint.volume != previousPoint.volume ||
            currentPoint.flowRate != previousPoint.flowRate ||
            currentPoint.weightRate != previousPoint.weightRate
        val newMeasurement = previousPoint == null || freshTelemetry || controlMeasurementChanged
        if (newMeasurement) {
            val sampleTime = state.telemetryTime.takeIf { freshTelemetry } ?: now
            val sampleSeconds = startTime?.let { sampleTime - it }
                ?.toDouble(DurationUnit.SECONDS)?.toFloat()?.coerceAtLeast(0f) ?: 0f
            data[sampleSeconds] = currentPoint
            lastSampleSeconds = sampleSeconds
            lastPoint = currentPoint
            lastTelemetryTime = state.telemetryTime
        } else if (currentPoint != previousPoint) {
            // Refresh non-control telemetry such as boiler temperatures on the latest sample
            // without inventing another machine sample time.
            data[lastSampleSeconds] = currentPoint
            lastPoint = currentPoint
        }
    }

    private fun DeviceState.toDataPoint(previous: BrewDataPoint?) = BrewDataPoint(
        pressure = pressure ?: 0f,
        weight = weight ?: previous?.weight ?: 0f,
        volume = volume ?: previous?.volume ?: 0f,
        flowRate = flowRate ?: 0f,
        weightRate = weightRate ?: 0f,
        brewBoilerTempC = brewBoilerTemp,
        steamBoilerTempC = steamBoilerTemp,
        machineElapsedSeconds = time,
    )

    private class Accumulator {
        var trace: ProfileExecutionTrace? = null
        val data = mutableMapOf<Float, BrewDataPoint>()
        var recorder: FreeHandRecorder? = null
        var isBrewing = false
        var startTime: Instant? = null
        var endTime: Instant? = null
        var status: DeviceState.BrewStatus = DeviceState.BrewStatus.Idle
        var timeInSeconds: Int = 0
        var lastSampleSeconds: Float = 0f
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
            endTime = null
            this.status = status
            lastSampleSeconds = 0f
            lastPoint = null
            lastTelemetryTime = null
        }
    }
}
