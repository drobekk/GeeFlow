package dev.drobek.geeflow.domain.brew.model

import kotlin.time.Instant

data class BrewSession(
    val userId: Long,
    val profileId: Long? = null,
    val profileName: String? = null,
    val startTime: Instant? = null,
    val inProgress: Boolean = false,
    val dataPoints: Map<Float, BrewDataPoint> = emptyMap()
)

data class BrewDataPoint(
    val pressure: Float,
    val weight: Float,
    val volume: Float,
    val flowRate: Float,
    val weightRate: Float
)
