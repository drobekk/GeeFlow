package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.api.DeviceController
import org.koin.core.annotation.Factory

@Factory
class ConnectSmartScaleUseCase(
    private val deviceController: DeviceController
) {
    suspend operator fun invoke(name: String) {
        deviceController.connectSmartScale(name)
    }
}
