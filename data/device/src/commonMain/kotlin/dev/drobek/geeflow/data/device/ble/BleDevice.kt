package dev.drobek.geeflow.data.device.ble

data class BleDevice(
    val peripheralId: String,
    val name: String?,
    val rssi: Int? = null,
)

data class CharacteristicData(
    val uuid: String,
    val value: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CharacteristicData) return false
        return uuid == other.uuid && value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + value.contentHashCode()
        return result
    }
}

sealed interface BleConnectionState {
    data object Disconnected : BleConnectionState
    data object Connecting : BleConnectionState
    data class Connected(val peripheralId: String) : BleConnectionState
}

class BleConnectException(message: String, cause: Throwable? = null) : Exception(message, cause)
