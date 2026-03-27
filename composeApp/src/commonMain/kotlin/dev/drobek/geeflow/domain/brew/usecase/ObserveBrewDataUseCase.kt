package dev.drobek.geeflow.domain.brew.usecase

import dev.drobek.geeflow.data.device.impl.DeviceControllerProvider
import dev.drobek.geeflow.domain.brew.model.BrewDataPoint
import dev.drobek.geeflow.domain.brew.model.BrewSession
import dev.drobek.geeflow.domain.device.model.MachineState
import dev.drobek.geeflow.domain.user.usecase.GetSelectedUserUseCase
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
    private val getSelectedUserUseCase: GetSelectedUserUseCase
) {
    operator fun invoke(deviceId: String): Flow<BrewSession> {
        val scanFlow = provider.getController(deviceId).machineState
            .scan(Accumulator()) { acc, state ->
                val status = state.brewStatus
                val currentlyBrewing = status == MachineState.BrewStatus.Manual || status == MachineState.BrewStatus.Profile

                if (currentlyBrewing) {
                    val now = Clock.System.now()
                    if (!acc.isBrewing) {
                        acc.data.clear()
                        acc.isBrewing = true
                        acc.startTime = now
                        acc.status = status
                        acc.lastTick = -1
                        acc.lastPoint = null
                    }

                    val elapsed = acc.startTime?.let { now - it } ?: Duration.ZERO
                    val elapsedSeconds = elapsed.toDouble(DurationUnit.SECONDS).toFloat()

                    acc.timeInSeconds = elapsedSeconds.toInt()
                    val currentPoint = BrewDataPoint(
                        pressure = state.pressure ?: 0f,
                        weight = state.weight ?: 0f,
                        volume = state.volume ?: 0f,
                        flowRate = state.flowRate ?: 0f,
                        weightRate = state.weightRate ?: 0f
                    )
                    val currentTick = (elapsedSeconds * 10).roundToInt()
                    val prevTick = acc.lastTick
                    val prevPoint = acc.lastPoint
                    if (prevPoint != null && currentTick > prevTick) {
                        for (tick in (prevTick + 1)..currentTick) {
                            val t = if (currentTick == prevTick) 1f else (tick - prevTick).toFloat() / (currentTick - prevTick)
                            acc.data[tick / 10f] = BrewDataPoint(
                                pressure = lerp(prevPoint.pressure, currentPoint.pressure, t),
                                weight = lerp(prevPoint.weight, currentPoint.weight, t),
                                volume = lerp(prevPoint.volume, currentPoint.volume, t),
                                flowRate = lerp(prevPoint.flowRate, currentPoint.flowRate, t),
                                weightRate = lerp(prevPoint.weightRate, currentPoint.weightRate, t),
                            )
                        }
                    } else {
                        acc.data[currentTick / 10f] = currentPoint
                    }
                    acc.lastTick = currentTick
                    acc.lastPoint = currentPoint
                } else if (status == MachineState.BrewStatus.Idle) {
                    acc.isBrewing = false
                }
                acc
            }

        return combine(scanFlow, getSelectedUserUseCase()) { acc, user ->
            val isManual = acc.status == MachineState.BrewStatus.Manual
            BrewSession(
                userId = user?.id ?: 0L,
                elapsedSeconds = acc.timeInSeconds,
                profileId = if (isManual) null else null, // TODO Get Profile Id
                profileName = if (isManual) "M" else null,
                startTime = acc.startTime,
                inProgress = acc.isBrewing,
                dataPoints = acc.data.toMap()
            )
        }.distinctUntilChanged()
    }

    private class Accumulator {
        val data = mutableMapOf<Float, BrewDataPoint>()
        var isBrewing = false
        var startTime: Instant? = null
        var status: MachineState.BrewStatus = MachineState.BrewStatus.Idle
        var timeInSeconds: Int = 0
        var lastTick: Int = -1
        var lastPoint: BrewDataPoint? = null
    }
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
