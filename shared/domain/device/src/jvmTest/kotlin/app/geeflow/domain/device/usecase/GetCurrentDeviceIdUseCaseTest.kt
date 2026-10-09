package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetCurrentDeviceIdUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)

    init {
        every { provider.currentDeviceId } returns MutableStateFlow(9L)
    }

    @Test
    fun `when dependency provides value then returns that value`() = runTest {
        val useCase = GetCurrentDeviceIdUseCase(provider)

        val result = useCase()

        assertEquals(9L, result)
    }
}
