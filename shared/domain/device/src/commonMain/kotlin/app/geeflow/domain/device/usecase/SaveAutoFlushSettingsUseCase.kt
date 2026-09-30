package app.geeflow.domain.device.usecase

import app.geeflow.data.device.AutoFlushSettingsRepository
import app.geeflow.data.device.model.AutoFlushSettings
import org.koin.core.annotation.Factory

@Factory
class SaveAutoFlushSettingsUseCase(private val repository: AutoFlushSettingsRepository) {
    suspend operator fun invoke(deviceId: Long, settings: AutoFlushSettings) = repository.save(deviceId, settings)
}
