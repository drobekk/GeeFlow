package app.geeflow.domain.device.usecase

import app.geeflow.data.device.MaintenanceSettingsRepository
import app.geeflow.data.device.model.CleaningType
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SkipMaintenanceReminderUseCaseTest {
    private val maintenance = mockk<MaintenanceSettingsRepository>(relaxUnitFun = true)

    @Test
    fun `when invoked then forwards arguments to dependency`() = runTest {
        val useCase = SkipMaintenanceReminderUseCase(maintenance)

        useCase(9, setOf(CleaningType.Daily))

        coVerify(exactly = 1) { maintenance.postpone(9, setOf(CleaningType.Daily), any()) }
    }
}
