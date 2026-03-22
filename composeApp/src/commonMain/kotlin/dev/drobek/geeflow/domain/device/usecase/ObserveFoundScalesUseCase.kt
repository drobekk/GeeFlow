package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.api.DeviceController
import dev.drobek.geeflow.domain.device.model.SmartScale
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Factory

@Factory
class ObserveFoundScalesUseCase(
    private val deviceController: DeviceController
) {
    operator fun invoke(): StateFlow<List<SmartScale>> {
        return deviceController.foundScales
    }
}
