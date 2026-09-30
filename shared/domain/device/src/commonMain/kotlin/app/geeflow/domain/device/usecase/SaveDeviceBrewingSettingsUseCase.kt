package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceBrewingSettingsRepository
import app.geeflow.data.device.model.DeviceBrewingSettings
import org.koin.core.annotation.Factory

@Factory
class SaveDeviceBrewingSettingsUseCase(private val repository: DeviceBrewingSettingsRepository) {
    suspend operator fun invoke(deviceId: Long, settings: DeviceBrewingSettings) = repository.save(deviceId, settings)
}
