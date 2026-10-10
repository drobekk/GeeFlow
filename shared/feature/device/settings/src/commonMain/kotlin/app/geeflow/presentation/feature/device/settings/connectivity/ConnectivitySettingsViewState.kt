package app.geeflow.presentation.feature.device.settings.connectivity

data class ConnectivitySettingsViewState(
    val selectedType: ConnectivityAccessoryType = ConnectivityAccessoryType.SmartScale,
    val scale: AccessoryState = AccessoryState(),
    val grinder: AccessoryState = AccessoryState(),
) {
    val selectedAccessory: AccessoryState
        get() = if (selectedType == ConnectivityAccessoryType.SmartScale) scale else grinder

    data class AccessoryState(
        val enabled: Boolean = false,
        val isSearching: Boolean = false,
        val devices: List<AccessoryViewItem> = emptyList(),
    )

    data class AccessoryViewItem(
        val name: String,
        val connectionStatus: AccessoryConnectionStatus,
    )

    enum class AccessoryConnectionStatus {
        Connected,
        Connecting,
        Disconnected,
    }
}

enum class ConnectivityAccessoryType {
    SmartScale,
    SingleDose,
}
