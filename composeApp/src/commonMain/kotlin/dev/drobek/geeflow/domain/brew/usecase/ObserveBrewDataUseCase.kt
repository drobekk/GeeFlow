package dev.drobek.geeflow.domain.brew.usecase

import dev.drobek.geeflow.data.device.api.DeviceController
import dev.drobek.geeflow.domain.brew.model.BrewDataPoint
import dev.drobek.geeflow.domain.device.model.MachineState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import org.koin.core.annotation.Factory
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant

@Factory
class ObserveBrewDataUseCase(
    private val deviceController: DeviceController
) {
    operator fun invoke(): Flow<Map<Float, BrewDataPoint>> {
        return deviceController.machineState
            .scan(Accumulator()) { acc, state ->
                val status = state.brewStatus
                val currentlyBrewing = status == MachineState.BrewStatus.Manual || status == MachineState.BrewStatus.Profile

                if (currentlyBrewing) {
                    val now = Clock.System.now()
                    if (!acc.isBrewing) {
                        acc.data.clear()
                        acc.isBrewing = true
                        acc.startTime = now
                    }

                    val elapsed = acc.startTime?.let { now - it } ?: Duration.ZERO
                    val elapsedSeconds = elapsed.toDouble(DurationUnit.SECONDS).toFloat()

                    acc.data[elapsedSeconds] = BrewDataPoint(
                        pressure = state.pressure ?: 0f,
                        weight = state.weight ?: 0f,
                        volume = state.volume ?: 0f,
                        flowRate = state.flowRate ?: 0f,
                        weightRate = state.weightRate ?: 0f
                    )
                } else if (status == MachineState.BrewStatus.Idle) {
                    acc.isBrewing = false
                }
                acc
            }
            .map { it.data.toMap() }
            .distinctUntilChanged()
    }

    private class Accumulator {
        val data = mutableMapOf<Float, BrewDataPoint>()
        var isBrewing = false
        var startTime: Instant? = null
    }
}
