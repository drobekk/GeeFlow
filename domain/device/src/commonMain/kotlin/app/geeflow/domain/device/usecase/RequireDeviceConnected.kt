package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceController
import app.geeflow.data.device.model.DeviceState
import app.geeflow.domain.exception.DeviceNotConnectedException

fun DeviceController.requireConnected(deviceId: Long) {
    if (deviceState.value.connectionStatus != DeviceState.ConnectionStatus.Connected) {
        throw DeviceNotConnectedException(deviceId)
    }
}
