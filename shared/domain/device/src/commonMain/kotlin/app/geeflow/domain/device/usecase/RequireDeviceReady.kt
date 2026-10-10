package app.geeflow.domain.device.usecase

import app.geeflow.data.device.DeviceController
import app.geeflow.domain.exception.DeviceErrorException

/** Guards operations that start or control machine work; stopping remains available. */
fun DeviceController.requireReady(deviceId: Long) {
    requireConnected(deviceId)
    val state = deviceState.value
    if (state.hasError) throw DeviceErrorException(deviceId, state.error?.code)
}
