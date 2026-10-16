package app.geeflow.data.device.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SupportedDeviceTest {
    @Test
    fun `when LITA LA is named explicitly then recognizes the hot water model`() {
        val model = SupportedDevice.fromDeviceName("Wendougee_LITA_LA_001122334455")

        assertEquals(SupportedDevice.LitaLA, model)
    }

    @Test
    fun `when listing models then offers only supported concrete variants`() {
        val models = SupportedDevice.all().map { it.model }

        assertEquals(listOf("Data-S", "LITA BA", "LITA LA", "Demo"), models)
    }

    @Test
    fun `when BT name has no exact model then requires manual selection`() {
        val names = listOf("Wendougee_001122334455", "Unknown")

        val models = names.map(SupportedDevice::fromDeviceName)

        models.forEach { assertNull(it) }
    }

    @Test
    fun `when name identifies a model then recognizes it independently of firmware`() {
        val names = listOf("Wendougee Data-S V200", "LITA_BA_001122334455", "LITA_001122334455")

        val models = names.map(SupportedDevice::fromDeviceName)

        assertEquals(listOf(SupportedDevice.WendougeeDataS, SupportedDevice.LitaBA, null), models)
    }

    @Test
    fun `when legacy device has known model and newer firmware then retains that model`() {
        val device = Device(
            name = "Coffee",
            connection = DeviceConnection.Ble("id", "mac"),
            manufacturer = "Wendougee",
            model = "Data-S",
            version = "V999"
        )

        val model = device.supportedDevice

        assertEquals(SupportedDevice.WendougeeDataS, model)
    }

    @Test
    fun `when persisted model id is unknown then does not fall back to legacy model`() {
        val device = Device(
            name = "Coffee",
            connection = DeviceConnection.Ble("id", "mac"),
            manufacturer = "Wendougee",
            model = "Data-S",
            modelId = "unknown"
        )

        val model = device.supportedDevice

        assertNull(model)
    }

    @Test
    fun `when commercial LITA is selected then does not infer native category`() {
        val models =
            listOf("LITA BA", "LITA LA").map {
                SupportedDevice.find("Wendougee", it, "")
            }

        val categories = models.map { it?.categoryCode }

        categories.forEach { assertNull(it) }
    }
}
