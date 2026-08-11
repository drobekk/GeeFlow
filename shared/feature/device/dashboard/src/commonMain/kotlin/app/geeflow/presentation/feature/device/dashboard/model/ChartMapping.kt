package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.user.model.ChartType
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType

internal fun ChartType.toDashboard(): DashboardChartType = when (this) {
    ChartType.PRESSURE -> DashboardChartType.Pressure
    ChartType.FLOW_RATE -> DashboardChartType.FlowRate
    ChartType.WEIGHT_RATE -> DashboardChartType.WeightRate
    ChartType.VOLUME -> DashboardChartType.Volume
    ChartType.WEIGHT -> DashboardChartType.Weight
}

internal fun DashboardChartType.toDomain(): ChartType = when (this) {
    DashboardChartType.Pressure -> ChartType.PRESSURE
    DashboardChartType.FlowRate -> ChartType.FLOW_RATE
    DashboardChartType.WeightRate -> ChartType.WEIGHT_RATE
    DashboardChartType.Volume -> ChartType.VOLUME
    DashboardChartType.Weight -> ChartType.WEIGHT
}

internal fun Set<ChartType>.toDashboard(): Set<DashboardChartType> = mapTo(mutableSetOf()) { it.toDashboard() }

internal fun BrewSession.toChartData(): Map<Float, ChartData> = dataPoints.mapValues { (_, point) ->
    ChartData(
        pressure = point.pressure,
        weight = point.weight,
        weightPerSecond = point.weightRate,
        volume = point.volume,
        volumePerSecond = point.flowRate,
    )
}
