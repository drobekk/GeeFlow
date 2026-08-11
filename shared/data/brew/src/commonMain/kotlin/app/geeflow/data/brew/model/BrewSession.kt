package app.geeflow.data.brew.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

data class BrewSession(
    val userId: Long,
    val mode: BrewMode = BrewMode.Manual,
    val startTime: Instant? = null,
    val inProgress: Boolean = false,
    val elapsedSeconds: Int = 0,
    val dataPoints: Map<Float, BrewDataPoint> = emptyMap(),
)

@Serializable
data class BrewDataPoint(
    val pressure: Float,
    val weight: Float,
    val volume: Float,
    val flowRate: Float,
    val weightRate: Float,
)
