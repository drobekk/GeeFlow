package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceBrewingSettingsRepository
import app.geeflow.data.device.model.DeviceBrewingSettings
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SaveDeviceBrewingSettingsUseCaseTest {
    private val brewing = mockk<DeviceBrewingSettingsRepository>(relaxUnitFun = true)

    @Test
    fun `when invoked then forwards arguments to dependency`() = runTest {
        val useCase = SaveDeviceBrewingSettingsUseCase(brewing)
        val settings = DeviceBrewingSettings()

        useCase(9, settings)

        coVerify(exactly = 1) { brewing.save(9, settings) }
    }
}
