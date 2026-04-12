package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.DeviceControllerProvider
import dev.drobek.geeflow.data.device.model.DeviceState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory

data class CleaningStatus(
    val inProgress: Boolean,
    val flush: Progress,
    val rest: Progress,
    val cycle: Progress,
) {
    data class Progress(
        val current: Int,
        val target: Int,
    )
}

@Factory
class GetCleaningStatusUseCase(
    private val provider: DeviceControllerProvider,
) {
    operator fun invoke(deviceId: Long): Flow<CleaningStatus> =
        provider.getController(deviceId).deviceState.map { state ->
            val config = state.config
            val time = state.time ?: 0

            if (state.brewStatus != DeviceState.BrewStatus.Cleaning || config == null) {
                val flushTarget = config?.cleaningTimeSec?.toInt() ?: 0
                val restTarget = config?.cleaningStandbySec?.toInt() ?: 0
                val cycleTarget = config?.cleaningCount ?: 0

                return@map CleaningStatus(
                    inProgress = false,
                    flush = CleaningStatus.Progress(0, flushTarget),
                    rest = CleaningStatus.Progress(0, restTarget),
                    cycle = CleaningStatus.Progress(0, cycleTarget),
                )
            }

            val flushTime = config.cleaningTimeSec.toInt()
            val restTime = config.cleaningStandbySec.toInt()
            val totalCycles = config.cleaningCount
            val cycleDuration = flushTime + restTime

            if (cycleDuration == 0) {
                return@map CleaningStatus(
                    inProgress = false,
                    flush = CleaningStatus.Progress(0, flushTime),
                    rest = CleaningStatus.Progress(0, restTime),
                    cycle = CleaningStatus.Progress(0, totalCycles),
                )
            }

            val currentCycle = ((time - 1) / cycleDuration).coerceIn(0, totalCycles - 1)
            val timeInCycle = (time - 1) % cycleDuration + 1

            val (currentFlush, currentRest) = if (timeInCycle <= flushTime) {
                timeInCycle to 0
            } else {
                flushTime to (timeInCycle - flushTime)
            }

            CleaningStatus(
                inProgress = true,
                flush = CleaningStatus.Progress(currentFlush, flushTime),
                rest = CleaningStatus.Progress(currentRest, restTime),
                cycle = CleaningStatus.Progress(currentCycle + 1, totalCycles),
            )
        }
}
