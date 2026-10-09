package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveDevicesUseCaseTest {
    private val devices = mockk<DeviceRepository>(relaxUnitFun = true)
    private val device =
        Device(id = 9, name = "Machine", connection = DeviceConnection.Ble("peripheral", "AA:BB:CC:DD:EE:FF"))

    @Test
    fun `when dependency provides value then returns that value`() = runTest {
        val expected = MutableStateFlow(listOf(device))
        every { devices.devices } returns expected
        val useCase = ObserveDevicesUseCase(devices)

        val result = useCase()

        assertEquals(expected, result)
    }
}
