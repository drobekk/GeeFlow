package dev.drobek.geeflow.data.device.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface DeviceConnection {

    @Serializable
    @SerialName("ble")
    data class Ble(
        val peripheralId: String,
        val macAddress: String,
    ) : DeviceConnection
}
