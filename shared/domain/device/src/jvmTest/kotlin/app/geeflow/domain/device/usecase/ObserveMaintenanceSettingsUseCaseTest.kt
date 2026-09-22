package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceControllerProvider
import app.geeflow.data.device.MaintenanceSettingsRepository
import app.geeflow.data.device.impl.controller.DemoDeviceController
import app.geeflow.data.device.model.CleaningType
import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.MaintenanceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveMaintenanceSettingsUseCaseTest {
    @Test
    fun `initial deep cycles respect machine limits and reconnect does not overwrite programs`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val controller = DemoDeviceController(scope)
        val repository = MemoryRepository()
        val provider = object : DeviceControllerProvider {
            override val currentDeviceId = 1L
            override fun getController(deviceId: Long) = controller
            override fun disconnectCurrent() = Unit
        }
        try {
            controller.connect(Device(name = "Demo", connection = DeviceConnection.Ble("demo", "demo")))
            withTimeout(3000) {
                controller.deviceState.first { it.connectionStatus == DeviceState.ConnectionStatus.Connected }
            }
            controller.setCleaningSettings(5f, 6f, 8)
            val observe = ObserveMaintenanceSettingsUseCase(repository, provider)
            val first = observe(1).first()
            assertEquals(8, first.daily.cycles)
            assertEquals(10, first.deep.cycles)
            assertEquals(6, first.deep.restSeconds)
            controller.setCleaningSettings(1f, 1f, 1)
            assertEquals(first, observe(1).first())
        } finally {
            scope.cancel()
        }
    }

    private class MemoryRepository : MaintenanceSettingsRepository {
        private val saved = MutableStateFlow<MaintenanceSettings?>(null)
        override fun observe(deviceId: Long) = saved
        override suspend fun initialize(deviceId: Long, settings: MaintenanceSettings) {
            if (saved.value == null) saved.value = settings
        }
        override suspend fun save(deviceId: Long, settings: MaintenanceSettings, today: Long) = Unit
        override suspend fun postpone(deviceId: Long, types: Set<CleaningType>, today: Long) = Unit
        override suspend fun remove(deviceId: Long) = Unit
    }
}
