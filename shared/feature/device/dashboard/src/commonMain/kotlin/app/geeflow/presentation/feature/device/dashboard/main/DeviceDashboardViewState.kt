package app.geeflow.presentation.feature.device.dashboard.main

import app.geeflow.presentation.feature.device.dashboard.components.BrewButtonState
import app.geeflow.presentation.feature.device.dashboard.model.ChartData

data class DeviceDashboardViewState(
    val user: User = User(),
    val device: Device = Device(),
    val brew: Brew = Brew(),
    val dialog: Dialog? = null,
    val visibleCharts: Set<DashboardChartType> = setOf(
        DashboardChartType.Pressure,
        DashboardChartType.FlowRate,
        DashboardChartType.WeightRate,
    ),
    val brewButtonState: BrewButtonState = BrewButtonState.Idle,
) {
    enum class DashboardChartType {
        Pressure,
        FlowRate,
        WeightRate,
        Volume,
        Weight,
    }

    data class User(
        val id: String = "",
        val name: String = "",
        val photoFileName: String? = null,
    )

    data class Brew(
        val name: String = "",
        val description: String = "",
        val time: Int = 0,
        val data: Map<Float, ChartData> = getEmptyChartData(),
        /**
         * Target curve of a recorded brew being replayed. Non-null only while a history entry is
         * shown, so the charts can overlay that brew's own profile instead of the selected one —
         * an empty map means the recorded brew had no profile.
         */
        val historyTarget: Map<Float, ChartData>? = null,
    )

    data class Device(
        val id: Long = 0L,
        val name: String = "",
        val brewBoilerTemp: String? = null,
        val steamBoilerTemp: String? = null,
        val pressure: String? = null,
        val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
        val brewStatus: BrewStatus = BrewStatus.Idle,
        val smartScaleConnected: Boolean = false,
        val alarm: Boolean = false,
    ) {
        val isBrewing = brewStatus != BrewStatus.Idle

        enum class ConnectionStatus {
            Disconnected,
            Connecting,
            Synchronizing,
            Connected,
        }

        enum class BrewStatus {
            Manual,
            Profile,
            Idle,
        }
    }

    sealed interface Dialog {
        object BluetoothPermissionMissing : Dialog

        /** Full text of the brewed profile's description, which the brew bar can only show truncated. */
        data class BrewDescription(val name: String, val description: String) : Dialog
    }
}

internal fun getEmptyChartData() = mapOf(
    0f to ChartData(
        pressure = 0.0f,
        weight = 0f,
        weightPerSecond = 0f,
        volume = 0f,
        volumePerSecond = 0f,
    ),
)
