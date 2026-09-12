package app.geeflow.data.brew.model

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable
enum class FreeHandControlMode { Pressure, Flow }

/** Uninterpolated measurements. Time is relative to the first measurement of the active brew. */
@Serializable
data class FreeHandSample(val elapsedMillis: Long, val data: BrewDataPoint)

@Serializable
data class FreeHandRecording(
    val controlMode: FreeHandControlMode,
    val samples: List<FreeHandSample>,
) {
    /** Resampling is optional; stored measurements retain their original times and values. */
    fun playbackPoints(): List<BrewDataPoint> {
        require(samples.isNotEmpty()) { "The free hand recording has no measurements" }
        require(
            samples.first().elapsedMillis == 0L && samples.zipWithNext().all { (a, b) ->
                a.elapsedMillis < b.elapsedMillis
            }
        ) { "Recording timestamps must start at zero and increase" }
        val count = (samples.last().elapsedMillis.toDouble() / SAMPLE_INTERVAL_MS).roundToInt() + 1
        return List(count) { index ->
            val time = index * SAMPLE_INTERVAL_MS
            val point = if (index == count - 1) {
                samples.last().data
            } else {
                samples.minBy {
                    abs(it.elapsedMillis - time)
                }.data
            }
            require(
                listOf(point.pressure, point.flowRate, point.volume, point.weight, point.weightRate).all {
                    it.isFinite() && it >= 0f
                }
            ) { "Recording contains an invalid measurement" }
            point
        }
    }

    companion object {
        const val SAMPLE_INTERVAL_MS = 500L
    }
}

class RecordingCapacityExceededException : IllegalArgumentException("The recording exceeds the playback table capacity")
