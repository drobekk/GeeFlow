package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.MaintenanceSettingsRepository
import app.geeflow.data.device.model.CleaningType
import app.geeflow.data.device.model.DeviceCapability
import app.geeflow.data.device.model.DeviceState
import app.geeflow.domain.device.ProfileExecutionCoordinator
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory
class StartCleaningUseCase(
    private val coordinator: ProfileExecutionCoordinator,
    private val maintenance: MaintenanceSettingsRepository,
    private val provider: DeviceControllerProvider,
) {
    suspend operator fun invoke(deviceId: Long, type: CleaningType = CleaningType.Daily) {
        val settings = requireNotNull(maintenance.observe(deviceId).first())
        val types = if (type == CleaningType.Deep) CleaningType.entries.toSet() else setOf(type)
        maintenance.postpone(deviceId, types, maintenanceDay())
        coordinator.withUnownedControl(deviceId) {
            with(provider.getController(deviceId)) {
                requireConnected(deviceId)
                check(DeviceCapability.CleaningMode in capabilities && DeviceCapability.CleaningSettings in capabilities)
                check(deviceState.value.brewStatus == DeviceState.BrewStatus.Idle)
                val program = settings.program(type)
                require(program.flushSeconds in constraints.cleaningTimeRange)
                require(program.restSeconds in constraints.cleaningRestRange)
                require(program.cycles in constraints.cleaningCountRange)
                setCleaningSettings(program.flushSeconds.toFloat(), program.restSeconds.toFloat(), program.cycles)
                startCleaning()
            }
        }
    }
}
