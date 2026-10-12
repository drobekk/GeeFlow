package app.geeflow.data.device

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DeviceControllerProvider {
    val currentDeviceId: StateFlow<Long?>
    fun getController(deviceId: Long): DeviceController
    fun observeController(deviceId: Long): Flow<DeviceController>
    suspend fun connect(deviceId: Long): Unit = error("Connection is not implemented by this provider")
    suspend fun forgetController(deviceId: Long) {
        getController(deviceId).connection.disconnect()
    }
    suspend fun disconnectCurrent()
}
