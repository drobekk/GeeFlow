package app.geeflow.presentation.feature.device.settings.connectivity

data class ConnectivitySettingsViewState(
    val scale: AccessoryState = AccessoryState(),
) {
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
