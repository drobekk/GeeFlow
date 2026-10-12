package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.SupportedDevice
import app.geeflow.data.device.model.isDemo
import app.geeflow.data.device.model.supportedDevice
import org.koin.core.annotation.Factory

@Factory
class SetDeviceModelUseCase(
    private val repository: DeviceRepository,
    private val provider: DeviceControllerProvider,
) {
    suspend operator fun invoke(deviceId: Long, modelId: String) {
        val model = requireNotNull(SupportedDevice.byId(modelId))
        require(model != SupportedDevice.GeeFlowDemo) { "A physical device cannot become a demo" }
        val device = requireNotNull(repository.getDeviceById(deviceId)) { "Device does not exist" }
        require(!device.isDemo) { "A demo cannot become a physical device" }
        if (device.supportedDevice == model) return
        provider.forgetController(deviceId)
        repository.updateModel(deviceId, model)
    }
}
