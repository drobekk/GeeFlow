package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.model.DeviceState
import app.geeflow.domain.exception.DeviceNotConnectedException
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SetWaterAlarmUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxUnitFun = true)
    private val state = MutableStateFlow(DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected))

    init {
        every { provider.getController(9) } returns controller
        every { controller.deviceState } returns state
    }

    @Test
    fun `when device is connected then sends requested command`() = runTest {
        val useCase = SetWaterAlarmUseCase(provider)

        useCase(9, true)

        coVerifyOrder {
            controller.setWaterAlarm(true)
        }
    }

    @Test
    fun `when device is disconnected then rejects command`() = runTest {
        val useCase = SetWaterAlarmUseCase(provider)
        state.value = state.value.copy(connectionStatus = DeviceState.ConnectionStatus.Disconnected)

        val result = runCatching { useCase(9, true) }

        assertTrue(result.exceptionOrNull() is DeviceNotConnectedException)
        coVerify(exactly = 0) { controller.setWaterAlarm(true) }
    }
}
