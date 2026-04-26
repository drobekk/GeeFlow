package app.geeflow.data.device

interface DeviceControllerProvider {
    val currentDeviceId: Long?
    fun getController(deviceId: Long): DeviceController
    fun disconnectCurrent()
}
