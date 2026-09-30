package app.geeflow.data.device.impl

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.impl.controller.DemoDeviceController
import app.geeflow.data.device.impl.controller.WendougeeDataSController
import app.geeflow.data.device.model.SupportedDevice
import app.geeflow.data.device.model.supportedDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

@Single(binds = [DeviceControllerProvider::class])
class DefaultDeviceControllerProvider(
    private val deviceRepository: DeviceRepository,
    private val wendougeeDataSController: Lazy<WendougeeDataSController>,
    private val demoDeviceController: Lazy<DemoDeviceController>,
) : DeviceControllerProvider {
    private val mutableCurrentDeviceId = MutableStateFlow<Long?>(null)
    override val currentDeviceId = mutableCurrentDeviceId.asStateFlow()

    override fun getController(deviceId: Long): DeviceController {
        val device = deviceRepository.getDeviceById(deviceId)
        val controller = when (device?.supportedDevice) {
            SupportedDevice.WendougeeDataS -> wendougeeDataSController.value
            SupportedDevice.GeeFlowDemo -> demoDeviceController.value
            null -> wendougeeDataSController.value
        }
        mutableCurrentDeviceId.value = deviceId
        return controller
    }

    override fun disconnectCurrent() {
        val id = currentDeviceId.value
        if (id != null) {
            getController(id).disconnect()
            mutableCurrentDeviceId.value = null
        } else {
            if (wendougeeDataSController.isInitialized()) {
                wendougeeDataSController.value.disconnect()
            }
            if (demoDeviceController.isInitialized()) {
                demoDeviceController.value.disconnect()
            }
        }
    }
}
