package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DisconnectCurrentDeviceUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)

    @Test
    fun `when invoked then forwards arguments to dependency`() = runTest {
        val useCase = DisconnectCurrentDeviceUseCase(provider)

        useCase()

        coVerify(exactly = 1) { provider.disconnectCurrent() }
    }
}
