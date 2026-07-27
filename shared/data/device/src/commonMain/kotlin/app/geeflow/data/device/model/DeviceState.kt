package app.geeflow.data.device.model

data class DeviceState(
    val steamBoilerTemp: Float? = null,
    val brewBoilerTemp: Float? = null,
    val pressure: Float? = null,
    val time: Int? = null,
    val volume: Float? = null,
    val flowRate: Float? = null,
    val weight: Float? = null,
    val weightRate: Float? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val brewStatus: BrewStatus = BrewStatus.Idle,
    val config: Config? = null,
    val smartScale: SmartScale? = null,
    val smartScaleEnabled: Boolean = false,
    val smartScaleSearchActive: Boolean = false,
    val waterLevelAlarm: Boolean = false,
) {
    data class Config(
        val targetSteamTemp: Float,
        val targetBrewTemp: Float,
        val steamBoilerEnabled: Boolean,
        val brewBoilerEnabled: Boolean,
        val manualBrewTimeSec: Float,
        val manualBrewPressure: Float,
        val cleaningTimeSec: Float,
        val cleaningStandbySec: Float,
        val cleaningCount: Int,
        val heatingMode: HeatingMode,
        val waterAlarmEnabled: Boolean,
    )

    enum class ConnectionStatus {
        Disconnected,
        Connecting,
        Synchronizing,
        Connected,
    }

    enum class BrewStatus {
        Manual,
        Profile,
        FreeVariable,
        Cleaning,
        Idle,
    }

    enum class HeatingMode {
        FullSpeed,
        Pulse,
    }

    enum class BoilerType {
        Steam,
        Brew,
    }
}
