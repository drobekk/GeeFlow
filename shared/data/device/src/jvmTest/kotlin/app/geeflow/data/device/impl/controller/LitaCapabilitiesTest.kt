package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.model.DeviceCapability
import app.geeflow.data.device.model.SupportedDevice
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LitaCapabilitiesTest {
    @Test
    fun `when LA is selected then enables hot water without changing shared brewing capabilities`() = runTest {
        val transport = mockk<WendougeeBleTransport>()
        val ba = WendougeeController(SupportedDevice.LitaBA, backgroundScope, transport)
        val la = WendougeeController(SupportedDevice.LitaLA, backgroundScope, transport)

        val added = la.capabilities - ba.capabilities

        assertEquals(setOf(DeviceCapability.HotWaterDispensing), added)
        assertEquals(ba.profilingCapabilities, la.profilingCapabilities)
    }

    @Test
    fun `when LITA model is selected then retains shared brewing and profile capabilities`() = runTest {
        val transport = mockk<WendougeeBleTransport>()
        val data = WendougeeController(
            model = SupportedDevice.WendougeeDataS,
            scope = backgroundScope,
            transport = transport,
        )

        val lita = WendougeeController(
            model = SupportedDevice.LitaBA,
            scope = backgroundScope,
            transport = transport,
        )

        assertEquals(data.capabilities, lita.capabilities)
        assertEquals(data.profilingCapabilities, lita.profilingCapabilities)
    }
}
