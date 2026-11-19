package app.geeflow.presentation.feature.device.dashboard.model

/** A zero-filled legacy field cannot establish that a native cup-flow sensor was present. */
internal fun hasRecordedNativeCupFlow(points: List<ChartData>): Boolean =
    points.any { it.weightPerSecond.isFinite() && it.weightPerSecond != 0f }
