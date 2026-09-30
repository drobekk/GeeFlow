package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceBrewingSettingsRepository
import org.koin.core.annotation.Factory

@Factory
class ObserveDeviceBrewingSettingsUseCase(private val repository: DeviceBrewingSettingsRepository) {
    operator fun invoke(deviceId: Long) = repository.observe(deviceId)
}
