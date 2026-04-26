package app.geeflow.data.device.impl.discovery

import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.SupportedDevice
import org.koin.core.annotation.Single

/**
 * Wendougee Data-S advertisements carry their hardware MAC as a hex suffix in the
 * device name (e.g. "Wendougee_AABBCCDDEEFF"). This is the single source of truth for
 * parsing that format and constructing the matching [Device]/[DeviceConnection.Ble].
 */
@Single(binds = [BleDeviceDiscoverer::class])
class WendougeeBleDeviceDiscoverer : BleDeviceDiscoverer {

    override val supportedDevice: SupportedDevice = SupportedDevice.WendougeeDataS

    override fun tryRecognise(advertisement: BleAdvertisement): Device? {
        val name = advertisement.name ?: return null
        val match = HEX_PATTERN.find(name) ?: return null

        val rawMac = match.value.uppercase()
        val formattedMac = rawMac.chunked(2).joinToString(":")
        val prefix = name.substring(0, match.range.first)
            .trimEnd('_', '-', ' ')
            .replace('_', ' ')
            .trim()
        val displayName = prefix.ifEmpty { supportedDevice.manufacturer }

        return Device(
            id = 0L,
            name = displayName,
            connection = DeviceConnection.Ble(
                peripheralId = advertisement.peripheralId,
                macAddress = formattedMac,
            ),
            manufacturer = supportedDevice.manufacturer,
            model = supportedDevice.model,
            version = supportedDevice.version,
        )
    }

    /** Convenience for the controller's reconnect-by-MAC fallback. */
    fun macAddressOf(advertisement: BleAdvertisement): String? =
        tryRecognise(advertisement)?.let { (it.connection as? DeviceConnection.Ble)?.macAddress }

    private companion object {
        private val HEX_PATTERN = Regex("([a-fA-F0-9]{12})$")
    }
}
