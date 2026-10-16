package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.SupportedDevice
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SetDeviceModelUseCaseTest {
    private val repository = mockk<DeviceRepository>(relaxed = true)
    private val provider = mockk<DeviceControllerProvider>(relaxed = true)
    private val useCase = SetDeviceModelUseCase(repository, provider)

    @Test
    fun `when selecting unknown device model then disconnects before persisting selection`() = runTest {
        every {
            repository.getDeviceById(9)
        } returns Device(name = "Machine", connection = DeviceConnection.Ble("id", "mac"))

        useCase(9, SupportedDevice.LitaBA.id)

        coVerifyOrder {
            provider.forgetController(9)
            repository.updateModel(9, SupportedDevice.LitaBA)
        }
    }

    @Test
    fun `when selecting demo for physical device then rejects without changing model`() = runTest {
        val selected = SupportedDevice.GeeFlowDemo.id

        val result = runCatching { useCase(9, selected) }

        assertTrue(result.isFailure)
        verify(exactly = 0) { repository.updateModel(any(), any()) }
    }
}
