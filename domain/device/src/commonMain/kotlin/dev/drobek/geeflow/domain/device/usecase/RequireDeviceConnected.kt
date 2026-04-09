package dev.drobek.geeflow.domain.device.usecase

import dev.drobek.geeflow.data.device.DeviceController
import dev.drobek.geeflow.data.device.model.DeviceState
import dev.drobek.geeflow.domain.exception.DeviceNotConnectedException

fun DeviceController.requireConnected(deviceId: Long) {
    if (deviceState.value.connectionStatus != DeviceState.ConnectionStatus.Connected) {
        throw DeviceNotConnectedException(deviceId)
    }
}
