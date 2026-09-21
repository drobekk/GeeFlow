package app.geeflow.presentation.feature.device.dashboard.model

internal data class BrewChartModel(
    val measurements: Map<Float, ChartData> = emptyMap(),
    val targets: Map<Float, ChartData> = emptyMap(),
    val boundaries: List<ChartPhaseBoundary> = emptyList(),
)
