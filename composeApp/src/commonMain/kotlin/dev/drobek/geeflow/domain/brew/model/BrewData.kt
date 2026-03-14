package dev.drobek.geeflow.domain.brew.model

data class BrewDataPoint(
    val pressure: Float,
    val weight: Float,
    val volume: Float,
    val flowRate: Float,
    val weightRate: Float
)

data class BrewSession(
    val dataPoints: Map<Float, BrewDataPoint>
)
