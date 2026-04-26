package app.geeflow.data.device.impl.discovery

import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.SupportedDevice

/**
 * Per-device-family recogniser. Inspects raw advertising info from [BleAdvertisement]
 * and either returns a fully-formed [Device] (id = 0L, ready to insert) when the
 * advertisement matches this discoverer's [supportedDevice], or null otherwise.
 *
 * All device-specific name parsing, MAC extraction, and connection-shape construction
 * lives here, never in the generic BLE layer.
 */
interface BleDeviceDiscoverer {
    val supportedDevice: SupportedDevice
    fun tryRecognise(advertisement: BleAdvertisement): Device?
}

/**
 * The minimum slice of advertising data discoverers need. The generic BLE client
 * fills this in from whatever the platform provides; discoverers stay platform-agnostic.
 */
data class BleAdvertisement(
    val peripheralId: String,
    val name: String?,
)
