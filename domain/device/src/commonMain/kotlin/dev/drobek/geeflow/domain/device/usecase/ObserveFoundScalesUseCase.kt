package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.DeviceControllerProvider
import dev.drobek.geeflow.data.device.model.SmartScale
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

@Factory
class ObserveFoundScalesUseCase(private val provider: DeviceControllerProvider) {
    operator fun invoke(deviceId: Long): StateFlow<List<SmartScale>> =
        provider.getController(deviceId).foundScales
}
