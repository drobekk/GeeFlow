package app.geeflow.domain.device.usecase

import app.geeflow.data.device.MaintenanceSettingsRepository
import app.geeflow.data.device.model.MaintenanceSettings
import org.koin.core.annotation.Factory

@Factory
class SaveMaintenanceSettingsUseCase(private val repository: MaintenanceSettingsRepository) {
    suspend operator fun invoke(deviceId: Long, settings: MaintenanceSettings) {
        repository.save(deviceId, settings, maintenanceDay())
    }
}
