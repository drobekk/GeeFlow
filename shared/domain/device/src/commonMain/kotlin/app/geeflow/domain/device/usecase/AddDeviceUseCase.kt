package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.supportedDevice
import org.koin.core.annotation.Factory

@Factory
class AddDeviceUseCase(
    private val deviceRepository: DeviceRepository,
) {
    operator fun invoke(device: Device): Long {
        val ble = device.connection as? DeviceConnection.Ble
        if (ble != null) {
            val existing = deviceRepository.getDeviceByBleMacAddress(ble.macAddress)
            if (existing != null) return existing.id
        }
        val name = device.supportedDevice?.model ?: device.name
        return deviceRepository.addDevice(device.copy(name = name))
    }
}
