package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.ProfilingCapabilities
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetProfilingCapabilitiesUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxUnitFun = true)

    init {
        every { provider.getController(9) } returns controller
    }

    @Test
    fun `when dependency provides value then returns that value`() = runTest {
        val expected = ProfilingCapabilities()
        every { controller.profilingCapabilities } returns expected
        val useCase = GetProfilingCapabilitiesUseCase(provider)

        val result = useCase(9)

        assertEquals(expected, result)
    }
}
