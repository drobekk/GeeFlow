package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.impl.DeviceControllerProvider
import dev.drobek.geeflow.domain.device.model.SmartScale
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

@Factory
class ObserveFoundScalesUseCase(private val provider: DeviceControllerProvider) {
    operator fun invoke(deviceId: String): StateFlow<List<SmartScale>> =
        provider.getController(deviceId).foundScales
}
