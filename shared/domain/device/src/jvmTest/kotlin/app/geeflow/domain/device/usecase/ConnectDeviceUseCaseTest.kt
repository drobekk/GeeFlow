package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ConnectDeviceUseCaseTest {
    @Test
    fun `when connecting then delegates to the connection coordinator`() = runTest {
        val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
        val useCase = ConnectDeviceUseCase(provider)

        useCase(9)

        coVerify(exactly = 1) { provider.connect(9) }
    }

    @Test
    fun `when connection fails then propagates failure to the caller`() = runTest {
        val provider = mockk<DeviceControllerProvider>()
        coEvery { provider.connect(9) } throws IllegalStateException("disconnected")
        val useCase = ConnectDeviceUseCase(provider)

        val result = runCatching { useCase(9) }

        assertFailsWith<IllegalStateException> { result.getOrThrow() }
    }
}
