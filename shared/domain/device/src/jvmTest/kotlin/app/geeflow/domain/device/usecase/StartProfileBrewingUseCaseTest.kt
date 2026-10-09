package app.geeflow.domain.device.usecase

import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.Condition
import app.geeflow.data.brew.model.ProfileStep
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.domain.exception.ScaleNotConnectedException
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class StartProfileBrewingUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxUnitFun = true)
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val coordinator = mockk<ProfileExecutionCoordinator>(relaxUnitFun = true)
    private val state = MutableStateFlow(DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected))
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))

    init {
        every { provider.getController(9) } returns controller
        every { controller.deviceState } returns state
        every { users.selectedUser } returns selected
    }

    @Test
    fun `when profile starts then coordinator receives selected user id`() = runTest {
        val profile = BrewProfile(
            userId = 1,
            name = "Shot",
            description = "",
            finishCondition = null,
            steps = listOf(ProfileStep.Pressure(30, 9f))
        )
        val useCase = StartProfileBrewingUseCase(provider, users, coordinator)

        useCase(9, profile)

        coVerify(exactly = 1) { coordinator.start(9, profile.copy(userId = 7)) }
    }

    @Test
    fun `when weight profile has no connected scale then rejects brew`() = runTest {
        val profile = BrewProfile(
            userId = 7,
            name = "Shot",
            description = "",
            finishCondition = Condition.Weight(36f),
            steps = listOf(ProfileStep.Pressure(30, 9f))
        )
        val useCase = StartProfileBrewingUseCase(provider, users, coordinator)

        val result = runCatching { useCase(9, profile) }

        assertTrue(result.exceptionOrNull() is ScaleNotConnectedException)
        coVerify(exactly = 0) { coordinator.start(any(), any()) }
    }
}
