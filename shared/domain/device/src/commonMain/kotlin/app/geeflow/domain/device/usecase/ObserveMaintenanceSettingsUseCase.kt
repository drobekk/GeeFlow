package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.MaintenanceSettingsRepository
import app.geeflow.data.device.model.CleaningProgram
import app.geeflow.data.device.model.MaintenanceSettings
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory

@Factory
class ObserveMaintenanceSettingsUseCase(
    private val repository: MaintenanceSettingsRepository,
    private val provider: DeviceControllerProvider,
) {
    operator fun invoke(deviceId: Long) = flow {
        if (repository.observe(deviceId).first() == null) {
            val controller = provider.getController(deviceId)
            val config = controller.deviceState.map { it.config }.filterNotNull().first()
            val limits = controller.constraints
            val daily = CleaningProgram(
                flushSeconds = config.cleaningTimeSec.toInt().coerceIn(limits.cleaningTimeRange),
                restSeconds = config.cleaningStandbySec.toInt().coerceIn(limits.cleaningRestRange),
                cycles = config.cleaningCount.coerceIn(limits.cleaningCountRange),
            )
            repository.initialize(
                deviceId,
                MaintenanceSettings(
                    daily = daily,
                    deep = daily.copy(cycles = (daily.cycles * 2).coerceIn(limits.cleaningCountRange)),
                ),
            )
        }
        emitAll(repository.observe(deviceId).filterNotNull())
    }
}
