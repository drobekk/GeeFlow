package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AddDeviceUseCaseTest {
    private val devices = mockk<DeviceRepository>(relaxUnitFun = true)
    private val device =
        Device(id = 9, name = "Machine", connection = DeviceConnection.Ble("peripheral", "AA:BB:CC:DD:EE:FF"))

    @Test
    fun `when BLE address is already stored then returns existing id without adding`() = runTest {
        every { devices.getDeviceByBleMacAddress(any()) } returns device.copy(id = 10)
        val useCase = AddDeviceUseCase(devices)

        val result = useCase(device)

        assertEquals(10L, result)
        verify(exactly = 0) { devices.addDevice(any()) }
    }

    @Test
    fun `when BLE address is new then adds device and returns id`() = runTest {
        every { devices.getDeviceByBleMacAddress(any()) } returns null
        every { devices.addDevice(device) } returns 11
        val useCase = AddDeviceUseCase(devices)

        val result = useCase(device)

        assertEquals(11L, result)
        verify(exactly = 1) { devices.addDevice(device) }
    }

    @Test
    fun `manual entry retains serial label and reuses existing address deduplication`() = runTest {
        val manual = device.copy(
            name = "LITA BA · AB00000001",
            model = "LITA BA",
            manufacturer = "Wendougee",
            modelSource = "manual-entry"
        )
        every { devices.getDeviceByBleMacAddress(any()) } returns null
        every { devices.addDevice(manual) } returns 12L
        assertEquals(12L, AddDeviceUseCase(devices)(manual))
        verify(exactly = 1) { devices.addDevice(manual) }
    }
}
