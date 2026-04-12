package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.DeviceControllerProvider
import dev.drobek.geeflow.data.device.DeviceRepository
import dev.drobek.geeflow.data.device.model.DeviceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

@Factory
class ConnectDeviceUseCase(
    private val provider: DeviceControllerProvider,
    private val deviceRepository: DeviceRepository,
    private val updateDeviceConnection: UpdateDeviceConnectionUseCase,
    private val scope: CoroutineScope,
) {
    operator fun invoke(deviceId: Long) {
        val device = deviceRepository.getDeviceById(deviceId) ?: return
        val controller = provider.getController(deviceId)

        // Observe resolved connection events (e.g. iOS peripheral UUID change) and persist them
        scope.launch {
            controller.resolvedConnection
                .takeWhile {
                    controller.deviceState.value.connectionStatus != DeviceState.ConnectionStatus.Disconnected
                }
                .collect { connection ->
                    if (connection != device.connection) {
                        updateDeviceConnection(deviceId, connection)
                    }
                }
        }

        controller.connect(device)
    }
}
