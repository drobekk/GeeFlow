package app.geeflow.domain.device.usecase

import app.geeflow.data.device.MaintenanceSettingsRepository
import app.geeflow.data.device.model.CleaningType
import org.koin.core.annotation.Factory

@Factory
class SkipMaintenanceReminderUseCase(private val repository: MaintenanceSettingsRepository) {
    suspend operator fun invoke(deviceId: Long, types: Set<CleaningType>) = repository.postpone(deviceId, types, maintenanceDay())
}
