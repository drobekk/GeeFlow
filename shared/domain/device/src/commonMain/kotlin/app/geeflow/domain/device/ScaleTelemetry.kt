package app.geeflow.domain.device

import app.geeflow.data.brew.model.BrewScaleSample
import app.geeflow.data.device.BookooScalePacketEvent
import kotlin.time.Instant

/** Converts one received BOOKOO notification into the durable brew-history representation. */
fun BookooScalePacketEvent.toBrewScaleSample(
    brewStartedAt: Instant,
    previous: BrewScaleSample?,
    smoothedFlowGramsPerSecond: Float? = null,
): BrewScaleSample {
    val frame = frame
    val previousSample = previous
    val previousWeight = previousSample?.weightGrams
    val elapsed = (receivedAt - brewStartedAt).inWholeMilliseconds.coerceAtLeast(0L)
    val derivedFlow = if (decoded && frame != null && previousSample?.decoded == true && previousWeight != null) {
        val interval = elapsed - previousSample.elapsedMillis
        if (interval in MIN_FLOW_INTERVAL_MILLIS..MAX_FLOW_INTERVAL_MILLIS) {
            (frame.weightGrams - previousWeight) * MILLIS_PER_SECOND / interval
        } else null
    } else null
    return BrewScaleSample(
        elapsedMillis = elapsed,
        receivedAtEpochMillis = receivedAt.toEpochMilliseconds(),
        scaleElapsedMillis = frame?.elapsedMillis,
        weightGrams = frame?.weightGrams,
        reportedFlowGramsPerSecond = frame?.reportedFlowGramsPerSecond,
        derivedFlowGramsPerSecond = derivedFlow,
        smoothedFlowGramsPerSecond = smoothedFlowGramsPerSecond,
        batteryPercent = frame?.batteryPercent,
        rawPacketHex = packetHex,
        decoded = decoded,
    )
}

private const val MIN_FLOW_INTERVAL_MILLIS = 25L
private const val MAX_FLOW_INTERVAL_MILLIS = 2_000L
private const val MILLIS_PER_SECOND = 1_000f
