package app.geeflow.domain.device.usecase

import app.geeflow.data.device.AutoFlushSettingsRepository
import org.koin.core.annotation.Factory

@Factory
class ObserveAutoFlushSettingsUseCase(private val repository: AutoFlushSettingsRepository) {
    operator fun invoke(deviceId: Long) = repository.observe(deviceId)
}
