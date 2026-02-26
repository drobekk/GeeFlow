package dev.drobek.geeflow.presentation.feature.device.dashboard

data class DeviceDashboardViewState(
    val user: User = User(),
    val device: Device = Device(),
    val brewProfiles: List<Profile> = emptyList(),
    val dialog: Dialog? = null
) {
    data class User(
        val id: String = "",
        val name: String = "",
    )

    data class Device(
        val id: String = "",
        val name: String = "",
        val brewBoilerTemp: String? = null,
        val steamBoilerTemp: String? = null,
        val pressure: String? = null,
        val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected
    ) {
        enum class ConnectionStatus {
            Disconnected, Connecting, Connected
        }
    }

    data class Profile(
        val name: String,
        val description: String,
        val brewByWeight: Boolean
    )

    sealed interface Dialog {
        object BluetoothPermissionMissing : Dialog
    }
}
