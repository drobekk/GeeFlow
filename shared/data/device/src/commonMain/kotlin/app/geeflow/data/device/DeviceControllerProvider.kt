package app.geeflow.data.device

import kotlinx.coroutines.flow.StateFlow

interface DeviceControllerProvider {
    val currentDeviceId: StateFlow<Long?>
    fun getController(deviceId: Long): DeviceController
    fun disconnectCurrent()
}
