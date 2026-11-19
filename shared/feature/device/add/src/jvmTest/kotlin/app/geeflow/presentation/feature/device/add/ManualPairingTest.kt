package app.geeflow.presentation.feature.device.add

import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.DeviceManufacturer
import app.geeflow.data.device.model.SupportedDevice
import app.geeflow.presentation.feature.device.add.AddDeviceViewState.ManualEntry
import app.geeflow.presentation.feature.device.add.AddDeviceViewState.ManualError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ManualPairingTest {
    private val entry = ManualEntry(serialNumber = "AB00000001")
    private val machine = Device(
        name = "Wendougee",
        connection = DeviceConnection.Ble("live-peripheral-id", "AA:BB:CC:DD:EE:FF"),
        manufacturer = DeviceManufacturer.WENDOUGEE,
    )

    @Test
    fun `serial alone is valid but is never converted to an address`() {
        assertNull(entry.validationError())
        assertNull(normalizeBluetoothAddress(entry.serialNumber))
        val item = requireNotNull(entry.candidate(machine, "Wendougee_AABBCCDDEEFF"))
        val confirmed = requireNotNull(entry.confirmedDevice(item))
        assertEquals(machine.connection, confirmed.connection)
        assertEquals("LITA BA · AB00000001", confirmed.name)
        assertEquals(SupportedDevice.LitaBA.id, confirmed.modelId)
        assertEquals("manual-entry", confirmed.modelSource)
    }

    @Test
    fun `serial matching requires a full token and does not hide unverified machines`() {
        assertTrue(requireNotNull(entry.candidate(machine, "WDG_AB00000001_AABBCCDDEEFF")).serialMatches)
        assertFalse(requireNotNull(entry.candidate(machine, "WDG_XAB000000012_AABBCCDDEEFF")).serialMatches)
        assertFalse(requireNotNull(entry.candidate(machine, null)).serialMatches)
    }

    @Test
    fun `optional Bluetooth address is normalized and filters out other devices`() {
        for (raw in listOf("aabbccddeeff", "aa-bb-cc-dd-ee-ff", " AA:BB:CC:DD:EE:FF ")) {
            assertEquals("AA:BB:CC:DD:EE:FF", normalizeBluetoothAddress(raw))
            assertTrue(entry.copy(bluetoothAddress = raw).candidate(machine, null) != null)
        }
        assertNull(entry.copy(bluetoothAddress = "11:22:33:44:55:66").candidate(machine, null))
    }

    @Test
    fun `missing serial invalid address and malformed input are rejected`() {
        assertEquals(ManualError.SerialRequired, entry.copy(serialNumber = " ").validationError())
        assertEquals(ManualError.InvalidSerial, entry.copy(serialNumber = "A".repeat(65)).validationError())
        assertEquals(ManualError.InvalidSerial, entry.copy(serialNumber = "https://machine").validationError())
        assertEquals(ManualError.InvalidAddress, entry.copy(bluetoothAddress = "AB00000001").validationError())
        assertNull(normalizeBluetoothAddress("AA:BB:CC:DD:EE:FF:00"))
        assertNull(normalizeBluetoothAddress("prefixAABBCCDDEEFF"))
    }

    @Test
    fun `known conflicting model and non Wendougee candidate cannot be confirmed`() {
        val otherModel = machine.copy(model = "LITA LR", modelId = SupportedDevice.LitaLR.id)
        val item = requireNotNull(entry.candidate(otherModel, null))
        assertFalse(item.modelCompatible)
        assertNull(entry.confirmedDevice(item))
        assertNull(entry.candidate(machine.copy(manufacturer = "Other"), null))
        assertNull(
            entry.copy(modelId = SupportedDevice.GeeFlowDemo.id).confirmedDevice(requireNotNull(entry.candidate(machine, null)))
        )
    }
}
