package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.DeviceRepository
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.DeviceState
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectDeviceUseCaseTest {
    private val provider = mockk<DeviceControllerProvider>(relaxUnitFun = true)
    private val controller = mockk<DeviceController>(relaxUnitFun = true)
    private val devices = mockk<DeviceRepository>(relaxUnitFun = true)
    private val state = MutableStateFlow(DeviceState(connectionStatus = DeviceState.ConnectionStatus.Connected))
    private val device =
        Device(id = 9, name = "Machine", connection = DeviceConnection.Ble("peripheral", "AA:BB:CC:DD:EE:FF"))

    init {
        every { provider.getController(9) } returns controller
        every { controller.deviceState } returns state
    }

    @Test
    fun `when device exists then connects and persists resolved connection`() = runTest {
        val resolved = MutableSharedFlow<DeviceConnection>()
        every { devices.getDeviceById(9) } returns device
        every { controller.resolvedConnection } returns resolved
        val useCase =
            ConnectDeviceUseCase(
                provider,
                devices,
                UpdateDeviceConnectionUseCase(devices),
                backgroundScope,
            )
        val connection = DeviceConnection.Ble("new-peripheral", "AA:BB:CC:DD:EE:FF")

        useCase(9)
        runCurrent()
        resolved.emit(connection)
        runCurrent()

        verify(exactly = 1) { controller.connect(device) }
        verify(exactly = 1) { devices.updateConnection(9, connection) }
    }

    @Test
    fun `when device does not exist then does not connect`() = runTest {
        every { devices.getDeviceById(9) } returns null
        val useCase =
            ConnectDeviceUseCase(
                provider,
                devices,
                UpdateDeviceConnectionUseCase(devices),
                backgroundScope,
            )

        useCase(9)

        verify { provider wasNot Called }
    }
}
