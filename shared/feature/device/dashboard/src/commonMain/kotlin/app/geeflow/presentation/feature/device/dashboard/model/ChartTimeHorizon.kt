package app.geeflow.presentation.feature.device.dashboard.model

internal fun liveChartTimeHorizon(latestMeasurementSeconds: Double): Double {
    if (!latestMeasurementSeconds.isFinite()) return InitialLiveChartHorizonSeconds
    return maxOf(InitialLiveChartHorizonSeconds, latestMeasurementSeconds.coerceAtLeast(0.0) / LiveChartDataFraction)
}

internal fun hasElapsedChartMeasurements(seconds: Iterable<Float>): Boolean =
    seconds.any { it.isFinite() && it > 0f }

private const val InitialLiveChartHorizonSeconds = 5.0
private const val LiveChartDataFraction = 0.93
