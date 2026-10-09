package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.user.UserRepository
import app.geeflow.data.user.model.User
import app.geeflow.domain.device.ProfileExecutionCoordinator
import app.geeflow.domain.device.ProfileRunState
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ObserveBrewDataUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxUnitFun = true)
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val coordinator = mockk<ProfileExecutionCoordinator>(relaxUnitFun = true)
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))
    private val getSelected = GetSelectedUserUseCase(users)
    private val point = BrewDataPoint(9f, 36f, 40f, 2f, 1f)

    init {
        every { users.selectedUser } returns selected
        every { provider.getController(9) } returns controller
        every { controller.deviceState } returns MutableStateFlow(
            DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected)
        )
        every { coordinator.state } returns MutableStateFlow(ProfileRunState())
    }

    @Test
    fun `when device is idle then emits empty session for selected user`() = runTest {
        val useCase = ObserveBrewDataUseCase(coordinator, provider, getSelected)

        val result = useCase(9).first()

        assertEquals(7L, result.userId)
        assertFalse(result.inProgress)
        assertTrue(result.dataPoints.isEmpty())
    }

    @Test
    fun `when manual brew is active then captures telemetry`() = runTest {
        every { controller.deviceState } returns MutableStateFlow(
            DeviceState(
                brewStatus = DeviceState.BrewStatus.Manual,
                pressure = 9f,
                volume = 40f,
                weight = 36f,
                flowRate = 2f,
                weightRate = 1f
            )
        )
        val useCase = ObserveBrewDataUseCase(coordinator, provider, getSelected)

        val result = useCase(9).first()

        assertTrue(result.inProgress)
        assertEquals(BrewMode.Manual, result.mode)
        assertEquals(point, result.dataPoints.values.single())
        assertNotNull(result.startTime)
    }
}
