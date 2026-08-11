package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.scan
import org.koin.core.annotation.Factory
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant

@Factory
class ObserveBrewDataUseCase(
    private val provider: DeviceControllerProvider,
    private val getSelectedUserUseCase: GetSelectedUserUseCase,
) {
    operator fun invoke(deviceId: Long): Flow<BrewSession> {
        val scanFlow = provider.getController(deviceId).deviceState
            .scan(Accumulator(), ::accumulateBrewData)

        return combine(scanFlow, getSelectedUserUseCase()) { acc, user ->
            BrewSession(
                userId = user?.id ?: 0L,
                mode = acc.status.toBrewMode(),
                elapsedSeconds = acc.timeInSeconds,
                startTime = acc.startTime,
                inProgress = acc.isBrewing,
                dataPoints = acc.data.toMap(),
            )
        }.distinctUntilChanged()
    }

    private fun DeviceState.BrewStatus.toBrewMode(): BrewMode = when (this) {
        DeviceState.BrewStatus.Profile -> BrewMode.Profile
        DeviceState.BrewStatus.FreeVariable -> BrewMode.Freehand
        else -> BrewMode.Manual
    }

    private fun accumulateBrewData(acc: Accumulator, state: DeviceState): Accumulator {
        val status = state.brewStatus
        val currentlyBrewing = status == DeviceState.BrewStatus.Manual ||
            status == DeviceState.BrewStatus.Profile ||
            status == DeviceState.BrewStatus.FreeVariable

        if (currentlyBrewing) {
            val now = Clock.System.now()
            if (!acc.isBrewing) {
                acc.reset(now, status)
            }

            val elapsed = acc.startTime?.let { now - it } ?: Duration.ZERO
            val elapsedSeconds = elapsed.toDouble(DurationUnit.SECONDS).toFloat()

            acc.timeInSeconds = elapsedSeconds.toInt()
            val currentPoint = BrewDataPoint(
                pressure = state.pressure ?: 0f,
                weight = state.weight ?: 0f,
                volume = state.volume ?: 0f,
                flowRate = state.flowRate ?: 0f,
                weightRate = state.weightRate ?: 0f,
            )
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
        } else if (status == DeviceState.BrewStatus.Idle) {
            acc.isBrewing = false
        }
        return acc
    }

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

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
