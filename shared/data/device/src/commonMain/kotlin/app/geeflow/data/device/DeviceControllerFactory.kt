package app.geeflow.data.device

import app.geeflow.data.device.model.SupportedDevice

fun interface DeviceControllerFactory {
    fun create(model: SupportedDevice): DeviceController
}
