package app.geeflow.presentation.feature.device.settings.connectivity

data class ConnectivitySettingsViewState(
    val smartScaleEnabled: Boolean = false,
    val isSearching: Boolean = false,
    val scales: List<ScaleViewItem> = emptyList(),
) {
    data class ScaleViewItem(
        val name: String,
        val connectionStatus: ScaleConnectionStatus,
    )

    enum class ScaleConnectionStatus {
        Connected,
        Connecting,
        Disconnected,
    }

    val nothingConnected = scales.none { it.connectionStatus == ScaleConnectionStatus.Connected }
}
