package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.Config
import app.geeflow.data.device.model.DeviceState.HeatingMode
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetCleaningStatusUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxUnitFun = true)
    private val state = MutableStateFlow(DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected))
    private val config = Config(130f, 93f, true, true, 30f, 9f, 5f, 3f, 3, HeatingMode.FullSpeed, true)

    init {
        every { provider.getController(9) } returns controller
        every { controller.deviceState } returns state
    }

    @Test
    fun `when cleaning is idle then calculates progress`() = runTest {
        state.value = state.value.copy(
            config = config,
            brewStatus = DeviceState.BrewStatus.Idle,
            time = 0,
        )
        val useCase = GetCleaningStatusUseCase(provider)

        val result = useCase(9).first()

        assertEquals(false, result.inProgress)
        assertEquals(0, result.flush.current)
        assertEquals(0, result.rest.current)
        assertEquals(0, result.cycle.current)
    }

    @Test
    fun `when cleaning is flushing then calculates progress`() = runTest {
        state.value = state.value.copy(
            config = config,
            brewStatus = DeviceState.BrewStatus.Cleaning,
            time = 3,
        )
        val useCase = GetCleaningStatusUseCase(provider)

        val result = useCase(9).first()

        assertEquals(true, result.inProgress)
        assertEquals(3, result.flush.current)
        assertEquals(0, result.rest.current)
        assertEquals(1, result.cycle.current)
    }

    @Test
    fun `when cleaning is resting then calculates progress`() = runTest {
        state.value = state.value.copy(
            config = config,
            brewStatus = DeviceState.BrewStatus.Cleaning,
            time = 7,
        )
        val useCase = GetCleaningStatusUseCase(provider)

        val result = useCase(9).first()

        assertEquals(true, result.inProgress)
        assertEquals(5, result.flush.current)
        assertEquals(2, result.rest.current)
        assertEquals(1, result.cycle.current)
    }

    @Test
    fun `when cleaning enters next cycle then calculates progress`() = runTest {
        state.value = state.value.copy(
            config = config,
            brewStatus = DeviceState.BrewStatus.Cleaning,
            time = 9,
        )
        val useCase = GetCleaningStatusUseCase(provider)

        val result = useCase(9).first()

        assertEquals(true, result.inProgress)
        assertEquals(1, result.flush.current)
        assertEquals(0, result.rest.current)
        assertEquals(2, result.cycle.current)
    }
}
