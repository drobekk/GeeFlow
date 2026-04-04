package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.api.DeviceController
import dev.drobek.geeflow.domain.device.model.DeviceState
import dev.drobek.geeflow.domain.exception.DeviceNotConnectedException

fun DeviceController.requireConnected(deviceId: String) {
    if (deviceState.value.connectionStatus != DeviceState.ConnectionStatus.Connected) {
        throw DeviceNotConnectedException(deviceId)
    }
}
