package app.geeflow.presentation.feature.device.dashboard.freecontrol

import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Brew
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.FlowRate
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.Pressure
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType.WeightRate

data class FreeControlViewState(
    val deviceName: String = "",
    val profileName: String = "",
    val mode: ControlMode = ControlMode.Pressure,
    val pressureTarget: Float = 6.0f,
    val flowTarget: Float = 6.0f,
    val pressureRange: ClosedFloatingPointRange<Float> = 0f..0f,
    val flowRange: ClosedFloatingPointRange<Float> = 0f..0f,
    val brew: Brew = Brew(),
    val brewStatus: FreeBrewStatus = FreeBrewStatus.Idle,
    val sessionCompleted: Boolean = false,
    val visibleCharts: Set<DashboardChartType> = setOf(Pressure, FlowRate, WeightRate),
    val showRenameDialog: Boolean = false,
)

enum class ControlMode { Pressure, Flow }

enum class FreeBrewStatus { Idle, Active }
