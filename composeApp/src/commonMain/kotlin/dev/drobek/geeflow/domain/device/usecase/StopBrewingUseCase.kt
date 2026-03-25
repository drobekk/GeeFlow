package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.impl.DeviceControllerProvider
import dev.drobek.geeflow.domain.device.model.MachineState
import org.koin.core.annotation.Factory

@Factory
class StopBrewingUseCase(private val provider: DeviceControllerProvider) {
    suspend operator fun invoke(deviceId: String) {
        val controller = provider.getController(deviceId)
        if (controller.machineState.value.brewStatus == MachineState.BrewStatus.Profile) {
            controller.stopProfileBrewing()
        } else {
            controller.stopManualBrewing()
        }
    }
}
