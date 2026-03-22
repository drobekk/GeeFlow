package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.api.DeviceController
import org.koin.core.annotation.Factory

@Factory
class StopSmartScaleSearchUseCase(
    private val deviceController: DeviceController
) {
    suspend operator fun invoke() {
        deviceController.stopSmartScaleSearch()
    }
}
