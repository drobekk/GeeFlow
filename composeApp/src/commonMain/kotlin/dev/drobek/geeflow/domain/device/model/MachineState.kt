package dev.drobek.geeflow.domain.device.model

data class MachineState(
    val steamBoilerTemp: Float = 0.0f,
    val brewBoilerTemp: Float = 0.0f,
    val pressure: Float = 0.0f,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected
) {
    enum class ConnectionStatus {
        Disconnected,
        Connecting,
        Connected
    }
}
