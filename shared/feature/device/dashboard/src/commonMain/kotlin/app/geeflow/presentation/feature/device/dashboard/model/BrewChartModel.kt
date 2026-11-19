package app.geeflow.presentation.feature.device.dashboard.model

import app.geeflow.data.brew.model.BrewScaleSample

internal data class BrewChartModel(
    val measurements: Map<Float, ChartData> = emptyMap(),
    val scaleMeasurements: List<BrewScaleSample> = emptyList(),
    val targets: Map<Float, ChartData> = emptyMap(),
    val boundaries: List<ChartPhaseBoundary> = emptyList(),
    val liveTimeline: Boolean = false,
    val fitToMeasurements: Boolean = false,
    val shotEndSeconds: Double? = null,
)
