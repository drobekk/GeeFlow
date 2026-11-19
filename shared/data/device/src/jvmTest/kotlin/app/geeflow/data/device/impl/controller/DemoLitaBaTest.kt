package app.geeflow.data.device.impl.controller

import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceCapability
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.SupportedDevice
import app.geeflow.data.device.model.isDemo
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DemoLitaBaTest {
    @Test
    fun `demo mirrors BA capabilities profiling and limits`() = runTest {
        val demo = DemoDeviceController(backgroundScope)
        val ba = WendougeeController(SupportedDevice.LitaBA, backgroundScope, mockk())
        assertEquals(ba.capabilities, demo.capabilities)
        assertEquals(ba.constraints, demo.constraints)
        assertEquals(ba.profilingCapabilities, demo.profilingCapabilities)
        assertTrue(DeviceCapability.PressureProfiling in demo.capabilities)
        assertTrue(DeviceCapability.FlowProfiling in demo.capabilities)
        assertFalse(DeviceCapability.HotWaterDispensing in demo.capabilities)
        assertFailsWith<UnsupportedOperationException> { demo.hotWater.start() }
    }

    @Test
    fun `BA demonstration retains virtual identity and simulated connection`() = runTest {
        val controller = DemoDeviceController(backgroundScope)
        val device = Device(
            name = "LITA BA · Demo",
            manufacturer = SupportedDevice.GeeFlowDemo.manufacturer,
            model = SupportedDevice.GeeFlowDemo.model,
            version = SupportedDevice.GeeFlowDemo.version,
            connection = DeviceConnection.Ble("DE:MO:00:00:00:00", "DE:MO:00:00:00:00"),
        )
        assertTrue(device.isDemo)
        controller.connection.connect(device)
        assertEquals(
            app.geeflow.data.device.model.DeviceState.ConnectionStatus.Connected,
            controller.deviceState.value.connectionStatus,
        )
        controller.connection.disconnect()
    }
}
