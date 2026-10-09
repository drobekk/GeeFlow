package app.geeflow.domain.device.usecase

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceConstraints
import app.geeflow.data.device.model.ProfileExecution
import app.geeflow.data.device.model.ProfileIssue
import app.geeflow.data.device.model.ProfileIssueCode
import app.geeflow.data.device.model.ProfilingCapabilities
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GetProfileSupportUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxUnitFun = true)
    private val constraints = DeviceConstraints(80..100, 120..140, 1..10, 1..60, 1..30, 1..30, 1..10, 0f..12f, 0f..10f)

    init {
        every { provider.getController(9) } returns controller
        every { controller.constraints } returns constraints
        every { controller.assessNativeProfile(any()) } returns listOf(ProfileIssue(ProfileIssueCode.NativeFeature))
    }

    @Test
    fun `when controller supports no profiling then reports unsupported profile`() = runTest {
        every { controller.profilingCapabilities } returns ProfilingCapabilities()
        val profile = BrewProfile(
            userId = 7,
            name = "Shot",
            description = "",
            finishCondition = null,
            steps = listOf(ProfileStep.Pressure(30, 9f))
        )
        val useCase = GetProfileSupportUseCase(provider)

        val result = useCase(9, profile)

        assertEquals(ProfileExecution.Unsupported, result.execution)
        assertFalse(result.bindingAllowed)
    }
}
