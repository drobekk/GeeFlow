package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.api.DeviceRepository
import dev.drobek.geeflow.domain.device.model.Device
import dev.drobek.geeflow.domain.device.model.SupportedDevice
import org.koin.core.annotation.Factory

@Factory
class AddDeviceUseCase(
    private val deviceRepository: DeviceRepository
) {
    operator fun invoke(
        macAddress: String,
        name: String,
        supportedDevice: SupportedDevice = SupportedDevice.WendougeeDataS
    ) {
        val cleanMac = macAddress.filter { it.isLetterOrDigit() }.uppercase()
        val normalizedMacAddress = if (cleanMac.length == 12) {
            cleanMac.chunked(2).joinToString(":")
        } else {
            macAddress.uppercase()
        }

        deviceRepository.addDevice(
            Device(
                macAddress = normalizedMacAddress,
                name = name,
                manufacturer = supportedDevice.manufacturer,
                model = supportedDevice.model,
                version = supportedDevice.version
            )
        )
    }
}
