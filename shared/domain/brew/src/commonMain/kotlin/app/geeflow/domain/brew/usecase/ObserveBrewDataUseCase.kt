package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import org.koin.core.annotation.Factory
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.TimeSource

@Factory
class ObserveBrewDataUseCase(
    private val provider: DeviceControllerProvider,
    private val getSelectedUserUseCase: GetSelectedUserUseCase,
) {
    operator fun invoke(deviceId: Long): Flow<BrewSession> {
        val deviceState = provider.getController(deviceId).deviceState
        val sessionFlow = flow {
            val acc = Accumulator()
            var stoppedAt: TimeSource.Monotonic.ValueTimeMark? = null
            while (currentCoroutineContext().isActive) {
                val state = deviceState.value
                val currentlyBrewing = state.brewStatus.isBrewing()
                if (acc.isBrewing && !currentlyBrewing && stoppedAt == null) {
                    stoppedAt = TimeSource.Monotonic.markNow()
                }
                val captureFinished = stoppedAt?.elapsedNow()?.let { it >= POST_BREW_CAPTURE_SECONDS.seconds } == true
                val restarted = stoppedAt != null && currentlyBrewing
                if (acc.isBrewing && (restarted || captureFinished)) {
                    acc.isBrewing = false
                    emit(acc.toSession())
                }
                if (currentlyBrewing) stoppedAt = null
                accumulateBrewData(acc, state)
                emit(acc.toSession())
                // Keep a fixed chart cadence and finish even when deviceState stops emitting.
                delay(SAMPLE_INTERVAL_MS)
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
    )

    private fun DeviceState.BrewStatus.isBrewing() = this == DeviceState.BrewStatus.Manual ||
        this == DeviceState.BrewStatus.Profile || this == DeviceState.BrewStatus.FreeVariable

    private fun DeviceState.BrewStatus.toBrewMode(): BrewMode = when (this) {
        DeviceState.BrewStatus.Profile -> BrewMode.Profile
        DeviceState.BrewStatus.FreeVariable -> BrewMode.Freehand
        else -> BrewMode.Manual
    }

    private fun accumulateBrewData(acc: Accumulator, state: DeviceState): Accumulator {
        val status = state.brewStatus
        val currentlyBrewing = status.isBrewing()

        if (currentlyBrewing || (acc.isBrewing && status == DeviceState.BrewStatus.Idle)) {
            val now = Clock.System.now()
            if (!acc.isBrewing) {
                acc.reset(now, status)
            }

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
        val data = mutableMapOf<Float, BrewDataPoint>()
        var isBrewing = false
        var startTime: Instant? = null
        var status: DeviceState.BrewStatus = DeviceState.BrewStatus.Idle
        var timeInSeconds: Int = 0
        var lastTick: Int = -1
        var lastPoint: BrewDataPoint? = null

        fun reset(now: Instant, status: DeviceState.BrewStatus) {
            data.clear()
            isBrewing = true
            startTime = now
            this.status = status
            lastTick = -1
            lastPoint = null
        }
    }
}

private const val TICKS_PER_SECOND = 10
private const val TICKS_PER_SECOND_FLOAT = 10f
private const val SAMPLE_INTERVAL_MS = 100L
private const val POST_BREW_CAPTURE_SECONDS = 2

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
