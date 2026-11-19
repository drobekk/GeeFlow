package app.geeflow.presentation.feature.device.add

import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.DeviceManufacturer
import app.geeflow.data.device.model.SupportedDevice
import app.geeflow.data.device.model.supportedDevice
import app.geeflow.presentation.feature.device.add.AddDeviceViewState.ManualEntry
import app.geeflow.presentation.feature.device.add.AddDeviceViewState.ManualError

/** A serial is a label, not a Bluetooth address. Never derive an address from it. */
internal fun ManualEntry.validationError(): ManualError? = when {
    serialNumber.isBlank() -> ManualError.SerialRequired
    serialNumber.trim().length > MaxSerialLength ||
        !serialNumber.trim().matches(Regex("[A-Za-z0-9][A-Za-z0-9 _/.-]*")) -> ManualError.InvalidSerial
    bluetoothAddress.isNotBlank() && normalizeBluetoothAddress(bluetoothAddress) == null -> ManualError.InvalidAddress
    else -> null
}

internal fun normalizeBluetoothAddress(raw: String): String? {
    val value = raw.trim().uppercase()
    if (!value.matches(Regex("([0-9A-F]{12}|([0-9A-F]{2}:){5}[0-9A-F]{2}|([0-9A-F]{2}-){5}[0-9A-F]{2})"))) return null
    return value.replace(":", "").replace("-", "").chunked(2).joinToString(":")
}

internal fun ManualEntry.candidate(device: Device, advertisedName: String?): AddDeviceViewState.ManualDeviceItem? {
    val ble = device.connection as? DeviceConnection.Ble ?: return null
    if (device.manufacturer != DeviceManufacturer.WENDOUGEE) return null
    val address = normalizeBluetoothAddress(bluetoothAddress)
    if (address != null && normalizeBluetoothAddress(ble.macAddress) != address) return null
    val serial = serialNumber.trim().uppercase()
    val names = listOfNotNull(device.name, advertisedName).map { it.uppercase() }
    // Match a complete token, allowing the underscore separators used by BLE names.
    val serialPattern = Regex("(^|[^A-Z0-9])${Regex.escape(serial)}($|[^A-Z0-9])")
    return AddDeviceViewState.ManualDeviceItem(
        device = device,
        serialMatches = names.any { serialPattern.containsMatchIn(it) },
        modelCompatible = device.supportedDevice?.id?.let { it == modelId } ?: true,
    )
}

internal fun ManualEntry.confirmedDevice(item: AddDeviceViewState.ManualDeviceItem): Device? {
    val model = SupportedDevice.byId(modelId)?.takeIf { it != SupportedDevice.GeeFlowDemo } ?: return null
    if (validationError() != null || !item.modelCompatible) return null
    return item.device.copy(
        name = "${model.model} · ${serialNumber.trim()}",
        manufacturer = model.manufacturer,
        model = model.model,
        version = model.version,
        modelId = model.id,
        modelSource = "manual-entry",
        serialNumber = serialNumber.trim(),
    )
}

private const val MaxSerialLength = 64
