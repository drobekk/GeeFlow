package dev.drobek.geeflow.presentation.feature.device.dashboard

data class DeviceDashboardViewState(
    val user: User = User(),
    val device: Device = Device(),
    val brew: Brew = Brew(),
    val brewProfiles: List<Profile> = emptyList(),
    val dialog: Dialog? = null,
    val visibleCharts: Set<DashboardChartType> = setOf(
        DashboardChartType.Pressure,
        DashboardChartType.FlowRate,
        DashboardChartType.WeightRate
    )
) {
    enum class DashboardChartType {
        Pressure,
        FlowRate,
        WeightRate,
        Volume,
        Weight
    }

    data class User(
        val id: String = "",
        val name: String = "",
    )

    data class Brew(
        val name: String = "",
        val time: Float = 0f,
        val data: Map<Float, Data> = getEmptyChartData()
    ) {
        data class Data(
            val pressure: Float,
            val weight: Float,
            val weightPerSecond: Float,
            val volume: Float,
            val volumePerSecond: Float
        )
    }

    data class Device(
        val id: String = "",
        val name: String = "",
        val brewBoilerTemp: String? = null,
        val steamBoilerTemp: String? = null,
        val pressure: String? = null,
        val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
        val brewStatus: BrewStatus = BrewStatus.Idle
    ) {
        val isBrewing = brewStatus != BrewStatus.Idle

        enum class ConnectionStatus {
            Disconnected, Connecting, Connected
        }

        enum class BrewStatus {
            Manual, Profile, Idle
        }
    }

    data class Profile(
        val id: String,
        val number: String,
        val name: String,
        val description: String,
        val brewByWeight: Boolean,
        val bound: Boolean = false,
        val selected: Boolean = false
    )

    sealed interface Dialog {
        object BluetoothPermissionMissing : Dialog
    }
}

internal fun getEmptyChartData() = mapOf(
    0f to DeviceDashboardViewState.Brew.Data(
        pressure = 0.0f,
        weight = 0f,
        weightPerSecond = 0f,
        volume = 0f,
        volumePerSecond = 0f
    )
)
