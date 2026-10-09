package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class UpdateDeviceConnectionUseCaseTest {
    private val devices = mockk<DeviceRepository>(relaxUnitFun = true)
    private val device =
        Device(id = 9, name = "Machine", connection = DeviceConnection.Ble("peripheral", "AA:BB:CC:DD:EE:FF"))

    @Test
    fun `when invoked then forwards arguments to dependency`() = runTest {
        val useCase = UpdateDeviceConnectionUseCase(devices)

        useCase(9, device.connection)

        coVerify(exactly = 1) { devices.updateConnection(9, device.connection) }
    }
}
