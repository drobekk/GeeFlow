package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveDeviceUseCaseTest {
    private val devices = mockk<DeviceRepository>(relaxUnitFun = true)
    private val device =
        Device(id = 9, name = "Machine", connection = DeviceConnection.Ble("peripheral", "AA:BB:CC:DD:EE:FF"))

    @Test
    fun `when device is present then emits requested device`() = runTest {
        every { devices.devices } returns MutableStateFlow(listOf(device))
        val useCase = ObserveDeviceUseCase(devices)

        val result = useCase(9).first()

        assertEquals(device, result)
    }

    @Test
    fun `when device is absent then emits null`() = runTest {
        every { devices.devices } returns MutableStateFlow(emptyList())
        val useCase = ObserveDeviceUseCase(devices)

        val result = useCase(9).first()

        assertNull(result)
    }
}
