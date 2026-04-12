package dev.drobek.geeflow.presentation.feature.device.dashboard.model

data class ChartData(
    val pressure: Float,
    val weight: Float,
    val weightPerSecond: Float,
    val volume: Float,
    val volumePerSecond: Float,
)
