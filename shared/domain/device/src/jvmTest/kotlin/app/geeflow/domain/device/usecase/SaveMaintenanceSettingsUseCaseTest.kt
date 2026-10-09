package app.geeflow.domain.device.usecase

import app.geeflow.data.device.MaintenanceSettingsRepository
import app.geeflow.data.device.model.CleaningProgram
import app.geeflow.data.device.model.MaintenanceSettings
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SaveMaintenanceSettingsUseCaseTest {
    private val maintenance = mockk<MaintenanceSettingsRepository>(relaxUnitFun = true)

    @Test
    fun `when invoked then forwards arguments to dependency`() = runTest {
        val useCase = SaveMaintenanceSettingsUseCase(maintenance)
        val settings = MaintenanceSettings(daily = CleaningProgram(5, 3, 3), deep = CleaningProgram(5, 3, 6))

        useCase(9, settings)

        coVerify(exactly = 1) { maintenance.save(9, settings, any()) }
    }
}
