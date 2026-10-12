package app.geeflow.data.device

import app.geeflow.data.device.model.Device
import app.geeflow.data.device.model.DeviceConnection

interface DeviceConnectionControl {
    suspend fun connect(device: Device): DeviceConnection
    suspend fun disconnect()
}
