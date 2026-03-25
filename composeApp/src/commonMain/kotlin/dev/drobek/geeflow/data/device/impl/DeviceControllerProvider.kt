package dev.drobek.geeflow.data.device.impl

import dev.drobek.geeflow.data.device.api.DeviceController
import dev.drobek.geeflow.data.device.api.DeviceRepository
import dev.drobek.geeflow.domain.device.model.SupportedDevice
import dev.drobek.geeflow.domain.device.model.supportedDevice
import dev.drobek.geeflow.data.device.impl.controller.DemoDeviceController
import dev.drobek.geeflow.data.device.impl.controller.WendougeeDataSController
import org.koin.core.annotation.Singleton

@Singleton
class DeviceControllerProvider(
    private val deviceRepository: DeviceRepository,
    private val wendougeeDataSController: WendougeeDataSController,
    private val demoDeviceController: DemoDeviceController
) {
    fun getController(deviceId: String): DeviceController {
        val device = deviceRepository.getDeviceByMacAddress(deviceId)
        return when (device?.supportedDevice) {
            SupportedDevice.WendougeeDataS -> wendougeeDataSController
            SupportedDevice.GeeFlowDemo -> demoDeviceController
            null -> wendougeeDataSController
        }
    }
}
