package app.geeflow.data.device.impl.discovery

import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.SupportedDevice
import org.koin.core.annotation.Single

/** Names may carry a hardware MAC suffix; Android also exposes the actual BLE address as the peripheral id. */
@Single(binds = [BleDeviceDiscoverer::class])
class WendougeeBleDeviceDiscoverer : BleDeviceDiscoverer {
    override val supportedDevice: SupportedDevice = SupportedDevice.WendougeeDataS

    override fun tryRecognise(advertisement: BleAdvertisement): Device? {
        val name = advertisement.name ?: return null
        val match = HEX_PATTERN.find(name)
        val brand = name.substring(0, match?.range?.first ?: name.length).lowercase()
        val knownBrand = listOf("wendougee", "wdg", "lita").any(brand::contains)
        if (!knownBrand && SupportedDevice.fromDeviceName(name) == null) return null
        return device(advertisement)
    }

    /** Only manual setup may recognise an otherwise unknown name containing the exact user-entered serial. */
    fun tryRecogniseSerial(advertisement: BleAdvertisement, serialNumber: String): Device? {
        val serial = serialNumber.trim().uppercase()
        val name = advertisement.name?.uppercase() ?: return null
        if (serial.isEmpty() || !Regex("(^|[^A-Z0-9])${Regex.escape(serial)}($|[^A-Z0-9])").containsMatchIn(name)) return null
        // The complete name may itself be a hexadecimal serial. Use the real Android BLE address.
        val mac = advertisement.peripheralId.takeIf { MAC_PATTERN.matches(it) }?.uppercase() ?: return null
        return device(advertisement, mac)
    }

    private fun device(advertisement: BleAdvertisement, address: String? = null): Device? {
        val name = advertisement.name ?: return null
        val mac = address ?: macAddressOf(advertisement) ?: return null
        val match = HEX_PATTERN.find(name)
        val prefix = name.substring(0, match?.range?.first ?: name.length)
            .trimEnd('_', '-', ' ')
            .replace('_', ' ')
            .trim()
        val model = SupportedDevice.fromDeviceName(name)
        return Device(
            id = 0L,
            name = prefix.ifEmpty { supportedDevice.manufacturer },
            connection = DeviceConnection.Ble(peripheralId = advertisement.peripheralId, macAddress = mac),
            manufacturer = supportedDevice.manufacturer,
            model = model?.model.orEmpty(),
            modelId = model?.id,
            modelSource = model?.let { "bluetooth-name" },
        )
    }

    /** Used for reconnect too, so serial-only Android names remain reachable after restarting the app. */
    fun macAddressOf(advertisement: BleAdvertisement): String? {
        val raw = advertisement.name?.let { HEX_PATTERN.find(it)?.value }
            ?: advertisement.peripheralId.takeIf { MAC_PATTERN.matches(it) }?.replace(":", "")
            ?: return null
        return raw.uppercase().chunked(2).joinToString(":")
    }

    fun matchesAddress(advertisement: BleAdvertisement, targetMac: String): Boolean =
        advertisement.peripheralId.equals(targetMac, ignoreCase = true) || macAddressOf(advertisement) == targetMac

    private companion object {
        private val HEX_PATTERN = Regex("([a-fA-F0-9]{12})$")
        private val MAC_PATTERN = Regex("([a-fA-F0-9]{2}:){5}[a-fA-F0-9]{2}")
    }
}
