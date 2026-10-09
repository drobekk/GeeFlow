package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceBrewingSettingsRepository
import app.geeflow.data.device.model.DeviceBrewingSettings
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveDeviceBrewingSettingsUseCaseTest {
    private val brewing = mockk<DeviceBrewingSettingsRepository>(relaxUnitFun = true)

    @Test
    fun `when dependency provides value then returns that value`() = runTest {
        val expected = flowOf(DeviceBrewingSettings())
        every { brewing.observe(9) } returns expected
        val useCase = ObserveDeviceBrewingSettingsUseCase(brewing)

        val result = useCase(9)

        assertEquals(expected, result)
    }
}
