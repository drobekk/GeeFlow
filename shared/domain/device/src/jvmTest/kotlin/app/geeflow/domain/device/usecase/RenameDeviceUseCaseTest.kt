package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class RenameDeviceUseCaseTest {
    private val devices = mockk<DeviceRepository>(relaxUnitFun = true)

    @Test
    fun `when invoked then forwards arguments to dependency`() = runTest {
        val useCase = RenameDeviceUseCase(devices)

        useCase(9, "New name")

        coVerify(exactly = 1) { devices.renameDevice(9, "New name") }
    }
}
