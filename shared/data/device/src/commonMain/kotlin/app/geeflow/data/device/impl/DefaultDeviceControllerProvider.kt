package app.geeflow.data.device.impl

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.impl.controller.DemoDeviceController
import app.geeflow.data.device.impl.controller.WendougeeDataSController
import app.geeflow.data.device.model.SupportedDevice
import app.geeflow.data.device.model.supportedDevice
import org.koin.core.annotation.Single

@Single(binds = [DeviceControllerProvider::class])
class DefaultDeviceControllerProvider(
    private val deviceRepository: DeviceRepository,
    private val wendougeeDataSController: WendougeeDataSController,
    private val demoDeviceController: DemoDeviceController,
) : DeviceControllerProvider {
    override var currentDeviceId: Long? = null
        private set

    override fun getController(deviceId: Long): DeviceController {
        currentDeviceId = deviceId
        val device = deviceRepository.getDeviceById(deviceId)
        return when (device?.supportedDevice) {
            SupportedDevice.WendougeeDataS -> wendougeeDataSController
            SupportedDevice.GeeFlowDemo -> demoDeviceController
            null -> wendougeeDataSController
        }
    }

    override fun disconnectCurrent() {
        val id = currentDeviceId ?: return
        getController(id).disconnect()
        currentDeviceId = null
    }
}
