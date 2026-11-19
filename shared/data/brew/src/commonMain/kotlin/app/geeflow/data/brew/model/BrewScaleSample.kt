package app.geeflow.data.brew.model

import kotlinx.serialization.Serializable

/** One raw BOOKOO notification, kept at its received cadence for brew history and charting. */
@Serializable
data class BrewScaleSample(
    /** Time from the start of the brew, in milliseconds. */
    val elapsedMillis: Long,
    /** Wall-clock receive time, in epoch milliseconds. */
    val receivedAtEpochMillis: Long,
    /** The timer reported by the scale itself. */
    val scaleElapsedMillis: Long? = null,
    val weightGrams: Float? = null,
    val reportedFlowGramsPerSecond: Float? = null,
    val derivedFlowGramsPerSecond: Float? = null,
    /** One-second trailing regression of scale weight, for a steadier derived-flow trace. */
    val smoothedFlowGramsPerSecond: Float? = null,
    val batteryPercent: Int? = null,
    val rawPacketHex: String,
    val decoded: Boolean,
)
