package app.geeflow.data.device.impl.discovery

import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.SupportedDevice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WendougeeManualDiscoveryTest {
    private val discoverer = WendougeeBleDeviceDiscoverer()

    @Test
    fun `legacy branded MAC suffix still resolves on UUID based platforms`() {
        val ad = BleAdvertisement("native-uuid", "Wendougee_AABBCCDDEEFF")
        assertEquals(DeviceConnection.Ble("native-uuid", "AA:BB:CC:DD:EE:FF"), discoverer.tryRecognise(ad)?.connection)
    }

    @Test
    fun `LITA without MAC suffix uses actual Android address`() {
        val ad = BleAdvertisement("AA:BB:CC:DD:EE:FF", "LITA BA")
        val device = discoverer.tryRecognise(ad)
        assertEquals(SupportedDevice.LitaBA.id, device?.modelId)
        assertEquals(DeviceConnection.Ble(ad.peripheralId, ad.peripheralId), device?.connection)
        assertEquals(ad.peripheralId, discoverer.macAddressOf(ad))
    }

    @Test
    fun `serial-only name is admitted only by explicit manual search and reconnect uses actual address`() {
        val ad = BleAdvertisement("AA:BB:CC:DD:EE:FF", "AB00000001")
        assertNull(discoverer.tryRecognise(ad))
        assertNull(discoverer.tryRecogniseSerial(ad, "AB00000002"))
        assertNull(discoverer.tryRecogniseSerial(ad.copy(name = "XAB000000012"), "AB00000001"))
        val device = discoverer.tryRecogniseSerial(ad, "ab00000001")
        assertEquals(DeviceConnection.Ble(ad.peripheralId, ad.peripheralId), device?.connection)
        assertNull(device?.modelId)
        assertEquals(ad.peripheralId, discoverer.macAddressOf(ad))
    }

    @Test
    fun `serial cannot invent a missing BLE address or identify unrelated anonymous devices`() {
        assertNull(discoverer.tryRecogniseSerial(BleAdvertisement("native-uuid", "AB00000001"), "AB00000001"))
        assertNull(discoverer.tryRecognise(BleAdvertisement("AA:BB:CC:DD:EE:FF", "Headphones")))
        assertNull(discoverer.tryRecogniseSerial(BleAdvertisement("AA:BB:CC:DD:EE:FF", null), "AB00000001"))
        assertNull(discoverer.tryRecogniseSerial(BleAdvertisement("AA:BB:CC:DD:EE:FF", "AB00000001"), ""))
    }
    @Test
    fun `a hexadecimal serial is not interpreted as a MAC and actual address remains reconnectable`() {
        val ad = BleAdvertisement("11:22:33:44:55:66", "ABCDEF123456")
        val device = discoverer.tryRecogniseSerial(ad, "ABCDEF123456")
        assertEquals(DeviceConnection.Ble(ad.peripheralId, ad.peripheralId), device?.connection)
        assertTrue(discoverer.matchesAddress(ad, "11:22:33:44:55:66"))
    }

}
