package app.geeflow.domain.brew.usecase

import app.geeflow.data.brew.model.BrewDataPoint
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.BrewSession
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveBrewDataUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxed = true)
    private val users = mockk<UserRepository>(relaxUnitFun = true)
    private val coordinator = mockk<ProfileExecutionCoordinator>(relaxUnitFun = true)
    private val selected = MutableStateFlow<User?>(User(id = 7, name = "Ada"))
    private val getSelected = GetSelectedUserUseCase(users)
    private val point = BrewDataPoint(9f, 36f, 40f, 2f, 1f)

    init {
        every { users.selectedUser } returns selected
        every { provider.getController(9) } returns controller
        every { provider.observeController(9) } returns flowOf(controller)
        every { controller.deviceState } returns MutableStateFlow(
            DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected)
        )
        every { coordinator.state } returns MutableStateFlow(ProfileRunState())
    }

    @Test
    fun `when telemetry has a two second gap then records only received samples`() = runTest {
        val started = Instant.parse("2026-10-12T12:00:00Z")
        val states = MutableStateFlow(
            DeviceState(
                brewStatus = DeviceState.BrewStatus.Manual,
                telemetryTime = started,
                pressure = 2f,
            ),
        )
        every { controller.deviceState } returns states
        val useCase = ObserveBrewDataUseCase(coordinator, provider, getSelected)
        val sessions = mutableListOf<BrewSession>()

        backgroundScope.launch { useCase(9).collect { sessions.add(it) } }
        runCurrent()
        states.value = states.value.copy(telemetryTime = started + 2.seconds, pressure = 8f)
        runCurrent()
        val result = sessions.last()

        assertEquals(listOf(0f, 2f), result.dataPoints.keys.toList())
        assertEquals(listOf(2f, 8f), result.dataPoints.values.map { it.pressure })
    }

    @Test
    fun `when settings change without a new measurement then does not add a sample`() = runTest {
        val started = Instant.parse("2026-10-12T12:00:00Z")
        val states = MutableStateFlow(
            DeviceState(
                brewStatus = DeviceState.BrewStatus.Manual,
                telemetryTime = started,
                pressure = 2f,
            ),
        )
        every { controller.deviceState } returns states
        val useCase = ObserveBrewDataUseCase(coordinator, provider, getSelected)
        val sessions = mutableListOf<BrewSession>()

        backgroundScope.launch { useCase(9).collect { sessions.add(it) } }
        runCurrent()
        states.value = states.value.copy(brewBoilerTemp = 93f)
        runCurrent()
        val result = sessions.last()

        assertEquals(1, result.dataPoints.size)
    }

    @Test
    fun `when new telemetry repeats a value then preserves both real samples`() = runTest {
        val started = Instant.parse("2026-10-12T12:00:00Z")
        val states = MutableStateFlow(
            DeviceState(
                brewStatus = DeviceState.BrewStatus.Manual,
                telemetryTime = started,
                pressure = 2f,
            ),
        )
        every { controller.deviceState } returns states
        val useCase = ObserveBrewDataUseCase(coordinator, provider, getSelected)
        val sessions = mutableListOf<BrewSession>()

        backgroundScope.launch { useCase(9).collect { sessions.add(it) } }
        runCurrent()
        states.value = states.value.copy(telemetryTime = started + 1.seconds)
        runCurrent()
        val result = sessions.last()

        assertEquals(listOf(0f, 1f), result.dataPoints.keys.toList())
        assertEquals(listOf(2f, 2f), result.dataPoints.values.map { it.pressure })
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
