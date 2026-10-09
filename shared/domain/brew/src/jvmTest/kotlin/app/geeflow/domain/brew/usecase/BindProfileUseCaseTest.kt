package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.BrewProfileRepository
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.ProfileIssue
import app.geeflow.data.device.model.ProfileIssueCode
import app.geeflow.data.device.model.ProfilingCapabilities
import app.geeflow.domain.exception.ProfileBindingNotAllowedException
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BindProfileUseCaseTest {
    private val profiles = mockk<BrewProfileRepository>(relaxUnitFun = true)
    private val devices = mockk<DeviceRepository>(relaxUnitFun = true)
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxUnitFun = true)
    private val profile = BrewProfile(
        id = 3,
        userId = 7,
        name = "Shot",
        description = "Recipe",
        finishCondition = null,
        steps = listOf(ProfileStep.Pressure(30, 9f)),
        autoLinkOpen = true,
        position = 4
    )

    init {
        every { provider.getController(9) } returns controller
        every { controller.deviceState } returns MutableStateFlow(
            DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected)
        )
        every { controller.assessNativeProfile(any()) } returns listOf(ProfileIssue(ProfileIssueCode.NativeFeature))
    }

    @Test
    fun `when binding succeeds then persists binding after hardware write`() = runTest {
        every { controller.assessNativeProfile(profile) } returns emptyList()
        every { controller.profilingCapabilities } returns ProfilingCapabilities(binding = true)
        val useCase = BindProfileUseCase(profiles, devices, provider)

        useCase(9, profile)

        coVerifyOrder {
            controller.bindProfile(profile)
            devices.bindProfile(9, 3)
        }
    }

    @Test
    fun `when hardware binding fails then does not persist binding`() = runTest {
        every { controller.assessNativeProfile(profile) } returns emptyList()
        every { controller.profilingCapabilities } returns ProfilingCapabilities(binding = true)
        val failure = IllegalStateException("Hardware write failed")
        coEvery { controller.bindProfile(profile) } throws failure
        val useCase = BindProfileUseCase(profiles, devices, provider)

        val result = runCatching { useCase(9, profile) }

        assertSame(failure, result.exceptionOrNull())
        verify(exactly = 0) { devices.bindProfile(any(), any()) }
    }

    @Test
    fun `when stored profile is missing then fails before accessing controller`() = runTest {
        every { profiles.getBrewProfileById(3) } returns null
        val useCase = BindProfileUseCase(profiles, devices, provider)

        val result = runCatching { useCase(9, 3) }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
        verify { provider wasNot Called }
    }

    @Test
    fun `when controller has no binding support then rejects before hardware write`() = runTest {
        every { controller.profilingCapabilities } returns ProfilingCapabilities()
        val useCase = BindProfileUseCase(profiles, devices, provider)

        val result = runCatching { useCase(9, profile) }

        assertTrue(result.exceptionOrNull() is ProfileBindingNotAllowedException)
        coVerify(exactly = 0) { controller.bindProfile(any()) }
        verify(exactly = 0) { devices.bindProfile(any(), any()) }
    }
}
