package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.model.SupportedDevice
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LitaCapabilitiesTest {
    @Test
    fun `when LITA model is selected then retains shared brewing and profile capabilities`() = runTest {
        val transport = mockk<WendougeeBleTransport>()
        val data = WendougeeController(
            model = SupportedDevice.WendougeeDataS,
            scope = backgroundScope,
            transport = transport,
        )

        val lita = WendougeeController(
            model = SupportedDevice.Lita,
            scope = backgroundScope,
            transport = transport,
        )

        assertEquals(data.capabilities, lita.capabilities)
        assertEquals(data.profilingCapabilities, lita.profilingCapabilities)
    }
}
