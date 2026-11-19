package app.geeflow.presentation.feature.device.dashboard.main

import app.geeflow.data.brew.model.BrewProgram
import app.geeflow.data.brew.model.PhaseTransition
import app.geeflow.data.brew.model.BrewScaleSample
import app.geeflow.data.device.model.DeviceError
import app.geeflow.data.user.model.FlowChartRange
import app.geeflow.presentation.feature.device.dashboard.components.BrewButtonState
import app.geeflow.presentation.feature.device.dashboard.model.ChartData
import kotlin.time.Instant

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
    val flowChartRange: FlowChartRange = FlowChartRange.FOUR,
    val preferredShotDoseGrams: Float = 18f,
    val grindDraft: String = "",
    val hotWaterSupported: Boolean = false,
    val manualCyclesExcludedFromCharts: Boolean = true,
    val flushDurationSeconds: Int = 0,
    val flushPressure: String = "",
    val flushElapsedSeconds: Int = 0,
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
        val phaseProgram: BrewProgram? = null,
        val phaseTransitions: List<PhaseTransition> = emptyList(),
        val recordedPhaseIds: Set<String> = emptySet(),
        val historyTarget: Map<Float, ChartData>? = null,
        /** Weight stop target used by this shot, retained for its stopped/history readout. */
        val historyStopWeightGrams: Float? = null,
        /** Final reading from the selected scale source, retained after an app-monitored cutoff. */
        val historyFinalWeightGrams: Float? = null,
        val scaleSamples: List<BrewScaleSample> = emptyList(),
        val startedAt: Instant? = null,
        val endedAt: Instant? = null,
        val historyId: Long? = null,
        val doseGrams: Float? = null,
        val recipeRatio: Float? = null,
        val recipeTargetGrams: Float? = null,
        val grindSetting: String? = null,
        val extractionStartMillis: Long? = null,
        val extractionStartEstimated: Boolean? = null,
        val finalWeightGrams: Float? = null,
        val isLiveRecipe: Boolean = false,
        val extractionPhaseId: String? = null,
    )

    data class Device(
        val id: Long = 0L,
        val name: String = "",
        val isDemo: Boolean = false,
        val brewBoilerTemp: String? = null,
        val steamBoilerTemp: String? = null,
        val pressure: String? = null,
        val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
        val brewStatus: BrewStatus = BrewStatus.Idle,
        val dispensingHotWater: Boolean = false,
        val smartScaleConnected: Boolean = false,
        val alarm: Boolean = false,
        val error: DeviceError? = null,
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
        data object Errors : Dialog
        object BluetoothPermissionMissing : Dialog

        /** Full text of the brewed profile's description, which the brew bar can only show truncated. */
        data class BrewDescription(val name: String, val description: String) : Dialog
    }
}

internal val DeviceDashboardViewState.Brew.shotDurationSeconds: Double?
    get() = if (startedAt != null && endedAt != null) {
        (endedAt - startedAt).inWholeMilliseconds / 1000.0
    } else time.takeIf { it > 0 }?.toDouble()

internal fun getEmptyChartData() = mapOf(
    0f to ChartData(
        pressure = 0.0f,
        weight = 0f,
        weightPerSecond = 0f,
        volume = 0f,
        volumePerSecond = 0f,
    ),
)
